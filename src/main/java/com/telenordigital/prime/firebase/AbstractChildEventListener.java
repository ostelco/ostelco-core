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
        public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {}

        @Override
        public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}

        @Override
        public void onChildRemoved(DataSnapshot snapshot) {}

        @Override
        public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}

        @Override
        public void onCancelled(DatabaseError error) {}
}