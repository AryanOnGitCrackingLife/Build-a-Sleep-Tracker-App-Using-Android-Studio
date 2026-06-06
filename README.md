# 🌙 Sleep Pattern Tracker

A user-friendly Android application designed to help users monitor and improve their sleep habits. Built with **Kotlin**, this app features real-time sleep tracking, dynamic sleep stage estimations, bedtime goal setting, and historical data persistence using SharedPreferences.

---

## 🎯 Project Objective

To design and implement a functional Android application that monitors sleep patterns through start/stop tracking, duration calculation, and bedtime goal setting. The app emphasizes an intuitive, night-friendly UI, data persistence for historical records, and basic analytics to provide users with visual feedback on their sleep health.

---

## ⚙️ Tech Stack

* **Environment:** Android Studio
* **Language:** Kotlin
* **Minimum SDK:** API 24 (Android 7.0 Nougat)
* **UI/Layout:** XML (`activity_main.xml`), `Theme.AppCompat.Light.DarkActionBar` (Night-mode friendly)
* **Data Storage:** Android `SharedPreferences`

---

## 🚀 Features & Implementation

### 1. User Interface (UI) Design
* **Night-Friendly Theme:** Utilizes dark backgrounds and light text to minimize eye strain during nighttime use.
* **Live Tracking Display:** Integrates an Android `Chronometer` widget to display real-time elapsed sleep duration.
* **Interactive Controls:** Includes Start, Stop, Reset, and Settings buttons with built-in error prevention (e.g., blocking the Stop button if tracking hasn't started).

### 2. Core Tracking Logic & Sleep Stages
* **Time Calculation:** Tracks exact sleep start times and calculates elapsed durations using `System.currentTimeMillis()`.
* **Dynamic Stage Estimation:** Automatically updates the UI with estimated sleep stages (e.g., shifting to "Deep Sleep" after 1.5 hours of continuous tracking).
* **Reset Functionality:** Allows users to clear the current session data and reset the chronometer instantly.

### 3. Data Persistence & Analytics
* **Historical Records:** Saves daily sleep logs locally using `SharedPreferences`, ensuring user data persists between app sessions.
* **Analytics Dashboard:** Features a summary screen that calculates and displays the user's average historical sleep duration.
* **Bedtime Goal Setting:** Utilizes a `TimePicker` to allow users to set target bedtimes, comparing their actual sleep duration against their custom goals (e.g., "You slept 1.5h less than your goal").

---

## 🛠️ Local Installation & Setup

**1. Clone the repository**
```bash
git clone [https://github.com/AryanOnGitCrackingLife/Sleep-Pattern-Tracker.git](https://github.com/AryanOnGitCrackingLife/Sleep-Pattern-Tracker.git)
