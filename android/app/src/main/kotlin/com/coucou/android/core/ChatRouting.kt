package com.coucou.android.core

/**
 * Chat model routing is independent of pairing, the Android network layer,
 * provider secrets and the desktop wire protocol.
 *
 * [saved] is the user's previous explicit selection. It remains stored when a
 * desktop disconnects so it can be restored after reconnection, but it must
 * never block the available phone AI in the meantime.
 */
object ChatRouting {
    enum class Destination { PHONE, DESKTOP, UNAVAILABLE }

    fun selected(
        saved: String?,
        phoneId: String?,
        desktopIds: List<String>,
        desktopReady: Boolean,
    ): String? = when {
        phoneId != null && saved == phoneId -> phoneId
        desktopReady && saved != null && saved in desktopIds -> saved
        phoneId != null -> phoneId
        desktopIds.isNotEmpty() -> desktopIds.first()
        else -> null
    }

    fun destination(
        modelId: String?,
        phoneId: String?,
        desktopIds: List<String>,
        desktopReady: Boolean,
    ): Destination = when {
        modelId == null -> Destination.UNAVAILABLE
        phoneId != null && modelId == phoneId -> Destination.PHONE
        desktopReady && modelId in desktopIds -> Destination.DESKTOP
        else -> Destination.UNAVAILABLE
    }
}
