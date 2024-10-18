package com.sapienter.jbilling.server.util.audit;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Martin on 10/10/2016.
 */
public class LogMessage {

    String module, status, action, message, oldStr, table;
    Integer affectedUser, rowId, oldInt, moduleId;
    Date oldDate;
    Map<String, Object> properties=new HashMap<>();

    public static class Builder {
        String module, status, action, message, oldStr, table;
        Integer affectedUser, rowId, oldInt, moduleId;
        Date oldDate;
        Map<String, Object> properties=new HashMap<>();

        public Builder() {
        }

        public Builder module(String module) {
            this.module = module;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder oldStr(String oldStr) {
            this.oldStr = oldStr;
            return this;
        }

        public Builder affectedUser(Integer affectedUser) {
            this.affectedUser = affectedUser;
            return this;
        }

        public Builder table(String table) {
            this.table = table;
            return this;
        }

        public Builder rowId(Integer rowId) {
            this.rowId = rowId;
            return this;
        }

        public Builder oldInt(Integer oldInt) {
            this.oldInt = oldInt;
            return this;
        }

        public Builder moduleId(Integer moduleId) {
            this.moduleId = moduleId;
            return this;
        }

        public Builder oldDate(Date oldDate) {
            this.oldDate = oldDate;
            return this;
        }
        public Builder property(String name, Object object) {
            properties.put(name,object);
            return this;
        }

        public LogMessage build() {
            return new LogMessage(this);
        }

    }

    private LogMessage(Builder builder) {
        module = builder.module;
        status = builder.status;
        action = builder.action;
        message = builder.message;
        affectedUser = builder.affectedUser;

        oldStr = builder.oldStr;
        table = builder.table;
        moduleId = builder.moduleId;

        rowId = builder.rowId;
        oldInt = builder.oldInt;
        oldDate = builder.oldDate;
        properties=builder.properties;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendProperty(sb, "module", module);
        appendProperty(sb, "status", status);
        appendProperty(sb, "action", action);

        appendProperty(sb, "affected-user", affectedUser);
        appendProperty(sb, "table", table);
        appendProperty(sb, "row", rowId);

        appendProperty(sb, "OldInt", oldInt);
        appendProperty(sb, "OldString", oldStr);
        appendProperty(sb, "oldDate", oldDate);
        appendProperty(sb, "ModuleId", moduleId);
        for(Map.Entry<String,Object> prop:properties.entrySet()){
            appendProperty(sb, prop.getKey(), prop.getValue());
        }
        appendProperty(sb, "message", message);

        int lastComma = sb.lastIndexOf(",");
        if (lastComma == sb.length() - 1) {
            sb.deleteCharAt(lastComma);
        }

        return sb.toString();
    }

    private void appendProperty(StringBuilder sb, String propertyName, Object value) {
        if (value != null) {
            sb.append(" " + propertyName + "=\"").append(value).append("\",");
        }
    }
}
