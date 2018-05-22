package org.ostelco.prime.storage.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

abstract class AbstractValueEventListener : ValueEventListener {
    override fun onDataChange(snapshot: DataSnapshot) {
        // Intentionally left blank.
    }

    override fun onCancelled(error: DatabaseError) {
        // Intentionally left blank.
    }
}
