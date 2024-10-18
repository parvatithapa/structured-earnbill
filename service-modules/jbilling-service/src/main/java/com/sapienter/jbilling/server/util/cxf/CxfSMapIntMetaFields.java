package com.sapienter.jbilling.server.util.cxf;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@XmlType(name = "CxfSMapIntMetaFields")
@XmlAccessorType(XmlAccessType.FIELD)
class CxfSMapIntMetaFields implements BaseCxfMap<Integer, MetaFieldValueWS[], CxfSMapIntMetaFields.KeyValueEntry> {

    @XmlElement(nillable = false, name = "entry")
    List<KeyValueEntry> entries = new ArrayList<KeyValueEntry>();

    @Override
    public List<KeyValueEntry> getEntries() {
        return entries;
    }

    @Override
    public String toString() {
        return "CxfSMapIntMetaFields{" +
                "entries=" + entries +
                '}';
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "KeyValueSIntMetaFields")
    static class KeyValueEntry implements BaseCxfMap.KeyValueEntry<Integer, MetaFieldValueWS[]> {
        //Map keys cannot be null
        @XmlElement(required = true, nillable = false)
        Integer key;
        MetaFieldValueWS[] value;

        @Override
        public Integer getKey() {
            return key;
        }

        @Override
        public void setKey(Integer key) {
            this.key = key;
        }

        @Override
        public MetaFieldValueWS[] getValue() {
            return value;
        }

        @Override
        public void setValue(MetaFieldValueWS[] value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "KeyValueEntry{" +
                    "key=" + key +
                    ", value=" + Arrays.toString(value) +
                    '}';
        }
    }
}