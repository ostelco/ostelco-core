package com.telenordigital.prime.firebase;

public  abstract class AbstractValueEventListener implements ValueEventListener {
    @Override
    public void onDataChange(final DataSnapshot snapshot) {
        // Intentionally left blank.
    }

    @Override
    public void onCancelled(final DatabaseError error) {
        // Intentionally left blank.
    }
}
