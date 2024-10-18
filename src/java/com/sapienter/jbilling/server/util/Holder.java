package com.sapienter.jbilling.server.util;

/**
 * Generic class which contains a reference to an object
 */
public class Holder<T> {
    T target;

    public Holder() {
    }

    public Holder(T target) {
        this.target = target;
    }

    public T getTarget() {
        return target;
    }

    public void setTarget(T target) {
        this.target = target;
    }
}
