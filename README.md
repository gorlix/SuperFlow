# 🌊 SuperFlow for Supernote

> **Status:** 🚧 Early Planning & Feasibility Stage
> **Platform:** Supernote (Chauvet OS / React Native SDK)

SuperFlow is an open-source automation plugin for the Supernote e-ink tablet. It bridges the gap between analog handwriting and digital workflows by turning generic areas of your paper into **Dynamic Action Zones**—without adding any UI clutter to your daily notes.

## 🎯 The Problem

The Supernote offers a fantastic, distraction-free writing experience. However, automating metadata (like applying tags, creating Headings, or exporting to third-party apps) often requires interrupting your flow to navigate menus or use the lasso tool.

## ✨ The Solution: Dynamic Rules Engine

SuperFlow treats handwriting as spatial commands. By referencing a simple layout map, the plugin analyzes _where_ you wrote something on your current page and executes highly responsive actions accordingly.

### Core Features (V1 Roadmap)

- **🧠 Dynamic Rules Engine:** SuperFlow does not enforce hardcoded constraints. You draw normal rectangles anywhere on a template map and assign custom actions (like adding a keyword, syncing to a PC, or moving a task) to them visually.
- **⚡ Compiled JSON Performance:** To guarantee extreme speed, the visual node mapping is compiled into a lightweight JSON configuration.
- **🔋 Zero Latency:** SuperFlow operates on a **Manual Trigger** via the sidebar. It does not poll pen strokes in the background, ensuring 100% native writing speed (0ms latency), no UI freezing, and zero battery drain while you work.

## 🏗️ How it Works (The JSON Architecture)

Behind the scenes, SuperFlow relies on an optimized 5-step workflow to maintain zero distraction and maximum agility:

1. **The Template:** You generate a single `Template.note` file and draw generic box boundaries (Hotzones) over areas you desire to be interactive.
2. **Learn Phase:** Within SuperFlow's settings, you press the "Learn Template" button. The plugin quickly scans your template file and identifies every Hotzone.
3. **Dynamic Mapping:** A clean interface lists all the Hotzones discovered. You then visually assign one or multiple actions (Add-ons) to each zone.
4. **JSON Compilation:** Once saved, SuperFlow compiles this mapping logic into a lightning-fast, lightweight JSON map stored on the device.
5. **Execute Phase:** When you take notes on any generic notebook page and hit the manual "Process" button, SuperFlow bypasses the heavy template file entirely. It instantly loads the compiled JSON, cross-references it with your active pen strokes in milliseconds, and executes your configured actions!

## 🧩 Extensibility: The Add-on Architecture

SuperFlow is being designed as a modular platform. The core plugin compiles and processes spatial coordinates, while the execution is handled by modular Add-ons mapping JSON zones to executable features.

Planned base features include Keyword injection, PKM Sync (Obsidian), and Webhook Triggers.

## 💻 Development & Setup

This project is being developed on **Arch Linux** using the official Supernote React Native SDK.

### Prerequisites

- Node.js (LTS)
- Android SDK & ADB
- Supernote device with Sideloading enabled

### Quick Start (Coming Soon)

_Instructions for cloning, building, and installing the APK via ADB will be added once the initial boilerplate is fully tested._

## 🤝 Contributing

Contributions, advice, and pull requests are highly welcome! I am specifically looking for:

- **SDK Experts:** Advice on the best practices for handling file reading and metadata injection safely.
- **TypeScript/React Native Devs:** Help with structuring the modular Add-on architecture.

## 📄 License

MIT License. See `LICENSE` for more information.

---

_Let's make our notes work for us._
