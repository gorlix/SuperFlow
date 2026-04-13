# 🌊 SuperFlow for Supernote

> **Status:** 🚧 Early Planning & Feasibility Stage
> **Platform:** Supernote (Chauvet OS / React Native SDK)

SuperFlow is an open-source automation plugin for the Supernote e-ink tablet. It bridges the gap between analog handwriting and digital workflows by turning specific areas of your paper into **Action Zones**—without adding any UI clutter to your daily notes.

## 🎯 The Problem
The Supernote offers a fantastic, distraction-free writing experience. However, automating metadata (like applying tags, creating Headings, or exporting to third-party apps) often requires interrupting your flow to navigate menus or use the lasso tool. 

## ✨ The Solution: Invisible Automation
SuperFlow treats handwriting as spatial commands. By referencing a separate "Template Note", the plugin analyzes *where* you wrote something on your current page and triggers actions accordingly.

### Core Features (V1 Roadmap)
* **🖋️ Automatic Headings:** Write inside your predefined "Heading Zone" and SuperFlow will automatically parse the strokes and inject the `heading` metadata into the `.note` file. No lasso selection needed.
* **🔘 Paper Switches:** Draw any mark (a dot, a cross, a scribble) inside a predefined "Switch Zone" to trigger boolean actions (e.g., append a #urgent tag, create a bookmark, or move the file).
* **⚡ Zero Latency:** SuperFlow operates on a **Manual Trigger** via the sidebar. It does not poll pen strokes in the background, ensuring 100% native writing speed (0ms latency) and zero battery drain while you work.

## 🏗️ How it Works (The Template System)
To keep your working pages clean, SuperFlow uses a Reference Template architecture:

1. **Map It:** Create a single `Template_Work.note`. Draw boxes or place stickers to define your Action Zones (e.g., a top bar for Headings, a bottom-right square for a specific Tag).
2. **Write It:** Take notes on your regular notebooks as usual. You don't need to draw the boxes on every page.
3. **Process It:** Hit the "Process" button in the SuperFlow sidebar. The plugin overlays the coordinates of your template onto your current page, detects intersections with your pen strokes, and executes the linked actions.

## 🧩 Extensibility: The Add-on Architecture
SuperFlow is being designed as a modular platform. The core plugin handles the "Spatial Mapping Engine" (detecting strokes inside zones), while the execution is handled by modular Add-ons.

Planned future Add-on integrations:
* **PKM Sync:** Send specific blocks to Obsidian, Logseq, or Notion.
* **Task Management:** Push handwritten action items to Todoist or Google Tasks.
* **Webhooks:** Trigger custom APIs.

## 💻 Development & Setup
This project is being developed on **Arch Linux** using the official Supernote React Native SDK.

### Prerequisites
* Node.js (LTS)
* Android SDK & ADB (Ensure your user is in the `adbusers` group if on Linux)
* Supernote device with Sideloading enabled

### Quick Start (Coming Soon)
*Instructions for cloning, building, and installing the APK via ADB will be added once the initial boilerplate is pushed.*

## 🤝 Contributing
I am a student developing this to optimize my own workflow, and I am currently in the **feasibility and exploration stage** of the Ratta SDK. 

Contributions, advice, and pull requests are highly welcome! I am specifically looking for:
* **SDK Experts:** Advice on the best practices for handling `PluginNoteAPI` to read/write `.note` metadata safely.
* **TypeScript/React Native Devs:** Help with structuring the Add-on architecture.
* **UI/UX:** Ideas on how to build the plugin's settings panel for mapping template zones to actions.

## 📄 License
MIT License. See `LICENSE` for more information.

---
*Let's make our notes work for us.*
