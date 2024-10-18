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
package com.sapienter.jbilling.server.notification.db;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;

import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;

@SuppressWarnings("serial")
@Entity
@TableGenerator(
        name = "invoice_notification_message_arch_GEN", 
        table = "jbilling_seqs", 
        pkColumnName = "name", 
        valueColumnName = "next_id", 
        pkColumnValue = "invoice_notification_message_arch", 
        allocationSize = 100)
@Table(name = "invoice_notification_message_arch")
public class InvoiceNotificationMessageArchDTO implements Serializable {

   
        
    private int id;
    private NotificationMessageArchDTO notificationMessageArch;
    private UserDTO baseUser;
    private InvoiceDTO invoice;
    private Integer jobExecutionId;
    
    private Set<NotificationMessageArchLineDTO> notificationMessageArchLines =
            new HashSet<NotificationMessageArchLineDTO>(0);

    public InvoiceNotificationMessageArchDTO() {
    }

    public InvoiceNotificationMessageArchDTO(int id) {
        this.id = id;        
    }

    public InvoiceNotificationMessageArchDTO(int id, UserDTO baseUser, InvoiceDTO invoice,            
            NotificationMessageArchDTO notificationMessageArch, Integer jobExecutionId) {
        this.id = id;
        this.baseUser = baseUser;
        this.invoice = invoice;
        this.notificationMessageArch = notificationMessageArch;
        this.jobExecutionId = jobExecutionId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "invoice_notification_message_arch_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserDTO getBaseUser() {
        return this.baseUser;
    }

    public void setBaseUser(UserDTO baseUser) {
        this.baseUser = baseUser;
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    public InvoiceDTO getInvoice() {
        return this.invoice;
    }

    public void setInvoice(InvoiceDTO invoice) {
        this.invoice = invoice;
    }
        
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_message_arch_id")
    public NotificationMessageArchDTO getNotificationMessageArch() {
        return this.notificationMessageArch;
    }

    public void setNotificationMessageArch(
            NotificationMessageArchDTO notificationMessageArch) {
        this.notificationMessageArch = notificationMessageArch;
    }
    
    @Column(name = "job_execution_id")
    public Integer getJobExecutionId() {
        return this.jobExecutionId;
    }

    public void setJobExecutionId(Integer jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
    }

    public String getAuditKey(Serializable id) {
        StringBuilder key = new StringBuilder();
        key.append(getNotificationMessageArch().getBaseUser().getCompany().getId())
                .append("-usr-")
                .append(getNotificationMessageArch().getBaseUser().getId())
                .append("-msg-")
                .append(getNotificationMessageArch().getId())                
                .append("-inv-")
                .append(getInvoice().getId())
                .append("-")
                .append(getInvoice().getId())
                .append("-")
                .append(id);

        return key.toString();
    }
}
