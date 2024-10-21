package com.sapienter.jbilling.server.item;
/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */


import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlTransient;

/**
 * Created by marcolin on 06/10/15.
 */
public class PricingField implements Serializable {

    private static final PricingField[] EMPTY_FIELDS = new PricingField[0];
    private static final String STRING_ENCODING = "UTF-8";
    private static final String DELIMITER = ",";
    private static final String EMPTY = "";
    private static final String COLONS = ":";
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mma");
    private String name;
    private Type type;
    private Integer position = 1;
    private String value = null;
    private long resultId; // at the time, only used for mediation of batch

    public enum Type { STRING, INTEGER, DECIMAL, DATE, BOOLEAN, LONG }

    public PricingField() { }

    /**
     * Constructs a new PricingField from a given encoded String.
     *
     * This constructor is designed for internal use only.
     *
     * @see #encode(PricingField)
     * @param encoded encoded string to parse
     */
    public PricingField(String encoded) {
        String[] fields = encoded.split(COLONS, -1);

        if (fields == null || !(fields.length == 4 || fields.length == 2)) {
            this.name = EMPTY;
            this.type = Type.INTEGER;
            this.value = "0";
            return;
        }

        try {
            this.name = fields[0] != null ? URLDecoder.decode(fields[0], STRING_ENCODING) : fields[0];
            if(fields.length == 4) {
                this.position = Integer.parseInt(fields[1]);
                this.type = mapType(fields[2]);
                this.value = fields[3].equals("null") ? null : URLDecoder.decode(fields[3], STRING_ENCODING);
            }
            if(fields.length == 2) {
                this.value = fields[1].equals("null") ? null : URLDecoder.decode(fields[1], STRING_ENCODING);
            }
        } catch (UnsupportedEncodingException e) {
            // Should never happen
        }
    }

    /**
     * Copy constructor, creates a new instance of the given PricingField with
     * the same member values.
     *
     * @param field pricing field to copy
     */
    public PricingField(PricingField field) {
        this.name = field.getName();
        this.type = field.getType();
        this.position = field.getPosition();
        this.value = field.getStrValue();
    }

    /**
     * Constructs a new PricingField of type {@code STRING}
     *
     * @param name field name
     * @param value field value
     */
    public PricingField(String name, String value) {
        this.name = name;
        this.type = Type.STRING;
        setStrValue(value);
    }

    /**
     * Constructs a new PricingField of type {@code DATE}
     *
     * @param name field name
     * @param value field value
     */
    public PricingField(String name, Date value) {
        this.name = name;
        this.type = Type.DATE;
        setDateValue(value);
    }

    /**
     * Constructs a new PricingField of type {@code INTEGER}
     *
     * @param name field name
     * @param value field value
     */
    public PricingField(String name, Integer value) {
        this.name = name;
        this.type = Type.INTEGER;
        setIntValue(value);
    }

    /**
     * Constructs a new PricingField of type {@code DECIMAL}
     *
     * @param name field name
     * @param value field value
     */
    public PricingField(String name, BigDecimal value) {
        this.name = name;
        this.type = Type.DECIMAL;
        setDecimalValue(value);
    }

    /**
     * Constructs a new PricingField of type {@code BOOLEAN}
     *
     * @param name field name
     * @param value field value
     */
    public PricingField(String name, Boolean value) {
        this.name = name;
        this.type = Type.BOOLEAN;
        setBooleanValue(value);
    }

    public PricingField(String name, Long value) {
        this.name = name;
        this.type = Type.LONG;
        setLongValue(value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getPosition() {
        return position;
    }

    public long getResultId() {
        return resultId;
    }

    public void setResultId(long resultId) {
        this.resultId = resultId;
    }

    /**
     * Returns this pricing fields value as a raw type.
     *
     * @return pricing field value
     */
    public Object getValue() {
        switch (type) {
            case STRING  : return value;
            case DATE    : return getDateValue();
            case INTEGER : return getIntValue();
            case DECIMAL : return getDecimalValue();
            case BOOLEAN : return getBooleanValue();
            case LONG    : return getLongValue();
            default: return null;
        }
    }

    @XmlTransient
    public String getStrValue() {
        return value;
    }

    public void setStrValue(String value) {
        this.value = value;
    }


    @XmlTransient
    public Date getDateValue() {
        if (value == null) return null;
        return new Date(Long.parseLong(value));
    }

    public void setDateValue(Date value) {
        if (value != null) {
            this.value = String.valueOf(value.getTime());
        } else {
            this.value = null;
        }
    }

    @XmlTransient
    public Calendar getCalendarValue() {
        if (value == null) return null;

        Calendar cal = Calendar.getInstance();
        cal.setTime(getDateValue());
        return cal;
    }

    @XmlTransient
    public Integer getIntValue() {
        if (value == null) return null;
        return Integer.valueOf(value);
    }

    public void setIntValue(Integer value) {
        if (value != null) {
            this.value = value.toString();
        } else {
            this.value = null;
        }
    }

    @XmlTransient
    public BigDecimal getDecimalValue() {
        if (value == null) return null;
        return new BigDecimal(value);
    }

    public void setDecimalValue(BigDecimal value) {
        if (value != null) {
            this.value = value.toString();
        } else {
            this.value = null;
        }
    }

    /**
     * Returns the decimal value as a double. This method is provided for backwards
     * compatibility, use {@link PricingField#getDecimalValue()} instead.
     *
     * @return decimal value as a double
     */
    @XmlTransient
    public Double getDoubleValue() {
        BigDecimal value = getDecimalValue();
        return (value != null ? value.doubleValue() : null);
    }

    /**
     * @see #getDoubleValue()
     * @return decimal value as a float
     */
    @XmlTransient
    public Double getFloatValue() {
        return getDoubleValue();
    }

    public void setBooleanValue(Boolean value) {
        if (value != null) {
            this.value = value.toString();
        } else {
            this.value = null;
        }
    }

    @XmlTransient
    public Boolean getBooleanValue() {
        if (value == null) return null;
        return Boolean.valueOf(this.value);
    }

    @XmlTransient
    public Long getLongValue() {
        if (value == null) {
            return null;
        }
        return Long.valueOf(this.value);
    }

    public void setLongValue(Long value) {
        if (value != null) {
            this.value = value.toString();
        } else {
            this.value = null;
        }
    }

    /**
     * Returns an appropriate {@link com.sapienter.jbilling.server.item.PricingField.Type} for the given string, or null if no matching type found.
     *
     * Type strings:
     *      string
     *      integer
     *      float
     *      double
     *      decimal
     *      date
     *      boolean
     *
     * @param myType type string
     * @return matching type
     */
    public static Type mapType(String myType) {  // todo: should be a member of the Type enum eg, Type$fromString(...);
        if (myType.equalsIgnoreCase("string")) {
            return Type.STRING;
        } else if (myType.equalsIgnoreCase("integer")) {
            return Type.INTEGER;
        } else if (myType.equalsIgnoreCase("float") || myType.equalsIgnoreCase("double") || myType.equalsIgnoreCase("decimal")) {
            return Type.DECIMAL;
        } else if (myType.equalsIgnoreCase("date")) {
            return Type.DATE;
        } else if (myType.equalsIgnoreCase("boolean")) {
            return Type.BOOLEAN;
        } else if (myType.equalsIgnoreCase("long")) {
            return Type.LONG;
        } else {
            return null;
        }
    }

    /**
     * Encodes a pricing field as a string. The encoded string is a semi-colon
     * delimited string in the format {@code :name:position:type:value}, where name and position are
     * optional.
     *
     * Example:
     *      :src::string:310-1010
     *      :dst::string:1-800-123-4567
     *      :userid:integer:1234
     *
     * @param field field to encode
     * @return encoded string
     */
    public static String encode(PricingField field) {
        StringBuffer sb = new StringBuffer();
        try {
            sb.append(URLEncoder.encode(field.getName(), STRING_ENCODING))
                    .append(':')
                    .append(field.getPosition());

            switch(field.getType()) {
                case STRING:
                    sb.append(":string:");
                    break;

                case INTEGER:
                    sb.append(":integer:");
                    break;

                case LONG:
                    sb.append(":long:");
                    break;

                case DECIMAL:
                    sb.append(":float:");
                    break;

                case DATE:
                    sb.append(":date:");
                    break;
                case BOOLEAN:
                    sb.append(":boolean:");
                    break;
            }

            sb.append(field.getStrValue() != null ? URLEncoder.encode(field.getStrValue(), STRING_ENCODING) : field.getStrValue());
        } catch (UnsupportedEncodingException e) {}

        return sb.toString();
    }

    /**
     * Parses a comma separated list of encoded PricingField strings and returns
     * an array of fields.
     *
     * @param pricingFields comma separated list of encoded pricing field strings
     * @return array of fields
     */
    public static PricingField[] getPricingFieldsValue(String pricingFields) {
        if (pricingFields == null) {
            return EMPTY_FIELDS;
        }

        String[] fields = pricingFields.split(DELIMITER);
        if (fields.length == 0) {
            return EMPTY_FIELDS;
        }

        List<PricingField> result = new ArrayList<PricingField>();
        for (String field : fields) {
            if (field != null && !field.equals(EMPTY)) {
                String[] tempFields = field.split(COLONS, -1);
                double length = tempFields.length;
                if (length == 4 || length == 2) {
                    result.add(new PricingField(field));
                }
            }
        }
        return result.toArray(new PricingField[result.size()]);
    }

    public static String[] getPricingFieldsValue(String pricingFields, List<String> headers) {
        final List<PricingField> fields = new ArrayList<>();
        if (pricingFields != null) {
            fields.addAll(Arrays.stream(pricingFields.split(DELIMITER))
                                .map(PricingField::new)
                                .collect(Collectors.toList()));
        }

        return headers.stream()
                      .map(header -> PricingField.getPricingFieldByHeader(header, fields))
                      .toArray(String[]::new);
    }

    private static String getPricingFieldByHeader(String field, List<PricingField> fields){
        PricingField fieldValue = fields.stream()
                                        .filter(f -> f.getName().equals(field))
                                        .findFirst()
                                        .orElse(null);

        if ( null != fieldValue && null != fieldValue.getValue()) {
            if (fieldValue.getType().equals(Type.DATE)) {
                return LocalDateTime.ofInstant(((Date)  fieldValue.getValue()).toInstant(), ZoneOffset.UTC).format(dateFormatter);
            } else {
                return fieldValue.getValue().toString();
            }
        } else {
            return EMPTY;
        }
    }

    /**
     * Returns a comma separated list of encoded PricingField strings from the given
     * array of fields.
     *
     * @param pricingFields array of fields to convert
     * @return comma separated list of encoded pricing field strings
     */
    public static String setPricingFieldsValue(PricingField[] pricingFields) {
        PricingField[] fields = pricingFields; // defensive copy
        StringBuffer result = new StringBuffer();
        if (fields != null && fields.length > 0) {
            for (int i = 0; i < fields.length; i++) {
                result.append(PricingField.encode(fields[i]));
                if (i < (fields.length - 1)) {
                    result.append(',');
                }
            }
        }
        return result.toString();
    }

    /**
     * Convenience method to find a pricing field by name.
     *
     * @param fields pricing fields
     * @param fieldName name
     * @return found pricing field or null if no field found.
     */
    public static PricingField find(List<PricingField> fields, String fieldName) {
        if(fields != null) {
            for (PricingField field : fields) {
                if (field.getName().equals(fieldName)){
                    return field;}
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "name: " + name
                + " type: " + type
                + " value: " + getValue()
                + " position: " + position
                + " resultId: " + resultId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PricingField that = (PricingField) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (position != null ? !position.equals(that.position) : that.position != null) return false;
        if (type != that.type) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (position != null ? position.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    public static String setPricingFieldsValue(List<PricingField> pricingFields) {
        PricingField[] fields = pricingFields.toArray(new PricingField[0]);
        return setPricingFieldsValue(fields);
    }

    public static void add(List<PricingField> fields, PricingField pricingField) {
        PricingField fieldToAdd = PricingField.find(fields, pricingField.getName());
        if (fieldToAdd != null) {
            fields.remove(fieldToAdd);
        }
        fields.add(pricingField);
    }

    public static void addAll(List<PricingField> fields, List<PricingField> fieldsToAdd) {
        for(PricingField fieldToAdd: fieldsToAdd) {
            PricingField.add(fields, fieldToAdd);
        }
    }

}
