# Firestore Rules test plan

These rules are a secure migration scaffold, not a complete production chat policy.

## Expected behavior

- An unauthenticated client cannot read or write any document.
- An authenticated user can read their own `users/{uid}` profile.
- A user can create/update only their own profile document, with the UID and phone number matching Firebase Authentication claims.
- A user cannot read or modify another user's profile.
- A user cannot delete a profile through the client.
- All chat metadata, message, receipt, device, and unknown collections are denied until trusted server-side membership and validation are implemented.

## Emulator test matrix

| Test | Expected |
| --- | --- |
| No auth → read own profile | Deny |
| User A → read User A profile | Allow |
| User A → read User B profile | Deny |
| User A → update User A displayName within limit | Allow |
| User A → write User B UID into profile | Deny |
| User A → spoof another phone number | Deny |
| User A → write unexpected privileged field | Deny |
| User A → delete own profile | Deny |
| User A → read/write any chat document | Deny |
| User A → read/write any message document | Deny |
| User A → read unknown collection | Deny |

## Deploy caution

The current Android chat client writes directly to `chats/{chatId}/messages`. These deny-by-default chat rules will therefore block that client path. This is intentional: do not deploy a permissive rule to make the current UI appear functional. First implement a trusted chat-creation/message service, migrate participant UIDs, then add emulator-tested membership rules and only then enable chat access.
