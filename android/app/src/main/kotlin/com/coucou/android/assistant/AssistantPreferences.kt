package com.coucou.android.assistant

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keys belong to the phone, never to the repository, a desktop session, or a log.
 * Each provider has its own encrypted blob in private app preferences; the AES key lives
 * in the Android Keystore. Model identifiers and provider choice are not secrets.
 */
class AssistantPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("coucou_phone_assistant", Context.MODE_PRIVATE)

    val provider: AssistantProvider
        get() = AssistantProvider.parse(prefs.getString("provider", null))

    fun setProvider(next: AssistantProvider) {
        prefs.edit().putString("provider", next.id).apply()
    }

    fun model(provider: AssistantProvider = this.provider): String =
        prefs.getString("model_" + provider.id, provider.defaultModel)
            ?.takeIf { AssistantPolicy.modelValid(provider, it) } ?: provider.defaultModel

    fun setModel(provider: AssistantProvider, value: String): Boolean {
        val candidate = value.trim()
        if (!AssistantPolicy.modelValid(provider, candidate)) return false
        prefs.edit().putString("model_" + provider.id, candidate).apply()
        return true
    }

    fun hasKey(provider: AssistantProvider = this.provider): Boolean = key(provider) != null

    /** Never expose the secret from AppModel's observable Compose state. */
    fun key(provider: AssistantProvider = this.provider): String? {
        val blob = prefs.getString("secret_" + provider.id, null) ?: return null
        return try {
            val all = Base64.decode(blob, Base64.NO_WRAP)
            if (all.size <= IV_SIZE) return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, all, 0, IV_SIZE))
            String(cipher.doFinal(all, IV_SIZE, all.size - IV_SIZE), Charsets.UTF_8)
                .takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            // A lost OS key invalidates only this entry, never the rest of the settings.
            prefs.edit().remove("secret_" + provider.id).apply()
            null
        }
    }

    fun setKey(provider: AssistantProvider, input: String): Boolean {
        val secret = input.trim()
        if (secret.length !in 8..4096 || secret.any { it == '\n' || it == '\r' }) return false
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
            val bytes = cipher.iv + cipher.doFinal(secret.toByteArray(Charsets.UTF_8))
            prefs.edit().putString("secret_" + provider.id, Base64.encodeToString(bytes, Base64.NO_WRAP)).commit()
        } catch (_: Exception) {
            false
        }
    }

    fun clearKey(provider: AssistantProvider) {
        prefs.edit().remove("secret_" + provider.id).commit()
    }

    fun request(): AssistantRequest? {
        val selected = provider
        val apiKey = key(selected) ?: return null
        return AssistantRequest(selected, model(selected), apiKey, emptyList())
    }

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
        }.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "coucou_phone_assistant_key"
        const val IV_SIZE = 12
    }
}
