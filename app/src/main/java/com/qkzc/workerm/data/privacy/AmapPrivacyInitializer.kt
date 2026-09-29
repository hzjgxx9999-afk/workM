package com.qkzc.workerm.data.privacy

import android.content.Context
import com.amap.api.maps.MapsInitializer

/** Initializes AMap only after the user accepted the privacy policy that discloses this SDK. */
object AmapPrivacyInitializer {
    fun initializeIfConsented(context: Context): Boolean {
        val appContext = context.applicationContext
        if (!PrivacyConsentStore(appContext).hasCurrentPolicyConsent()) return false

        MapsInitializer.updatePrivacyShow(appContext, true, true)
        MapsInitializer.updatePrivacyAgree(appContext, true)
        return true
    }
}
