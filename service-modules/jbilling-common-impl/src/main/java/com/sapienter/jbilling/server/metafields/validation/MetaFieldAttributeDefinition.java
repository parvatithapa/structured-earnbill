package com.sapienter.jbilling.server.metafields.validation;

/**
 * Created by marcolin on 29/10/15.
 */
public class MetaFieldAttributeDefinition {

    public enum Type { STRING, TIME, INTEGER, DECIMAL }

    private String name;
    private Type type = Type.STRING;
    private boolean required = false;

    public MetaFieldAttributeDefinition() {
    }

    public MetaFieldAttributeDefinition(String name) {
        this.name = name;
    }

    public MetaFieldAttributeDefinition(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public MetaFieldAttributeDefinition(String name, Type type, boolean required) {
        this.name = name;
        this.type = type;
        this.required = required;
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

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    @Override
    public String toString() {
        return "Attribute{" + type.name() + ": " + name + (required ? "(required)" : "") + "}";
    }
}
