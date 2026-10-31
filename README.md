# CircleLocate 🌍📱

**CircleLocate** is a modern, feature-rich Android mobile application built with **Kotlin** designed for tracking and locating phone numbers on an interactive map interface with real-time syncing capabilities.

---

## 🚀 Key Features

* **🔍 Phone Number & Name Search:** Easily input a target name and phone number (complete with country code selection) to retrieve the latest known device location.
* **🗺️ Google Maps Integration:** Visualizes tracked locations, map markers, and route paths on an interactive map view.
* **📜 Search History:** Automatically logs past search queries and tracking progress for quick reference.
* **📍 Device Permissions:** Prompts users securely for GPS and location device access to ensure precise functionality.
* **🌐 Bilingual Support (English & Arabic):** Full language toggling with automatic Right-to-Left (RTL) layout adaptation when Arabic is selected.
* **🌓 Theme Switching:** Seamlessly toggle between **Dark Mode** and **Light Mode**.
* **🔄 Check for Updates:** Built-in update checking function with status feedback dialogs.
* **🎨 Clean UI/UX:** Minimalist design accented with a vibrant signature green navigation bar and buttons.

---

## 🛠️ Tech Stack & Architecture

* **Language:** [Kotlin](https://kotlinlang.org/) (100%)
* **Platform:** Android
* **Build System:** Gradle (Kotlin DSL `build.gradle.kts`)
* **Mapping:** Google Maps SDK for Android

---

## 📁 Project Structure

```text
CircleLocate/
├── app/                  # Main application source code and resources
├── gradle/               # Gradle wrapper and configuration files
├── public/               # Assets and AI Studio metadata
├── build.gradle.kts      # Project-level Gradle configuration
├── settings.gradle.kts   # Module settings and repositories
└── README.md             # Project documentation
