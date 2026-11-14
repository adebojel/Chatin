# Chatin — Modern Messaging

Chatin is an Android-first messaging application with a companion web client and a Node.js real-time backend. The product direction is a familiar, fast messenger inspired by common messaging patterns (conversation inbox, one-to-one and group chats, media sharing, presence, delivery/read state, and voice/video calling), while keeping its own identity and implementation.

> **Project status:** active development. This repository contains working foundations and partially implemented features. It is not yet feature-equivalent to WhatsApp or Telegram and must not be described as production-ready or fully end-to-end encrypted until the remaining work and security tests are complete.

## Repository structure

| Path | Purpose |
| --- | --- |
| `app/` | Native Android application (Kotlin, Jetpack Compose) |
| `app/src/main/java/com/example/data/firebase/` | Firebase Authentication and Cloud Firestore integration |
| `public/` | Browser client served by Express |
| `server.js` | Node.js/Express API and Socket.IO realtime gateway |
| `schema.sql` | PostgreSQL schema used by the backend |
| `gradle/libs.versions.toml` | Android dependency/version catalog |
| `.env.example` | Backend environment variable template |

## Technology

- Android: Kotlin, Jetpack Compose, Coroutines/Flow, Room, Retrofit/OkHttp.
- Firebase: Firebase Authentication (phone verification), Cloud Firestore, Firebase App Check dependencies.
- Backend: Node.js, Express, Socket.IO, PostgreSQL, JWT, bcrypt.
- Web client: static HTML/CSS/JavaScript in `public/`.
- Media storage: optional S3-compatible storage (for example, Cloudflare R2).

## Quick start — Android

1. Install Android Studio with the Android SDK required by the project (compile SDK 36).
2. Open this repository in Android Studio and allow Gradle sync to finish.
3. Configure the Firebase Android app for package `com.aistudio.chatin.kxqrvz`.
4. Download the matching `google-services.json` from Firebase Console and place it at `app/google-services.json`. Do not commit environment-specific configuration if your team policy treats it as private; restrict the Firebase API key to the required Android app and APIs.
5. In Firebase Console, enable the Authentication providers used by the app and create a Cloud Firestore database in the intended region.
6. Apply and test Firestore Security Rules before allowing real users to access the database. Do not use test mode or public read/write rules.
7. Build and run the Android app on a physical device. Phone Auth requires a real device or configured Firebase test phone numbers.

## Quick start — backend

Requirements: Node.js 18+ (use the version supported by your deployment platform), npm, and PostgreSQL.

```bash
cp .env.example .env
npm install
```

Set at least the following variables in the deployment environment (never commit `.env`):

| Variable | Purpose |
| --- | --- |
| `PORT` | HTTP listening port (default 3000) |
| `JWT_SECRET` | Random secret, at least 32 characters |
| `URL_POSTGRESQL_CLOUD_ANDA` | PostgreSQL connection string |
| `ALLOWED_ORIGINS` | Comma-separated trusted web origins |
| `GEMINI_API_KEY` | Optional Gemini AI proxy |
| `S3_ENDPOINT`, `S3_ACCESS_KEY_ID`, `S3_SECRET_ACCESS_KEY`, `S3_BUCKET_NAME` | Optional S3-compatible media storage |

Apply the database schema:

```bash
psql "$URL_POSTGRESQL_CLOUD_ANDA" -f schema.sql
npm start
```

The backend intentionally refuses to start without a sufficiently strong `JWT_SECRET`. Media upload and phone-number-change OTP endpoints remain unavailable until their real providers are configured. Put the service behind HTTPS/TLS termination before exposing it publicly.

## Firebase data currently used by Android

The current Firebase manager uses these paths:

- `users/{phoneNumber}` for profile and presence fields.
- `chats/{chatId}` for conversation metadata.
- `chats/{chatId}/messages/{messageId}` for message documents.

The current message writer stores the `text` field as readable text. **This is not end-to-end encryption.** Do not use real sensitive conversations until authenticated membership checks, strict Firestore Rules, data minimization, and an audited E2EE protocol are implemented. Phone numbers should not be used as public document IDs; migrate to Firebase Auth UID identifiers.

See [Firebase setup and security notes](docs/FIREBASE_SETUP.md).

## Product roadmap

1. **Identity and account security:** Firebase Auth UID as canonical identity, verified phone login, session lifecycle, abuse/rate limiting, App Check.
2. **Messenger experience:** polished inbox, search, chat details, group membership, message composer, reply/forward/reactions, attachments, voice notes, presence and read receipts.
3. **Reliability:** Room-backed local cache, offline outbox, retry with idempotency, pagination, conflict handling, background sync and push notifications.
4. **Privacy and safety:** Firestore least-privilege rules, report/block controls, privacy settings, secure media access, audited E2EE design and key verification.
5. **Calls:** real WebRTC signaling, ICE/STUN/TURN, call permissions, lifecycle handling and network testing.
6. **Quality:** unit/instrumentation tests, emulator/device matrix, CI build, dependency scanning and security review.

## Development checks

For Android, run the Gradle tasks from the repository root:

```bash
./gradlew test
./gradlew assembleDebug
```

For the backend:

```bash
npm install
node --check server.js
npm start
```

A successful static check is not a substitute for integration tests, Firebase Rules emulator tests, device testing, or a penetration test. Record the actual results in pull requests.

## Security reporting

Do not publish credentials, OTP values, private keys, signing files, or production user data in issues. Report vulnerabilities privately to the repository owner and include reproduction steps without exposing real user information.

## License

See the repository license file. If no license file is present, usage and redistribution permissions are not yet explicitly documented.
