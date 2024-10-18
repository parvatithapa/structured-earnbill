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

/*
 * Created on Jan 25, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.sapienter.jbilling.server.util.api.validation.CreateValidationGroup;
import com.sapienter.jbilling.server.util.api.validation.EntitySignupValidationGroup;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import com.sapienter.jbilling.server.util.api.validation.UpdateValidationGroup;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/** @author Emil */
@ApiModel(value = "Contact Data", description = "ContactWS Model")
public class ContactWS implements Serializable {

    private static final long serialVersionUID = 20140605L;

    private Integer id;
    @NotEmpty(message = "validation.error.notnull", groups = EntitySignupValidationGroup.class)
    @Size(min = 0, max = 200, message = "validation.error.size,0,200")
    private String organizationName;
    @NotEmpty(message = "validation.error.notnull", groups = {EntitySignupValidationGroup.class, UpdateValidationGroup.class, CreateValidationGroup.class})
    @Size(min = 0, max = 100, message = "validation.error.size,0,100")
    private String address1;
    @Size(min = 0, max = 100, message = "validation.error.size,0,100")
    private String address2;
    @Size(min = 0, max = 50, message = "validation.error.size,0,50")
    private String city;
    @NotEmpty(message = "validation.error.notnull", groups = {EntitySignupValidationGroup.class,UpdateValidationGroup.class, CreateValidationGroup.class})
    @Size(min = 0, max = 30, message = "validation.error.size,0,30")
    private String stateProvince;
    @NotEmpty(message = "validation.error.notnull", groups = EntitySignupValidationGroup.class)
    @Size(min = 0, max = 15, message = "validation.error.size,0,15")
    private String postalCode;
    @NotEmpty(message = "validation.error.notnull", groups = {EntitySignupValidationGroup.class,UpdateValidationGroup.class, CreateValidationGroup.class})
    @Size(min = 0, max = 2, message = "validation.error.size,0,2")
    @Pattern(regexp = "^$|[A-Z]{2}", message = "validation.error.contact.countryCode.pattern")
    private String countryCode;
    @NotEmpty(message = "validation.error.notnull", groups = EntitySignupValidationGroup.class)
    @Size(min = 0, max = 30, message = "validation.error.size,0,30")
    private String lastName;
    @NotEmpty(message = "validation.error.notnull", groups = EntitySignupValidationGroup.class)
    @Size(min = 0, max = 30, message = "validation.error.size,0,30")
    private String firstName;
    @Size(min = 0, max = 30, message = "validation.error.size,0,30")
    private String initial;
    private String title;
    private String phoneCountryCode;
    private String phoneAreaCode;
    @Size(min = 0, max = 20, message = "validation.error.size,0,20")
    @NotEmpty(message = "validation.error.notnull", groups = EntitySignupValidationGroup.class)
    private String phoneNumber;
    private Integer faxCountryCode;
    private Integer faxAreaCode;
    private String faxNumber;
    @NotEmpty(message = "validation.error.notnull", groups = EntitySignupValidationGroup.class)
    //Bug4089 regexp for meeting rfc3696 restriction for email addresses. 
    @Pattern(regexp = "^([a-zA-Z0-9#\\!$%&'\\*\\+/=\\?\\^_`\\{\\|}~-]|(\\\\@|\\\\,|\\\\\\[|\\\\\\]|\\\\ ))+(\\.([a-zA-Z0-9!#\\$%&'\\*\\+/=\\?\\^_`\\{\\|}~-]|(\\\\@|\\\\,|\\\\\\[|\\\\\\]))+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.([A-Za-z]{2,})$", message = "validation.error.email")
    @Size(min = 6, max = 320, message = "validation.error.size,6,320")
    private String email;
    @ConvertToTimezone
    private Date createDate;
    private int deleted;
    private Boolean include;
    private Boolean invoiceAsReseller;

    public ContactWS() {
        super();
    }

    public ContactWS(Integer id, String address1,
            String address2, String city, String stateProvince,
            String postalCode, String countryCode, int deleted, String phoneCountryCode, String phoneAreaCode,
                     String phoneNumber, String email) {
            this.id = id;
            this.address1 = address1;
            this.address2 = address2;
            this.city = city;
            this.stateProvince = stateProvince;
            this.postalCode = postalCode;
            this.countryCode = countryCode;
            this.phoneCountryCode = phoneCountryCode;
            this.phoneAreaCode = phoneAreaCode;
            this.phoneNumber = phoneNumber;
            this.email = email;
            this.deleted = deleted;
        }

    public ContactWS(Integer id, String organizationName, String address1,
                     String address2, String city, String stateProvince,
                     String postalCode, String countryCode, String lastName,
                     String firstName, String initial, String title,
                     String phoneCountryCode, String phoneAreaCode,
                     String phoneNumber, Integer faxCountryCode, Integer faxAreaCode,
                     String faxNumber, String email, Date createDate, Integer deleted,
                     Boolean include) {
        this.id = id;
        this.organizationName = organizationName;
        this.address1 = address1;
        this.address2 = address2;
        this.city = city;
        this.stateProvince = stateProvince;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
        this.lastName = lastName;
        this.firstName = firstName;
        this.initial = initial;
        this.title = title;
        this.phoneCountryCode = phoneCountryCode;
        this.phoneAreaCode = phoneAreaCode;
        this.phoneNumber = phoneNumber;
        this.faxCountryCode = faxCountryCode;
        this.faxAreaCode = faxAreaCode;
        this.faxNumber = faxNumber;
        this.email = email;
        this.createDate = createDate;
        this.deleted = deleted;
        this.include = include;
    }

    public ContactWS(ContactWS other) {
        setId(other.getId());
        setOrganizationName(other.getOrganizationName());
        setAddress1(other.getAddress1());
        setAddress2(other.getAddress2());
        setCity(other.getCity());
        setStateProvince(other.getStateProvince());
        setPostalCode(other.getPostalCode());
        setCountryCode(other.getCountryCode());
        setLastName(other.getLastName());
        setFirstName(other.getFirstName());
        setInitial(other.getInitial());
        setTitle(other.getTitle());
        setPhoneCountryCode(null != other.getPhoneCountryCode() ? other.getPhoneCountryCode().toString() : "");
        setPhoneAreaCode(null != other.getPhoneAreaCode() ? other.getPhoneAreaCode().toString() : "");
        setPhoneNumber(other.getPhoneNumber());
        setFaxCountryCode(other.getFaxCountryCode());
        setFaxAreaCode(other.getFaxAreaCode());
        setFaxNumber(other.getFaxNumber());
        setEmail(other.getEmail());
        setCreateDate(other.getCreateDate());
        setDeleted(other.getDeleted());
        setInclude(other.getInclude());
    }

    @ApiModelProperty(value = "Unique identifier of this contact")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Name of the organization the contact belongs to", required = true)
    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    @ApiModelProperty(value = "First line for the address", required = true)
    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    @ApiModelProperty(value = "Second line for the address")
    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    @ApiModelProperty(value = "City of this contact")
    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @ApiModelProperty(value = "State or Province of the contact's address", required = true)
    public String getStateProvince() {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince) {
        this.stateProvince = stateProvince;
    }

    @ApiModelProperty(value = "ZIP Code for the contact's address", required = true)
    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    @ApiModelProperty(value = "Country code for this contact",
            required = true)
    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @ApiModelProperty(value = "", required = true)
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @ApiModelProperty(value = "First name of this contact", required = true)
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @ApiModelProperty(value = "Middle name initials, if any")
    public String getInitial() {
        return initial;
    }

    public void setInitial(String initial) {
        this.initial = initial;
    }

    @ApiModelProperty(value = "Title for the contact, such as \"Mr.\" or \"Dr.\"")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @ApiModelProperty(value = "Phone number Country Code")
    public String getPhoneCountryCode() {
        return phoneCountryCode;
    }

    public void setPhoneCountryCode(String phoneCountryCode) {
        this.phoneCountryCode = phoneCountryCode;
    }

    @ApiModelProperty(value = "Phone number Area Code")
    public String getPhoneAreaCode() {
        return phoneAreaCode;
    }

    public void setPhoneAreaCode(String phoneAreaCode) {
        this.phoneAreaCode = phoneAreaCode;
    }

    @ApiModelProperty(value = "Phone number", required = true)
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @ApiModelProperty(value = "Country Code for the fax number, if any")
    public Integer getFaxCountryCode() {
        return faxCountryCode;
    }

    public void setFaxCountryCode(Integer faxCountryCode) {
        this.faxCountryCode = faxCountryCode;
    }

    @ApiModelProperty(value = "Area Code for the fax number, if any")
    public Integer getFaxAreaCode() {
        return faxAreaCode;
    }

    public void setFaxAreaCode(Integer faxAreaCode) {
        this.faxAreaCode = faxAreaCode;
    }

    @ApiModelProperty(value = "Fax number")
    public String getFaxNumber() {
        return faxNumber;
    }

    public void setFaxNumber(String faxNumber) {
        this.faxNumber = faxNumber;
    }

    @ApiModelProperty(value = "Email address of this contact", required = true)
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @ApiModelProperty(value = "Date this contact record was first created")
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @ApiModelProperty(value = "If the record has been deleted, this field contains '1', otherwise it contains '0'. Note that deletion cannot be carried out by simply setting a '1' in this field")
    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    @ApiModelProperty(value = "true if this contact is marked as included in notifications")
    public Boolean getInclude() {
        return include == null ? new Boolean(false) : include;
    }

    public void setInclude(Boolean include) {
        this.include = include;
    }

    @ApiModelProperty(value = "If this flag is set then when an order is added to an invoice the OrderAddedOnInvoiceEvent event is triggered AFTER it creates an order for reseller")
    public Boolean getInvoiceAsReseller() {
		return invoiceAsReseller;
	}

	public void setInvoiceAsReseller(Boolean invoiceAsReseller) {
		this.invoiceAsReseller = invoiceAsReseller;
	}

    @Override
    public String toString() {
        return "ContactWS{"
               + "id=" + id
               + ", title='" + title + '\''
               + ", lastName='" + lastName + '\''
               + ", firstName='" + firstName + '\''
               + ", initial='" + initial + '\''
               + ", organization='" + organizationName + '\''
               + ", address1='" + address1 + '\''
               + ", address2='" + address2 + '\''
               + ", city='" + city + '\''
               + ", stateProvince='" + stateProvince + '\''
               + ", postalCode='" + postalCode + '\''
               + ", countryCode='" + countryCode + '\''
               + ", phone='" + (phoneCountryCode != null ? phoneCountryCode : "")
                             + (phoneAreaCode != null ? phoneAreaCode : "")
                             + (phoneNumber != null ?  phoneNumber : "") + '\''
               + ", fax='" + (faxCountryCode != null ? faxCountryCode : "")
                           + (faxAreaCode != null ? faxAreaCode : "")
                           + (faxNumber != null ? faxNumber : "") + '\''
               + ", email='" + email + '\''
               + ", include='" + include + '\''
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContactWS)) return false;

        ContactWS contactWS = (ContactWS) o;

        return deleted == contactWS.deleted &&
                nullSafeEquals(id, contactWS.id) &&
                nullSafeEquals(organizationName, contactWS.organizationName) &&
                nullSafeEquals(address1, contactWS.address1) &&
                nullSafeEquals(address2, contactWS.address2) &&
                nullSafeEquals(city, contactWS.city) &&
                nullSafeEquals(stateProvince, contactWS.stateProvince) &&
                nullSafeEquals(postalCode, contactWS.postalCode) &&
                nullSafeEquals(countryCode, contactWS.countryCode) &&
                nullSafeEquals(lastName, contactWS.lastName) &&
                nullSafeEquals(firstName, contactWS.firstName) &&
                nullSafeEquals(initial, contactWS.initial) &&
                nullSafeEquals(title, contactWS.title) &&
                nullSafeEquals(phoneCountryCode, contactWS.phoneCountryCode) &&
                nullSafeEquals(phoneAreaCode, contactWS.phoneAreaCode) &&
                nullSafeEquals(phoneNumber, contactWS.phoneNumber) &&
                nullSafeEquals(faxCountryCode, contactWS.faxCountryCode) &&
                nullSafeEquals(faxAreaCode, contactWS.faxAreaCode) &&
                nullSafeEquals(faxNumber, contactWS.faxNumber) &&
                nullSafeEquals(email, contactWS.email) &&
                nullSafeEquals(createDate, contactWS.createDate) &&
                nullSafeEquals(include, contactWS.include) &&
                nullSafeEquals(invoiceAsReseller, contactWS.invoiceAsReseller);
    }

    @Override
    public int hashCode() {
        int result = nullSafeHashCode(organizationName);
        result = 31 * result + nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(address1);
        result = 31 * result + nullSafeHashCode(address2);
        result = 31 * result + nullSafeHashCode(city);
        result = 31 * result + nullSafeHashCode(stateProvince);
        result = 31 * result + nullSafeHashCode(postalCode);
        result = 31 * result + nullSafeHashCode(countryCode);
        result = 31 * result + nullSafeHashCode(lastName);
        result = 31 * result + nullSafeHashCode(firstName);
        result = 31 * result + nullSafeHashCode(initial);
        result = 31 * result + nullSafeHashCode(title);
        result = 31 * result + nullSafeHashCode(phoneCountryCode);
        result = 31 * result + nullSafeHashCode(phoneAreaCode);
        result = 31 * result + nullSafeHashCode(phoneNumber);
        result = 31 * result + nullSafeHashCode(faxCountryCode);
        result = 31 * result + nullSafeHashCode(faxAreaCode);
        result = 31 * result + nullSafeHashCode(faxNumber);
        result = 31 * result + nullSafeHashCode(email);
        result = 31 * result + nullSafeHashCode(createDate);
        result = 31 * result + deleted;
        result = 31 * result + nullSafeHashCode(include);
        result = 31 * result + nullSafeHashCode(invoiceAsReseller);
        return result;
    }
}
