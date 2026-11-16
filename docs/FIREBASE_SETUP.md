# Firebase configuration and deployment

This guide describes how to connect the existing Chatin Android client to a Firebase project. It does not claim that a Firebase project is provisioned or that production rules have been deployed.

## 1. Firebase project

1. Open the Firebase Console and select the intended project (the current checked-in Android configuration points to project ID `chatin-43eed`; verify ownership and environment before using it).
2. Register the Android application with package name `com.aistudio.chatin.kxqrvz`.
3. Download the Android `google-services.json` for that exact package and put it in `app/google-services.json`.
4. Confirm that the Google Services Gradle plugin is enabled in `app/build.gradle.kts` and that the Firebase BoM and Auth/Firestore libraries resolve.
5. Restrict the Firebase API key in Google Cloud Console to the required Android application (package + signing certificate fingerprints) and APIs. A Firebase client API key is not an authorization boundary; Security Rules and App Check remain essential.
6. Keep debug and production Firebase projects separate where possible.

## 2. Authentication

The Android Firebase manager uses `PhoneAuthProvider.verifyPhoneNumber` and `signInWithCredential`.

In Firebase Console:

- Enable **Authentication → Sign-in method → Phone**.
- Configure SMS region policy and authorized domains/settings as required.
- Add test phone numbers only for development; never use predictable test codes in production.
- Register SHA-1 and SHA-256 fingerprints for debug and release signing certificates when required by Firebase/Google sign-in flows.
- Configure Firebase App Check for the Android app and enforce it only after validating legitimate app traffic.

The app should treat Firebase Auth UID (`FirebaseUser.uid`) as the canonical user identifier. Do not use a phone number as a Firestore document ID: phone numbers are personal data, can change, and are easy to enumerate.

## 3. Firestore database

Create Cloud Firestore in the intended region. The current Android code reads and writes:

```text
users/{phoneNumber}
chats/{chatId}
chats/{chatId}/messages/{messageId}
```

The current `chats/{chatId}` metadata writer does not yet consistently write a verified participant UID list, and message documents currently include a plaintext `text` field. Therefore, do not deploy permissive rules and do not put production/private conversations into this database yet.

### Required application changes before production rules

- Migrate `users/{phoneNumber}` to `users/{firebaseAuthUid}`.
- Create each chat with a `memberUids` array containing the authenticated UIDs of all participants.
- Validate chat membership on every message read/write and metadata update.
- Validate message sender UID equals `request.auth.uid`; reject client-supplied ownership/status fields that can be forged.
- Use server timestamps and bounded field sizes; validate allowed message types.
- Store media in private object storage and authorize access using short-lived URLs or authenticated proxying.
- Implement a reviewed E2EE protocol before claiming message content is hidden from the service. Do not invent a home-grown cryptographic protocol.

### Illustrative rules pattern — not deployable until the data migration above is complete

The following demonstrates the intended membership boundary. It deliberately denies all access until the application schema and validation are completed. Do not replace it with broad `allow read, write: if request.auth != null` rules.

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function signedIn() {
      return request.auth != null;
    }

    function isChatMember(chatId) {
      return signedIn()
        && exists(/databases/$(database)/documents/chats/$(chatId))
        && request.auth.uid in
          get(/databases/$(database)/documents/chats/$(chatId)).data.memberUids;
    }

    match /users/{userId} {
      allow read, create, update: if signedIn() && request.auth.uid == userId;
      allow delete: if false;
    }

    match /chats/{chatId} {
      allow read: if isChatMember(chatId);
      allow create, update, delete: if false;

      match /messages/{messageId} {
        allow read: if isChatMember(chatId);
        allow create, update, delete: if false;
      }
    }
  }
}
```

This is a deny-by-default migration scaffold, not a complete chat ruleset. Add narrowly scoped server/admin operations and validated client writes only after the data model is migrated. Test rules with the Firebase Emulator Suite before deployment.

## 4. Firestore Rules yang tersimpan di repository

Repository menyediakan `firebase.json` dan `firestore.rules` sebagai konfigurasi awal deny-by-default.

- Profil hanya dapat dibaca/ditulis oleh pemilik UID yang cocok dengan Firebase Auth; nomor telepon harus cocok dengan claim Phone Auth.
- Penghapusan profil dari klien ditolak.
- Seluruh akses chat dan pesan sengaja ditolak sampai layanan tepercaya membuat chat dan menetapkan `memberUids`.
- Koleksi yang belum didefinisikan juga ditolak.

**Penting:** memasang rules ini akan memblokir jalur kirim/baca chat Android yang saat ini menulis langsung dari klien. Ini disengaja demi fail-closed security. Jangan deploy rules ke project aktif sebelum memahami dampaknya dan menyiapkan migrasi server-side. Jalankan Firebase Emulator Suite dan selesaikan [matriks pengujian rules](FIRESTORE_RULES_TEST_PLAN.md) terlebih dahulu.

Untuk deployment setelah migrasi dan tes:
```bash
firebase emulators:start --only firestore
firebase deploy --only firestore:rules
```

Perintah deploy hanya dilakukan oleh maintainer pada project yang benar setelah review dan uji emulator; jangan deploy ke project produksi saat chat service belum siap.

## 5. Firebase services expected by the product

| Service | Intended use | Current status in repository |
| --- | --- | --- |
| Firebase Authentication | Phone verification and user identity | SDK integration exists; console settings must be verified |
| Cloud Firestore | Realtime chat/profile/presence | SDK integration exists; current schema/rules need hardening |
| Firebase App Check | Reduce abuse from unauthorized clients | Dependencies exist; provider setup and enforcement must be completed |
| Firebase Cloud Messaging | Push notifications | Not confirmed as implemented |
| Cloud Storage for Firebase | Media storage | Not confirmed as the active Android upload path |

Do not assume that adding a dependency alone activates a service.

## 5. Operational checklist

- [ ] Verify Firebase project ownership and billing/quotas.
- [ ] Verify package name and signing fingerprints.
- [ ] Enable Phone Authentication and test on a physical device.
- [ ] Migrate user IDs to Firebase Auth UID.
- [ ] Add and validate chat membership fields.
- [ ] Implement strict Firestore Rules and emulator tests.
- [ ] Configure App Check and monitor rejected requests.
- [ ] Add FCM token lifecycle and notification delivery tests if push is enabled.
- [ ] Configure private media storage and retention policy.
- [ ] Review privacy notice, data retention, account deletion, and Indonesian personal-data obligations.
- [ ] Run Android build, unit tests, integration tests, and real-device tests.

## 6. Credentials and repository hygiene

Never commit `.env`, service-account JSON, private signing keys, keystore passwords, JWT secrets, SMS provider credentials, or S3 secret keys. The Android `google-services.json` contains client configuration rather than a server service-account private key, but its API key must still be restricted. If an unrestricted key has been exposed, rotate/restrict it in Google Cloud Console and review usage.
