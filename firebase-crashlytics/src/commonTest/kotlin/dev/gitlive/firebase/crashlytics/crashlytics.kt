/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.crashlytics

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.apps
import dev.gitlive.firebase.firebaseOptions
import dev.gitlive.firebase.initialize
import dev.gitlive.firebase.runBlockingTest
import dev.gitlive.firebase.runTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse

expect val emulatorHost: String
expect val context: Any
expect annotation class IgnoreForAndroidUnitTest()

@IgnoreForAndroidUnitTest
class FirebaseCrashlyticsTest {

    lateinit var crashlytics: FirebaseCrashlytics

    @BeforeTest
    fun initializeFirebase() {
        val app = Firebase.apps(context).firstOrNull() ?: Firebase.initialize(
            context,
            firebaseOptions
        )

        crashlytics = Firebase.crashlytics(app)
    }

    @AfterTest
    fun deinitializeFirebase() = runBlockingTest {
        Firebase.apps(context).forEach {
            it.delete()
        }
    }

    @Test
    fun testRecordException() = runTest {
        crashlytics.recordException(Exception("Test Exception"))
    }

    @Test
    fun testLog() = runTest {
        crashlytics.log("Test Log")
    }

    @Test
    fun testSetUserId() = runTest {
        crashlytics.setUserId("Test User Id")

    }

    @Test
    fun testSendUnsentReports() = runTest {
        crashlytics.sendUnsentReports()
    }

    @Test
    fun testDeleteUnsentReports() = runTest {
        crashlytics.deleteUnsentReports()
    }

    @Test
    fun testDidCrashOnPreviousExecution() = runTest {
        val didCrash = crashlytics.didCrashOnPreviousExecution()
        assertFalse { didCrash }
    }

    @Test
    fun testSetCrashlyticsCollectionEnabled() = runTest {
        crashlytics.setCrashlyticsCollectionEnabled(true)
    }
}
