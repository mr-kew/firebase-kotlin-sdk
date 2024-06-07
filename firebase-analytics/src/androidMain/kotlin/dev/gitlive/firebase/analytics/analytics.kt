package dev.gitlive.firebase.analytics

import android.os.Bundle
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.setConsent
import dev.gitlive.firebase.Firebase
import kotlinx.coroutines.tasks.await

actual val Firebase.analytics: FirebaseAnalytics
    get() = FirebaseAnalytics(com.google.firebase.Firebase.analytics)

actual class FirebaseAnalytics(val android: com.google.firebase.analytics.FirebaseAnalytics) {
    actual fun logEvent(name: String, parameters: Map<String, String>?) {
        android.logEvent(name, parameters?.toBundle())
    }
    actual fun logEvent(name: String, block: FirebaseAnalyticsParameters.() -> Unit) {
        val params = FirebaseAnalyticsParameters()
        params.block()
        logEvent(name, params.parameters)
    }
    actual fun setUserProperty(name: String, value: String) {
        android.setUserProperty(name, value)
    }
    actual fun setUserId(id: String) {
        android.setUserId(id)
    }
    actual fun resetAnalyticsData() {
        android.resetAnalyticsData()
    }
    actual fun setDefaultEventParameters(parameters: Map<String, String>) {
        android.setDefaultEventParameters(parameters.toBundle())
    }

    actual fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        android.setAnalyticsCollectionEnabled(enabled)
    }

    actual fun setSessionTimeoutInterval(sessionTimeoutInterval: Long) {
        android.setSessionTimeoutDuration(sessionTimeoutInterval)
    }

    actual suspend fun getSessionId(): Long? = android.sessionId.await()

    actual fun setConsent(consentSettings: Map<ConsentType, ConsentStatus>) {
        consentSettings.entries.associate {
            it.key to when (it.value) {
                ConsentStatus.GRANTED -> com.google.firebase.analytics.FirebaseAnalytics.ConsentStatus.GRANTED
                ConsentStatus.DENIED -> com.google.firebase.analytics.FirebaseAnalytics.ConsentStatus.DENIED
            }
        }.let { androidConsentSettings ->
            android.setConsent {
                androidConsentSettings.entries.forEach {
                    when (it.key) {
                        ConsentType.AD_PERSONALIZATION ->
                            this.adUserData = it.value

                        ConsentType.AD_STORAGE ->
                            this.adUserData = it.value

                        ConsentType.AD_USER_DATA ->
                            this.adUserData = it.value

                        ConsentType.ANALYTICS_STORAGE ->
                            this.adUserData = it.value
                    }
                }

            }
        }
    }

    actual enum class ConsentType {
        AD_PERSONALIZATION,
        AD_STORAGE,
        AD_USER_DATA,
        ANALYTICS_STORAGE
    }

    actual enum class ConsentStatus {
        GRANTED,
        DENIED
    }
}

actual class FirebaseAnalyticsException(message: String): Exception(message)

fun Map<String, String>.toBundle() = Bundle().apply {
    forEach { (key, value) -> putString(key, value) }
}
