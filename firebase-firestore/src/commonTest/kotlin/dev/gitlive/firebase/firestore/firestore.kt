/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.firestore

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.apps
import dev.gitlive.firebase.firebaseOptions
import dev.gitlive.firebase.initialize
import dev.gitlive.firebase.runBlockingTest
import dev.gitlive.firebase.runTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

expect val emulatorHost: String
expect val context: Any
expect annotation class IgnoreForAndroidUnitTest()

@IgnoreForAndroidUnitTest
class FirebaseFirestoreTest {

    @Serializable
    data class FirestoreTest(
        val prop1: String,
        val time: Double = 0.0,
        val count: Int = 0,
        val list: List<String> = emptyList(),
    )

    @Serializable
    data class FirestoreTimeTest(
        val prop1: String,
        val time: BaseTimestamp?
    )

    lateinit var firestore: FirebaseFirestore

    @BeforeTest
    fun initializeFirebase() {
        val app = Firebase.apps(context).firstOrNull() ?:Firebase.initialize(
            context,
            firebaseOptions
        )

        firestore = Firebase.firestore(app).apply {
            useEmulator(emulatorHost, 8080)
        }
    }

    @AfterTest
    fun deinitializeFirebase() = runBlockingTest {
        Firebase.apps(context).forEach {
            it.delete()
        }
    }

    @Test
    fun testStringOrderBy() = runTest {
        setupFirestoreData()
        val resultDocs = firestore
            .collection("testFirestoreQuerying")
            .orderBy("prop1")
            .get()
            .documents
        assertEquals(3, resultDocs.size)
        assertEquals("aaa", resultDocs[0].get("prop1"))
        assertEquals("bbb", resultDocs[1].get("prop1"))
        assertEquals("ccc", resultDocs[2].get("prop1"))
    }

    @Test
    fun testFieldOrderBy() = runTest {
        setupFirestoreData()

        val resultDocs = firestore.collection("testFirestoreQuerying")
            .orderBy(FieldPath("prop1")).get().documents
        assertEquals(3, resultDocs.size)
        assertEquals("aaa", resultDocs[0].get("prop1"))
        assertEquals("bbb", resultDocs[1].get("prop1"))
        assertEquals("ccc", resultDocs[2].get("prop1"))
    }

    @Test
    fun testStringOrderByAscending() = runTest {
        setupFirestoreData()

        val resultDocs = firestore.collection("testFirestoreQuerying")
            .orderBy("prop1", Direction.ASCENDING).get().documents
        assertEquals(3, resultDocs.size)
        assertEquals("aaa", resultDocs[0].get("prop1"))
        assertEquals("bbb", resultDocs[1].get("prop1"))
        assertEquals("ccc", resultDocs[2].get("prop1"))
    }

    @Test
    fun testFieldOrderByAscending() = runTest {
        setupFirestoreData()

        val resultDocs = firestore.collection("testFirestoreQuerying")
            .orderBy(FieldPath("prop1"), Direction.ASCENDING).get().documents
        assertEquals(3, resultDocs.size)
        assertEquals("aaa", resultDocs[0].get("prop1"))
        assertEquals("bbb", resultDocs[1].get("prop1"))
        assertEquals("ccc", resultDocs[2].get("prop1"))
    }

    @Test
    fun testStringOrderByDescending() = runTest {
        setupFirestoreData()

        val resultDocs = firestore.collection("testFirestoreQuerying")
            .orderBy("prop1", Direction.DESCENDING).get().documents
        assertEquals(3, resultDocs.size)
        assertEquals("ccc", resultDocs[0].get("prop1"))
        assertEquals("bbb", resultDocs[1].get("prop1"))
        assertEquals("aaa", resultDocs[2].get("prop1"))
    }

    @Test
    fun testFieldOrderByDescending() = runTest {
        setupFirestoreData()

        val resultDocs = firestore.collection("testFirestoreQuerying")
            .orderBy(FieldPath("prop1"), Direction.DESCENDING).get().documents
        assertEquals(3, resultDocs.size)
        assertEquals("ccc", resultDocs[0].get("prop1"))
        assertEquals("bbb", resultDocs[1].get("prop1"))
        assertEquals("aaa", resultDocs[2].get("prop1"))
    }

    @Test
    fun testServerTimestampFieldValue() = runTest {
        val doc = firestore
            .collection("testServerTimestampFieldValue")
            .document("test")

        doc.set(FirestoreTimeTest.serializer(), FirestoreTimeTest("ServerTimestamp", Timestamp.ServerTimestamp))

        assertNotEquals(Timestamp.ServerTimestamp, doc.get().get("time", BaseTimestamp.serializer()))
        assertNotEquals(Timestamp.ServerTimestamp, doc.get().data(FirestoreTimeTest.serializer()).time)
    }

    @Test
    fun testServerTimestampBehaviorNone() = runTest {
        val doc = firestore
            .collection("testServerTimestampBehaviorNone")
            .document("test${Random.nextInt()}")

        val deferredPendingWritesSnapshot = async {
            doc.snapshots.filter { it.exists }.first()
        }
        nonSkippedDelay(100) // makes possible to catch pending writes snapshot

        doc.set(
            FirestoreTimeTest.serializer(),
            FirestoreTimeTest("ServerTimestampBehavior", Timestamp.ServerTimestamp)
        )

        val pendingWritesSnapshot = deferredPendingWritesSnapshot.await()
        assertTrue(pendingWritesSnapshot.metadata.hasPendingWrites)
        assertNull(pendingWritesSnapshot.get("time", BaseTimestamp.serializer().nullable, ServerTimestampBehavior.NONE))
    }

    @Test
    fun testServerTimestampBehaviorEstimate() = runTest {
        val doc = firestore
            .collection("testServerTimestampBehaviorEstimate")
            .document("test${Random.nextInt()}")

        val deferredPendingWritesSnapshot = async {
            doc.snapshots.filter { it.exists }.first()
        }
        nonSkippedDelay(100) // makes possible to catch pending writes snapshot

        doc.set(FirestoreTimeTest.serializer(), FirestoreTimeTest("ServerTimestampBehavior", Timestamp.ServerTimestamp))

        val pendingWritesSnapshot = deferredPendingWritesSnapshot.await()
        assertTrue(pendingWritesSnapshot.metadata.hasPendingWrites)
        assertNotNull(pendingWritesSnapshot.get("time", BaseTimestamp.serializer().nullable, ServerTimestampBehavior.ESTIMATE))
        assertNotEquals(Timestamp.ServerTimestamp, pendingWritesSnapshot.data(FirestoreTimeTest.serializer(), ServerTimestampBehavior.ESTIMATE).time)
    }

    @Test
    fun testServerTimestampBehaviorPrevious() = runTest {
        val doc = firestore
            .collection("testServerTimestampBehaviorPrevious")
            .document("test${Random.nextInt()}")

        val deferredPendingWritesSnapshot = async {
            doc.snapshots.filter { it.exists }.first()
        }
        nonSkippedDelay(100) // makes possible to catch pending writes snapshot

        doc.set(FirestoreTimeTest.serializer(), FirestoreTimeTest("ServerTimestampBehavior", Timestamp.ServerTimestamp))

        val pendingWritesSnapshot = deferredPendingWritesSnapshot.await()
        assertTrue(pendingWritesSnapshot.metadata.hasPendingWrites)
        assertNull(pendingWritesSnapshot.get("time", BaseTimestamp.serializer().nullable, ServerTimestampBehavior.PREVIOUS))
    }

    @Test
    fun testDocumentAutoId() = runTest {
        val doc = firestore
            .collection("testDocumentAutoId")
            .document

        doc.set(FirestoreTest.serializer(), FirestoreTest("AutoId"))

        val resultDoc = firestore
            .collection("testDocumentAutoId")
            .document(doc.id)
            .get()

        assertEquals(true, resultDoc.exists)
        assertEquals("AutoId", resultDoc.get("prop1"))
    }

    @Test
    fun testStartAfterDocumentSnapshot() = runTest {
        setupFirestoreData()
        val query = firestore
            .collection("testFirestoreQuerying")
            .orderBy("prop1", Direction.ASCENDING)

        val firstPage = query.limit(2).get().documents // First 2 results
        assertEquals(2, firstPage.size)
        assertEquals("aaa", firstPage[0].get("prop1"))
        assertEquals("bbb", firstPage[1].get("prop1"))

        val lastDocumentSnapshot = firstPage.lastOrNull()
        assertNotNull(lastDocumentSnapshot)

        val secondPage = query.startAfter(lastDocumentSnapshot).get().documents
        assertEquals(1, secondPage.size)
        assertEquals("ccc", secondPage[0].get("prop1"))
    }

    @Test
    fun testStartAfterFieldValues() = runTest {
        setupFirestoreData()
        val query = firestore
            .collection("testFirestoreQuerying")
            .orderBy("prop1", Direction.ASCENDING)

        val firstPage = query.get().documents
        assertEquals(3, firstPage.size)
        assertEquals("aaa", firstPage[0].get("prop1"))
        assertEquals("bbb", firstPage[1].get("prop1"))
        assertEquals("ccc", firstPage[2].get("prop1"))

        val secondPage = query.startAfter("bbb").get().documents
        assertEquals(1, secondPage.size)
        assertEquals("ccc", secondPage[0].get("prop1"))
    }

    @Test
    fun testStartAtDocumentSnapshot() = runTest {
        setupFirestoreData()
        val query = firestore
            .collection("testFirestoreQuerying")
            .orderBy("prop1", Direction.ASCENDING)

        val firstPage = query.limit(2).get().documents // First 2 results
        assertEquals(2, firstPage.size)
        assertEquals("aaa", firstPage[0].get("prop1"))
        assertEquals("bbb", firstPage[1].get("prop1"))

        val lastDocumentSnapshot = firstPage.lastOrNull()
        assertNotNull(lastDocumentSnapshot)

        val secondPage = query.startAt(lastDocumentSnapshot).get().documents
        assertEquals(2, secondPage.size)
        assertEquals("bbb", secondPage[0].get("prop1"))
        assertEquals("ccc", secondPage[1].get("prop1"))
    }

    @Test
    fun testStartAtFieldValues() = runTest {
        setupFirestoreData()
        val query = firestore
            .collection("testFirestoreQuerying")
            .orderBy("prop1", Direction.ASCENDING)

        val firstPage = query.get().documents // First 2 results
        assertEquals(3, firstPage.size)
        assertEquals("aaa", firstPage[0].get("prop1"))
        assertEquals("bbb", firstPage[1].get("prop1"))
        assertEquals("ccc", firstPage[2].get("prop1"))

        val secondPage = query.startAt("bbb").get().documents
        assertEquals(2, secondPage.size)
        assertEquals("bbb", secondPage[0].get("prop1"))
        assertEquals("ccc", secondPage[1].get("prop1"))
    }

    @Test
    fun testEndBeforeDocumentSnapshot() = runTest {
        setupFirestoreData()
        val query = firestore
            .collection("testFirestoreQuerying")
            .orderBy("prop1", Direction.ASCENDING)

        val firstPage = query.limit(2).get().documents // First 2 results
        assertEquals(2, firstPage.size)
        assertEquals("aaa", firstPage[0].get("prop1"))
        assertEquals("bbb", firstPage[1].get("prop1"))

        val lastDocumentSnapshot = firstPage.lastOrNull()
        assertNotNull(lastDocumentSnapshot)

        val secondPage = query.endBefore(lastDocumentSnapshot).get().documents
        assertEquals(1, secondPage.size)
        assertEquals("aaa", secondPage[0].get("prop1"))
    }

    @Test
    fun testEndBeforeFieldValues() = runTest {
        setupFirestoreData()
        val query = firestore
            .collection("testFirestoreQuerying")
            .orderBy("prop1", Direction.ASCENDING)

        val firstPage = query.get().documents
        assertEquals(3, firstPage.size)
        assertEquals("aaa", firstPage[0].get("prop1"))
        assertEquals("bbb", firstPage[1].get("prop1"))
        assertEquals("ccc", firstPage[2].get("prop1"))

        val secondPage = query.endBefore("bbb").get().documents
        assertEquals(1, secondPage.size)
        assertEquals("aaa", secondPage[0].get("prop1"))
    }

    @Test
    fun testEndAtDocumentSnapshot() = runTest {
        setupFirestoreData()
        val query = firestore
            .collection("testFirestoreQuerying")
            .orderBy("prop1", Direction.ASCENDING)

        val firstPage = query.limit(2).get().documents // First 2 results
        assertEquals(2, firstPage.size)
        assertEquals("aaa", firstPage[0].get("prop1"))
        assertEquals("bbb", firstPage[1].get("prop1"))

        val lastDocumentSnapshot = firstPage.lastOrNull()
        assertNotNull(lastDocumentSnapshot)

        val secondPage = query.endAt(lastDocumentSnapshot).get().documents
        assertEquals(2, secondPage.size)
        assertEquals("aaa", secondPage[0].get("prop1"))
        assertEquals("bbb", secondPage[1].get("prop1"))
    }

    @Test
    fun testEndAtFieldValues() = runTest {
        setupFirestoreData()
        val query = firestore
            .collection("testFirestoreQuerying")
            .orderBy("prop1", Direction.ASCENDING)

        val firstPage = query.get().documents // First 2 results
        assertEquals(3, firstPage.size)
        assertEquals("aaa", firstPage[0].get("prop1"))
        assertEquals("bbb", firstPage[1].get("prop1"))
        assertEquals("ccc", firstPage[2].get("prop1"))

        val secondPage = query.endAt("bbb").get().documents
        assertEquals(2, secondPage.size)
        assertEquals("aaa", secondPage[0].get("prop1"))
        assertEquals("bbb", secondPage[1].get("prop1"))
    }

    @Test
    fun testIncrementFieldValue() = runTest {
        val doc = firestore
            .collection("testFirestoreIncrementFieldValue")
            .document("test1")

        doc.set(FirestoreTest.serializer(), FirestoreTest("increment1", count = 0))
        val dataBefore = doc.get().data(FirestoreTest.serializer())
        assertEquals(0, dataBefore.count)

        doc.update("count" to FieldValue.increment(5))
        val dataAfter = doc.get().data(FirestoreTest.serializer())
        assertEquals(5, dataAfter.count)
    }

    @Test
    fun testArrayUnion() = runTest {
        val doc = firestore
            .collection("testFirestoreArrayUnion")
            .document("test1")

        doc.set(FirestoreTest.serializer(), FirestoreTest("increment1", list = listOf("first")))
        val dataBefore = doc.get().data(FirestoreTest.serializer())
        assertEquals(listOf("first"), dataBefore.list)

        doc.update("list" to FieldValue.arrayUnion("second"))
        val dataAfter = doc.get().data(FirestoreTest.serializer())
        assertEquals(listOf("first", "second"), dataAfter.list)
    }

    @Test
    fun testArrayRemove() = runTest {
        val doc = firestore
            .collection("testFirestoreArrayRemove")
            .document("test1")

        doc.set(FirestoreTest.serializer(), FirestoreTest("increment1", list = listOf("first", "second")))
        val dataBefore = doc.get().data(FirestoreTest.serializer())
        assertEquals(listOf("first", "second"), dataBefore.list)

        doc.update("list" to FieldValue.arrayRemove("second"))
        val dataAfter = doc.get().data(FirestoreTest.serializer())
        assertEquals(listOf("first"), dataAfter.list)
    }

    @Test
    fun testLegacyDoubleTimestamp() = runTest {
        @Serializable
        data class DoubleTimestamp(
            @Serializable(with = DoubleAsTimestampSerializer::class)
            val time: Double?
        )

        val doc = firestore
            .collection("testLegacyDoubleTimestamp")
            .document("test${Random.nextInt()}")

        val deferredPendingWritesSnapshot = async {
            doc.snapshots.filter { it.exists }.first()
        }
        nonSkippedDelay(100) // makes possible to catch pending writes snapshot

        doc.set(DoubleTimestamp.serializer(), DoubleTimestamp(DoubleAsTimestampSerializer.serverTimestamp))

        val pendingWritesSnapshot = deferredPendingWritesSnapshot.await()
        assertTrue(pendingWritesSnapshot.metadata.hasPendingWrites)
        assertNotNull(pendingWritesSnapshot.get("time", DoubleAsTimestampSerializer, ServerTimestampBehavior.ESTIMATE ))
        assertNotEquals(DoubleAsTimestampSerializer.serverTimestamp, pendingWritesSnapshot.data(DoubleTimestamp.serializer(), ServerTimestampBehavior.ESTIMATE).time)
    }

    @Test
    fun testLegacyDoubleTimestampWriteNewFormatRead() = runTest {
        @Serializable
        data class LegacyDocument(
            @Serializable(with = DoubleAsTimestampSerializer::class)
            val time: Double
        )

        @Serializable
        data class NewDocument(
            val time: Timestamp
        )

        val doc = firestore
            .collection("testLegacyDoubleTimestampEncodeDecode")
            .document("testLegacy")

        val ms = 12345678.0

        doc.set(LegacyDocument.serializer(), LegacyDocument(time = ms))

        val fetched: NewDocument = doc.get().data(NewDocument.serializer())
        assertEquals(ms, fetched.time.toMilliseconds())
    }

    @Test
    fun testQueryByTimestamp() = runTest {
        @Serializable
        data class DocumentWithTimestamp(
            val time: Timestamp
        )

        val collection = firestore
            .collection("testQueryByTimestamp")

        val timestamp = Timestamp.fromMilliseconds(1693262549000.0)

        val pastTimestamp = Timestamp(timestamp.seconds - 60, 12345000) // note: iOS truncates 3 last digits of nanoseconds due to internal conversions
        val futureTimestamp = Timestamp(timestamp.seconds + 60, 78910000)

        collection.add(DocumentWithTimestamp.serializer(), DocumentWithTimestamp(pastTimestamp))
        collection.add(DocumentWithTimestamp.serializer(), DocumentWithTimestamp(futureTimestamp))

        val equalityQueryResult = collection.where(
            path = FieldPath(DocumentWithTimestamp::time.name),
            equalTo = pastTimestamp
        ).get().documents.map { it.data(DocumentWithTimestamp.serializer()) }.toSet()

        assertEquals(setOf(DocumentWithTimestamp(pastTimestamp)), equalityQueryResult)

        val gtQueryResult = collection.where(
            path = FieldPath(DocumentWithTimestamp::time.name),
            greaterThan = timestamp
        ).get().documents.map { it.data(DocumentWithTimestamp.serializer()) }.toSet()

        assertEquals(setOf(DocumentWithTimestamp(futureTimestamp)), gtQueryResult)
    }

    private suspend fun setupFirestoreData() {
        firestore.collection("testFirestoreQuerying")
            .document("one")
            .set(FirestoreTest.serializer(), FirestoreTest("aaa"))
        firestore.collection("testFirestoreQuerying")
            .document("two")
            .set(FirestoreTest.serializer(), FirestoreTest("bbb"))
        firestore.collection("testFirestoreQuerying")
            .document("three")
            .set(FirestoreTest.serializer(), FirestoreTest("ccc"))
    }

    private suspend fun nonSkippedDelay(timeout: Long) = withContext(Dispatchers.Default) {
        delay(timeout)
    }
}
