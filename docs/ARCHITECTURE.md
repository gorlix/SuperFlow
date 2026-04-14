# SuperFlow Architecture Reference

> **Audience:** Core maintainers and senior contributors
> **Version:** 0.1.0 — _Core Architecture Milestone_

This document is the definitive technical reference for the SuperFlow engine internals. It covers the full data-flow pipeline, the math underlying spatial detection, the UI state-machine design, and the quality-enforcement infrastructure that protects the codebase from regressions.

---

## Table of Contents

- [System Overview](#system-overview)
- [The 4-Step Workflow Pipeline](#the-4-step-workflow-pipeline)
  - [Step 1 — TemplateParser: Union-Find Clustering](#step-1--templateparser-union-find-clustering)
  - [Step 2 — ConfigManager: Draft State & Compilation](#step-2--configmanager-draft-state--compilation)
  - [Step 3 — SpatialMappingEngine: AABB Dispatch](#step-3--spatialmappingengine-aabb-dispatch)
  - [Step 4 — AddonManager: Sandboxed Execution](#step-4--addonmanager-sandboxed-execution)
- [The PluginAPI Facade Layer](#the-pluginapi-facade-layer)
- [The Data-Driven UI System](#the-data-driven-ui-system)
  - [Obsidian-Style State-Switching Router](#obsidian-style-state-switching-router)
  - [DynamicAddonForm: Schema Rendering](#dynamicaddonform-schema-rendering)
- [The "Iron Curtain": Quality Enforcement Infrastructure](#the-iron-curtain-quality-enforcement-infrastructure)
  - [SDK Mock Strategy](#sdk-mock-strategy)
  - [Test Suite Reference](#test-suite-reference)
  - [Husky + lint-staged Pre-commit Pipeline](#husky--lint-staged-pre-commit-pipeline)
- [Strict Separation of Concerns (SoC)](#strict-separation-of-concerns-soc)
- [Compiled JSON Config Schema](#compiled-json-config-schema)
- [File & Directory Reference](#file--directory-reference)

---

## System Overview

SuperFlow is a React Native plugin running on the Chauvet OS (Supernote's Android-based e-ink operating system). Its architecture is divided into two chronologically separate modes of operation:

- **The Learn Phase** — User-triggered, one-time per template. Scans a template note, extracts geometry, allows the user to bind Add-on actions to zones, and compiles a lightweight JSON config to persistent storage.
- **The Process Phase** — User-triggered, recurring. Loads the pre-compiled JSON, reads live ink from the current page, detects spatial matches, and dispatches Add-on actions.

**The 0ms Latency Guarantee:** SuperFlow never runs background processes, never polls pen strokes, and never hooks into the ink rendering pipeline. All computation is explicitly triggered by the user. This is the foundational constraint that all architectural decisions serve.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        LEARN PHASE (One-time Setup)                     │
│                                                                         │
│  [User draws boxes on template] ──► TemplateParser.extractHotzones()   │
│           Raw SDK Strokes               Union-Find Clustering           │
│                                                ▼                        │
│                                   ConfigManager.initializeDraft()       │
│                                         In-memory draft state           │
│                                                ▼                        │
│                              [User assigns Add-ons in Settings UI]      │
│                                   ConfigManager.mapActionToZone()       │
│                                                ▼                        │
│                                   ConfigManager.compileAndSave()        │
│                               Prune empty zones → write JSON to RNFS   │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                       PROCESS PHASE (Recurring Use)                     │
│                                                                         │
│  [User taps "Process Active Page"] ──► SpatialMappingEngine             │
│                                          .processActivePage()           │
│           ┌──────────────────────────────────────────────────┐          │
│           │ 1. PluginAPI.getActiveContext()  → file + page   │          │
│           │ 2. PluginAPI.loadJSONConfig()    → compiled map  │          │
│           │ 3. PluginAPI.getRawStrokes()     → live ink      │          │
│           │ 4. AABB Collision Detection      → matched zones │          │
│           │ 5. AddonManager.executeAction()  → DI dispatch   │          │
│           └──────────────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## The 4-Step Workflow Pipeline

### Step 1 — TemplateParser: Union-Find Clustering

**File:** `src/core/TemplateParser.js`
**Role:** Pure mathematics. Stateless. Zero SDK dependency. Transforms an array of raw Chauvet OS stroke objects into a list of clean, padded Hotzone rectangles.

#### Input Normalization

The Chauvet SDK has inconsistent property casing across firmware versions—coordinates may be delivered as `{x, y, width, height}` (lowercase), `{X, Y, Width, Height}` (uppercase), or nested under a `.rect` key. `normalizeStroke()` abstracts this inconsistency:

```javascript
static normalizeStroke(rawStroke) {
  const rectSource = rawStroke.rect || rawStroke || {};
  return {
    x: typeof rectSource.x === 'number' ? rectSource.x : rectSource.X || 0,
    y: typeof rectSource.y === 'number' ? rectSource.y : rectSource.Y || 0,
    width:  typeof rectSource.width  === 'number' ? rectSource.width  : rectSource.Width  || 0,
    height: typeof rectSource.height === 'number' ? rectSource.height : rectSource.Height || 0,
  };
}
```

All downstream functions operate exclusively on this normalized shape.

#### Union-Find Clustering Algorithm

The core challenge: a user drawing a rectangle with a physical pen produces **four independent strokes** (top edge, right edge, bottom edge, left edge), not one. The engine must cluster proximate strokes that belong to the same logical drawing into a single Hotzone.

SuperFlow implements a **Disjoint-Set Union (Union-Find)** data structure within `extractHotzones()`. This is a classical graph theory approach with **path-compressed `find()`** for near–O(1) amortized performance.

**The Algorithm:**

1. **Initialize:** Each stroke `i` starts as its own component (`parent[i] = i`).
2. **O(N²) Intersection Pass:** For every pair of strokes `(i, j)`, call `doRectsIntersect(rect_i, rect_j, PROXIMITY_THRESHOLD)`. If they overlap or are within **30px** of each other, call `union(i, j)` to merge their components.
3. **Cluster Grouping:** Walk the parent array, calling `find(i)` (with path compression) to group all strokes by their root component ID. Each unique root is one logical drawing.
4. **Bounding Box Consolidation:** For each cluster, compute the axis-aligned bounding box by taking `min(x)`, `min(y)`, `max(x + width)`, `max(y + height)` over all strokes in the group.
5. **Noise Filtering:** Discard any cluster whose bounding box is smaller than **80×80 pixels** (`MIN_ZONE_WIDTH` / `MIN_ZONE_HEIGHT`). This eliminates accidental marks or pen tap artifacts.
6. **Padding & Clamping:** Expand the final bounding box by **20px** on all sides (`ZONE_PADDING`). The top-left corner is clamped to `Math.max(0, minX - ZONE_PADDING)` to prevent negative coordinates, which would cause out-of-bounds crashes in the Chauvet OS renderer.

**Constants:**

| Constant              | Value  | Purpose                                               |
| --------------------- | ------ | ----------------------------------------------------- |
| `PROXIMITY_THRESHOLD` | `30`px | Maximum gap between strokes to be considered touching |
| `MIN_ZONE_WIDTH`      | `80`px | Minimum width to pass the noise filter                |
| `MIN_ZONE_HEIGHT`     | `80`px | Minimum height to pass the noise filter               |
| `ZONE_PADDING`        | `20`px | Expansion applied to the final bounding box           |

**`doRectsIntersect()` — Proximity-Aware AABB:**

This is a modified AABB check where `rect1` is expanded by `threshold` before testing against `rect2`. This is what enables pen-lift gap tolerance:

```javascript
static doRectsIntersect(rect1, rect2, threshold) {
  const r1Left   = rect1.x - threshold;
  const r1Right  = rect1.x + rect1.width + threshold;
  const r1Top    = rect1.y - threshold;
  const r1Bottom = rect1.y + rect1.height + threshold;
  // Standard AABB overlap test against the expanded rect1:
  return !(
    rect2.x > r1Right || rect2.x + rect2.width  < r1Left ||
    rect2.y > r1Bottom || rect2.y + rect2.height < r1Top
  );
}
```

**Output:** An array of `Hotzone` objects:

```typescript
{
  id: string; // e.g. "zone_1713100000000_0" (timestamp + counter)
  type: 'rect';
  x: number; // Padded, clamped top-left X
  y: number; // Padded, clamped top-left Y
  width: number; // Padded width
  height: number; // Padded height
}
```

---

### Step 2 — ConfigManager: Draft State & Compilation

**File:** `src/core/ConfigManager.js`
**Role:** Singleton state manager for the Learn Phase. Holds the user's in-progress zone-to-action mapping in memory, validates inputs, and compiles/persists the final config.

#### Singleton Pattern

`ConfigManager` is instantiated once at module load and exported as a default singleton (`export default new ConfigManager()`). All UI components interact with this single instance. This is intentional: the draft state must persist across React component re-renders and state-switches without being tied to React's component lifecycle.

#### State Lifecycle

```
null (initial)
  │
  ▼ initializeDraft(templateName, parsedZones)
  │
DraftState { templateName, hotzones: [...zones with empty actions[]] }
  │
  ├─ mapActionToZone(zoneId, addonId, params)    → push ActionConfig into zone.actions
  ├─ removeActionFromZone(zoneId, addonId)        → filter out ActionConfig from zone.actions
  │
  ▼ compileAndSave()
  │
  ├─ [Prune Step] Filter out zones where actions.length === 0
  ├─ [Persist Step] PluginAPI.saveJSONConfig(templateName, optimizedTree)
  └─ clearDraft() → back to null
```

#### Empty Zone Pruning

Before serializing to JSON, `compileAndSave()` filters the `hotzones` array to include only zones with at least one mapped action:

```javascript
const activeHotzones = this._draftState.hotzones.filter(
  zone => zone.actions && zone.actions.length > 0,
);
```

**Why this matters:** Zones without actions are useless during the Process Phase. Including them would force `SpatialMappingEngine` to perform AABB tests against dead zones on every page process. Pruning them at compile time guarantees the engine only processes actionable geometry, keeping the Process Phase as fast as the device allows.

#### Security: Addon Registry Validation

`mapActionToZone()` does not blindly accept any `addonId` string. It cross-references against `AddonManager.getAvailableAddons()` before writing. This prevents malformed manual JSON edits (or bugs in the UI layer) from mapping a non-existent or unregistered Add-on ID into the config:

```javascript
const doesAddonExist = validAddons.some(addon => addon.id === addonId);
if (!doesAddonExist) {
  throw new Error(
    `Security Halt: Attempted to map unregistered plugin [${addonId}].`,
  );
}
```

---

### Step 3 — SpatialMappingEngine: AABB Dispatch

**File:** `src/core/SpatialMappingEngine.js`
**Role:** The operational brain of the Process Phase. Orchestrates context resolution, config loading, stroke fetching, collision detection, and action dispatch. Exported as a singleton.

#### `processActivePage()` Execution Sequence

```
1. getActiveContext()      → { path, pageNum }
2. _extractTemplateName()  → strip extension from path basename
3. loadJSONConfig()        → compiled Hotzone map or null
4. getRawStrokes()         → raw ink elements from current page
5. normalizeStroke() map   → standardized { x, y, width, height } array
6. For each zone in config:
     For each normalized stroke:
       _doAABBIntersect(stroke, zone) → boolean
     If any intersection found:
       Build executionContext snapshot
       For each action in zone.actions:
         AddonManager.executeAction(action.id, action.params, executionContext)
```

The engine has **three clean-exit paths** that produce no error and no side effect:

- No compiled config exists for the active file → `return` (clean, expected)
- The current page has zero ink strokes → `return` (clean, expected)
- A specific zone has no matching strokes → `continue` to next zone

All fatal errors are caught in the top-level `try/catch` and logged. The engine never re-throws, ensuring a single malformed Add-on cannot block other zones from processing.

#### `_doAABBIntersect()` — Live Ink Collision Math

The AABB test used in the Process Phase is a standard partial-overlap detector. It returns `true` if any part of the stroke's bounding box overlaps with the zone's bounding box:

```
stroke overlaps zone iff:
  stroke.x          ≤ zone.x + zone.width   AND
  stroke.x + stroke.width  ≥ zone.x          AND
  stroke.y          ≤ zone.y + zone.height  AND
  stroke.y + stroke.height ≥ zone.y
```

**Why partial overlap, not full containment?** Natural, fast handwriting often produces strokes whose bounding box slightly exceeds the intended zone boundary. Requiring full containment would produce false negatives. Partial overlap is the correct threshold for organic pen input.

---

### Step 4 — AddonManager: Sandboxed Execution

**File:** `src/addons/AddonManager.js`
**Role:** In-memory registry and execution gatekeeper. Responsible for building the `safeToolkit` DI container that enforces the security boundary between Add-ons and the raw SDK.

#### The `safeToolkit` DI Container

This is the architectural mechanism that enforces the "Iron Curtain" between Add-ons and the Chauvet SDK. Before calling `addon.execute()`, `AddonManager` assembles a plain object whose methods are **explicit, whitelisted proxies** to specific `PluginAPI` methods:

```javascript
const safeToolkit = {
  injectKeyword: async (path, page, keyword) =>
    PluginAPI.injectKeyword(path, page, keyword),
  // Future capabilities are added here by the core team only,
  // after explicit security review.
};
```

An Add-on that calls `toolkit.injectKeyword()` is calling a closure that calls `PluginAPI.injectKeyword()`. The Add-on **never holds a reference** to `PluginAPI` itself. If an Add-on author tries to call `toolkit.PluginAPI` or `toolkit.dangerousMethod`, they will get `undefined`—the container simply doesn't expose it.

#### Registration & Duck-Typing Validation

`registerAddon()` performs a duck-typing check before accepting an instance:

```javascript
if (
  !addonInstance.constructor.id ||
  typeof addonInstance.execute !== 'function'
) {
  throw new Error('Invalid Addon instance provided to AddonManager');
}
```

This ensures that any object attempting to be registered as an Add-on actually quacks like one, regardless of its prototype chain.

---

## The PluginAPI Facade Layer

**File:** `src/core/PluginAPI.js`

`PluginAPI` is the **only file in the codebase** authorized to import `sn-plugin-lib` (the Chauvet SDK) and `react-native-fs`. It acts as an anti-corruption layer that:

1. Normalizes SDK response shapes (the `{ success, result }` envelope pattern).
2. Wraps filesystem operations (directory creation, file read/write) in clean async methods.
3. Generates the canonical config file path: `/storage/emulated/0/MyStyle/template/configs/<name>.json`

**Why this matters for testing:** Because `PluginAPI` is the sole SDK import point, it is the only module that needs to be mocked in the test suite. All other modules (`SpatialMappingEngine`, `ConfigManager`) are tested against the mock without any behavioral changes to their logic.

---

## The Data-Driven UI System

### Obsidian-Style State-Switching Router

**File:** `src/page/SuperFlowPage.js`

SuperFlow's navigation system is deliberately **not** a routing library (no React Navigation, no Expo Router). It is a pure in-component State-Switch pattern—identical in philosophy to how Obsidian's plugin modals work.

The root `SuperFlowPage` component holds a **single `currentView` state string**:

```javascript
const [currentView, setCurrentView] = useState('dashboard');
// Values: 'dashboard' | 'settings'
```

Rendering is a simple conditional:

```javascript
if (currentView === 'settings') {
  return <SuperFlowSettings onExit={closeSettings} />;
}
return <DashboardView />;
```

**Why State-Switching instead of a routing library?**

| Concern             | Routing Library                                                 | State-Switch                             |
| ------------------- | --------------------------------------------------------------- | ---------------------------------------- |
| Navigation payload  | `navigation.navigate('Settings', { data })` — async, serialized | Direct prop drilling — zero overhead     |
| Component lifecycle | Mount/unmount on every navigation                               | Conditional render within existing tree  |
| E-ink suitability   | Potentially triggers full re-renders, ghosting                  | Minimal DOM diff, no animation artifacts |
| Bundle size         | Adds significant dependency weight                              | Zero additional dependencies             |
| Render latency      | Library routing overhead (~1–10ms)                              | 0ms — pure JavaScript conditional        |

This pattern was chosen explicitly to satisfy the **0ms latency constraint** on an e-ink display. Navigation latency beyond ~16ms is perceptible as ghosting on the Chauvet OS screen.

The same State-Switch pattern is applied **within** `SuperFlowSettings.js`, which manages three sub-views:

```javascript
// 'zone_list' → 'addon_picker' → 'dynamic_form' → 'zone_list'
const [viewState, setViewState] = useState('zone_list');
```

### DynamicAddonForm: Schema Rendering

**File:** `src/components/SuperFlowSettings.js` — `DynamicAddonForm` component

The `DynamicAddonForm` component renders a configuration UI for any Add-on entirely based on that Add-on's `static settingsSchema` array. **No UI code is written per Add-on.**

```javascript
{
  addon.constructor.settingsSchema.map(field => {
    if (field.type === 'string') {
      return (
        <View key={field.key}>
          <Text>{field.label}</Text>
          <TextInput
            placeholder={field.placeholder || ''}
            value={formData[field.key] || ''}
            onChangeText={text => setFormData({...formData, [field.key]: text})}
          />
        </View>
      );
    }
    return null; // Unrecognized types are silently skipped; add new renderers here
  });
}
```

When the user taps "Confirm & Apply", `formData` (the local state object keyed by `field.key`) is passed to `ConfigManager.mapActionToZone()` as the `addonParams` argument and eventually serialized into the compiled JSON config.

---

## The "Iron Curtain": Quality Enforcement Infrastructure

The "Iron Curtain" is the informal name for SuperFlow's layered quality enforcement system. Its defining characteristic: **a failing test physically blocks the commit.** It is not advisory; it is mandatory.

### SDK Mock Strategy

The Chauvet SDK (`sn-plugin-lib`) and the filesystem library (`react-native-fs`) are **device-only** native modules. They do not exist in a standard Node.js test environment. If tests imported them directly, every test suite would crash on initialization.

SuperFlow solves this with a **global mock declaration** in `jest.setup.js`:

```javascript
// jest.setup.js — runs before every test file
jest.mock('sn-plugin-lib', () => ({
  PluginCommAPI: {},
  PluginFileAPI: {},
}));

jest.mock('react-native-fs', () => ({
  writeFile: jest.fn(),
  readFile: jest.fn(),
}));
```

This file is loaded via `setupFiles` in `jest.config.js`. Every test in the project automatically gets these mocks—no per-file `jest.mock()` boilerplate needed for the global mocks.

**Why this is safe:** Because `PluginAPI` is the only SDK import point (see the Facade Layer section), mocking `sn-plugin-lib` at the global level means that any test against any module that flows through `PluginAPI` can control every SDK call with `PluginAPI.method.mockResolvedValue(...)`. The actual business logic of `SpatialMappingEngine`, `ConfigManager`, and all Add-ons is tested **purely in isolation from the hardware.**

### Test Suite Reference

Three dedicated test files validate the core modules. Each test file also mock-isolates its dependencies.

#### `TemplateParser.test.js`

Tests the pure mathematical functions. No mocks required—`TemplateParser` has zero external dependencies.

| Test                                                                      | Validates                                                                                                                          |
| ------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `normalizes geometry inputs`                                              | `normalizeStroke()` handles both uppercase SDK format (`X`, `Y`) and nested `rect` format                                          |
| `clusters intersecting strokes via Union-Find and applies proper padding` | 4 strokes forming a box cluster into exactly 1 zone; padding expands to correct absolute pixels; `Math.max(0, ...)` clamping works |
| `drops artifacts smaller than minimum valid dimensions`                   | A stroke smaller than 80×80px is correctly filtered as noise                                                                       |

#### `SpatialMappingEngine.test.js`

Tests the orchestration logic. Mocks `PluginAPI` and `AddonManager` entirely.

| Test                                                       | Validates                                                                                                                                                                  |
| ---------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `safely extracts template name from absolute filepath`     | `_extractTemplateName('/storage/.../Meeting.note')` returns `'Meeting'`                                                                                                    |
| `AABB math correctly detects stroke inclusions`            | A stroke inside the zone returns `true`; a stroke outside returns `false`                                                                                                  |
| `processes mapped hotzones correctly through AddonManager` | Full `processActivePage()` integration: given a mocked config with one zone and one action, `AddonManager.executeAction` is called exactly once with the correct arguments |

#### `ConfigManager.test.js`

Tests the state machine and compile logic. Mocks `PluginAPI` and `AddonManager`.

| Test                                       | Validates                                                                                                                                    |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------- |
| `initializes draft cleanly`                | `initializeDraft()` creates the correct `DraftState` structure with empty `actions[]`                                                        |
| `maps valid addons safely`                 | `mapActionToZone()` pushes an `ActionConfig` correctly when the addon exists in the registry                                                 |
| `throws error when mapping invalid addon`  | `mapActionToZone()` throws `/Security Halt/` when the addon ID is not registered                                                             |
| `prunes empty zones inside compileAndSave` | Only zones with actions are written to storage; `PluginAPI.saveJSONConfig` receives only the active zone; `_draftState` is `null` after save |

### Husky + lint-staged Pre-commit Pipeline

**Files:** `.husky/pre-commit`, `.lintstagedrc`

Every `git commit` triggers the following mandatory sequence. If **any step exits non-zero, the commit is blocked**:

```
git commit
  │
  ├─ [Husky pre-commit hook]
  │      ├─ npm test             → Runs Jest. ALL tests must pass.
  │      └─ npx lint-staged      → Runs staged-file checks:
  │              ├─ *.{js,jsx,ts,tsx} → eslint --fix → prettier --write
  │              └─ *.{json,md}       → prettier --write
  │
  └─ [If all pass] → commit succeeds
     [If any fail] → commit aborted, developer must fix and retry
```

**What this guarantees:**

- A broken test can never enter `main`. Period.
- ESLint violations (including the `eslint-plugin-jsdoc` rules requiring JSDoc on all methods) are auto-fixed where possible, or block the commit otherwise.
- Code formatting is automatically normalized by Prettier on every commit, eliminating style-diff noise in PRs.
- The `--no-verify` flag is explicitly **prohibited** by the project's AI development guidelines.

---

## Strict Separation of Concerns (SoC)

The architecture enforces three hard layers. **Crossing these boundaries is an architectural violation** regardless of apparent convenience.

```
┌────────────────────────────────────────────────────────────┐
│  UI Layer (SuperFlowPage, SuperFlowSettings, DynamicForm)  │
│  ✅ Reads from ConfigManager._draftState                   │
│  ✅ Calls ConfigManager.mapActionToZone()                  │
│  ✅ Calls SpatialMappingEngine.processActivePage()         │
│  ❌ NEVER imports sn-plugin-lib                            │
│  ❌ NEVER calls PluginAPI directly                         │
└───────────────────────────────┬────────────────────────────┘
                                │
┌───────────────────────────────▼────────────────────────────┐
│  Engine Layer (TemplateParser, SpatialMappingEngine,        │
│               ConfigManager, AddonManager)                  │
│  ✅ Calls PluginAPI methods                                 │
│  ✅ Pure logic — independent of UI state                   │
│  ❌ NEVER imports React or React Native components         │
└───────────────────────────────┬────────────────────────────┘
                                │
┌───────────────────────────────▼────────────────────────────┐
│  SDK Facade Layer (PluginAPI)                               │
│  ✅ ONLY file that imports sn-plugin-lib                   │
│  ✅ ONLY file that imports react-native-fs                 │
│  ❌ NEVER called from UI Layer directly                     │
└────────────────────────────────────────────────────────────┘
```

---

## Compiled JSON Config Schema

The file produced by `ConfigManager.compileAndSave()`, stored at:
`/storage/emulated/0/MyStyle/template/configs/<TemplateName>.json`

```json
{
  "hotzones": [
    {
      "id": "zone_1713100000000_0",
      "type": "rect",
      "x": 0,
      "y": 45,
      "width": 320,
      "height": 130,
      "actions": [
        {
          "id": "core.inject_keyword",
          "params": {
            "text": "meeting-notes"
          }
        }
      ]
    }
  ]
}
```

**Design decisions:**

- **No empty-action zones** — Pruned at compile time. The engine never iterates dead zones.
- **Multiple actions per zone** — The `actions` array supports any number of Add-on entries. They are executed **sequentially** within `SpatialMappingEngine.processActivePage()`.
- **Human-readable indentation** — `JSON.stringify(data, null, 2)` — aids manual inspection and debugging directly on the device.
- **Self-contained params** — `params` is the verbatim `addonParams` object from ConfigManager's state. The engine does not transform it before passing it to Add-ons.

---

## File & Directory Reference

```
src/
├── core/
│   ├── TemplateParser.js           # Union-Find clustering, AABB proximity, padding logic
│   ├── TemplateParser.test.js      # Pure math unit tests
│   ├── SpatialMappingEngine.js     # Process Phase orchestrator, AABB live dispatch
│   ├── SpatialMappingEngine.test.js # Integration tests with mocked PluginAPI
│   ├── ConfigManager.js            # Learn Phase state machine, compile & prune
│   ├── ConfigManager.test.js       # State + compile tests with mocked PluginAPI
│   └── PluginAPI.js                # Sole SDK import point (sn-plugin-lib, RNFS)
│
├── addons/
│   ├── BaseAddon.js                # Abstract contract class + ExecutePayload typedef
│   ├── AddonManager.js             # Registry, safeToolkit DI builder, execution gatekeeper
│   └── core/
│       └── InjectKeywordAddon.js   # V1 reference implementation
│
├── components/
│   └── SuperFlowSettings.js        # DynamicAddonForm, AddonPicker, zone list UI
│
└── page/
    └── SuperFlowPage.js            # Root Obsidian-style State-Switch router

.husky/
└── pre-commit                      # npm test + lint-staged (Iron Curtain gate)

.lintstagedrc                       # eslint --fix + prettier --write on staged files
jest.config.js                      # Jest preset config with transformIgnorePatterns
jest.setup.js                       # Global SDK mocks (sn-plugin-lib, react-native-fs)
```
