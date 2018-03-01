package com.telenordigital.prime.firebase;

/**
 * Convenience class, so that in classes that actually do anything, it's only necessary
 * to implement those methods that actually do anything.
 */
public abstract class AbstractChildEventListener implements ChildEventListener {

    @Override
    public void onChildAdded(final DataSnapshot dataSnapshot, final String prevChildKey) {
        // Intended to be overridden in by subclass. Default is to do nothing.
    }

    @Override
    public void onChildChanged(final DataSnapshot snapshot, final String previousChildName) {
        // Intended to be overridden in by subclass. Default is to do nothing.
    }

    @Override
    public void onChildRemoved(final DataSnapshot snapshot) {
        // Intended to be overridden in by subclass. Default is to do nothing.
    }

    @Override
    public void onChildMoved(final DataSnapshot snapshot, final String previousChildName) {
        // Intended to be overridden in by subclass. Default is to do nothing.
    }

    @Override
    public void onCancelled(final DatabaseError error) {
        // Intended to be overridden in by subclass. Default is to do nothing.
    }
}
