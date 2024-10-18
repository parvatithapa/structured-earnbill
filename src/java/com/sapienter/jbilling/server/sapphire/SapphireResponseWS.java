package com.sapienter.jbilling.server.sapphire;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SapphireResponseWS {

    private String entityType;
    private Map<String, Object> entityFields;

    public SapphireResponseWS(String entityType, Map<String, Object> entityFields) {
        this.entityType = entityType;
        this.entityFields = entityFields;
    }

    @JsonProperty("entityType")
    public String getEntityType() {
        return entityType;
    }

    @JsonAnyGetter
    public Map<String, Object> getEntityFields() {
        return entityFields;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SapphireResponseWS [entityType=");
        builder.append(entityType);
        builder.append(", entityFields=");
        builder.append(entityFields);
        builder.append("]");
        return builder.toString();
    }
}
