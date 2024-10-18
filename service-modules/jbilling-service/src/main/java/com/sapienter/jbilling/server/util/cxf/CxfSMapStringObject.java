package com.sapienter.jbilling.server.util.cxf;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;

/**
 * 
 * @author jbilling-pranay
 * Date: 26-03-2019
 *
 */
@XmlType(name = "CxfSMapStringObject")
@XmlAccessorType(XmlAccessType.FIELD)
public class CxfSMapStringObject implements BaseCxfMap<String, Object, CxfSMapStringObject.StringObjectEntry> {

    @XmlElement(nillable = false, name = "entry")
    List<StringObjectEntry> entries = new ArrayList<>();

    @Override
    public List<StringObjectEntry> getEntries() {
        return entries;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "KeyValueStringObject")
    static class StringObjectEntry implements BaseCxfMap.KeyValueEntry<String, Object> {
        @Size(min = 0, max = 1000, message = "validation.error.size,0,1000")
        private String stringValue;
        private char[] charValue;
        @ConvertToTimezone
        private Date dateValue;
        private Boolean booleanValue;
        @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number")
        private String decimalValue;
        private Integer integerValue;
        private String[] listValue;

        @XmlElement(required = true, nillable = false)
        String key;
        @XmlElement(required = true, nillable = false)
        Object value;

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public void setKey(String key) {
            this.key = key;
        }

        @Override
        public Object getValue() {
            if (getStringValue() != null) {
                return getStringValue();
            } else if (getDateValue() != null) {
                return getDateValue();
            } else if (getBooleanValue() != null) {
                return getBooleanValue();
            } else if (getDecimalValue() != null) {
                return getDecimalValueAsDecimal();
            } else if (getIntegerValue() != null) {
                return getIntegerValue();
            } else if (getListValue() != null) {
                return getListValueAsList();
            } else if (getCharValue() != null) {
                return getCharValue();
            }
            return null;
        }

        @Override
        public void setValue(Object value) {
            setStringValue(null);
            setDateValue(null);
            setBooleanValue(null);
            setDecimalValue(null);
            setIntegerValue(null);
            setCharValue(null);

            if (value == null) return;
            if (value instanceof String) {
                setStringValue((String) value);
            } else if (value instanceof Date) {
                setDateValue((Date) value);
            } else if (value instanceof Boolean) {
                setBooleanValue((Boolean) value);
            } else if (value instanceof BigDecimal) {
                setBigDecimalValue((BigDecimal) value);
            } else if (value instanceof Integer) {
                setIntegerValue((Integer) value);
            } else if (value instanceof List) {
                // store List<String> as String[] for WS-compatible mode, perform manual convertion
                setListValue(((List<String>) value).toArray(new String[((List<String>) value).size()]));
            } else if (value instanceof String[]) {
                setListValue((String[]) value);
            } else if (value instanceof char[]) {
                setCharValue((char[]) value);
            }
        }

        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }

        public char[] getCharValue() {
            return charValue;
        }

        public void setCharValue(char[] charValue) {
            this.charValue = charValue;
        }

        public Date getDateValue() {
            return dateValue;
        }

        public void setDateValue(Date dateValue) {
            this.dateValue = dateValue;
        }

        public Boolean getBooleanValue() {
            return booleanValue;
        }

        public void setBooleanValue(Boolean booleanValue) {
            this.booleanValue = booleanValue;
        }

        public String getDecimalValue() {
            return decimalValue;
        }

        public void setDecimalValue(String decimalValue) {
            this.decimalValue = decimalValue;
        }

        public Integer getIntegerValue() {
            return integerValue;
        }

        public void setIntegerValue(Integer integerValue) {
            this.integerValue = integerValue;
        }

        public String[] getListValue() {
            return listValue;
        }

        public void setListValue(String[] listValue) {
            this.listValue = listValue;
        }

        public BigDecimal getDecimalValueAsDecimal() {
            return Util.string2decimal(decimalValue);
        }

        public void setBigDecimalValue(BigDecimal decimalValue) {
            this.decimalValue = decimalValue != null ? decimalValue.toPlainString() : null;
        }

        @SuppressWarnings("rawtypes")
        public List getListValueAsList() {
            if (listValue != null) {
                return new LinkedList<String>(Arrays.asList(listValue));
            } else {
                return Collections.emptyList();
            }
        }
    }
}
