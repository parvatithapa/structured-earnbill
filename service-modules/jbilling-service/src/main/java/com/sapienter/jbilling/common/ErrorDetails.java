package com.sapienter.jbilling.common;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.apache.http.HttpStatus;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Vojislav Stanojevikj
 * @since 25-Aug-2016.
 */
@XmlRootElement
@ApiModel(value = "Error Details", description = "Error details when exception gets thrown")
public class ErrorDetails implements Serializable {

    private String uuid;
    private int errorCode;
    private SessionInternalErrorMessages sessionInternalErrorMessages = new SessionInternalErrorMessages();
    private String params[];

    private ErrorDetails() {}

    public static ErrorDetails newInstance(String uuid, String[] errorMessages){
        return newInstance(uuid, errorMessages, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    public static ErrorDetails newInstance(String uuid, String[] errorMessages, int errorCode){
        return newInstance(uuid, errorMessages, errorCode, null);
    }

    public static ErrorDetails newInstance(String uuid, String[] errorMessages, int errorCode, String[] params){
        ErrorDetails eD = new ErrorDetails();
        eD.setErrorCode(errorCode);
        eD.setUuid(uuid);
        if (null != params)
            eD.setParams(Arrays.copyOf(params, params.length));
        if (null != errorMessages)
            eD.setErrorMessages(Arrays.copyOf(errorMessages, errorMessages.length));
        return eD;
    }

    public static ErrorDetails copyOf(ErrorDetails errorDetails) {
        return newInstance(errorDetails.uuid, errorDetails.getErrorMessages(),
                errorDetails.errorCode, errorDetails.params);
    }

    @XmlElement
    public String getUuid() {
        return uuid;
    }

    private void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @XmlElement
    @ApiModelProperty(value = "Error code (HTTP)")
    public int getErrorCode() {
        return errorCode;
    }

    private void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    private void setErrorMessages(String[] errorMessages){
        this.sessionInternalErrorMessages.setErrorMessages(errorMessages);
    }

    @XmlElement(name = "errorMessages")
    @ApiModelProperty(value = "Error description")
    public String getErrorsMessagesAsString(){
        StringBuilder builder = new StringBuilder();
        if (null != getErrorMessages()){
            for (String error : getErrorMessages()) {
                builder.append(error);
                builder.append(System.getProperty("line.separator"));
            }
        }
        return builder.toString();
    }

    @XmlTransient
    public String[] getErrorMessages(){
        return null != sessionInternalErrorMessages.getErrorMessages() ?
                Arrays.copyOf(sessionInternalErrorMessages.getErrorMessages(),
                        sessionInternalErrorMessages.getErrorMessages().length):
                null;
    }

    @XmlTransient
    public String[] getParams() {
        return null != params ?Arrays.copyOf(params, params.length) : null;
    }

    @XmlElement(name = "params")
    @ApiModelProperty(value = "Parameters for error messages")
    public String getParamsAsString(){
        StringBuilder builder = new StringBuilder();
        if (null != getParams()){
            for (String param : getParams()) {
                builder.append(param);
                builder.append(System.getProperty("line.separator"));
            }
        }
        return builder.toString();
    }

    public SessionInternalErrorMessages getSessionInternalErrorMessages() {
        return sessionInternalErrorMessages;
    }

    private void setParams(String[] params) {
        this.params = params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ErrorDetails that = (ErrorDetails) o;

        if (errorCode != that.errorCode) return false;
        if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) return false;
        if (sessionInternalErrorMessages != null ? !sessionInternalErrorMessages.equals(that.sessionInternalErrorMessages) : that.sessionInternalErrorMessages != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(params, that.params);

    }

    @Override
    public int hashCode() {
        int result = uuid != null ? uuid.hashCode() : 0;
        result = 31 * result + errorCode;
        result = 31 * result + (sessionInternalErrorMessages != null ? sessionInternalErrorMessages.hashCode() : 0);
        result = 31 * result + (params != null ? Arrays.hashCode(params) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ErrorDetails{" +
                "uuid='" + uuid + '\'' +
                ", errorCode=" + errorCode +
                ", sessionInternalErrorMessages=" + sessionInternalErrorMessages +
                ", params=" + Arrays.toString(params) +
                '}';
    }
}
