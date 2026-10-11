# Aerio - Patient Monitoring System

**Aerio** is a native Android application engineered in Kotlin and Jetpack Compose for continuous clinical patient monitoring using the Lepu AP-20 Pulse Oximeter BLE hardware sensor.

---

## Google Play Store Listing Information

> [!TIP]
> Use the structured information below directly when filling out your Google Play Console store listing and content rating forms.

### Basic Store Metadata
* **Application Title**: `Aerio - Patient Monitor`
* **Short Description** (Max 80 characters):
  `Real-time pulse oximetry monitoring & automated cloud sync for clinical stations.`
* **Category**: Medical / Health & Fitness
* **Content Rating**: Everyone / Clinical Staff
* **Target Audience**: Healthcare professionals, clinical staff, and patient monitoring stations.

### Full Store Description (Google Play Copy)
```
Aerio is a professional-grade Android patient monitoring application designed for real-time vital sign tracking with the Lepu AP-20 Pulse Oximeter sensor.

Key Features:
- Real-Time Vital Signs Tracking: Continuous monitoring of Oxygen Saturation (SpO₂ %), Pulse Rate (BPM), and Perfusion Index (PI).
- Smart Sensor Discovery: Intelligent Bluetooth Low Energy (BLE) scanning that filters specifically for AP-20 clinical pulse oximeter hardware.
- Offline-First Architecture: Validated vital sign measurements are queued locally in a secure SQLite Room database and automatically synchronized to the cloud backend every 30 seconds.
- Local Patient Persistence: Patient assignments are securely retained on the device, eliminating repetitive setup between sessions.
- Modern Clinical UI: Responsive Material 3 user interface designed for intuitive navigation on Android smartphones.
- Encrypted Security: JWT token authentication with Android KeyStore encrypted preferences for secure data transmission.

Aerio empowers healthcare stations with continuous, accurate, and seamless vital signs monitoring.
```

### Required Store Privacy Policy & Permission Declarations
When publishing Aerio on Google Play Console, declare the following permissions:
1. **Bluetooth & Location Services** (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`):
   - *Declaration*: Required strictly for discovering and pairing with nearby Lepu AP-20 Bluetooth Low Energy pulse oximeter hardware.
2. **Foreground Services** (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`):
   - *Declaration*: Required for maintaining uninterrupted background data collection and periodic 30-second cloud batch uploads while monitoring active patient vital signs.
3. **Internet & Network Access** (`INTERNET`, `ACCESS_NETWORK_STATE`):
   - *Declaration*: Required for transmitting encrypted batch monitoring data to the backend health server.

---

## Architecture Overview

The system architecture connects the Lepu AP-20 BLE pulse oximeter sensor through the Aerio Android smartphone app to the Node.js/Express backend server and MongoDB database:

```
+------------------+         +-------------------------------------------+         +------------------------+
|  Lepu AP-20 BLE  |  BLE    |           Aerio Android App               |  HTTPS  |   Node.js/Express      |
|  Pulse Oximeter  | ------> |  (BLE Service -> Room Queue -> Sync)      | ------> |   Cloud Backend        |
+------------------+         +-------------------------------------------+         +------------------------+
                                                                                       |
                                                                                       v
                                                                                   +------------------------+
                                                                                   |    MongoDB Database    |
                                                                                   +------------------------+
```

### Key Technical Characteristics
* **Language & UI**: 100% Native Kotlin, Jetpack Compose, Material Design 3.
* **Architecture Pattern**: MVVM (Model-View-ViewModel) with Kotlin Coroutines and StateFlow.
* **Local Persistence**: Room SQLite Database for offline queueing, EncryptedSharedPreferences (AES-256 GCM) for secure JWT token storage.
* **Networking**: Retrofit 2, OkHttp 4 with custom `AuthInterceptor` and HTTP logging.
* **BLE Framing Protocol**: Custom 12-byte packet framing decoder (`0xAA 0x55` header, `0x08` parameter packet ID).
* **Background Sync Engine**: Foreground Service with `dataSync` type for 30-second continuous cloud batch uploads.

---

## App Flow & Navigation

Aerio enforces a complete real-world clinical workflow:

1. **Station Authentication**: Login screen authenticates against `POST /api/auth/login`. Stores JWT token securely in EncryptedSharedPreferences.
2. **Station Registration**: Automatically registers device key with `POST /api/tablets/register` to acquire station `tabletId`.
3. **Patient Assignment Persistence**: Retrieves active patient assignment via `GET /api/tablets/me` or loads locally cached patient details from `SessionManager`. Patient stays assigned across app launches.
4. **Targeted BLE Sensor Discovery**: `BleDeviceScanner` filters nearby Bluetooth devices, showing **only relevant AP-20 pulse oximeters**.
5. **Real-time Live Dashboard**: Displays SpO₂ %, Pulse Rate (BPM), Perfusion Index, live BLE connection badge, and cloud sync queue status.
6. **Automated 30-Second Batch Sync**: `SyncCoordinator` uploads pending Room records every 30 seconds to `POST /api/monitoring`.
7. **Complete Logout Cleanup**: Clicking Logout clears JWT tokens, active patient cache, station session, and deletes local database queue records before returning to the login screen.

---

## SDK & Environment Configuration

* **Minimum SDK**: `minSdk 26` (Android 8.0 Oreo) - Guarantees modern BLE APIs, EncryptedSharedPreferences, and Foreground Services.
* **Target SDK**: `targetSdk 35` (Android 15) - Compliant with latest Android security policies, runtime permissions (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`), and foreground service type declarations (`dataSync`).
* **Compile SDK**: `compileSdk 37` - Supports latest Jetpack Compose libraries.
* **Toolchain**: Java 17 / Kotlin 2.2.

---

## API Configuration & Network Endpoints

### Backend Endpoints Table

| Method | Endpoint | Description | Header Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Authenticate station user | None |
| `POST` | `/api/tablets/register` | Register station device key | `Authorization: Bearer <token>` |
| `GET` | `/api/tablets/me` | Fetch station details & active patient | `Authorization: Bearer <token>` |
| `POST` | `/api/tablets/:tabletId/patient` | Assign patient to station | `Authorization: Bearer <token>` |
| `PATCH` | `/api/patients/:patientId/status` | Deactivate patient status | `Authorization: Bearer <token>` |
| `POST` | `/api/monitoring` | Upload batch monitoring readings | `Authorization: Bearer <token>` |

---

## Unit Testing & Build Instructions

### Building the APK
To assemble a debug build:
```bash
./gradlew :app:assembleDebug
```

To assemble a release build for Play Store upload:
```bash
./gradlew :app:bundleRelease
```

### Running Unit Tests
To run unit tests for AP-20 packet decoder framing and split packet handling:
```bash
./gradlew :app:testDebugUnitTest
```
Tests cover AP-20 frame extraction, header detection (`0xAA 0x55`), split packet reassembly, waveform filtering (`0x07`), and out-of-range vital sign validation.
