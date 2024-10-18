package com.sapienter.jbilling.server.util.cxf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlType(name = "CxfSMapStringString")
@XmlAccessorType(XmlAccessType.FIELD)
class CxfSMapStringString implements BaseCxfMap<String, String, CxfSMapStringString.KeyValueEntry> {

    @XmlElement(nillable = false, name = "entry")
    List<KeyValueEntry> entries = new ArrayList<KeyValueEntry>();

    @Override
    public List<KeyValueEntry> getEntries() {
        return entries;
    }


    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "KeyValueSStringString")
    static class KeyValueEntry implements BaseCxfMap.KeyValueEntry<String, String> {
        //Map keys cannot be null
        @XmlElement(required = true, nillable = false)
        String key;
        String value;

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public void setKey(String key) {
            this.key = key;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public void setValue(String value) {
            this.value = value;
        }
    }
}