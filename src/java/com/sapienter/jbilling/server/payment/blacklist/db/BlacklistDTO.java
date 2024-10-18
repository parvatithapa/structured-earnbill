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
package com.sapienter.jbilling.server.payment.blacklist.db;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;

import com.sapienter.jbilling.common.FormatLogger;

import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import org.apache.log4j.Logger;

import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;

@Entity
@TableGenerator(
        name="blacklist_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="blacklist",
        allocationSize = 100
        )
@Table(name = "blacklist")
//@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class BlacklistDTO implements AutoCloseable, Serializable {

    // constants
    
    // blacklist types
    public static final Integer TYPE_USER_ID = new Integer(1);
    public static final Integer TYPE_NAME = new Integer(2);
    public static final Integer TYPE_CC_NUMBER = new Integer(3);
    public static final Integer TYPE_ADDRESS = new Integer(4);
    public static final Integer TYPE_IP_ADDRESS = new Integer(5);
    public static final Integer TYPE_PHONE_NUMBER = new Integer(6);
    
    // blacklist sources
    public static final Integer SOURCE_CUSTOMER_SERVICE = new Integer(1);
    public static final Integer SOURCE_EXTERNAL_UPLOAD = new Integer(2);
    public static final Integer SOURCE_USER_STATUS_CHANGE = new Integer(3);
    public static final Integer SOURCE_BILLING_PROCESS = new Integer(4);

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BlacklistDTO.class));

    // mapped columns
    
    @Id @GeneratedValue(strategy=GenerationType.TABLE, generator="blacklist_GEN")
    private Integer id;

    @ManyToOne
    @JoinColumn(name="entity_id", nullable=false)
    private CompanyDTO company;

    @Column(name = "create_datetime", nullable=false, length=29)
    private Date createDate;

    @Column(name = "type", nullable=false)
    private Integer type;

    @Column(name = "source", nullable=false)
    private Integer source;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="credit_card_id")
    private PaymentInformationDTO creditCard;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="contact_id")
    private ContactDTO contact;

    @ManyToOne
    @JoinColumn(name="user_id")
    private UserDTO user;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="meta_field_value_id")
    private MetaFieldValue metaFieldValue;

    @Version
    @Column(name="OPTLOCK")
    private Integer versionNum;

    public BlacklistDTO() {
    }

    public BlacklistDTO(Integer id, CompanyDTO company, Date createDate, 
            Integer type, Integer source, PaymentInformationDTO creditCard,
            ContactDTO contact, UserDTO user, MetaFieldValue metaFieldValue) {
        this.id = id;
        this.company = company;
        this.createDate = createDate;
        this.type = type;
        this.source = source;
        this.creditCard = creditCard;
        this.contact = contact;
        this.user = user;
        this.metaFieldValue = metaFieldValue;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setCompany(CompanyDTO company) {
        this.company = company;
    }

    public CompanyDTO getCompany() {
        return company;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getType() {
        return type;
    }

    public void setSource(Integer source) {
        this.source = source;
    }

    public Integer getSource() {
        return source;
    }

    public void setCreditCard(PaymentInformationDTO creditCard) {
        this.creditCard = creditCard;
    }

    public PaymentInformationDTO getCreditCard() {
        return creditCard;
    }

    public void setContact(ContactDTO contact) {
        this.contact = contact;
    }

    public ContactDTO getContact() {
        return contact;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public UserDTO getUser() {
        return user;
    }

    public MetaFieldValue getMetaFieldValue() {
        return metaFieldValue;
    }

    public void setMetaFieldValue(MetaFieldValue metaFieldValue) {
        this.metaFieldValue = metaFieldValue;
    }

    public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    public String getAuditKey(Serializable id) {
        StringBuilder key = new StringBuilder();
        key.append(getCompany().getId())
                .append("-")
                .append(id);

        return key.toString();
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
     * <p>
     * <p>While this interface method is declared to throw {@code
     * Exception}, implementers are <em>strongly</em> encouraged to
     * declare concrete implementations of the {@code close} method to
     * throw more specific exceptions, or to throw no exception at all
     * if the close operation cannot fail.
     * <p>
     * <p> Cases where the close operation may fail require careful
     * attention by implementers. It is strongly advised to relinquish
     * the underlying resources and to internally <em>mark</em> the
     * resource as closed, prior to throwing the exception. The {@code
     * close} method is unlikely to be invoked more than once and so
     * this ensures that the resources are released in a timely manner.
     * Furthermore it reduces problems that could arise when the resource
     * wraps, or is wrapped, by another resource.
     * <p>
     * <p><em>Implementers of this interface are also strongly advised
     * to not have the {@code close} method throw {@link
     * InterruptedException}.</em>
     * <p>
     * This exception interacts with a thread's interrupted status,
     * and runtime misbehavior is likely to occur if an {@code
     * InterruptedException} is {@linkplain Throwable#addSuppressed
     * suppressed}.
     * <p>
     * More generally, if it would cause problems for an
     * exception to be suppressed, the {@code AutoCloseable.close}
     * method should not throw it.
     * <p>
     * <p>Note that unlike the {@link Closeable#close close}
     * method of {@link Closeable}, this {@code close} method
     * is <em>not</em> required to be idempotent.  In other words,
     * calling this {@code close} method more than once may have some
     * visible side effect, unlike {@code Closeable.close} which is
     * required to have no effect if called more than once.
     * <p>
     * However, implementers of this interface are strongly encouraged
     * to make their {@code close} methods idempotent.
     *
     * @throws Exception if this resource cannot be closed
     */
    @Override
    public void close() throws Exception {
        // Close PaymentInformationDTO object
        if(null != creditCard){
            creditCard.close();
        }
    }
}
