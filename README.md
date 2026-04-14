# 🌊 SuperFlow for Supernote

> **Platform:** Supernote (Chauvet OS / React Native SDK)
> **Version:** 0.1.0-alpha
> **License:** MIT

SuperFlow is an open-source automation plugin for the Supernote e-ink tablet. It bridges the gap between analog handwriting and digital workflows by turning ordinary areas of your notes into **Dynamic Action Zones (Hotzones)**—without adding any UI clutter to your daily writing experience, and with **zero impact on ink latency**.

---

## 📖 Table of Contents

- [The Core Idea](#the-core-idea)
- [Key Principles](#key-principles)
- [Getting Started: The Two-Phase Workflow](#getting-started-the-two-phase-workflow)
  - [Phase 1 — The Learn Phase](#phase-1--the-learn-phase-setting-up-your-template)
  - [Phase 2 — The Process Phase](#phase-2--the-process-phase-triggering-your-actions)
- [What is a Hotzone?](#what-is-a-hotzone)
- [What is an Add-on?](#what-is-an-add-on)
- [FAQ](#faq)
- [Contributing](#contributing)
- [License](#license)

---

## The Core Idea

The Supernote is a fantastic, distraction-free writing device. However, automating metadata—like applying keyword tags, creating headings, or syncing to a third-party app—traditionally requires you to stop writing and navigate menus.

**SuperFlow solves this by treating your handwriting as spatial commands.** You define rectangular regions on a template note. When you write inside one of those regions later, SuperFlow knows to execute the action you configured for it.

The critical insight: **SuperFlow never interferes with your writing in real time.** Processing only happens when _you_ explicitly ask for it.

---

## Key Principles

| Principle            | What it means for you                                                                                    |
| -------------------- | -------------------------------------------------------------------------------------------------------- |
| **0ms Latency**      | SuperFlow never runs in the background. Your e-ink pen always responds at full native speed.             |
| **Manual Trigger**   | Actions are only executed when you tap the **"Process Active Page"** button on the SuperFlow dashboard.  |
| **E-ink First**      | The UI is pure black and white—no animations, no gradients, no battery-draining rendering.               |
| **Template + Notes** | You create one template note with zones drawn on it. You then take notes freely, and process them later. |

---

## Getting Started: The Two-Phase Workflow

SuperFlow operates in exactly **two phases**: a one-time **Learn Phase** to teach the plugin your layout, and a recurring **Process Phase** to execute actions.

### Phase 1 — The Learn Phase (Setting Up Your Template)

The Learn Phase is how you tell SuperFlow where your Action Zones are and what they should do.

#### Step 1: Create Your Template Note

On your Supernote, create a new `.note` file. This will be your **template map**—it exists solely to define zone boundaries and is separate from the notes you will take every day.

Draw **simple rectangles** using your pen in the areas you want to become interactive zones. Each closed box you draw will become one Hotzone. You can draw multiple distinct boxes to create multiple independent zones.

> **Tip:** Draw your boxes deliberately and close them fully. The engine applies a **20px tolerance** to account for natural pen lifts at corners, so a nearly-closed rectangle will still be detected as a single zone.

#### Step 2: Open SuperFlow and Tap "Learn Template Map"

Open the SuperFlow plugin from the Supernote sidebar. You will see the main **Dashboard** with two buttons:

- **Process Active Page** — (for the Process Phase, covered below)
- **Learn Template Map** — tap this now

SuperFlow will immediately read the stroke geometry from your currently open template file and detect all the rectangular zones you drew. No upload, no cloud connection—everything happens on-device.

#### Step 3: Map Actions to Each Zone

After scanning, SuperFlow switches to the **Settings View**, which lists every Hotzone it found. For each zone, you will see its pixel coordinates and an **"+ Assign Action"** button.

Tap **"+ Assign Action"** on a zone to open the **Add-on Picker**—a list of every available action module (Add-on) installed on the device.

Select an Add-on. If the Add-on requires configuration (e.g., a keyword to inject), a **dynamic configuration form** will appear automatically. Fill in the required fields and tap **"Confirm & Apply"**.

You can assign **multiple Add-ons to a single zone** by tapping **"+ Assign Action"** again after saving the first one.

#### Step 4: Save & Compile

When all zones have been mapped to their desired actions, tap **"Save & Compile JSON"**.

SuperFlow will:

1. **Prune** any zones you left without an action (empty zones are silently discarded to keep the config lean).
2. **Compile** the full mapping into a lightweight JSON file stored at:
   ```
   /storage/emulated/0/MyStyle/template/configs/<TemplateName>.json
   ```
3. **Return you to the Dashboard**, ready for use.

> **To discard your changes** at any point during the Learn Phase, tap **"Discard Draft"**. The in-memory state will be wiped immediately and nothing will be saved.

---

### Phase 2 — The Process Phase (Triggering Your Actions)

The Process Phase is the everyday workflow. It happens in a single tap.

#### Step 1: Write Normally in Any Note

Take notes on any `.note` file, just as you always do. Write freely inside the areas that correspond to your template zones—SuperFlow does nothing while you write.

#### Step 2: Tap "Process Active Page"

When you are ready (after finishing a note session, or at any point you choose), open the SuperFlow plugin and tap the large **"Process Active Page"** button.

SuperFlow will:

1. Detect which `.note` file is currently open and load its matching compiled JSON config.
2. Read all pen strokes on the current page.
3. Run a fast mathematical check (**AABB collision detection**) to find which strokes fall inside which Hotzones.
4. Execute the Add-on action for every matched zone—in sequence.

> **If there is no config for the current file**, SuperFlow will exit cleanly with no error. You will simply see no change—there is nothing to process.

> **If the current page is blank**, SuperFlow detects this immediately and halts without any action.

---

## What is a Hotzone?

A **Hotzone** is a rectangular region on a page, defined in pixel coordinates. SuperFlow identifies Hotzones by analyzing the pen strokes on your template file. Multiple strokes drawn as a box are automatically **clustered** into one Hotzone using a graph-theory algorithm (Union-Find).

Every Hotzone has:

| Property          | Description                                                            |
| ----------------- | ---------------------------------------------------------------------- |
| `id`              | A unique identifier generated at scan time (e.g., `zone_1713100000_0`) |
| `x`, `y`          | Top-left corner coordinates (in screen pixels)                         |
| `width`, `height` | Dimensions of the zone                                                 |
| `actions`         | The list of Add-on actions mapped to this zone                         |

---

## What is an Add-on?

An **Add-on** is a self-contained action module that SuperFlow executes when a matching Hotzone is triggered. Add-ons are designed to be written by third-party developers and require **no modification** to the SuperFlow core.

**Built-in Add-ons (v0.1):**

| Add-on Name | ID                    | Description                                                               |
| ----------- | --------------------- | ------------------------------------------------------------------------- |
| Add Keyword | `core.inject_keyword` | Injects a configured keyword tag into the note at the triggered location. |

More Add-ons (PKM Sync, Webhook Trigger, etc.) are planned for future milestones.

---

## FAQ

**Q: Will processing slow down my writing?**
No. SuperFlow does not listen to pen strokes in real time. The "Process Active Page" button is the only trigger. Your handwriting always operates at the device's native 0ms latency.

**Q: What happens if I write outside all zones?**
Nothing. Only strokes that geometrically overlap with a mapped Hotzone will trigger an action. All other strokes are ignored.

**Q: Can I re-map or update a template?**
Yes. Simply open the template note, tap "Learn Template Map" again, and reconfigure your zones. Saving will overwrite the existing compiled JSON for that template.

**Q: What if SuperFlow can't find my zones?**
Make sure you drew closed or nearly-closed rectangles on your template. Single stray lines or marks smaller than **80×80 pixels** are automatically filtered out as noise.

**Q: Where is my configuration stored?**
On your Supernote's internal storage at:
`/storage/emulated/0/MyStyle/template/configs/<FileName>.json`

---

## Contributing

Contributions are welcome! SuperFlow is proudly an **AI-Assisted / Vibecoded** open-source project. See [`docs/ADDON_API.md`](docs/ADDON_API.md) to learn how to build a new Add-on and the [`AI_DEVELOPMENT_GUIDELINES.md`](.agents/AI_DEVELOPMENT_GUIDELINES.md) for coding standards.

Pre-commit hooks enforce tests and linting automatically. A commit that breaks any test **will be physically blocked** by the CI.

### Prerequisites

- Node.js ≥ 18
- Android SDK & ADB
- Supernote device with sideloading enabled

### Quick Start

```bash
git clone https://github.com/your-org/superflow.git
cd superflow
npm install
npm test        # Run the full test suite
npm run lint    # Run the ESLint linter
```

---

## License

MIT License. See `LICENSE` for full details.

---

_Let's make our notes work for us._
