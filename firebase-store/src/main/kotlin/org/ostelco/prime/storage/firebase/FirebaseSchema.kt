package org.ostelco.prime.storage.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.ostelco.prime.logger
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

private val config = FirebaseConfigRegistry.firebaseConfig

const val TIMEOUT: Long = 10 //sec

class EntityType<E>(
        val path: String,
        val entityClass: Class<E>)

class EntityStore<E>(
        firebaseDatabase: FirebaseDatabase,
        private val entityType: EntityType<E>) {

    private val LOG by logger()

    val databaseReference: DatabaseReference = firebaseDatabase.getReference("/${config.rootPath}/${entityType.path}")

    /**
     * Get Entity by Id
     *
     * @param id
     * @return Entity
     */
    fun get(id: String, reference: EntityStore<E>.() -> DatabaseReference = { databaseReference }): E? {
        var entity: E? = null
        val countDownLatch = CountDownLatch(1);
        reference().child(urlEncode(id)).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError?) {
                        countDownLatch.countDown()
                    }

                    override fun onDataChange(snapshot: DataSnapshot?) {
                        if (snapshot != null) {
                            entity = snapshot.getValue(entityType.entityClass)
                        }
                        countDownLatch.countDown()
                    }
                })
        countDownLatch.await(TIMEOUT, SECONDS)
        return entity
    }

    /**
     * Get all Entities
     *
     * @param reference This is a Function which returns DatabaseReference to be used.
     *                  Default value is a function which returns base path for that EntityType.
     *                  For special cases, it allows to use child path for getAll operation.
     *
     * @return Map of <id,Entity>
     */
    fun getAll(reference: EntityStore<E>.() -> DatabaseReference = { databaseReference }): Map<String, E> {
        val entities: MutableMap<String, E> = LinkedHashMap()
        val countDownLatch = CountDownLatch(1);
        reference().addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError?) {
                        countDownLatch.countDown()
                    }

                    override fun onDataChange(snapshot: DataSnapshot?) {
                        if (snapshot != null) {
                            for (child in snapshot.children) {
                                val value = child.getValue(entityType.entityClass)
                                entities.put(urlDecode(child.key), value)
                            }
                        }
                        countDownLatch.countDown()
                    }
                })
        countDownLatch.await(TIMEOUT, SECONDS)
        return entities
    }

    fun urlEncode(value: String) =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                    .replace(oldValue = ".", newValue = "%2E")

    private fun urlDecode(value: String) =
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
                    .replace(oldValue = "%2E", newValue = ".")

    /**
     * Check if entity exists for a given value
     */
    private fun exists(id: String, reference: EntityStore<E>.() -> DatabaseReference = { databaseReference }) = get(id, reference) != null

    /**
     * Inverse of exists
     */
    private fun dontExists(id: String, reference: EntityStore<E>.() -> DatabaseReference = { databaseReference }) = !exists(id, reference)

    /**
     * Create Entity for given id
     *
     * @return success
     */
    fun create(id: String, entity: E): Boolean {
        // fail if already exist
        if (exists(id)) {
            LOG.warn("Failed to create. id {} already exists", id)
            return false
        }
        return set(id, entity)
    }

    /**
     * Create Entity with auto-gen id, or null
     *
     * @param entity Entity to be added
     * @param reference This is a Function which returns DatabaseReference to be used.
     *                  Default value is a function which returns base path for that EntityType.
     *                  For special cases, it allows to use child path for add operation.
     *
     *
     * @return id, or null
     */
    fun add(entity: E, reference: EntityStore<E>.() -> DatabaseReference = { databaseReference }): String? {
        val newPushedEntry = reference().push()
        val future = newPushedEntry.setValueAsync(entity)
        future.get(TIMEOUT, SECONDS)
        return if (future.isDone) newPushedEntry.key else null
    }

    /**
     * Update Entity for given id
     *
     * @return success
     */
    fun update(id: String, entity: E): Boolean {
        if (dontExists(id)) {
            LOG.warn("Failed to update. id {} does not exists", id)
            return false
        }
        return set(id, entity)
    }

    /**
     * Set Entity for given id
     *
     * @return success
     */
    fun set(id: String, entity: E, reference: EntityStore<E>.() -> DatabaseReference = { databaseReference }): Boolean {
        val future = reference().child(urlEncode(id)).setValueAsync(entity)
        future.get(TIMEOUT, SECONDS)
        return future.isDone
    }

    /**
     * Delete Entity for given id
     *
     * @param id
     * @param checkIfExists Optional parameter if you want to skip checking if entry exists.
     *
     * @return success
     */
    fun delete(id: String, checkIfExists: Boolean = true, reference: EntityStore<E>.() -> DatabaseReference = { databaseReference }): Boolean {
        if (checkIfExists && dontExists(id, reference)) {
            return false
        }
        val future = reference().child(urlEncode(id)).removeValueAsync()
        future.get(TIMEOUT, SECONDS)
        return future.isDone
    }
}