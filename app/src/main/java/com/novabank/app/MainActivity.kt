package com.novabank.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.novabank.app.ui.NovaBankRoot
import dagger.hilt.android.AndroidEntryPoint

/**
 * FragmentActivity so androidx.biometric (BiometricPrompt) can attach its
 * headless fragment — the only framework view in an otherwise Compose app.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NovaBankRoot() }
    }
}
