package com.telenordigital.prime.firebase;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

/**
 * Convenience class, so that in classes that actually do anything, it's only necessary
 * to implement those methods that actually do anything.
 */
public  abstract class AbstractChildEventListener implements ChildEventListener {

    @Override
    public void onChildAdded(final DataSnapshot dataSnapshot, final String prevChildKey) {}

    @Override
    public void onChildChanged(final DataSnapshot snapshot, final String previousChildName) {}

    @Override
    public void onChildRemoved(final DataSnapshot snapshot) {}

    @Override
    public void onChildMoved(final DataSnapshot snapshot, final String previousChildName) {}

    @Override
    public void onCancelled(final DatabaseError error) {}
}