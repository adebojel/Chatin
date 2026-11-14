# Chatin product and engineering roadmap

## Product target

Build a dependable Android-first messenger with the interaction quality users expect from mainstream messaging products, without copying their branding or proprietary implementation.

## Experience requirements

### Inbox and navigation
- Conversation list with avatar, title, last-message preview, time, unread count, mute/pin/archive state.
- Search across contacts and conversations.
- Clear empty, loading, offline, retry, and error states.
- Responsive layouts for compact phones and larger screens.
- Accessible labels, touch targets, contrast, dynamic font scaling and TalkBack semantics.

### Conversation
- Text, emoji, image, video, document, and voice-note composition.
- Message actions: reply, forward, copy, delete-for-me, delete-for-everyone (policy-controlled), react, and select multiple.
- Sent/delivered/read state with honest state transitions.
- Typing indicator and online/last-seen controls with privacy settings.
- Group creation, member roles, invite flow, and membership changes.
- Pagination and stable ordering; do not load an unbounded conversation history.

### Reliability
- Room as the local source for rendered conversation state.
- Durable outbox with retry/backoff and idempotency keys.
- Firestore listeners scoped to authorized chats and removed when no longer needed.
- Reconcile server acknowledgements with optimistic UI; show failed messages and retry controls.
- FCM notifications with deep links and notification privacy controls.
- Handle process death, airplane mode, reconnect, duplicate delivery and clock skew.

### Calls
- WebRTC media and signaling state machine.
- STUN/TURN configuration and network fallback.
- Permission denial, Bluetooth/audio routing, interruption and background lifecycle handling.
- Call history must reflect actual call outcomes, not only local UI state.

### Trust and privacy
- Firebase Auth UID as identity; verified account recovery.
- Least-privilege Firestore Rules and App Check.
- Block/report, contact discovery consent, profile visibility and session/device management.
- Private media access and safe file validation.
- Audited E2EE protocol with key transparency/verification before any E2EE marketing claim.
- Data export/deletion and retention policy.

## Implementation order

1. **Baseline and CI:** reproducible Gradle/Node builds, lint, unit tests, dependency scanning.
2. **Identity migration:** Firebase UID, account/profile model, auth lifecycle and abuse controls.
3. **Data contract:** chat membership, message schema, Firestore Rules and emulator tests.
4. **Core chat:** inbox, pagination, send/ack/retry, offline outbox, receipts.
5. **Media:** upload/download lifecycle, thumbnails, size/type validation and private access.
6. **Groups and discovery:** group membership, contacts, search, block/report.
7. **Notifications and calls:** FCM and WebRTC end-to-end tests.
8. **Privacy/security release gate:** E2EE audit, threat model, penetration test, data deletion, backup/restore.
9. **Polish:** accessibility, localization, performance, battery/network profiling and release automation.

## Current known blockers

- Firebase chat documents currently store readable message text; this is not E2EE.
- Firestore chat metadata does not yet establish a reliable participant UID authorization model.
- Profile documents currently use phone numbers as document IDs.
- The backend has separate PostgreSQL/JWT identity and Firebase Authentication identity; a deliberate identity bridge/migration is required.
- WebRTC, push delivery, SMS provider integration, and production media storage must be validated independently.
- A previous security remediation disabled unsupported phone-change OTP and media upload paths until providers are configured.

## Definition of done

A feature is done only when its UI, data contract, authorization, failure states, tests, accessibility behavior, and production configuration are implemented and verified. A compile-only success does not establish production readiness.
