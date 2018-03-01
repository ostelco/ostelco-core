package com.telenordigital.prime.firebase;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

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
