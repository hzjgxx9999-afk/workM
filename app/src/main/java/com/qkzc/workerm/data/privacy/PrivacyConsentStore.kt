package com.qkzc.workerm.data.privacy

import android.content.Context

/** Stores the accepted privacy-policy version so third-party SDKs are initialized only after consent. */
class PrivacyConsentStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun recordCurrentPolicyConsent() {
        preferences.edit().putInt(KEY_ACCEPTED_POLICY_VERSION, CURRENT_POLICY_VERSION).apply()
    }

    fun hasCurrentPolicyConsent(): Boolean =
        preferences.getInt(KEY_ACCEPTED_POLICY_VERSION, NO_POLICY_VERSION) == CURRENT_POLICY_VERSION

    private companion object {
        const val PREFERENCES_NAME = "privacy_consent"
        const val KEY_ACCEPTED_POLICY_VERSION = "accepted_policy_version"
        const val NO_POLICY_VERSION = 0
        const val CURRENT_POLICY_VERSION = 1
    }
}
