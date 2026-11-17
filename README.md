# Notification Inbox

A native Android app that receives Firebase Cloud Messaging (FCM) push notifications, shows them as system notifications, and keeps a persistent local **Notification Inbox** with read/unread state.

Kotlin · Jetpack Compose · MVVM · Room · Hilt · Firebase Cloud Messaging · Coil

---

## Features

**Required**

| Requirement | How it's handled |
|---|---|
| Receive FCM push notifications | `InboxMessagingService` (extends `FirebaseMessagingService`) |
| System notification on channel `assignment` / `Assignment` showing icon, title and description | `SystemNotifier`; the channel is created at app start |
| Android 13+ notification permission | Requested on first launch. If it's denied, a banner links to the app's notification settings |
| Payload `id`, `title`, `description`, `bannerImg`, `timestamp` | Parsed and validated by `NotificationPayloadParser` |
| Inbox shows banner, title, description, timestamp and read/unread state | `InboxScreen` |
| History survives app restarts | Room database (`notification_inbox.db`) |
| System notification → Inbox | The notification's `PendingIntent` opens `MainActivity`, which lands on (or returns to) the inbox |
| Inbox → Details | Compose Navigation (`details/{id}`) |
| Opening details marks the item as read and the inbox updates | `DetailsViewModel` marks it read, and Room's `Flow` pushes the change to the inbox |
| No duplicate entries for the same `id` | `id` is the Room primary key, inserted with `OnConflictStrategy.IGNORE` |
| Errors and edge cases handled without crashing | See [Error and edge-case handling](#error-and-edge-case-handling) |

**Optional enhancements included**

- Unread count in the top bar and on the filter chip
- Mark all as read
- Delete a single notification (from the details screen)
- Clear the inbox, with a confirmation dialog
- All / Unread filter, sorted newest first
- Empty states (for "no notifications" and "all caught up")
- Relative timestamps in the list ("5 min. ago", "Yesterday"), which refresh every minute, and the full date and time on the details screen
- Placeholder shown while an image loads, and kept if it's missing, invalid or fails to load
- Notifications that FCM displays itself while the app is in the background are added to the inbox when tapped
- Opening a notification's details clears its entry from the system tray
- "Copy FCM token" menu action, which makes testing easier
- Material 3 with dynamic colour on Android 12+, dark theme, and edge-to-edge layout

---

## Architecture

Single-activity MVVM with a repository, wired together with Hilt.

```
FCM ──► InboxMessagingService ──► NotificationPayloadParser ──► NotificationRepository ──► Room
                 │                                                        ▲
                 └──► SystemNotifier (system tray)                        │ Flow
                                                                          │
              MainActivity ─► AppNavHost ─► InboxScreen   ◄── InboxViewModel
                                        └─► DetailsScreen ◄── DetailsViewModel
```

```
app/src/main/java/com/bookverse/notificationinbox/
├── NotificationInboxApp.kt        Hilt application; creates the notification channel
├── model/InboxNotification.kt     UI model
├── data/
│   ├── local/                     Room: entity, DAO, database
│   └── repository/                NotificationRepository (single source of truth)
├── di/DatabaseModule.kt           Hilt bindings for Room
├── fcm/
│   ├── InboxMessagingService.kt   Receives messages and stores them
│   ├── NotificationPayloadParser  Validates and normalises raw payload data (pure Kotlin, unit-tested)
│   └── SystemNotifier.kt          Channel, system notification, PendingIntent
└── ui/
    ├── MainActivity.kt            Single activity; handles notification-tap intents
    ├── navigation/AppNavHost.kt   Inbox → Details
    ├── inbox/                     Screen, ViewModel, permission banner
    ├── details/                   Screen and ViewModel
    ├── components/                BannerImage, EmptyState
    ├── util/TimeFormat.kt
    └── theme/
```

**Key decisions**

- **Room as the single source of truth.** Screens observe `Flow`s from Room, so every change shows up immediately on every screen without manual refreshes. This includes a new message arriving while the inbox is open, marking items read, and deleting them.
- **Duplicates are handled by the database.** The payload `id` is the primary key and inserts use `IGNORE` rather than `REPLACE`, so a duplicate can never overwrite an existing row or reset its read state. The insert result tells the service whether the message was new; duplicates don't post another system notification.
- **Parsing is isolated.** All payload validation lives in a pure Kotlin object with no Android dependencies. That keeps the service small and lets every edge case be covered by fast JVM unit tests.
- **The insert in the service is synchronous.** `onMessageReceived` already runs on a background thread, and Android may stop the process soon after it returns. Blocking until the insert finishes (a few milliseconds) guarantees the message is saved.

---

## Setup and build

### Requirements

- Android Studio Ladybug (2024.2) or newer
- JDK 17. The JDK bundled with Android Studio works.
- A device or emulator running Android 7.0 (API 24) or newer, with Google Play services

### 1. Firebase configuration

`google-services.json` is **not** in the repository, because it is project-specific and shouldn't be in a public repo. To add your own:

1. Open the [Firebase console](https://console.firebase.google.com/) and create a project, or use an existing one.
2. Add an **Android app** with the package name `com.bookverse.notificationinbox`.
3. Download `google-services.json` and put it in the **`app/`** folder:
   ```
   NotificationInbox/app/google-services.json
   ```
4. Cloud Messaging needs no further setup. The FCM HTTP v1 API is enabled by default for new projects.

The build fails with *"File google-services.json is missing"* until this file is in place.

### 2. Build

Open the project in Android Studio and run the `app` configuration, or use the command line:

```bash
./gradlew assembleDebug          # APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug           # build and install on a connected device
./gradlew testDebugUnitTest      # run unit tests
```

### 3. Get the device's FCM token

Launch the app, allow notifications, then open **⋮ → Copy FCM token**. The token is also printed to Logcat in debug builds (tag `InboxMessagingService`), but only when a new token is issued.

---

## Sending test notifications

### Option A: script (recommended, sends data-only messages)

`tools/payloads/` contains a sample payload for each scenario:

| File | Scenario |
|---|---|
| `01_sample.json` | The payload from the assignment spec |
| `02_missing_banner.json` | No `bannerImg` |
| `03_invalid_banner.json` | `bannerImg` is not a URL |
| `04_broken_image.json` | Valid URL that returns 404 |
| `05_minimal.json` | Title only, with no `id` or `timestamp` |
| `06_seconds_timestamp.json` | Timestamp in epoch seconds |
| `07_invalid_timestamp.json` | Non-numeric timestamp |
| `08_notification_and_data.json` | `notification` + `data` message, for the background case |

Send `01_sample.json` twice to test duplicate handling.

```bash
# Authenticate once, with a service account that has the Firebase Cloud Messaging API Admin role
gcloud auth activate-service-account --key-file=/path/to/service-account.json

./tools/send_notification.sh <firebase-project-id> "<device-token>" tools/payloads/01_sample.json
```

### Option B: Firebase console

**Engage → Messaging → New campaign → Notifications.** Put the payload keys (`id`, `title`, `description`, `bannerImg`, `timestamp`) under **Additional options → Custom data**, then send a test message to the device token.

The console always includes a `notification` block. While the app is in the background, Android shows these messages itself, and they reach the inbox only when tapped (see [Assumptions](#assumptions)).

---

## Error and edge-case handling

| Case | Behaviour |
|---|---|
| `bannerImg` missing, blank or `"null"` | A placeholder is shown, and all other content displays normally |
| `bannerImg` invalid (not an absolute http/https URL) | Treated as missing |
| Image fails to load (404, offline, not an image) | Coil reports an error and the placeholder stays. Text is unaffected |
| `title` or `description` missing | Shown as "Untitled notification" or "No description provided". A message with no title, no description and no banner is ignored |
| `id` missing | The FCM message id is used, because it stays the same if FCM redelivers the message. If that's also missing, a unique id is generated |
| `timestamp` missing, invalid, zero or negative | The message's sent time is used, or the time it was received |
| `timestamp` in seconds | Converted to milliseconds |
| Duplicate `id` | Not inserted again, read state is kept, and no second system notification is posted |
| App restart or process death | Data is in Room. Mark-as-read runs once per visit to the details screen, not again on rotation |
| Notification permission denied | The message is still saved to the inbox, and a banner explains how to turn notifications on |
| Database error | Logged. The inbox shows an empty state instead of crashing, and the system notification is still shown |
| Server-provided ids containing `/`, spaces, etc. | URL-encoded before being used in navigation routes |
| Details screen for an id that no longer exists | A "no longer available" message is shown |

---

## Libraries

| Library | Purpose |
|---|---|
| Jetpack Compose (Material 3) | UI |
| Navigation Compose | Inbox → Details navigation |
| Lifecycle ViewModel and Runtime Compose | State holders and lifecycle-aware collection |
| Room | Local persistence |
| Hilt | Dependency injection |
| Firebase Cloud Messaging (BoM) | Push notifications |
| kotlinx-coroutines-play-services | `await()` for the FCM token `Task` |
| Coil | Image loading with error states |
| JUnit 4 | Unit tests |

Versions are in `gradle/libs.versions.toml`.

---

## Assumptions

- **Messages are sent as data-only messages** (a `data` block with no `notification` block), as in the sample payload. This is the only way for the app to handle every message itself in both the foreground and the background.
- **Payload values are strings**, because FCM `data` values are always strings.
- `timestamp` is the time the notification was created, in epoch milliseconds. Values that look like seconds are converted.
- `bannerImg` is optional. `title` and `description` are expected, but the app copes without them.
- A duplicate should not alert the user again. Delivering the same `id` a second time is treated as a redelivery, not a new event.
- The inbox is sorted by payload `timestamp`, newest first.
- No backend is needed. The FCM token can be copied from the app for testing.

## Known limitations

- **Notification + data messages while the app is in the background:** Android shows these itself, and the app's code doesn't run until the user taps one. Such a message is added to the inbox when tapped (`MainActivity` reads it from the launch intent), but not if it's dismissed without being tapped. Data-only messages don't have this limitation.
- **Cleartext `http://` banner URLs** are blocked by Android 9+ network security defaults, so they show the placeholder.
- The banner image isn't shown in the system notification itself (as a `BigPictureStyle`), only in the app.
- **Relaunching:** Android may deliver an FCM-shown notification's tap to an already-running app without its extras. In that case the app still opens on the inbox, but that message isn't imported.
- Plain-text content only. HTML or markdown in `description` isn't rendered.
- Some OEM battery optimisations (e.g. aggressive task killers) can delay or drop FCM messages for apps that have been force-stopped. This is outside the app's control.
