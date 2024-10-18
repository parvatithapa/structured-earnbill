package com.sapienter.jbilling.server.util.cxf;

import com.sapienter.jbilling.server.pricing.PriceModelWS;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlType(name = "CxfSMapDatePriceModel")
@XmlAccessorType(XmlAccessType.FIELD)
class CxfSMapDatePriceModel implements BaseCxfMap<Date, PriceModelWS, CxfSMapDatePriceModel.KeyValueEntry> {

    @XmlElement(nillable = false, name = "entry")
    List<KeyValueEntry> entries = new ArrayList<KeyValueEntry>();

    @Override
    public List<KeyValueEntry> getEntries() {
        return entries;
    }


    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "KeyValueSDatePriceModel")
    static class KeyValueEntry implements BaseCxfMap.KeyValueEntry<Date, PriceModelWS> {
        //Map keys cannot be null
        @XmlElement(required = true, nillable = false)
        Date key;
        PriceModelWS value;

        @Override
        public Date getKey() {
            return key;
        }

        @Override
        public void setKey(Date key) {
            this.key = key;
        }

        @Override
        public PriceModelWS getValue() {
            return value;
        }

        @Override
        public void setValue(PriceModelWS value) {
            this.value = value;
        }
    }
}