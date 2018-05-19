package org.ostelco.prime.storage.firebase

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError


/**
 * Convenience class, so that in classes that actually do anything, it's only necessary
 * to implement those methods that actually do anything.
 */
abstract class AbstractChildEventListener : ChildEventListener {

    override fun onChildAdded(dataSnapshot: DataSnapshot, prevChildKey: String?) {
        // Intended to be overridden in by subclass. Default is to do nothing.
    }

    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
        // Intended to be overridden in by subclass. Default is to do nothing.
    }

    override fun onChildRemoved(snapshot: DataSnapshot) {
        // Intended to be overridden in by subclass. Default is to do nothing.
    }

    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
        // Intended to be overridden in by subclass. Default is to do nothing.
    }

    override fun onCancelled(error: DatabaseError) {
        // Intended to be overridden in by subclass. Default is to do nothing.
    }
}
