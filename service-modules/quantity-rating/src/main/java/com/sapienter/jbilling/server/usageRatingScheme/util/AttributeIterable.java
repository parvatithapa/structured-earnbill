package com.sapienter.jbilling.server.usageRatingScheme.util;


public class AttributeIterable<T> {

    private String key;
    private String value;
    private Iterable<T> iterable;

    private AttributeIterable() {}

    public AttributeIterable(Iterable<T> iterable, String key, String value) {
        this.key = key;
        this.value = value;
        this.iterable = iterable;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Iterable<T> getIterable() {
        return iterable;
    }
}
