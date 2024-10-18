package com.sapienter.jbilling.server.util.cxf;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Gerhard
 * @since 19/12/13
 */
public abstract class BaseCxfMapAdapter<K, V, S extends BaseCxfMap.KeyValueEntry, T extends BaseCxfMap<K, V, S>> extends XmlAdapter<T, Map<K, V>> {

    @Override public Map<K, V> unmarshal(T v) throws Exception {
        Map map = createJavaMap();
        for(BaseCxfMap.KeyValueEntry e : v.getEntries()) {
            map.put(e.getKey(), e.getValue());
        }
        return map;
    }

    @Override public T marshal(Map<K, V> v) throws Exception {
        T map = createCxfMap(); //new BaseCxfMap<K,V>();
        for(Object o : v.entrySet()) {
            Map.Entry e = (Map.Entry)o;

            S kve = createEntry();
            kve.setKey((K)e.getKey());
            kve.setValue(e.getValue());
            map.getEntries().add(kve);
        }
        return map;
    }

    protected Map createJavaMap() {
        return new LinkedHashMap();
    }

    protected abstract T createCxfMap();

    protected abstract S createEntry();
}
