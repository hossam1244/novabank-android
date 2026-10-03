package com.novabank.app.core.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tokens live in EncryptedSharedPreferences (AES-256 keys in the Android
 * Keystore) — never plain SharedPreferences.
 */
interface TokenStore {
    fun accessToken(): String?
    fun refreshToken(): String?
    fun save(access: String, refresh: String?)
    fun clear()
    var onCleared: (() -> Unit)?
}

@Singleton
class EncryptedTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) : TokenStore {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "novabank_secure_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override var onCleared: (() -> Unit)? = null

    override fun accessToken(): String? = prefs.getString(KEY_ACCESS, null)

    override fun refreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    override fun save(access: String, refresh: String?) {
        prefs.edit()
            .putString(KEY_ACCESS, access)
            .apply {
                refresh?.let { putString(KEY_REFRESH, it) }
            }
            .apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
        onCleared?.invoke()
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
