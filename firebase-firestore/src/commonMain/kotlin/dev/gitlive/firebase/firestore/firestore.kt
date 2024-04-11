/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.firestore

import dev.gitlive.firebase.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlin.jvm.JvmName

/** Returns the [FirebaseFirestore] instance of the default [FirebaseApp]. */
expect val Firebase.firestore: FirebaseFirestore

/** Returns the [FirebaseFirestore] instance of a given [FirebaseApp]. */
expect fun Firebase.firestore(app: FirebaseApp): FirebaseFirestore

expect class FirebaseFirestore {
    fun collection(collectionPath: String): CollectionReference
    fun collectionGroup(collectionId: String): Query
    fun document(documentPath: String): DocumentReference
    fun batch(): WriteBatch
    fun setLoggingEnabled(loggingEnabled: Boolean)
    suspend fun clearPersistence()
    suspend fun <T> runTransaction(func: suspend Transaction.() -> T): T
    fun useEmulator(host: String, port: Int)
    fun setSettings(persistenceEnabled: Boolean? = null, sslEnabled: Boolean? = null, host: String? = null, cacheSizeBytes: Long? = null)
    suspend fun disableNetwork()
    suspend fun enableNetwork()
}

@PublishedApi
internal sealed class SetOptions {
    data object Merge : SetOptions()
    data object Overwrite : SetOptions()
    data class MergeFields(val fields: List<String>) : SetOptions()
    data class MergeFieldPaths(val fieldPaths: List<FieldPath>) : SetOptions() {
        val encodedFieldPaths = fieldPaths.map { it.encoded }
    }
}

expect class NativeTransaction
@PublishedApi
internal expect class NativeTransactionWrapper internal constructor(native: NativeTransaction) {

    val native: NativeTransaction

    fun setEncoded(documentRef: DocumentReference, encodedData: EncodedObject, setOptions: SetOptions): NativeTransactionWrapper
    fun updateEncoded(documentRef: DocumentReference, encodedData: EncodedObject): NativeTransactionWrapper
    fun updateEncodedFieldsAndValues(documentRef: DocumentReference, encodedFieldsAndValues: List<Pair<String, Any?>>): NativeTransactionWrapper
    fun updateEncodedFieldPathsAndValues(documentRef: DocumentReference, encodedFieldsAndValues: List<Pair<EncodedFieldPath, Any?>>): NativeTransactionWrapper
    fun delete(documentRef: DocumentReference): NativeTransactionWrapper
    suspend fun get(documentRef: DocumentReference): NativeDocumentSnapshotWrapper
}

data class Transaction internal constructor(@PublishedApi internal val nativeWrapper: NativeTransactionWrapper) {

    constructor(native: NativeTransaction) : this(NativeTransactionWrapper(native))

    val native = nativeWrapper.native

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(documentRef, data, merge) { this.encodeDefaults = encodeDefaults }"))
    fun set(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean, merge: Boolean = false): Transaction = set(documentRef, data, merge) {
        this.encodeDefaults = encodeDefaults
    }
    inline fun set(documentRef: DocumentReference, data: Any, merge: Boolean = false, buildSettings: EncodeSettings.Builder.() -> Unit = {}): Transaction = setEncoded(documentRef, encodeAsObject(data, buildSettings), if (merge) SetOptions.Merge else SetOptions.Overwrite)


    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(documentRef, data, mergeFields) { this.encodeDefaults = encodeDefaults }"))
    fun set(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean, vararg mergeFields: String) = set(documentRef, data, *mergeFields) {
        this.encodeDefaults = encodeDefaults
    }
    inline fun set(documentRef: DocumentReference, data: Any, vararg mergeFields: String, buildSettings: EncodeSettings.Builder.() -> Unit = {}): Transaction = setEncoded(documentRef, encodeAsObject(data, buildSettings), SetOptions.MergeFields(mergeFields.asList()))

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(documentRef, data, mergeFieldPaths) { this.encodeDefaults = encodeDefaults }"))
    fun set(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) = set(documentRef, data, *mergeFieldPaths) {
        this.encodeDefaults = encodeDefaults
    }
    inline fun set(documentRef: DocumentReference, data: Any, vararg mergeFieldPaths: FieldPath, buildSettings: EncodeSettings.Builder.() -> Unit = {}): Transaction = setEncoded(documentRef, encodeAsObject(data, buildSettings), SetOptions.MergeFieldPaths(mergeFieldPaths.asList()))

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(documentRef, strategy, data, merge) { this.encodeDefaults = encodeDefaults }"))
    fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, merge: Boolean = false) = set(documentRef, strategy, data, merge) {
        this.encodeDefaults = encodeDefaults
    }
    inline fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, merge: Boolean = false, buildSettings: EncodeSettings.Builder.() -> Unit = {}): Transaction = setEncoded(documentRef, encodeAsObject(strategy, data, buildSettings), if (merge) SetOptions.Merge else SetOptions.Overwrite)

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(documentRef, strategy, data, mergeFields) { this.encodeDefaults = encodeDefaults }"))
    fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFields: String) = set(documentRef, strategy, data, *mergeFields) {
        this.encodeDefaults = encodeDefaults
    }
    inline fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, vararg mergeFields: String, buildSettings: EncodeSettings.Builder.() -> Unit = {}): Transaction = setEncoded(documentRef, encodeAsObject(strategy, data, buildSettings), SetOptions.MergeFields(mergeFields.asList()))

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(documentRef, strategy, data, mergeFieldPaths) { this.encodeDefaults = encodeDefaults }"))
    fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) = set(documentRef, strategy, data, *mergeFieldPaths) {
        this.encodeDefaults = encodeDefaults
    }
    inline fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, vararg mergeFieldPaths: FieldPath, buildSettings: EncodeSettings.Builder.() -> Unit = {}): Transaction = setEncoded(documentRef, encodeAsObject(strategy, data, buildSettings), SetOptions.MergeFieldPaths(mergeFieldPaths.asList()))

    @PublishedApi
    internal fun setEncoded(documentRef: DocumentReference, encodedData: EncodedObject, setOptions: SetOptions): Transaction = Transaction(nativeWrapper.setEncoded(documentRef, encodedData, setOptions))

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("update(documentRef, data) { this.encodeDefaults = encodeDefaults }"))
    fun update(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean) = update(documentRef, data) {
        this.encodeDefaults = encodeDefaults
    }
    inline fun update(documentRef: DocumentReference, data: Any, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = updateEncoded(documentRef, encodeAsObject(data, buildSettings))

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("update(documentRef, strategy, data) { this.encodeDefaults = encodeDefaults }"))
    fun <T : Any> update(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) = update(documentRef, strategy, data) {
        this.encodeDefaults = encodeDefaults
    }
    inline fun <T : Any> update(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = updateEncoded(documentRef, encodeAsObject(strategy, data, buildSettings))

    @JvmName("updateFields")
    inline fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<String, Any?>, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = updateEncodedFieldsAndValues(documentRef, encodeFieldAndValue(fieldsAndValues, buildSettings).orEmpty())
    @JvmName("updateFieldPaths")
    inline fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<FieldPath, Any?>, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = updateEncodedFieldPathsAndValues(documentRef, encodeFieldAndValue(fieldsAndValues, buildSettings).orEmpty())

    @PublishedApi
    internal fun updateEncoded(documentRef: DocumentReference, encodedData: EncodedObject): Transaction = Transaction(nativeWrapper.updateEncoded(documentRef, encodedData))

    @PublishedApi
    internal fun updateEncodedFieldsAndValues(documentRef: DocumentReference, encodedFieldsAndValues: List<Pair<String, Any?>>): Transaction = Transaction(nativeWrapper.updateEncodedFieldsAndValues(documentRef, encodedFieldsAndValues))

    @PublishedApi
    internal fun updateEncodedFieldPathsAndValues(documentRef: DocumentReference, encodedFieldsAndValues: List<Pair<EncodedFieldPath, Any?>>): Transaction = Transaction(nativeWrapper.updateEncodedFieldPathsAndValues(documentRef, encodedFieldsAndValues))

    fun delete(documentRef: DocumentReference): Transaction = Transaction(nativeWrapper.delete(documentRef))
    suspend fun get(documentRef: DocumentReference): DocumentSnapshot = DocumentSnapshot(nativeWrapper.get(documentRef))
}

expect open class NativeQuery

@PublishedApi
internal expect open class NativeQueryWrapper internal constructor(native: NativeQuery) {

    open val native: NativeQuery

    fun limit(limit: Number): NativeQueryWrapper
    val snapshots: Flow<QuerySnapshot>
    fun snapshots(includeMetadataChanges: Boolean = false): Flow<QuerySnapshot>
    suspend fun get(): QuerySnapshot

    fun where(filter: Filter): NativeQueryWrapper

    fun orderBy(field: String, direction: Direction): NativeQueryWrapper
    fun orderBy(field: EncodedFieldPath, direction: Direction): NativeQueryWrapper

    fun startAfter(document: NativeDocumentSnapshot): NativeQueryWrapper
    fun startAfter(vararg fieldValues: Any): NativeQueryWrapper
    fun startAt(document: NativeDocumentSnapshot): NativeQueryWrapper
    fun startAt(vararg fieldValues: Any): NativeQueryWrapper

    fun endBefore(document: NativeDocumentSnapshot): NativeQueryWrapper
    fun endBefore(vararg fieldValues: Any): NativeQueryWrapper
    fun endAt(document: NativeDocumentSnapshot): NativeQueryWrapper
    fun endAt(vararg fieldValues: Any): NativeQueryWrapper
}

open class Query internal constructor(internal val nativeQuery: NativeQueryWrapper) {

    constructor(native: NativeQuery) : this(NativeQueryWrapper(native))

    open val native = nativeQuery.native

    fun limit(limit: Number): Query = Query(nativeQuery.limit(limit))
    val snapshots: Flow<QuerySnapshot> = nativeQuery.snapshots
    fun snapshots(includeMetadataChanges: Boolean = false): Flow<QuerySnapshot> = nativeQuery.snapshots(includeMetadataChanges)
    suspend fun get(): QuerySnapshot = nativeQuery.get()

    fun where(builder: FilterBuilder.() -> Filter?) = builder(FilterBuilder())?.let { Query(nativeQuery.where(it)) } ?: this

    fun orderBy(field: String, direction: Direction = Direction.ASCENDING) = Query(nativeQuery.orderBy(field, direction))
    fun orderBy(field: FieldPath, direction: Direction = Direction.ASCENDING) = Query(nativeQuery.orderBy(field.encoded, direction))

    fun startAfter(document: DocumentSnapshot) = Query(nativeQuery.startAfter(document.native))
    fun startAfter(vararg fieldValues: Any) = Query(nativeQuery.startAfter(*(fieldValues.map { it.safeValue }.toTypedArray())))
    fun startAt(document: DocumentSnapshot) = Query(nativeQuery.startAt(document.native))
    fun startAt(vararg fieldValues: Any) = Query(nativeQuery.startAt(*(fieldValues.map { it.safeValue }.toTypedArray())))

    fun endBefore(document: DocumentSnapshot) = Query(nativeQuery.endBefore(document.native))
    fun endBefore(vararg fieldValues: Any) = Query(nativeQuery.endBefore(*(fieldValues.map { it.safeValue }.toTypedArray())))
    fun endAt(document: DocumentSnapshot) = Query(nativeQuery.endAt(document.native))
    fun endAt(vararg fieldValues: Any) = Query(nativeQuery.endAt(*(fieldValues.map { it.safeValue }.toTypedArray())))
}

@Deprecated("Deprecated in favor of using a [FilterBuilder]", replaceWith = ReplaceWith("where { field equalTo equalTo }", "dev.gitlive.firebase.firestore"))
fun Query.where(field: String, equalTo: Any?) = where {
    field equalTo equalTo
}

@Deprecated("Deprecated in favor of using a [FilterBuilder]", replaceWith = ReplaceWith("where { path equalTo equalTo }", "dev.gitlive.firebase.firestore"))
fun Query.where(path: FieldPath, equalTo: Any?) = where {
    path equalTo equalTo
}

@Deprecated("Deprecated in favor of using a [FilterBuilder]", replaceWith = ReplaceWith("where {  }", "dev.gitlive.firebase.firestore"))
fun Query.where(field: String, lessThan: Any? = null, greaterThan: Any? = null, arrayContains: Any? = null) = where {
    all(
        *listOfNotNull(
            lessThan?.let { field lessThan it },
            greaterThan?.let { field greaterThan it },
            arrayContains?.let { field contains it }
        ).toTypedArray()
    )
}

@Deprecated("Deprecated in favor of using a [FilterBuilder]", replaceWith = ReplaceWith("where {  }", "dev.gitlive.firebase.firestore"))
fun Query.where(path: FieldPath, lessThan: Any? = null, greaterThan: Any? = null, arrayContains: Any? = null) = where {
    all(
        *listOfNotNull(
            lessThan?.let { path lessThan it },
            greaterThan?.let { path greaterThan it },
            arrayContains?.let { path contains it }
        ).toTypedArray()
    )
}

@Deprecated("Deprecated in favor of using a [FilterBuilder]", replaceWith = ReplaceWith("where {  }", "dev.gitlive.firebase.firestore"))
fun Query.where(field: String, inArray: List<Any>? = null, arrayContainsAny: List<Any>? = null) = where {
    all(
        *listOfNotNull(
            inArray?.let { field inArray it },
            arrayContainsAny?.let { field containsAny  it },
        ).toTypedArray()
    )
}

@Deprecated("Deprecated in favor of using a [FilterBuilder]", replaceWith = ReplaceWith("where {  }", "dev.gitlive.firebase.firestore"))
fun Query.where(path: FieldPath, inArray: List<Any>? = null, arrayContainsAny: List<Any>? = null) = where {
    all(
        *listOfNotNull(
            inArray?.let { path inArray it },
            arrayContainsAny?.let { path containsAny  it },
        ).toTypedArray()
    )
}

internal val Any.safeValue: Any get() = when (this) {
    is Timestamp -> nativeValue
    is GeoPoint -> nativeValue
    is DocumentReference -> native.nativeValue
    is Map<*, *> -> this.mapNotNull { (key, value) -> key?.let { it.safeValue to value?.safeValue } }
    is Collection<*> -> this.mapNotNull { it?.safeValue }
    else -> this
}

expect class NativeWriteBatch

@PublishedApi
internal expect class NativeWriteBatchWrapper internal constructor(native: NativeWriteBatch) {
    val native: NativeWriteBatch
    fun setEncoded(documentRef: DocumentReference, encodedData: EncodedObject, setOptions: SetOptions): NativeWriteBatchWrapper
    fun updateEncoded(documentRef: DocumentReference, encodedData: EncodedObject): NativeWriteBatchWrapper
    fun updateEncodedFieldsAndValues(documentRef: DocumentReference, encodedFieldsAndValues: List<Pair<String, Any?>>): NativeWriteBatchWrapper
    fun updateEncodedFieldPathsAndValues(documentRef: DocumentReference, encodedFieldsAndValues: List<Pair<EncodedFieldPath, Any?>>): NativeWriteBatchWrapper
    fun delete(documentRef: DocumentReference): NativeWriteBatchWrapper
    suspend fun commit()
}

data class WriteBatch internal constructor(@PublishedApi internal val nativeWrapper: NativeWriteBatchWrapper) {

    constructor(native: NativeWriteBatch) : this(NativeWriteBatchWrapper(native))

    val native = nativeWrapper.native

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(documentRef, data, merge) { this.encodeDefaults = encodeDefaults }"))
    inline fun <reified T : Any> set(documentRef: DocumentReference, data: T, encodeDefaults: Boolean, merge: Boolean = false) = set(documentRef, data, merge) {
        this.encodeDefaults = encodeDefaults
    }
    inline fun <reified T : Any> set(documentRef: DocumentReference, data: T, merge: Boolean = false, buildSettings: EncodeSettings.Builder.() -> Unit = {}) =
        setEncoded(documentRef, encodeAsObject(data, buildSettings), if (merge) SetOptions.Merge else SetOptions.Overwrite)

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(documentRef, data, mergeFields) { this.encodeDefaults = encodeDefaults }"))
    inline fun <reified T : Any> set(documentRef: DocumentReference, data: T, encodeDefaults: Boolean, vararg mergeFields: String) = set(documentRef, data, *mergeFields) {
        this.encodeDefaults = encodeDefaults
    }
    inline fun <reified T : Any> set(documentRef: DocumentReference, data: T, vararg mergeFields: String, buildSettings: EncodeSettings.Builder.() -> Unit = {}) =
        setEncoded(documentRef, encodeAsObject(data, buildSettings), SetOptions.MergeFields(mergeFields.asList()))

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(documentRef, data, mergeFieldPaths) { this.encodeDefaults = encodeDefaults }"))
    inline fun <reified T : Any> set(documentRef: DocumentReference, data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) = set(documentRef, data, *mergeFieldPaths) {
        this.encodeDefaults = encodeDefaults
    }
    inline fun <reified T : Any> set(documentRef: DocumentReference, data: T, vararg mergeFieldPaths: FieldPath, buildSettings: EncodeSettings.Builder.() -> Unit = {}) =
        setEncoded(documentRef, encodeAsObject(data, buildSettings), SetOptions.MergeFieldPaths(mergeFieldPaths.asList()))

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(documentRef, strategy, data, merge) { this.encodeDefaults = encodeDefaults }"))
    fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, merge: Boolean = false) = set(documentRef, strategy, data, merge) {
        this.encodeDefaults = encodeDefaults
    }
    inline fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, merge: Boolean = false, buildSettings: EncodeSettings.Builder.() -> Unit = {}) =
        setEncoded(documentRef, encodeAsObject(strategy, data, buildSettings), if (merge) SetOptions.Merge else SetOptions.Overwrite)

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(documentRef, strategy, data, mergeFields) { this.encodeDefaults = encodeDefaults }"))
    fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFields: String) = set(documentRef, strategy, data, *mergeFields){
        this.encodeDefaults = encodeDefaults
    }
    inline fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, vararg mergeFields: String, buildSettings: EncodeSettings.Builder.() -> Unit = {}) =
        setEncoded(documentRef, encodeAsObject(strategy, data, buildSettings), SetOptions.MergeFields(mergeFields.asList()))

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(documentRef, strategy, data, mergeFieldPaths) { this.encodeDefaults = encodeDefaults }"))
    fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) = set(documentRef, strategy, data, *mergeFieldPaths) {
        this.encodeDefaults = encodeDefaults
    }
    inline fun <T : Any> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, vararg mergeFieldPaths: FieldPath, buildSettings: EncodeSettings.Builder.() -> Unit = {}) =
        setEncoded(documentRef, encodeAsObject(strategy, data, buildSettings), SetOptions.MergeFieldPaths(mergeFieldPaths.asList()))

    @PublishedApi
    internal fun setEncoded(documentRef: DocumentReference, encodedData: EncodedObject, setOptions: SetOptions) = WriteBatch(nativeWrapper.setEncoded(documentRef, encodedData, setOptions))

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("update(documentRef, data) { this.encodeDefaults = encodeDefaults }"))
    inline fun <reified T : Any> update(documentRef: DocumentReference, data: T, encodeDefaults: Boolean) = update(documentRef, data) {
        this.encodeDefaults = encodeDefaults
    }
    inline fun <reified T : Any> update(documentRef: DocumentReference, data: T, buildSettings: EncodeSettings.Builder.() -> Unit = {}) =
        updateEncoded(documentRef, encodeAsObject(data, buildSettings))

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("update(documentRef, strategy, data) { this.encodeDefaults = encodeDefaults }"))
    fun <T : Any> update(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) = update(documentRef, strategy, data) {
        this.encodeDefaults = encodeDefaults
    }
    inline fun <T : Any> update(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, buildSettings: EncodeSettings.Builder.() -> Unit = {}) =
        updateEncoded(documentRef, encodeAsObject(strategy, data, buildSettings))

    @JvmName("updateField")
    inline fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<String, Any?>, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = updateEncodedFieldsAndValues(documentRef, encodeFieldAndValue(fieldsAndValues, buildSettings).orEmpty())
    @JvmName("updateFieldPath")
    inline fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<FieldPath, Any?>, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = updateEncodedFieldPathsAndValues(documentRef, encodeFieldAndValue(fieldsAndValues, buildSettings).orEmpty())

    @PublishedApi
    internal fun updateEncoded(documentRef: DocumentReference, encodedData: EncodedObject) = WriteBatch(nativeWrapper.updateEncoded(documentRef, encodedData))

    @PublishedApi
    internal fun updateEncodedFieldsAndValues(documentRef: DocumentReference, encodedFieldsAndValues: List<Pair<String, Any?>>) = WriteBatch(nativeWrapper.updateEncodedFieldsAndValues(documentRef, encodedFieldsAndValues))

    @PublishedApi
    internal fun updateEncodedFieldPathsAndValues(documentRef: DocumentReference, encodedFieldsAndValues: List<Pair<EncodedFieldPath, Any?>>) = WriteBatch(nativeWrapper.updateEncodedFieldPathsAndValues(documentRef, encodedFieldsAndValues))

    fun delete(documentRef: DocumentReference): WriteBatch = WriteBatch(nativeWrapper.delete(documentRef))
    suspend fun commit() = nativeWrapper.commit()
}

/** A class representing a platform specific Firebase DocumentReference. */
expect class NativeDocumentReferenceType

@PublishedApi
internal expect class NativeDocumentReference(nativeValue: NativeDocumentReferenceType) {
    val nativeValue: NativeDocumentReferenceType
    val id: String
    val path: String
    val snapshots: Flow<NativeDocumentSnapshotWrapper>
    val parent: NativeCollectionReferenceWrapper
    fun snapshots(includeMetadataChanges: Boolean = false): Flow<NativeDocumentSnapshotWrapper>

    fun collection(collectionPath: String): NativeCollectionReferenceWrapper
    suspend fun get(): NativeDocumentSnapshotWrapper
    suspend fun setEncoded(encodedData: EncodedObject, setOptions: SetOptions)
    suspend fun updateEncoded(encodedData: EncodedObject)
    suspend fun updateEncodedFieldsAndValues(encodedFieldsAndValues: List<Pair<String, Any?>>)
    suspend fun updateEncodedFieldPathsAndValues(encodedFieldsAndValues: List<Pair<EncodedFieldPath, Any?>>)
    suspend fun delete()
}

/** A class representing a Firebase DocumentReference. */
@Serializable(with = DocumentReferenceSerializer::class)
data class DocumentReference internal constructor(@PublishedApi internal val native: NativeDocumentReference) {

    internal val nativeValue get() = native.nativeValue

    val id: String get() = native.id
    val path: String get() = native.path
    val snapshots: Flow<DocumentSnapshot> get() = native.snapshots.map(::DocumentSnapshot)
    val parent: CollectionReference get() = CollectionReference(native.parent)
    fun snapshots(includeMetadataChanges: Boolean = false): Flow<DocumentSnapshot> = native.snapshots(includeMetadataChanges).map(::DocumentSnapshot)

    fun collection(collectionPath: String): CollectionReference = CollectionReference(native.collection(collectionPath))
    suspend fun get(): DocumentSnapshot = DocumentSnapshot(native.get())

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(data, merge) { this.encodeDefaults = encodeDefaults }"))
    suspend inline fun <reified T : Any> set(data: T, encodeDefaults: Boolean, merge: Boolean = false) = set(data, merge) {
        this.encodeDefaults = encodeDefaults
    }
    suspend inline fun <reified T : Any> set(data: T, merge: Boolean = false, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = native.setEncoded(
        encodeAsObject(data, buildSettings), if (merge) SetOptions.Merge else SetOptions.Overwrite)

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(data, mergeFields) { this.encodeDefaults = encodeDefaults }"))
    suspend inline fun <reified T : Any> set(data: T, encodeDefaults: Boolean, vararg mergeFields: String) = set(data, *mergeFields)  {
        this.encodeDefaults = encodeDefaults
    }
    suspend inline fun <reified T : Any> set(data: T, vararg mergeFields: String, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = native.setEncoded(
        encodeAsObject(data, buildSettings), SetOptions.MergeFields(mergeFields.asList()))

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(data, mergeFieldPaths) { this.encodeDefaults = encodeDefaults }"))
    suspend inline fun <reified T : Any> set(data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) = set(data, *mergeFieldPaths)  {
        this.encodeDefaults = encodeDefaults
    }
    suspend inline fun <reified T : Any> set(data: T, vararg mergeFieldPaths: FieldPath, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = native.setEncoded(
        encodeAsObject(data, buildSettings), SetOptions.MergeFieldPaths(mergeFieldPaths.asList()))

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(strategy, data, merge) { this.encodeDefaults = encodeDefaults }"))
    suspend fun <T : Any> set(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, merge: Boolean = false) = set(strategy, data, merge) {
        this.encodeDefaults = encodeDefaults
    }
    suspend inline fun <T : Any> set(strategy: SerializationStrategy<T>, data: T, merge: Boolean = false, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = native.setEncoded(
        encodeAsObject(strategy, data, buildSettings), if (merge) SetOptions.Merge else SetOptions.Overwrite)

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(strategy, data, mergeFields) { this.encodeDefaults = encodeDefaults }"))
    suspend fun <T : Any> set(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFields: String) = set(strategy, data, *mergeFields) {
        this.encodeDefaults = encodeDefaults
    }
    suspend inline fun <T : Any> set(strategy: SerializationStrategy<T>, data: T, vararg mergeFields: String, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = native.setEncoded(
        encodeAsObject(strategy, data, buildSettings), SetOptions.MergeFields(mergeFields.asList()))

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("set(strategy, data, mergeFieldPaths) { this.encodeDefaults = encodeDefaults }"))
    suspend fun <T : Any> set(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) = set(strategy, data, *mergeFieldPaths) {
        this.encodeDefaults = encodeDefaults
    }
    suspend inline fun <T : Any> set(strategy: SerializationStrategy<T>, data: T, vararg mergeFieldPaths: FieldPath, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = native.setEncoded(
        encodeAsObject(strategy, data, buildSettings), SetOptions.MergeFieldPaths(mergeFieldPaths.asList()))

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("update(data) { this.encodeDefaults = encodeDefaults }"))
    suspend inline fun <reified T : Any> update(data: T, encodeDefaults: Boolean) = update(data) {
        this.encodeDefaults = encodeDefaults
    }
    suspend inline fun <reified T : Any> update(data: T, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = native.updateEncoded(encodeAsObject(data, buildSettings))

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("update(strategy, data) { this.encodeDefaults = encodeDefaults }"))
    suspend fun <T : Any> update(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) = update(strategy, data)  {
        this.encodeDefaults = encodeDefaults
    }
    suspend inline fun <T : Any> update(strategy: SerializationStrategy<T>, data: T, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = native.updateEncoded(
        encodeAsObject(strategy, data, buildSettings)
    )

    @JvmName("updateFields")
    suspend inline fun update(vararg fieldsAndValues: Pair<String, Any?>, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = native.updateEncodedFieldsAndValues(encodeFieldAndValue(fieldsAndValues, buildSettings).orEmpty())

    @JvmName("updateFieldPaths")
    suspend inline fun update(vararg fieldsAndValues: Pair<FieldPath, Any?>, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = native.updateEncodedFieldPathsAndValues(encodeFieldAndValue(fieldsAndValues, buildSettings).orEmpty())

    suspend fun delete() = native.delete()
}

expect class NativeCollectionReference : NativeQuery

@PublishedApi
internal expect class NativeCollectionReferenceWrapper internal constructor(native: NativeCollectionReference) : NativeQueryWrapper {

    override val native: NativeCollectionReference

    val path: String
    val document: NativeDocumentReference
    val parent: NativeDocumentReference?

    fun document(documentPath: String): NativeDocumentReference
    suspend fun addEncoded(data: EncodedObject): NativeDocumentReference
}

data class CollectionReference internal constructor(@PublishedApi internal val nativeWrapper: NativeCollectionReferenceWrapper) : Query(nativeWrapper) {

    constructor(native: NativeCollectionReference) : this(NativeCollectionReferenceWrapper(native))

    override val native = nativeWrapper.native

    val path: String get() = nativeWrapper.path
    val document: DocumentReference get() = DocumentReference(nativeWrapper.document)
    val parent: DocumentReference? get() = nativeWrapper.parent?.let(::DocumentReference)

    fun document(documentPath: String): DocumentReference = DocumentReference(nativeWrapper.document(documentPath))

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("add(data) { this.encodeDefaults = encodeDefaults }"))
    suspend inline fun <reified T : Any> add(data: T, encodeDefaults: Boolean) = add(data) {
        this.encodeDefaults = encodeDefaults
    }
    suspend inline fun <reified T : Any> add(data: T, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = addEncoded(
        encodeAsObject(data, buildSettings)
    )

    @Deprecated("Deprecated. Use builder instead", replaceWith = ReplaceWith("add(strategy, data) { this.encodeDefaults = encodeDefaults }"))
    suspend fun <T : Any> add(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) = add(strategy, data) {
        this.encodeDefaults = encodeDefaults
    }
    suspend inline fun <T : Any> add(strategy: SerializationStrategy<T>, data: T, buildSettings: EncodeSettings.Builder.() -> Unit = {}) = addEncoded(
        encodeAsObject(strategy, data, buildSettings)
    )

    @PublishedApi
    internal suspend fun addEncoded(data: EncodedObject): DocumentReference = DocumentReference(nativeWrapper.addEncoded(data))
}

expect class FirebaseFirestoreException : FirebaseException

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
expect val FirebaseFirestoreException.code: FirestoreExceptionCode

expect enum class FirestoreExceptionCode {
    OK,
    CANCELLED,
    UNKNOWN,
    INVALID_ARGUMENT,
    DEADLINE_EXCEEDED,
    NOT_FOUND,
    ALREADY_EXISTS,
    PERMISSION_DENIED,
    RESOURCE_EXHAUSTED,
    FAILED_PRECONDITION,
    ABORTED,
    OUT_OF_RANGE,
    UNIMPLEMENTED,
    INTERNAL,
    UNAVAILABLE,
    DATA_LOSS,
    UNAUTHENTICATED
}

expect enum class Direction {
    ASCENDING,
    DESCENDING
}

expect class QuerySnapshot {
    val documents: List<DocumentSnapshot>
    val documentChanges: List<DocumentChange>
    val metadata: SnapshotMetadata
}

expect enum class ChangeType {
    ADDED ,
    MODIFIED,
    REMOVED
}

expect class DocumentChange {
    val document: DocumentSnapshot
    val newIndex: Int
    val oldIndex: Int
    val type: ChangeType
}

expect class NativeDocumentSnapshot
@PublishedApi
internal expect class NativeDocumentSnapshotWrapper internal constructor(native: NativeDocumentSnapshot) {

    val native: NativeDocumentSnapshot

    val exists: Boolean
    val id: String
    val reference: NativeDocumentReference
    val metadata: SnapshotMetadata

    fun contains(field: String): Boolean
    fun contains(fieldPath: EncodedFieldPath): Boolean

    fun getEncoded(field: String, serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE): Any?
    fun getEncoded(fieldPath: EncodedFieldPath, serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE): Any?
    fun encodedData(serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE): Any?
}

data class DocumentSnapshot internal constructor(@PublishedApi internal val nativeWrapper: NativeDocumentSnapshotWrapper) {

    constructor(native: NativeDocumentSnapshot) : this(NativeDocumentSnapshotWrapper(native))

    val native = nativeWrapper.native

    val exists: Boolean get() = nativeWrapper.exists
    val id: String get() = nativeWrapper.id
    val reference: DocumentReference get() = DocumentReference(nativeWrapper.reference)
    val metadata: SnapshotMetadata get() = nativeWrapper.metadata

    fun contains(field: String): Boolean = nativeWrapper.contains(field)
    fun contains(fieldPath: FieldPath): Boolean = nativeWrapper.contains(fieldPath.encoded)

    inline fun <reified T> get(field: String, serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE, buildSettings: DecodeSettings.Builder.() -> Unit = {}): T = decode(value = getEncoded(field, serverTimestampBehavior), buildSettings)
    inline fun <T> get(field: String, strategy: DeserializationStrategy<T>, serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE, buildSettings: DecodeSettings.Builder.() -> Unit = {}): T = decode(strategy, getEncoded(field, serverTimestampBehavior), buildSettings)

    @PublishedApi
    internal fun getEncoded(field: String, serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE): Any? = nativeWrapper.getEncoded(field, serverTimestampBehavior)

    inline fun <reified T> get(fieldPath: FieldPath, serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE, buildSettings: DecodeSettings.Builder.() -> Unit = {}): T = decode(value = getEncoded(fieldPath, serverTimestampBehavior), buildSettings)
    inline fun <T> get(fieldPath: FieldPath, strategy: DeserializationStrategy<T>, serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE, buildSettings: DecodeSettings.Builder.() -> Unit = {}): T = decode(strategy, getEncoded(fieldPath, serverTimestampBehavior), buildSettings)

    @PublishedApi
    internal fun getEncoded(fieldPath: FieldPath, serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE): Any? = nativeWrapper.getEncoded(fieldPath.encoded, serverTimestampBehavior)


    inline fun <reified T> data(serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE, buildSettings: DecodeSettings.Builder.() -> Unit = {}): T = decode(encodedData(serverTimestampBehavior), buildSettings)
    inline fun <T> data(strategy: DeserializationStrategy<T>, serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE, buildSettings: DecodeSettings.Builder.() -> Unit = {}): T = decode(strategy, encodedData(serverTimestampBehavior), buildSettings)

    @PublishedApi
    internal fun encodedData(serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE): Any? = nativeWrapper.encodedData(serverTimestampBehavior)
}

enum class ServerTimestampBehavior {
    ESTIMATE,
    NONE,
    PREVIOUS
}

expect class SnapshotMetadata {
    val hasPendingWrites: Boolean
    val isFromCache: Boolean
}

expect class FieldPath(vararg fieldNames: String) {
    companion object {
        val documentId: FieldPath
    }
    @Deprecated("Use companion object instead", replaceWith = ReplaceWith("FieldPath.documentId"))
    val documentId: FieldPath
    val encoded: EncodedFieldPath
}

expect class EncodedFieldPath
