package com.novabank.app.core

import com.novabank.app.core.storage.TokenStore

/** Plain in-memory token store for JVM tests (no Android Keystore). */
class FakeTokenStore : TokenStore {
    private var access: String? = null
    private var refresh: String? = null
    override var onCleared: (() -> Unit)? = null

    override fun accessToken(): String? = access
    override fun refreshToken(): String? = refresh

    override fun save(access: String, refresh: String?) {
        this.access = access
        this.refresh = refresh
    }

    override fun clear() {
        access = null
        refresh = null
        onCleared?.invoke()
    }
}
