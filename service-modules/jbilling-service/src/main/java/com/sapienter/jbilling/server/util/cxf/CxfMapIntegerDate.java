package com.sapienter.jbilling.server.util.cxf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlType(name = "CxfMapIntegerDate")
@XmlAccessorType(XmlAccessType.FIELD)
class CxfMapIntegerDate implements BaseCxfMap<Integer, Date, CxfMapIntegerDate.KeyValueEntry> {

    @XmlElement(nillable = false, name = "entry")
    List<KeyValueEntry> entries = new ArrayList<KeyValueEntry>();

    @Override
    public List<KeyValueEntry> getEntries() {
        return entries;
    }


    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "KeyValueIntegerDate")
    static class KeyValueEntry implements BaseCxfMap.KeyValueEntry<Integer, Date> {
        //Map keys cannot be null
        @XmlElement(required = true, nillable = false)
        Integer key;
        Date value;

        @Override
        public Integer getKey() {
            return key;
        }

        @Override
        public void setKey(Integer key) {
            this.key = key;
        }

        @Override
        public Date getValue() {
            return value;
        }

        @Override
        public void setValue(Date value) {
            this.value = value;
        }
    }
}