package com.sapienter.jbilling.server.payment.tasks.westpac;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

class ErrorData {
    private String fieldName;
    private String message;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ErrorData [fieldName=" + fieldName + ", message=" + message + "]";
    }
}

public class PayWayError {
    private List<ErrorData> data;

    public List<ErrorData> getData() {
        return data;
    }

    public void setData(List<ErrorData> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "PayWayError [data=" + data + "]";
    }

    @JsonIgnore
    public String getErrorMessage() {
        if(CollectionUtils.isEmpty(data)) {
            return "";
        }
        StringBuilder error = new StringBuilder();
        error.append("Invalid Fields [")
             .append(data.stream().map(ErrorData::getFieldName).collect(Collectors.joining(",")))
             .append("]");
        return error.toString();
    }
}
