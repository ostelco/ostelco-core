package org.ostelco.prime.storage.entities;

import org.ostelco.prime.storage.AsMappable;

import java.util.HashMap;
import java.util.Map;

public interface Subscriber extends AsMappable {

    long getNoOfBytesLeft();

    String getMsisdn();

    @Override
    default Map<String, Object> asMap() {
        final Map<String, Object> result = new HashMap<>();
        result.put("noOfBytesLeft", getNoOfBytesLeft());
        result.put("msisdn", getMsisdn());
        return result;
    }
}
