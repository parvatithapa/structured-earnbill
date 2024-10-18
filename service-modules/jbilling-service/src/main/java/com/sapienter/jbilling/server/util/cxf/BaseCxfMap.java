package com.sapienter.jbilling.server.util.cxf;

import java.util.List;

public interface BaseCxfMap<K, V, T > {

    public List<T> getEntries();

    public interface KeyValueEntry<K, V> {
        public K getKey();
        public void setKey(K key);
        public V getValue();
        public void setValue(V value);
    }
}