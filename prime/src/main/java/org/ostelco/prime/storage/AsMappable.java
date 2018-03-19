package org.ostelco.prime.storage;

import java.util.Map;

public interface AsMappable {
    /**
     * Returns a map that is intended to be written to a Firebase subtree.
     *
     * @return A map representing the content of the instance implementing
     *         this interface.
     */
    Map<String, Object> asMap();
}
