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

## User-device test findings and recovery fix (October 26, 2026)

The user reported that tests 1-6 passed on a phone: launch, provider settings,
standalone AI chat, cancellation, voice and Mochi/island. Tests 7-8 exposed
two usability problems: connectivity recovery showed no clear indication,
and occasional English wording remained in the phone assistant.

The targeted follow-up adds:

- A phone-only Android ConnectivityManager observer using a **validated**
  Internet connection, independent of the optional desktop link.
- A clear offline message in Chat and a temporary "connection restored"
  message when Android regains Internet; accessibility live-region feedback.
- Immediate failure of an in-flight phone reply if Internet is lost, with a
  phone-specific error message, without waiting for the HTTP timeout.
- Phone-specific failure wording for API key, provider, rate-limit and
  unreachable-service problems. Desktop errors retain their separate wording.
- Consistent default output language instructions for **both Gemini and NVIDIA**;
  explicit user requests for other languages still take priority. Spanish
  branding and the unpaired Chat empty state are also corrected.
- New unit tests for provider language instructions and input sanitization.

**Repeat on-device tests:** Open Chat with a phone model selected; turn off both
Wi-Fi and mobile data. Verify the offline banner appears promptly. Sending
a message should retain the draft and show an error instead of silently
stalling. Restore Internet; verify "Conexión restablecida" appears and a
new request succeeds. Also ask each provider a question in Spanish and
inspect the wording. No API key should be shown in screenshots or logs.

This fixes the code paths, but the two checks only count as **device-verified**
after the user installs the updated debug APK and repeats them. Previous
passing tests 1-6 are not invalidated by this review but need regression
verification after the update.

## Automated verification (GitHub Actions)

**PASS**: The Android workflow on commit `c9ac988fecf45195e6ebc2c7d0d3995d35c96002`
successfully completed `./gradlew test assembleDebug lint --console=plain` on
GitHub-hosted Ubuntu, using Java 17 and Android SDK tooling. This included
compilation of debug and release Kotlin, the debug APK, the debug/release
unit test suites, release verification, lint and the forbidden-dependency
check. Gradle reported **BUILD SUCCESSFUL** with 103 tasks executed.

Run: https://github.com/Ys-Andro/MyCouCoy/actions/runs/40435943015

Two intermediate iterations of the stability patch found and resolved an
unresolved Kotlin import and two outdated source-based UI assertions.
The final tested source passed. The separate phone-link workflow's Android,
Windows and relay jobs also completed successfully on the same commit;
longer interop/E2E jobs may have separate completion times.

**Remaining manual gate:** There has been no on-device interaction, battery,
Android overlay, speech engine or provider-key test in this session.
Run the acceptance checks above on a real phone before declaring the
entire phase device-verified. No API credential was put in Git.
