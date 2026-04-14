# SuperFlow Add-on API Reference

> **Audience:** Third-party Add-on developers
> **Version:** 0.1.0

This document is the complete, authoritative guide for building SuperFlow Add-ons. By the end, you will be able to write a production-ready Add-on in under 30 minutes—without ever needing to touch the Chauvet SDK directly.

---

## Table of Contents

- [Core Concepts](#core-concepts)
- [The Security Contract](#the-security-contract)
- [Creating an Add-on: Step-by-Step](#creating-an-add-on-step-by-step)
  - [Step 1: Extend `BaseAddon`](#step-1-extend-baseaddon)
  - [Step 2: Declare Static Metadata](#step-2-declare-static-metadata)
  - [Step 3: Define Your `settingsSchema`](#step-3-define-your-settingsschema)
  - [Step 4: Implement `execute(payload)`](#step-4-implement-executepayload)
  - [Step 5: Register with `AddonManager`](#step-5-register-with-addonmanager)
- [The `settingsSchema`: Data-Driven UI Reference](#the-settingsschema-data-driven-ui-reference)
- [The `ExecutePayload`: Full API Reference](#the-executepayload-full-api-reference)
  - [`payload.context`](#payloadcontext)
  - [`payload.toolkit`](#payloadtoolkit)
  - [`payload.addonParams`](#payloadaddonparams)
- [Full Reference Example: `InjectKeywordAddon`](#full-reference-example-injectkeywordaddon)
- [Error Handling Conventions](#error-handling-conventions)
- [Coding Standards Checklist](#coding-standards-checklist)

---

## Core Concepts

SuperFlow's extensibility is built on a single, simple idea: **all Add-ons are equal, and all Add-ons are isolated.**

When a user's pen stroke matches a configured Hotzone, the **`AddonManager`** singleton looks up the corresponding Add-on, builds a sandboxed execution environment, and calls the Add-on's `execute()` method. The Add-on never speaks to the Chauvet SDK directly. It only speaks to the `toolkit` proxy that was handed to it.

This design means:

- **You cannot crash the host.** An Add-on error is caught by `AddonManager` and logged; it cannot throw the entire plugin.
- **You cannot over-reach.** The `toolkit` only exposes the SDK capabilities the core team has explicitly whitelisted.
- **You cannot register or call other Add-ons.** Each Add-on is a leaf-node actor—it receives a payload and executes a single, focused job.

---

## The Security Contract

> **CRITICAL: Read this before writing a single line of code.**

All SuperFlow Add-ons **MUST** adhere to the following rules without exception.

| Rule                         | ✅ Compliant                             | ❌ Violation                                    |
| ---------------------------- | ---------------------------------------- | ----------------------------------------------- |
| **No direct SDK imports**    | Use `toolkit.injectKeyword(...)`         | `import { PluginFileAPI } from 'sn-plugin-lib'` |
| **No file system access**    | N/A                                      | `import RNFS from 'react-native-fs'`            |
| **Extend `BaseAddon`**       | `class MyAddon extends BaseAddon`        | A plain function or standalone class            |
| **Throw on failure**         | `throw new Error('Descriptive message')` | Silently swallowing errors or `return false`    |
| **No global state mutation** | Only read from `payload`                 | Modifying a module-level variable               |

An Add-on that violates the **"no direct SDK import"** rule will compile, but will cause test failures against the mocked environment and will be **rejected at the pre-commit hook stage** before it can ever be merged.

---

## Creating an Add-on: Step-by-Step

### Step 1: Extend `BaseAddon`

Create a new file inside `src/addons/`. The file name should clearly describe what the Add-on does.

```javascript
// src/addons/MyCustomAddon.js
import BaseAddon from './BaseAddon';

export default class MyCustomAddon extends BaseAddon {
  // Implementation follows...
}
```

`BaseAddon` is the contract interface. It provides the static property stubs (`id`, `name`, `settingsSchema`) and the abstract `execute()` method signature that you must override.

### Step 2: Declare Static Metadata

Your Add-on class requires three static properties that uniquely identify it to the system and the UI:

```javascript
export default class MyCustomAddon extends BaseAddon {
  /**
   * Unique, namespaced identifier. Used as the key in all JSON config maps.
   * Convention: '<author_namespace>.<action_name>'
   * @type {string}
   */
  static id = 'com.yourname.my_custom_addon';

  /**
   * Human-readable display name shown in the Add-on Picker UI.
   * @type {string}
   */
  static name = 'My Custom Action';

  // settingsSchema defined next...
}
```

> **Important:** The `static id` must be **globally unique**. Use a reverse-domain namespace convention (e.g., `com.yourname.addon_name`) to avoid collisions with other Add-on authors. The `core.*` namespace is reserved for built-in SuperFlow Add-ons.

### Step 3: Define Your `settingsSchema`

The `settingsSchema` is the central innovation of SuperFlow's developer experience. It is a declarative array that tells the SuperFlow UI exactly which input fields to render when the user configures your Add-on. **You write no React Native UI code whatsoever.**

```javascript
static settingsSchema = [
  {
    key: 'targetTag',          // The key used to retrieve this value in execute()
    type: 'string',            // The field type (see reference table below)
    label: 'Tag Name',         // The label rendered above the input field
    placeholder: 'e.g., #task', // Placeholder text (for 'string' fields)
  },
  {
    key: 'overwrite',
    type: 'boolean',
    label: 'Overwrite Existing Tags',
  },
];
```

At runtime, the `DynamicAddonForm` component in `SuperFlowSettings.js` iterates this array and renders the appropriate native input element for each field type. The collected values are then passed to your `execute()` method via `payload.addonParams`.

### Step 4: Implement `execute(payload)`

This is the single method you must implement. It is called by `AddonManager` every time a stroke triggers a Hotzone mapped to your Add-on.

```javascript
/**
 * Executes the Add-on's core logic.
 * @async
 * @param {import('./BaseAddon').ExecutePayload} payload The DI container.
 * @returns {Promise<void>}
 * @throws {Error} Must throw with a descriptive message on any failure.
 */
async execute(payload) {
  const { context, toolkit, addonParams } = payload;

  // 1. Validate your parameters first.
  const tag = addonParams?.targetTag;
  if (!tag || typeof tag !== 'string' || tag.trim() === '') {
    throw new Error('MyCustomAddon: "targetTag" is missing or invalid.');
  }

  // 2. Use the toolkit proxy to interact with the Chauvet OS.
  const success = await toolkit.injectKeyword(
    context.activeFilePath,
    context.currentPageNum,
    tag.trim(),
  );

  // 3. Throw on failure so AddonManager can log it cleanly.
  if (!success) {
    throw new Error(`MyCustomAddon: Keyword injection failed for tag "${tag}".`);
  }
}
```

### Step 5: Register with `AddonManager`

Finally, instantiate your Add-on and register it. This is typically done in the plugin's entry point (`index.js`) alongside the built-in Add-ons:

```javascript
// index.js
import AddonManager from './src/addons/AddonManager';
import InjectKeywordAddon from './src/addons/core/InjectKeywordAddon';
import MyCustomAddon from './src/addons/MyCustomAddon';

AddonManager.registerAddon(new InjectKeywordAddon());
AddonManager.registerAddon(new MyCustomAddon());
```

Once registered, your Add-on will immediately appear in the **Add-on Picker** UI. No further configuration is needed.

---

## The `settingsSchema`: Data-Driven UI Reference

The `settingsSchema` static property is an array of **field descriptor objects**. The `DynamicAddonForm` component renders a native UI element for each entry.

### Supported Field Types

| `type` value | Rendered Component          | User Input    | Notes                       |
| ------------ | --------------------------- | ------------- | --------------------------- |
| `'string'`   | `<TextInput>` (single-line) | Free text     | Supports `placeholder`      |
| `'boolean'`  | _(planned)_ `<Switch>`      | Toggle on/off | Not yet implemented in v0.1 |

### Field Descriptor Properties

| Property      | Type     | Required | Description                                                                                                |
| ------------- | -------- | -------- | ---------------------------------------------------------------------------------------------------------- |
| `key`         | `string` | ✅ Yes   | The property name used to retrieve the value from `payload.addonParams`. Must be unique within the schema. |
| `type`        | `string` | ✅ Yes   | The field type. Currently supports `'string'`.                                                             |
| `label`       | `string` | ✅ Yes   | The human-readable label displayed above the input field in the UI.                                        |
| `placeholder` | `string` | ❌ No    | Placeholder text for `type: 'string'` inputs. Defaults to empty.                                           |

### Schema Example: Multiple Fields

```javascript
static settingsSchema = [
  {
    key: 'keyword',
    type: 'string',
    label: 'Keyword Text',
    placeholder: 'Enter the keyword to inject...',
  },
  {
    key: 'prefix',
    type: 'string',
    label: 'Optional Prefix',
    placeholder: 'e.g., #',
  },
];
```

When the user saves the form, `payload.addonParams` will look like:

```json
{
  "keyword": "meeting notes",
  "prefix": "#"
}
```

> **Empty Schema:** If your Add-on requires no user configuration, declare an empty array: `static settingsSchema = []`. The UI will render no fields and will call `execute()` with `addonParams = {}`.

---

## The `ExecutePayload`: Full API Reference

The `payload` argument passed to `execute()` is a **Dependency Injection container**. It is the complete, safe API surface available to your Add-on.

### `payload.context`

A snapshot of the state at the moment the "Process Active Page" button was tapped.

| Property                 | Type            | Description                                                                                                                                                                          |
| ------------------------ | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `context.activeFilePath` | `string`        | Absolute device path to the currently open `.note` file (e.g., `/storage/emulated/0/Note/Meeting.note`).                                                                             |
| `context.currentPageNum` | `number`        | The **0-indexed** page number of the currently active page within the note.                                                                                                          |
| `context.matchedZoneId`  | `string`        | The unique `id` of the Hotzone whose geometry matched the triggering stroke (e.g., `zone_1713100000_0`).                                                                             |
| `context.triggerStrokes` | `Array<object>` | The array of normalized ink stroke geometries (`{x, y, width, height}`) that geometrically intersected this zone. Provided for future-proof OCR or gesture-recognition capabilities. |

### `payload.toolkit`

The **safe, whitelisted proxy** to the Chauvet SDK. This is the **only** mechanism through which an Add-on may interact with the device.

> **You must not import `sn-plugin-lib` or `react-native-fs` directly.** If you need a native capability that is not yet in the `toolkit`, open a GitHub issue to request an official whitelisted proxy.

#### `toolkit.injectKeyword(path, page, keyword)`

Injects a keyword tag into the specified note file and page.

| Parameter   | Type               | Description                                                      |
| ----------- | ------------------ | ---------------------------------------------------------------- |
| `path`      | `string`           | Absolute path to the `.note` file. Use `context.activeFilePath`. |
| `page`      | `number`           | 0-indexed page number. Use `context.currentPageNum`.             |
| `keyword`   | `string`           | The keyword text string to inject.                               |
| **Returns** | `Promise<boolean>` | Resolves to `true` on success, `false` on Chauvet OS failure.    |

**Usage:**

```javascript
const ok = await toolkit.injectKeyword(
  context.activeFilePath,
  context.currentPageNum,
  'myKeyword',
);
if (!ok) throw new Error('Injection failed.');
```

### `payload.addonParams`

The plain object containing the values the user filled into your Add-on's settings form at configuration time. Each key corresponds to a `key` property in your `settingsSchema`.

```javascript
// For an addon with settingsSchema = [{ key: 'text', type: 'string', ... }]
const text = payload.addonParams.text; // → "the value the user typed"
```

> **Always validate your params.** The user may have left a field empty. Your `execute()` method is responsible for validating params and throwing an informative error if required fields are missing or malformed.

---

## Full Reference Example: `InjectKeywordAddon`

The following is the complete source code of `InjectKeywordAddon`, the core V1 reference implementation. Study it as the canonical pattern for all future Add-on development.

```javascript
// src/addons/core/InjectKeywordAddon.js
import BaseAddon from '../BaseAddon';

/**
 * @class InjectKeywordAddon
 * @augments BaseAddon
 * @description Native V1 reference Add-on. Acts as the blueprint for evaluating
 * strict Dependency Injection. When a hotzone mapped to this addon is triggered,
 * it parses the configured JSON text and injects it as a native Chauvet Keyword
 * into the user's current spatial writing path.
 */
export default class InjectKeywordAddon extends BaseAddon {
  static id = 'core.inject_keyword';
  static name = 'Add Keyword';

  static settingsSchema = [
    {
      key: 'text',
      type: 'string',
      label: 'Keyword Text',
      placeholder: 'Enter keyword to inject...',
    },
  ];

  /**
   * Dispatches the keyword injection logic ensuring total SDK separation via `toolkit`.
   * @async
   * @param {import('../BaseAddon').ExecutePayload} payload DI executing runtime block.
   * @returns {Promise<void>}
   * @throws {Error} Halts and reports cleanly if the injection encounters a proxy failure or bad parameter.
   */
  async execute(payload) {
    const {context, toolkit, addonParams} = payload;

    // 1. Data Validation: Ensure the User configured a keyword in the settings panel.
    const keywordText = addonParams?.text;
    if (
      !keywordText ||
      typeof keywordText !== 'string' ||
      keywordText.trim() === ''
    ) {
      throw new Error(
        'InjectKeywordAddon aborted: Invalid or missing "text" parameter.',
      );
    }

    // 2. Safe Execution: Use the explicitly injected Toolkit Proxy.
    // Note: 'sn-plugin-lib' is never imported in this file.
    const success = await toolkit.injectKeyword(
      context.activeFilePath,
      context.currentPageNum,
      keywordText.trim(),
    );

    if (!success) {
      throw new Error(
        `Execution failed: Proxy rejected keyword injection at path ${context.activeFilePath}`,
      );
    }
  }
}
```

### Annotated Walkthrough

1. **`static id = 'core.inject_keyword'`** — The unique, namespaced ID used in the JSON config map and the `AddonManager` registry. Never change this after an Add-on has been deployed.
2. **`static settingsSchema`** — One field descriptor. The `DynamicAddonForm` will render one `<TextInput>` for `key: 'text'`. The user's input is accessible in `execute()` via `addonParams.text`.
3. **Param validation first** — `execute()` validates `addonParams.text` before doing anything else. A missing or blank keyword causes an immediate, descriptive throw.
4. **`toolkit.injectKeyword()`, not `PluginFileAPI.insertKeyWord()`** — The toolkit proxy is the only allowed path to the SDK. The raw SDK class is never referenced here.
5. **Throw on failure** — If the proxy returns `false`, the method throws. `AddonManager` will catch this and log it without crashing the rest of the processing loop.

---

## Error Handling Conventions

| Scenario                                         | Correct Action                                               |
| ------------------------------------------------ | ------------------------------------------------------------ |
| Required `addonParams` key is missing or invalid | `throw new Error('AddonName: descriptive message.')`         |
| `toolkit` method returns `false`                 | `throw new Error('AddonName: operation failed at path X.')`  |
| Unexpected internal logic error                  | Let the error propagate naturally; `AddonManager` catches it |
| Non-critical / optional operation fails          | Log with `console.warn(...)`, do **not** throw               |

**Always prefix your error messages with your Add-on's class name.** This makes log triage dramatically faster.

---

## Coding Standards Checklist

Before submitting a Pull Request, verify every item:

- [ ] Class extends `BaseAddon`
- [ ] `static id` follows `namespace.action_name` convention and is globally unique
- [ ] `static name` is a clear, human-readable string
- [ ] `static settingsSchema` is defined (even if empty `[]`)
- [ ] `execute()` is `async` and returns `Promise<void>`
- [ ] `execute()` validates all `addonParams` before use
- [ ] `execute()` throws `Error` (never `return false`) on failure
- [ ] **No `import` of `sn-plugin-lib` anywhere in the Add-on file**
- [ ] **No `import` of `react-native-fs` anywhere in the Add-on file**
- [ ] All methods and the class itself have verbose **JSDoc comments**
- [ ] Add-on is registered in `index.js`
- [ ] A corresponding Jest test file exists under `src/addons/` with mocked `toolkit`
- [ ] `npm test` passes with zero failures
- [ ] `npm run lint` passes with zero errors
