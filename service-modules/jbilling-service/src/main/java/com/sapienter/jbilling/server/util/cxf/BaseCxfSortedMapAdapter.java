package com.sapienter.jbilling.server.util.cxf;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Gerhard
 * @since 19/12/13
 */
public abstract class BaseCxfSortedMapAdapter<K, V, S extends BaseCxfMap.KeyValueEntry, T extends BaseCxfMap<K, V, S>> extends BaseCxfMapAdapter<K, V, S, T> {

    protected Map createJavaMap() {
        return new TreeMap();
    }
}
