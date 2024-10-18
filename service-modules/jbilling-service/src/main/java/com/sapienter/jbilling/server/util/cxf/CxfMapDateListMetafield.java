package com.sapienter.jbilling.server.util.cxf;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlType(name = "CxfMapDateListMetafield")
@XmlAccessorType(XmlAccessType.FIELD)
class CxfMapDateListMetafield implements BaseCxfMap<Date, List<MetaFieldValueWS>, CxfMapDateListMetafield.KeyValueEntry> {

    @XmlElement(nillable = false, name = "entry")
    List<KeyValueEntry> entries = new ArrayList<KeyValueEntry>();

    public List<KeyValueEntry> getEntries() {
        return entries;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "KeyValueMapDateListMetafield")
    static class KeyValueEntry implements BaseCxfMap.KeyValueEntry<Date, List<MetaFieldValueWS>> {
        //Map keys cannot be null
        @XmlElement(required = true, nillable = false)
        Date key;
        List<MetaFieldValueWS> value;

        public Date getKey() {
            return key;
        }

        public void setKey(Date key) {
            this.key = key;
        }

        public List<MetaFieldValueWS> getValue() {
            return value;
        }

        public void setValue(List<MetaFieldValueWS> value) {
            this.value = value;
        }
    }
}