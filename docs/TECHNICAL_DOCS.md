# SuperFlow — Technical Documentation

> **Status:** `v0.1.0-alpha` — Functional hardware bridge. SDK discovery complete.
> **Platform:** Supernote (Chauvet OS / React Native `sn-plugin-lib`)
> **Author:** Gorlix

This document is the authoritative technical reference for SuperFlow's internal architecture. It records **verified, on-device behavior** discovered through hardware debugging, acting as the definitive "skip the trial-and-error" guide for all future developers or AI sessions working on this codebase.

---

## Table of Contents

- [System Architecture](#system-architecture)
- [Plugin Identity & Manifest](#plugin-identity--manifest)
- [Boot Sequence: `index.js`](#boot-sequence-indexjs)
- [SDK API Reference (Verified)](#sdk-api-reference-verified)
  - [Critical: Correct Stroke-Fetching API](#critical-correct-stroke-fetching-api)
  - [The Sync Strategy: `saveCurrentNote()` First](#the-sync-strategy-savecurrentnote-first)
  - [SDK Response Envelope](#sdk-response-envelope)
- [`PluginAPI.js` — Full Method Reference](#pluginapijs--full-method-reference)
- [SpatialMappingEngine](#spatialmappingengine)
- [The "Black Box" Logging System](#the-black-box-logging-system)
- [Build & Install Guide](#build--install-guide)
- [File System Paths Reference](#file-system-paths-reference)
- [Current Status & Known Constraints](#current-status--known-constraints)

---

## System Architecture

The following diagram shows the full data flow from hardware ink strokes to an executed Add-on action during the **Process Phase**.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            SUPERNOTE HARDWARE                               │
│                                                                             │
│   User writes on page  ──►  Ink strokes persisted to .note file on disk    │
└─────────────────────────────────────────────────────────────────────────────┘
                                        │
                 User taps "Process Active Page" in the SuperFlow UI
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  SuperFlowPage.js  (UI Layer)                                               │
│  handleProcessActions() → calls SpatialMappingEngine.processActivePage()   │
│  Appends result/error to Black Box log file                                 │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  SpatialMappingEngine.js  (Engine Layer)                                    │
│                                                                             │
│  1. PluginAPI.getActiveContext()   → { path, pageNum }                      │
│  2. PluginAPI.loadJSONConfig()     → compiled Hotzone map (RNFS)            │
│  3. PluginAPI.getRawStrokes()      → ink elements array                     │
│     │                                                                       │
│     └── TemplateParser.normalizeStroke()  → { x, y, width, height }        │
│                                                                             │
│  4. AABB collision loop over config.hotzones                                │
│     → _doAABBIntersect(stroke, zone) : boolean                              │
│                                                                             │
│  5. AddonManager.executeAction(id, params, context)                         │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  PluginAPI.js  (SDK Facade Layer — SOLE SDK IMPORT POINT)                   │
│                                                                             │
│  PluginNoteAPI.saveCurrentNote()       ← sync before read (CRITICAL)       │
│  PluginFileAPI.getElements(path, page) ← CORRECT stroke-fetching API       │
│  PluginCommAPI.getCurrentFilePath()                                         │
│  PluginCommAPI.getCurrentPageNum()                                          │
│  PluginFileAPI.insertKeyWord()                                              │
│  PluginNoteAPI.setTitle()                                                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  AddonManager.js                                                            │
│  Builds safeToolkit DI container → calls addon.execute(payload)             │
│                                                                             │
│  Addon (e.g. InjectKeywordAddon) receives:                                  │
│    { context, toolkit, addonParams }                                        │
│    Uses toolkit.injectKeyword() — NEVER imports sn-plugin-lib directly     │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Plugin Identity & Manifest

### `app.json`

The React Native application identity file, read at runtime by `AppRegistry.registerComponent()`.

```json
{
  "name": "plugin_superflow",
  "displayName": "SuperFlow"
}
```

### `PluginConfig.json`

The Supernote plugin manifest, read by the Chauvet OS plugin host to register and display the plugin.

```json
{
  "pluginID": "plugin_superflow",
  "pluginKey": "plugin_superflow",
  "author": "Gorlix",
  "iconPath": "./assets/icon/icon.png",
  "versionName": "0.1.0-alpha",
  "versionCode": "1",
  "jsMainPath": "index",
  "name": { "en": "SuperFlow", "it": "SuperFlow", ... },
  "desc": { "en": "The ultimate automation engine...", ... }
}
```

| Field         | Value                    | Notes                                                 |
| ------------- | ------------------------ | ----------------------------------------------------- |
| `pluginID`    | `plugin_superflow`       | Must match `app.json` `name`                          |
| `pluginKey`   | `plugin_superflow`       | Used by OS for persistent identification              |
| `jsMainPath`  | `index`                  | Points to the compiled JS bundle entry (no extension) |
| `versionName` | `0.1.0-alpha`            | Human-readable version; updated at each release       |
| `versionCode` | `1`                      | Integer monotone build code                           |
| `iconPath`    | `./assets/icon/icon.png` | Relative to project root                              |

---

## Boot Sequence: `index.js`

`index.js` is the entry point compiled into the JS bundle. It runs an **async boot sequence** to handle crashes gracefully and log every initialization step to the Black Box before the React component tree mounts.

### Sequence

```
runBootSequence()
  │
  ├─ 1. Resolve icon asset URI via Image.resolveAssetSource()
  │
  ├─ 2. AppRegistry.registerComponent('plugin_superflow', () => App)
  │      Mounts the React Native component tree
  │
  ├─ 3. PluginManager.init()
  │      Initializes the Chauvet OS plugin host connection
  │
  ├─ 4. PluginManager.registerButton(1, ['NOTE', 'DOC'], { id: 100, name: 'SuperFlow', ...})
  │      Registers the sidebar trigger button.
  │      - buttonType 1 = sidebar widget
  │      - contexts ['NOTE', 'DOC'] = visible in Note and Document files
  │      - id: 100 = the button identifier sent back when tapped
  │
  └─ [On any exception] → logs fatal crash details to SuperFlow_Log.txt
```

### Key Observations

- The icon `URI` is resolved from the React Native asset bundle at boot time (not at registration time) and conditionally included in `registerButton()`. If resolution fails, the button is registered without an icon rather than crashing.
- Both boot success **and** all errors are written to `SuperFlow_Log.txt` before the process exits, making the log file valid even for initialization failures.
- `PluginManager.closePlugin()` is exposed in `SuperFlowPage.js` via the **"X"** exit button in the top-right corner, which gracefully dismisses the plugin UI.

---

## SDK API Reference (Verified)

This section documents the **verified behavior** of `sn-plugin-lib` as discovered through on-device testing. This is the definitive record to avoid repeating the discovery process.

### Critical: Correct Stroke-Fetching API

> **⚠️ `PluginNoteAPI.getLayerData` is INVALID and does not exist on device.**

The correct method to retrieve ink strokes from a `.note` file is:

```javascript
// ✅ CORRECT
PluginFileAPI.getElements(pageIndex, notePath);

// ❌ WRONG — method does not exist
PluginNoteAPI.getLayerData(...);
```

The argument order is **`(pageIndex, notePath)`** — page first, path second. This is the inverse of what the signature name might suggest.

### The Sync Strategy: `saveCurrentNote()` First

Before calling `PluginFileAPI.getElements()`, you **must** call `PluginNoteAPI.saveCurrentNote()`. Without this call, the `.note` file on disk may not reflect the latest pen strokes in memory, causing `getElements()` to return stale or partial data.

```javascript
// Step 1: Force a save to flush in-memory strokes to disk
await PluginNoteAPI.saveCurrentNote();

// Step 2: Now safe to read the file's elements
const response = await PluginFileAPI.getElements(pageIndex, notePath);
```

SuperFlow implements this in `PluginAPI.getRawStrokes()` with a graceful safety check:

```javascript
if (PluginNoteAPI && typeof PluginNoteAPI.saveCurrentNote === 'function') {
  try {
    await PluginNoteAPI.saveCurrentNote();
  } catch (e) {
    console.warn(`[PluginAPI] saveCurrentNote failed: ${e.message}`);
  }
}
```

The `try/catch` ensures that a failed save (e.g., nothing to save) does not abort the stroke-fetch. The app continues and reads whatever data is available.

### SDK Response Envelope

All `sn-plugin-lib` APIs return a **consistent response wrapper object**, not the data directly:

```javascript
// The SDK ALWAYS returns this shape:
{
  "success": boolean,   // Whether the call succeeded
  "result": any,        // The actual return value (when success = true)
  "error": {            // Populated only on failure
    "message": string
  }
}
```

**`getElements()` specifically returns:**

```javascript
{
  "success": true,
  "result": [            // Array of element objects on the page
    {
      "type": number,    // Element type: 0 = ink stroke, others = other elements
      "rect": { ... },   // OR flat coordinates: x/y/width/height or X/Y/Width/Height
      ...
    }
  ]
}
```

> **The SDK does not return `result` as a direct array.** It is always wrapped in the object above. Code that does `const strokes = await PluginFileAPI.getElements(...)` and then iterates directly will crash. Always unwrap: `response.result`.

### SDK Module Exports

**Verified exports from `sn-plugin-lib`:**

| Export          | Description                                                                           |
| --------------- | ------------------------------------------------------------------------------------- |
| `PluginManager` | Plugin lifecycle: `init()`, `registerButton()`, `closePlugin()`                       |
| `PluginCommAPI` | Communication: `getCurrentFilePath()`, `getCurrentPageNum()`                          |
| `PluginFileAPI` | File operations: `getElements(pageIndex, path)`, `insertKeyWord(path, page, keyword)` |
| `PluginNoteAPI` | Note operations: `saveCurrentNote()`, `setTitle(path, page, text)`                    |

The `SuperFlowPage.js` component performs live SDK API discovery on every "Process" trigger, logging all available exports and method names to the Black Box log file. This is intentional diagnostic infrastructure for the alpha phase.

---

## `PluginAPI.js` — Full Method Reference

`PluginAPI` is the **sole authorized SDK import point**. All other modules interact with the device exclusively through this facade.

**File:** `src/core/PluginAPI.js`
**Imports:** `PluginCommAPI`, `PluginFileAPI`, `PluginNoteAPI` from `sn-plugin-lib`; `RNFS` from `react-native-fs`

---

### `static _getConfigPath(configName)`

Generates the canonical absolute path for a template's JSON config file.

| Parameter    | Type     | Description                                   |
| ------------ | -------- | --------------------------------------------- |
| `configName` | `string` | Base name of the template (e.g., `'Meeting'`) |
| **Returns**  | `string` | Absolute path on device storage               |

```
/storage/emulated/0/MyStyle/template/configs/<configName>.json
```

---

### `static async getActiveContext()`

Retrieves the active note file path and current page number in a single parallel call.

| Parameter   | —                                            | —                                                                                  |
| ----------- | -------------------------------------------- | ---------------------------------------------------------------------------------- |
| **Returns** | `Promise<{ path: string, pageNum: number }>` | Current note context                                                               |
| **Throws**  | `Error`                                      | If either `getCurrentFilePath()` or `getCurrentPageNum()` returns `success: false` |

**SDK calls:** `PluginCommAPI.getCurrentFilePath()` + `PluginCommAPI.getCurrentPageNum()` (parallel via `Promise.all`)

---

### `static async getRawStrokes(notePath, pageIndex)`

Saves the current note to disk, then reads and returns all ink stroke elements from the specified page.

| Parameter   | Type                     | Description                                         |
| ----------- | ------------------------ | --------------------------------------------------- |
| `notePath`  | `string`                 | Absolute path to the `.note` file                   |
| `pageIndex` | `number`                 | 0-indexed page number                               |
| **Returns** | `Promise<Array<object>>` | Array of stroke elements where `element.type === 0` |

**Implementation notes:**

- Calls `PluginNoteAPI.saveCurrentNote()` before reading (see [Sync Strategy](#the-sync-strategy-savecurrentnote-first))
- Unwraps `response.result` from the SDK envelope before filtering
- Filters to `el.type === 0` (ink strokes only; other types are ignored)
- Logs element count to console

---

### `static async saveJSONConfig(templateName, data)`

Persists a compiled Hotzone map as a JSON file to RNFS storage. Creates the directory if it does not exist.

| Parameter      | Type               | Description                                     |
| -------------- | ------------------ | ----------------------------------------------- |
| `templateName` | `string`           | Base template name (e.g., `'Daily Planner'`)    |
| `data`         | `object`           | The compiled `{ hotzones: [...] }` JSON payload |
| **Returns**    | `Promise<boolean>` | `true` on success, `false` on RNFS error        |

**Output path:** `/storage/emulated/0/MyStyle/template/configs/<templateName>.json`
**Format:** `JSON.stringify(data, null, 2)` — 2-space indented for maintainability

---

### `static async loadJSONConfig(templateName)`

Reads and parses a previously compiled config from RNFS storage.

| Parameter      | Type                      | Description                                                             |
| -------------- | ------------------------- | ----------------------------------------------------------------------- |
| `templateName` | `string`                  | Base template name                                                      |
| **Returns**    | `Promise<object \| null>` | Parsed JSON config, or `null` if the file does not exist or parse fails |

---

### `static async injectKeyword(notePath, page, keyword)`

Injects a keyword tag into the specified note page.

| Parameter   | Type               | Description                       |
| ----------- | ------------------ | --------------------------------- |
| `notePath`  | `string`           | Absolute target `.note` file path |
| `page`      | `number`           | 0-based page index                |
| `keyword`   | `string`           | The keyword text to inject        |
| **Returns** | `Promise<boolean>` | `true` if `res.success === true`  |

**SDK call:** `PluginFileAPI.insertKeyWord(notePath, page, keyword)`

---

### `static async injectTitle(notePath, page, titleText)`

Injects a title or heading into the specified note page.

| Parameter   | Type               | Description                       |
| ----------- | ------------------ | --------------------------------- |
| `notePath`  | `string`           | Absolute target `.note` file path |
| `page`      | `number`           | 0-based page index                |
| `titleText` | `string`           | The title text to inject          |
| **Returns** | `Promise<boolean>` | `true` if `res.success === true`  |

**SDK call:** `PluginNoteAPI.setTitle(notePath, page, titleText)`

---

## SpatialMappingEngine

**File:** `src/core/SpatialMappingEngine.js`
**Exported as:** Singleton instance (`export default new SpatialMappingEngine()`)

The engine is the orchestrator for the entire Process Phase. It is intentionally stateless—all relevant state (the active file, the compiled config, the strokes) is fetched on every invocation.

### `processActivePage()` — Guaranteed Exit Conditions

The engine has three **clean, error-free exit paths** that log an informational message and return without throwing:

| Condition                                         | Log message                                                  |
| ------------------------------------------------- | ------------------------------------------------------------ |
| Active filepath is malformed/empty                | `Active file string is malformed or invalid.`                |
| No compiled config exists for the active template | `Clean exit. No mapped configuration exists for <name>.json` |
| Current page has zero ink strokes                 | `Page ink context is completely blank. Halting execution.`   |

All other errors are caught by the top-level `try/catch` block and logged via `console.error`. The engine **never re-throws**, ensuring that a malformed Add-on cannot prevent subsequent zones from processing.

### AABB Collision Formula

```
stroke overlaps zone  ←→
  stroke.x          ≤  zone.x + zone.width    AND
  stroke.x + stroke.width  ≥  zone.x          AND
  stroke.y          ≤  zone.y + zone.height   AND
  stroke.y + stroke.height ≥  zone.y
```

This is a **partial overlap** test—any part of a stroke bounding box touching the zone is sufficient to trigger. This is the correct behavior for natural handwriting, where strokes may slightly exceed zone boundaries.

---

## The "Black Box" Logging System

Because the Supernote device has no developer console visible to the user, SuperFlow implements a **persistent file-based diagnostic log** that survives crashes and provides a complete audit trail of boot and process events.

### Log File Location

```
/storage/emulated/0/MyStyle/Plugins/SuperFlow_Log.txt
```

### When it is Written

| Event             | Source             | Content                                                                                                                |
| ----------------- | ------------------ | ---------------------------------------------------------------------------------------------------------------------- |
| **Boot**          | `index.js`         | Written (overwritten) with full boot sequence trace on every plugin launch                                             |
| **Process Phase** | `SuperFlowPage.js` | **Appended** with each process trigger; includes context path, SDK API discovery, success/error result and stack trace |

### Writing Mechanism

Both writers use `RNFS.mkdir()` + `RNFS.appendFile()` (or `writeFile()` at boot) inside a `finally` block, guaranteeing the log is written **even when a crash occurs** before the log call:

```javascript
} finally {
  try {
    const logDir = `${RNFS.ExternalStorageDirectoryPath}/MyStyle/Plugins`;
    const logFile = `${logDir}/SuperFlow_Log.txt`;
    await RNFS.mkdir(logDir);
    await RNFS.appendFile(logFile, logString, 'utf8');
  } catch (fsErr) {
    console.warn('Failed to append log:', fsErr);
  }
}
```

### Boot Log Format

```
--- SUPERFLOW BOOT ---
Registering real App Component...
Calling PluginManager.init()...
Calling PluginManager.registerButton()...

--- END OF BOOT COMPLETE (NO CRASHES CATCHED) ---
```

Or on fatal crash:

```
--- SUPERFLOW BOOT ---
...
!!! FATAL CRASH DETECTED !!!
Name: TypeError
Message: Cannot read property 'init' of undefined
Stack: ...
```

### Process Phase Log Format

```
--- PROCESS TRIGGERED ---
Process Started.
Context OK. Path: /storage/emulated/0/Note/Meeting.note
All SDK Exports: PluginManager, PluginCommAPI, PluginFileAPI, PluginNoteAPI
Available API methods: saveCurrentNote, getTitle, setTitle, ...
Fetching and parsing spatial strokes...
Mapping completed.
```

Or on error:

```
--- PROCESS TRIGGERED ---
Process Started.
Context OK. Path: /storage/emulated/0/Note/Meeting.note
Mapping Error: Cannot read property 'hotzones' of null
Stack: at SpatialMappingEngine.processActivePage ...
```

### Reading the Log

Transfer the file to your computer via ADB:

```bash
adb pull /storage/emulated/0/MyStyle/Plugins/SuperFlow_Log.txt ./SuperFlow_Log.txt
```

Or read it live:

```bash
adb shell cat /storage/emulated/0/MyStyle/Plugins/SuperFlow_Log.txt
```

---

## Build & Install Guide

### Prerequisites

| Tool              | Version                   | Purpose                                              |
| ----------------- | ------------------------- | ---------------------------------------------------- |
| Node.js           | ≥ 18                      | Runtime for the build tools                          |
| `jq` or `python3` | any                       | Required by `buildPlugin.sh` to parse JSON           |
| `zip`             | any                       | Required by `buildPlugin.sh` to package the `.snplg` |
| ADB               | SDK Platform Tools        | Transfer the `.snplg` to device                      |
| Supernote device  | Firmware with sideloading | Target hardware                                      |

### Dependencies

```bash
npm install
```

### Running Tests (Mandatory Before Committing)

```bash
npm test       # Jest — ALL tests must pass
npm run lint   # ESLint — zero errors
```

### Building the Plugin Package

```bash
./buildPlugin.sh
```

**What this script does:**

1. Reads `package.json` for project name and version
2. Runs `npx react-native bundle` to produce the JS bundle at `build/generated/<name>.bundle`
3. Copies `PluginConfig.json` into `build/generated/` and updates `iconPath`
4. Detects if native Android code exists; if so, runs `gradle buildCustomApkDebug` and copies the APK as `app.npk`
5. Zips the entire `build/generated/` directory into `build/outputs/<name>.zip`
6. Renames the zip to `build/outputs/plugin_superflow.snplg`

**Output artifact:**

```
build/outputs/plugin_superflow.snplg
```

A `.snplg` file is a zip archive containing:

- `plugin_superflow.bundle` — the compiled JS bundle
- `PluginConfig.json` — updated manifest with resolved paths
- `icon.png` — the plugin icon
- `app.npk` — (if native code is present) the compiled APK

### Installing on Device

**Via ADB:**

```bash
# 1. Transfer the .snplg file
adb push build/outputs/plugin_superflow.snplg /storage/emulated/0/MyStyle/

# 2. On the device, open the Supernote Plugin Manager and import
#    the plugin from MyStyle/plugin_superflow.snplg

# 3. After install, tail the log to verify boot succeeded
adb shell cat /storage/emulated/0/MyStyle/Plugins/SuperFlow_Log.txt
```

**Iterative Development Cycle:**

```bash
# 1. Make changes
# 2. Run tests (pre-commit gate will enforce this anyway)
npm test && npm run lint
# 3. Build
./buildPlugin.sh
# 4. Push to device
adb push build/outputs/plugin_superflow.snplg /storage/emulated/0/MyStyle/
# 5. Reinstall via device Plugin Manager UI
# 6. Read log
adb shell cat /storage/emulated/0/MyStyle/Plugins/SuperFlow_Log.txt
```

---

## File System Paths Reference

All paths are **absolute** on the Supernote's Android filesystem. Relative paths are not supported by any SDK API.

| Resource              | Absolute Path                                               |
| --------------------- | ----------------------------------------------------------- |
| External storage root | `/storage/emulated/0/`                                      |
| Note files            | `/storage/emulated/0/Note/<FileName>.note`                  |
| Plugin configs root   | `/storage/emulated/0/MyStyle/template/configs/`             |
| Config for "Meeting"  | `/storage/emulated/0/MyStyle/template/configs/Meeting.json` |
| Black Box log         | `/storage/emulated/0/MyStyle/Plugins/SuperFlow_Log.txt`     |
| Plugin install target | `/storage/emulated/0/MyStyle/plugin_superflow.snplg`        |

In code, the root is accessed via:

```javascript
import RNFS from 'react-native-fs';
// RNFS.ExternalStorageDirectoryPath === '/storage/emulated/0'
```

---

## Current Status & Known Constraints

### ✅ Verified Working

| Component                                          | Status                         |
| -------------------------------------------------- | ------------------------------ |
| Boot sequence & `PluginManager.init()`             | ✅ Working on device           |
| `PluginManager.registerButton()` — sidebar trigger | ✅ Working on device           |
| `PluginCommAPI.getCurrentFilePath()`               | ✅ Working on device           |
| `PluginCommAPI.getCurrentPageNum()`                | ✅ Working on device           |
| `PluginNoteAPI.saveCurrentNote()`                  | ✅ Working on device           |
| `PluginFileAPI.getElements(pageIndex, path)`       | ✅ Working on device           |
| Black Box log writing (`RNFS.appendFile`)          | ✅ Working on device           |
| TemplateParser Union-Find clustering               | ✅ Verified by Jest unit tests |
| AABB collision detection                           | ✅ Verified by Jest unit tests |
| ConfigManager draft state + compilation            | ✅ Verified by Jest unit tests |

### ⚠️ Known Constraints / Open Questions

| Issue                                      | Notes                                                                                                                                                                   |
| ------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `PluginNoteAPI.getLayerData`               | **Does not exist.** Do not use. Use `PluginFileAPI.getElements` instead.                                                                                                |
| Stroke `type` field value                  | Strokes are filtered by `el.type === 0`. If a firmware update changes this constant, stroke detection will silently return zero results. Monitor via the Black Box log. |
| `PluginFileAPI.getElements` argument order | `(pageIndex, notePath)` — page first, path second. This is confirmed correct, but unintuitive.                                                                          |
| `PluginNoteAPI.setTitle`                   | Newly added to `PluginAPI.injectTitle()`. Not yet mapped to any UI or Add-on. Untested on device in this session.                                                       |
| `Learn Phase` stroke reading               | Uses the same `saveCurrentNote()` + `getElements()` path. Template scanning and zone detection are untested on physical hardware in this cycle.                         |
| `versionName` in PluginConfig              | Updated to `0.1.0-alpha` to reflect the current pre-release state.                                                                                                      |

### 🔜 Next Steps

1. **On-device Learn Phase test**: Draw boxes on a template `.note`, trigger "Settings", verify that `TemplateParser` correctly clusters the strokes into Hotzones.
2. **End-to-end Process Phase test**: Map a Hotzone to `InjectKeywordAddon`, write inside the zone on a regular note, tap "Process Active Page", verify the keyword appears.
3. **Stabilize `injectTitle`**: Write a test Add-on that calls `toolkit.setTitle` and verify on device.
4. **i18n audit**: Ensure all new UI strings added to `SuperFlowPage.js` (e.g., `'ui_close'`, `'button_processing'`) are present in all locale JSON files (`en`, `it`).
