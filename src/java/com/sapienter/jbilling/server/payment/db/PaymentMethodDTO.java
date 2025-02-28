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
package com.sapienter.jbilling.server.payment.db;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.sapienter.jbilling.server.audit.Auditable;

import com.sapienter.jbilling.server.process.db.ProcessRunTotalPmDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDescription;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "payment_method")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class PaymentMethodDTO extends AbstractDescription implements Serializable, Auditable {

    private int id;
    private Set<PaymentDTO> payments = new HashSet<PaymentDTO>(0);
    private Set<ProcessRunTotalPmDTO> processRunTotalPms = new HashSet<ProcessRunTotalPmDTO>(
            0);

    public PaymentMethodDTO() {
    }

    public PaymentMethodDTO(int id) {
        setId(id);
    }

    public PaymentMethodDTO(int id, Set<PaymentDTO> payments,
            Set<ProcessRunTotalPmDTO> processRunTotalPms) {
        this.id = id;
        this.payments = payments;
        this.processRunTotalPms = processRunTotalPms;
    }

    @Id
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Transient
    protected String getTable() {
        return Constants.TABLE_PAYMENT_METHOD;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "paymentMethod")
    public Set<PaymentDTO> getPayments() {
        return this.payments;
    }

    public void setPayments(Set<PaymentDTO> payments) {
        this.payments = payments;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "paymentMethod")
    public Set<ProcessRunTotalPmDTO> getProcessRunTotalPms() {
        return this.processRunTotalPms;
    }

    public void setProcessRunTotalPms(
            Set<ProcessRunTotalPmDTO> processRunTotalPms) {
        this.processRunTotalPms = processRunTotalPms;
    }

    public String getAuditKey(Serializable id) {
        return id.toString();
    }

    public String getDescription(PaymentDTO payment, Integer languageId) {
        if (payment == null) {
            throw new NullPointerException("payment details not found");
        }
        String methodName = super.getDescription(languageId);
        if( !methodName.equals(com.sapienter.jbilling.common.CommonConstants.CUSTOM) ) {
            return methodName;
        }
        Integer informationId = payment.getPaymentInstrumentsInfo().get(0).getId();
        return new PaymentInformationDAS().findCustomPaymentMethodType(informationId);
    }
}
