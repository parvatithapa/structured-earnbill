package com.sapienter.jbilling.server.metafields.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.cxf.CxfSMapStringStringAdapter;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.ListUtils;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * WS class from ValidationRule
 *
 *  @author Panche Isajeski
 */
@ApiModel(value = "ValidationRule Data", description = "ValidationRuleWS Model")
public class ValidationRuleWS implements Serializable {
	
	 public static final String ERROR_MSG_LABEL= "errorMessage";
    private int id;
    @NotNull(message = "validation.error.null.rule.type")
    private String ruleType;
    private SortedMap<String, String> ruleAttributes = new TreeMap<String, String>();

    @NotEmpty(message = "validation.error.empty.error.message")
    private List<InternationalDescriptionWS> errorMessages = ListUtils.lazyList(new ArrayList<InternationalDescriptionWS>(),
            FactoryUtils.instantiateFactory(InternationalDescriptionWS.class));
    private boolean enabled = true;

    public ValidationRuleWS() {
    }


    @ApiModelProperty(value = "The id of the validation rule entity")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ApiModelProperty(value = "The type of the validation rule", required = true)
    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    @XmlJavaTypeAdapter(CxfSMapStringStringAdapter.class)
    @ApiModelProperty(value = "The attributes of the validation rule")
    public SortedMap<String, String> getRuleAttributes() {
        return ruleAttributes;
    }

    public void setRuleAttributes(SortedMap<String, String> ruleAttributes) {
        this.ruleAttributes = ruleAttributes;
    }

    public void addRuleAttribute(String name, String value) {
        this.ruleAttributes.put(name, value);
    }

    @ApiModelProperty(value = "The error messages of the validation rule", required = true)
    public List<InternationalDescriptionWS> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<InternationalDescriptionWS> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public void addErrorMessage(int langId, String errorMessage) {
        InternationalDescriptionWS errorMessageWS=new InternationalDescriptionWS(ERROR_MSG_LABEL, langId, errorMessage);
        this.errorMessages.add(errorMessageWS);
    }

    @ApiModelProperty(value = "Is this validation rule enabled or not")
    @JsonProperty(value = "enabled")
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "ValidationRuleWS{" +
                "id=" + id +
                ", ruleType=" + ruleType +
                ", ruleAttributes=" + ruleAttributes +
                ", errorMessages=" + errorMessages +
                ", enabled=" + enabled +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ValidationRuleWS that = (ValidationRuleWS) o;

        if (id != that.id) return false;
        if (enabled != that.enabled) return false;
        if (ruleType != null ? !ruleType.equals(that.ruleType) : that.ruleType != null) return false;
        if (ruleAttributes != null ? !ruleAttributes.equals(that.ruleAttributes) : that.ruleAttributes != null)
            return false;
        return !(errorMessages != null ? !errorMessages.equals(that.errorMessages) : that.errorMessages != null);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (ruleType != null ? ruleType.hashCode() : 0);
        result = 31 * result + (ruleAttributes != null ? ruleAttributes.hashCode() : 0);
        result = 31 * result + (errorMessages != null ? errorMessages.hashCode() : 0);
        result = 31 * result + (enabled ? 1 : 0);
        return result;
    }
}
