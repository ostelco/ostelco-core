package org.ostelco.dropwizard.firebase;

public final class FirebaseSetupException extends Exception {
    public FirebaseSetupException(final Throwable t) {
        super(t);
    }

    public FirebaseSetupException(final String s, final Throwable t) {
        super(s, t);
    }
    
    public FirebaseSetupException(final String s) {
        super(s);
    }
}
