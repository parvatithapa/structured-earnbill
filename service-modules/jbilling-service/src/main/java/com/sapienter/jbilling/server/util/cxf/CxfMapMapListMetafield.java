package com.sapienter.jbilling.server.util.cxf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlType(name = "CxfMapMapListMetafield")
@XmlAccessorType(XmlAccessType.FIELD)
class CxfMapMapListMetafield {

    @XmlElement(nillable = false, name = "entry")
    List<KeyValueEntry> entries = new ArrayList<KeyValueEntry>();

    List getEntries() {
        return entries;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "KeyValueMapMapListMetafield")
    static class KeyValueEntry {
        //Map keys cannot be null
        @XmlElement(required = true, nillable = false)
        Integer key;
        CxfMapDateListMetafield value;
    }
}