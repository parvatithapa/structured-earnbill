package com.sapienter.jbilling.server.util.cxf;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.*;

/**
 * @author Gerhard
 * @since 19/12/13
 */
public class CxfMapMapListMetafieldAdapter extends XmlAdapter<CxfMapMapListMetafield, Map<Integer, Map<Date, List<MetaFieldValueWS>>>> {

    @Override
    public Map unmarshal(CxfMapMapListMetafield v) throws Exception {
        Map map = new LinkedHashMap();
        for(CxfMapMapListMetafield.KeyValueEntry e : v.entries) {
            Map<Date, List<MetaFieldValueWS>> entryMap = new HashMap<Date, List<MetaFieldValueWS>>();
            for(CxfMapDateListMetafield.KeyValueEntry e2 : e.value.entries) {
                entryMap.put(e2.getKey(), e2.getValue());
            }
            map.put(e.key, entryMap);
        }
        return map;
    }

    @Override
    public CxfMapMapListMetafield marshal(Map<Integer, Map<Date, List<MetaFieldValueWS>>> v) throws Exception {
        CxfMapMapListMetafield map = new CxfMapMapListMetafield();
        for(Object o : v.entrySet()) {
            Map.Entry e = (Map.Entry)o;
            CxfMapMapListMetafield.KeyValueEntry kve = new CxfMapMapListMetafield.KeyValueEntry();
            kve.key = (Integer)e.getKey();
            Map<Date, List<MetaFieldValueWS>> eValue = (Map<Date, List<MetaFieldValueWS>>)e.getValue();
            CxfMapDateListMetafield map2 = new CxfMapDateListMetafield();
            kve.value = map2;
            for(Object o2: eValue.entrySet()) {
                Map.Entry e2 = (Map.Entry)o2;
                CxfMapDateListMetafield.KeyValueEntry kve2 = new CxfMapDateListMetafield.KeyValueEntry();
                kve2.key = (Date)e2.getKey();
                kve2.value = (List<MetaFieldValueWS>) e2.getValue();
                map2.entries.add(kve2);
            }
            map.entries.add(kve);
        }
        return map;
    }
}
