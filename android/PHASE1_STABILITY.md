# Phase 1: Android stability gate

Scope: existing native phone AI chat. No agent tools, new permissions, persistent
background processing, or changes to Coucou Desktop.

## Code review fixes included

1. Android's chosen **System** language now resolves through the effective resource
   locale. Spanish phone settings display the new assistant UI in Spanish.
2. The dynamic island no longer requires an active desktop connection when the phone's
   own assistant is replying. Disconnected desktop sessions cannot produce a stale
   island card.
3. NVIDIA's previous default was listed as a deprecated free endpoint. New default:
   `nvidia/nemotron-3.5-lightning-30b-a3b`, which NVIDIA lists as a free endpoint.
   Gemini remains `gemini-3.8-flash`. The user can set different supported model IDs.
4. Gemini model identifiers are restricted to one path segment, so model settings
   cannot change the API request path.
5. AI streaming now parses complete Server-Sent Events (including multiline events),
   ignores keep-alives, limits events to 256 KiB and emitted response text to the
   existing 40,000-character chat limit. Error event data is handled.
6. The assistant's temporary FINISHED and ERROR character states revert to IDLE
   after five seconds, with pending resets cancelled on new requests.
7. Added isolated unit tests for provider model rules, SSE framing, locale selection
   and provider conversation isolation.

## Build and test (Windows, JDK 17, Android SDK 35)

From PowerShell in the project's `android` directory:

```powershell
.\gradlew.bat test assembleDebug lint --console=plain
```

The existing Android GitHub Actions workflow also executes `test assembleDebug lint`
on pushes to `main`, **provided GitHub Actions is enabled for the fork**.

## Manual acceptance checks (real Android 11+)

- Without pairing a computer, open Settings and configure a provider/model/key.
  Send a Chat message and watch the streamed answer complete.
- Set system language to Spanish without explicitly choosing an app language;
  assistant Settings, Home and Chat should display their Spanish text.
- Enable the island with Android overlay permission, pair a PC, disconnect it,
  then ask phone AI a question: the working island must still appear after
  leaving Coucou, if device overlay restrictions allow it.
- Begin a long response and tap Stop; old chunks must not appear in subsequent
  replies. Send another request immediately.
- Change providers and check that a previous provider's conversation is not sent
  as history to the new provider.
- Disconnect the Internet, use a wrong API key, try a nonexistent model and
  restore Internet; the chat must fail gracefully and recover on a new send.
- Speak a message through Android's installed recognizer and listen to a
  finished answer through system Text-to-Speech.
- Verify that legacy desktop pairing, approvals, session listings and the demo
  continue to operate normally.

## Verification limitations

Changes were reviewed statically via the GitHub repository. The assistant runtime
used for this edit has no Android SDK or Gradle cache and cannot reach GitHub via
its shell network, so an Android APK build, tests and device run **have not been
executed here**. A merge to `main` is not evidence of a passing build. GitHub
Actions had zero runs for this fork at the time of review. Do not declare Phase 1
fully validated until the build and acceptance checks pass.

No real API credentials were inserted into the repository.
