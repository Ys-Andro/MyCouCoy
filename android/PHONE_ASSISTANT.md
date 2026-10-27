# Coucou AI on Android (first native milestone)

This fork can chat with an AI model **directly from the Android phone**, without pairing a
computer. Desktop-linked agents, approvals, widgets and the existing island remain available.

## Set up

1. Open **Settings > AI on this phone** (on a Spanish phone: **IA en el teléfono**).
2. Select **Google Gemini** or **NVIDIA API**.
3. Enter a model ID available for your provider/account and save it.
4. Paste your provider API key, then tap **Save key**.
5. Open **Chat**, select the phone model in the model selector, and send a message.

The on-phone provider is automatically available in Chat when its key exists. If you also
pair a Coucou desktop, its offered models appear in the same selector. Desktop communication
is not required for phone models.

## Security and privacy

- Provider hosts are fixed in code. **No custom endpoints** or redirects are accepted.
- Gemini uses the Google Generative Language API. NVIDIA uses NVIDIA's hosted API.
- All API traffic is direct HTTPS from the phone to the provider.
- Separate API credentials are encrypted with AES-256-GCM and an Android Keystore key.
- Keys are never written into code, Gradle configuration, Git or debug logging.
- The settings screen uses FLAG_SECURE to prevent screenshot and recording leakage while
  entering a key. Key text isn't saved in Activity state and stored keys are never displayed.
- Chat history continues to use the existing **app-private, local** conversation file.
  Clear history from Chat to remove it.
- The user must obtain and manage their own provider keys and quota. Model IDs are editable.

## Architecture

- `assistant/AssistantPolicy.kt`: model identifier validation, provider metadata and
  bounded conversation context.
- `assistant/AssistantPreferences.kt`: private settings and Keystore encrypted credentials.
- `assistant/CloudAssistant.kt`: cancellable HTTPS/SSE streaming; no UI/network blocking.
- `app/AppModel.kt`: local/desktop routing and lifecycle; local traffic does not use LinkClient.
- `ui/AssistantSettings.kt`: editable model and non-persistent secret editor.
- `ui/ChatScreen.kt`: native AI feedback and mixed model selector.
- `ui/IslandOverlay.kt`: unchanged; now fed native activity state by AppModel.

## Current boundaries

This milestone is a **standalone AI chat**, not yet a device-control agent. It does not
control other apps, see the screen, capture the microphone, run background tasks or execute
model-suggested commands. Those require separate permission-aware tools, user controls and
instrumented Android device testing. The native chat currently uses the provider's cloud
API and requires Internet; it is not an offline on-device model.

The system retains the current pre-existing Android 11+ requirement and desktop features.

## Build

With Android SDK 35 and JDK 17:

```bash
cd android
./gradlew test assembleDebug lint
```

The native AI feature has pure policy unit tests. HTTP provider behavior and real-device
UI/network/battery interactions still require integration and instrumented testing.

## Intellectual property

The Android code is licensed separately from the Coucou name, Mochi character, artwork
and sounds. See `LICENSE`, `PERMISSION.md` and `../LICENSE-ASSETS.md` before publishing,
redistributing modified branding or using protected assets outside the permitted scope.
