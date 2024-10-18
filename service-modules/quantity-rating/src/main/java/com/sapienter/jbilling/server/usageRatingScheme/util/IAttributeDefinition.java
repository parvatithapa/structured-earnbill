package com.sapienter.jbilling.server.usageRatingScheme.util;

public interface IAttributeDefinition {

    enum Type {
        STRING,
        INTEGER,
        DECIMAL
    }

    enum InputType {
        TEXT,
        SELECT
    }

    String getName();

    Type getType();

    boolean isRequired();

    InputType getInputType();

    AttributeIterable getIterable();
}
