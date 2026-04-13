# 🤖 AI Development Guidelines (The "Vibecoding" Manifesto)

Welcome to the **SuperFlow** repository! 

> **The "Vibecoded" Declaration**
> SuperFlow is proudly an **AI-Assisted / Vibecoded** project. We actively encourage the use of **Google Antigravity** to write, refactor, and architect this codebase. 
> 
> However, "vibecoded" does not mean sloppy. We welcome Pull Requests from both human engineers and AI agents, provided that **all code strictly adheres to the high-quality architectural and documentation standards** outlined in this manifesto.

---

## 🏛️ Core Project Constraints

Because SuperFlow runs on the Chauvet OS (an e-ink Android operating system), system resources are precious and battery longevity is paramount.

1. **E-ink Optimization (Zero UI Jank):** UI components must be extremely lightweight. Avoid meaningless animations, and use visually contrasting standard colors (pure blacks, pure whites) suitable for e-ink displays.
2. **The "Manual Trigger" Rule:** **NO BACKGROUND POLLING**. SuperFlow guarantees 0ms ink latency while writing because it does not actively listen for pen strokes. Pen strokes are only parsed when the user explicitly triggers the process via the sidebar.
3. **Strict Separation of Concerns (SoC):**
   - **UI Layer:** Pure rendering logic. Knows nothing about how strokes are mapped.
   - **SDK API (`PluginAPI`):** The only layer allowed to touch the Ratta `PluginNoteAPI`.
   - **Spatial Engine (`SpatialMappingEngine`):** Pure mathematics and bounding logic. Independent of the SDK and UI.

---

## 📐 Strict Coding Standards

Code maintainability and an exceptional Developer Experience (DX) are non-negotiable, primarily because SuperFlow relies on third-party plugin developers building Add-ons.

* **Hyper-Descriptive JSDoc:** Every single file, class, method, and utility function MUST have verbose JSDoc comments. You must explain *why* a function exists, its parameters, its return types, and its role in the overall architecture. Avoid guessing; document intent.
* **i18n Ready (Multilingual):** Absolutely **no hardcoded UI strings** in the codebase. Every UI text must route through `react-i18next` targeting our localization JSONs. The default required languages are English and Italian.
* **Third-Party Friendly (DX):** Keep functions small and single-responsibility. Naming conventions must be exquisitely clear. A completely new open-source contributor should be able to read the code like a story.

---

## ⚙️ The Mandatory AI Workflow

To maintain the integrity of this codebase, **Google Antigravity** (and any other AI Agent) working on this repository MUST strictly follow this mandatory 4-Step Process:

**Step 1: Codebase Analysis**
Stop and read. You must thoroughly analyze the current context, the `README.md`, and these guidelines before writing a single line of code.

**Step 2: Implementation Plan (Wait for Approval)**
Outline a detailed, step-by-step plan of what you intend to delete, modify, and create. 
*CRITICAL:* You must STOP here and ask the human user for explicit approval before proceeding.

**Step 3: Implementation & Documentation**
Execute the approved plan. You must strictly adhere to the SoC and JSDoc requirements. Code logically, predictably, and securely.

**Step 4: Git Commit**
Run the project's required linters (e.g., `eslint .`). You may only execute a Git commit once all checks pass with zero errors. Structure a comprehensive, professional commit message outlining the architectural changes.
