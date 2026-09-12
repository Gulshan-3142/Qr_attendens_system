# 🚀 SmartQR Attendance + Student Attendance Analytics System

A production-ready, enterprise-grade College Attendance Management System featuring dual interfaces:
1. **Student Android Application** built with **Kotlin, Jetpack Compose, Material 3, CameraX, Google ML Kit, FusedLocationProviderClient, and Room Offline Caching**.
2. **Faculty & Admin Real-Time Web Portal** built with **Node.js, Express, SQLite (WAL mode), Tailwind CSS, Lucide Icons, and Server-Sent Events (SSE)**.

---

## 🏛️ System Architecture

```
                                  +----------------------------------------------------+
                                  |         FACULTY / ADMIN WEB PORTAL (Live)          |
                                  | - Classroom Session Creator (GPS & Radius Picker)  |
                                  | - Dynamic Rolling QR Generator (30s/60s Countdown) |
                                  | - Real-Time Student Check-in Feed (SSE)            |
                                  | - Manual Attendance Override & CSV Export          |
                                  +-------------------------+--------------------------+
                                                            |
                                                            v
+-----------------------------+               +-------------+--------------------------+
|     STUDENT ANDROID APP     |  REST / JWT   |            BACKEND REST API            |
| (Jetpack Compose & Room)    | ------------> | (Express + SQLite / PostgreSQL)        |
| - Student Verification      |               | - 8-Step Verification Pipeline         |
| - CameraX + ML Kit Scanner  |               | - Server-Side Haversine Geofence       |
| - GPS Accuracy & Anti-Mock  |               | - Dynamic Signed Token Verification    |
| - 5 Attendance Calculators  |               | - Authoritative Attendance Engine      |
| - Day Planner & Roadmap     |               | - Academic Calendar & Holiday Logic    |
+-----------------------------+               +----------------------------------------+
```

---

## 🔐 8-Step Server-Side Attendance Verification Pipeline

When a student scans a classroom QR code, the Android app captures the dynamic QR token, high-accuracy GPS coordinates, and mock provider integrity flags, sending them to `POST /api/attendance/scan`:

1. **Student Authorization**: Validates student identity and subject enrollment.
2. **Anti-Spoofing Check**: Detects simulated/mock GPS locations (`isMockLocation == true`) and rejects immediately.
3. **GPS Accuracy Filter**: Ensures device accuracy meets the threshold ($\le 25\text{m}$).
4. **QR Signature & Nonce Verification**: Validates the cryptographic HMAC-SHA256 signature and freshness.
5. **Dynamic Expiry Check**: Rejects expired tokens ($> 60\text{s}$).
6. **Geofence Calculation**: Computes exact geodesic distance using the Haversine formula:
   $$d = 2R \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta \lambda}{2}\right)}\right)$$
   Rejects if $d > \text{allowedRadius}$ (e.g. $47\text{m} > 20\text{m}$) with exact distance feedback.
7. **Duplicate Attendance Check**: Prevents scanning twice for the same class session.
8. **Atomic Record Creation & Live Broadcast**: Dispatches real-time SSE event to the teacher dashboard.

---

## 📱 Features

### 1. Student Android App (Jetpack Compose)
- **Onboarding & Verification**: Validates Name, Roll Number (`22CS101`), and College ID (`C2022101`) against university records.
- **Modern Dashboard**: Welcome header, Student profile card with verification badge, circular animated attendance % gauge, Present/Absent/Total classes, and Average classes/day.
- **CameraX + ML Kit Scanner**: Laser-sweep overlay, flashlight toggle, real-time GPS accuracy badge, and multi-step verification modal.
- **All 5 Attendance Calculators**:
  1. *Leave-Day Calculator*: Calculates safe missable classes while maintaining target % ($\ge 75\%$).
  2. *Skip-Class Calculator*: Interactive slider simulating the impact of skipping $N$ classes.
  3. *Target Attendance Calculator*: Computes consecutive classes needed to achieve a target %.
  4. *Exact Day Planner*: Computes academic days and calendar dates required, with a visual roadmap.
  5. *Holiday Impact Preview*: Shows how institutional holidays affect recovery timelines.
- **Attendance History**: Search and filter by subject, status (Present/Absent/Late), and date range.
- **Offline Mode**: Local Room DB cache allows instant dashboard loading without network connectivity.

### 2. Faculty & Admin Web Dashboard
- **Session Creator**: Configure subject, classroom name, GPS coordinates, and custom geofence radius ($15\text{m}$, $20\text{m}$, $35\text{m}$, $50\text{m}$).
- **Dynamic Projector QR**: High-resolution scannable QR code with a 60-second animated rotating countdown progress bar and auto-refresh.
- **Real-Time Live Monitor**: Live student check-in stream, present/absent counters, manual check-in toggle, and CSV report export.

---

## ⚡ Quick Start & Setup

### 1. Backend Server Setup

```bash
cd backend
npm install
npm run seed     # Pre-populates sample students, subjects, academic calendar, and past 30 days history
npm start        # Starts backend server at http://localhost:3000
```

- **Faculty Web Portal**: Open `http://localhost:3000` in your web browser.
- **Faculty Login**: `rajesh.kumar@college.edu` / `password123`
- **Pre-Seeded Students**:
  - `22CS101` / `C2022101` — Aarav Sharma (~82.5% attendance)
  - `22CS102` / `C2022102` — Priya Patel
  - `22CS103` / `C2022103` — Rohan Gupta
  - `22CS104` / `C2022104` — Ananya Verma
  - `22CS105` / `C2022105` — Vikram Malhotra

### 2. Run Backend Automated Test Suite

```bash
cd backend
npm test
```

### 3. Android Application Setup & Build

1. Open **Android Studio** (Hedgehog or newer).
2. Select **Open an Existing Project** and choose the `android` folder:
   `/Users/gulshan/.gemini/antigravity/scratch/qr-attendance-system/android`
3. Allow Gradle to sync dependencies.
4. **Backend URL Configuration**:
   - For Android Emulator: Default `http://10.0.2.2:3000/api/` connects automatically to localhost.
   - For Physical Device: Update the API URL under the app's **Profile Screen** to your machine's LAN IP (e.g. `http://192.168.1.50:3000/api/`).
5. Run the app on an Android Emulator or connected physical device ($API \ge 26$, Android 8.0+).

### 4. Generating APK in Android Studio

- **Debug APK**:
  `Build` > `Build Bundle(s) / APK(s)` > `Build APK(s)`
  (Generated at `android/app/build/outputs/apk/debug/app-debug.apk`)
- **Release APK**:
  `Build` > `Generate Signed Bundle / APK...`

---

## 📡 REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/student/verify` | Verify student Roll No + College ID |
| `POST` | `/api/auth/teacher/login` | Teacher sign-in |
| `GET` | `/api/student/profile` | Retrieve student profile and enrollments |
| `GET` | `/api/student/attendance/summary` | Authoritative overall attendance % and counts |
| `GET` | `/api/student/attendance/subjects` | Subject-wise breakdown and trends |
| `GET` | `/api/student/attendance/history` | Paginated and filtered attendance logs |
| `GET` | `/api/student/analytics/trends` | 7-day, 30-day, and semester trend datasets |
| `POST` | `/api/attendance/scan` | 8-step QR + GPS Geofence attendance recording |
| `POST` | `/api/student/calculators/leave` | Compute leave-day capacity |
| `POST` | `/api/student/calculators/skip` | Simulate skipping classes |
| `POST` | `/api/student/calculators/target` | Compute consecutive classes needed for target % |
| `POST` | `/api/student/calculators/planner` | Compute days required & roadmap |
| `POST` | `/api/student/calculators/holiday-impact` | Preview institutional holiday impact |
| `POST` | `/api/teacher/sessions/create` | Create classroom session with GPS & geofence |
| `POST` | `/api/teacher/sessions/:id/qr` | Refresh dynamic signed QR token |
| `GET` | `/api/teacher/sessions/:id/live-attendance` | Get real-time attendee roster |
| `GET` | `/api/teacher/sessions/:id/stream` | Server-Sent Events stream for live check-ins |
| `POST` | `/api/teacher/sessions/:id/manual-attendance` | Manual attendance override |
| `POST` | `/api/teacher/sessions/:id/close` | End class attendance session |
