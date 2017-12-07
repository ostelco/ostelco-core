package com.telenordigital.prime.firebase;

import java.util.Map;

public interface AsMappable {
    /**
     * Returns a map that is intended to be written to a Firebase subtree.
     * @return
     */
     Map<String, Object> asMap();
}
