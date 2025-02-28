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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;


import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@TableGenerator(
        name = "notification_message_line_GEN", 
        table = "jbilling_seqs", 
        pkColumnName = "name", 
        valueColumnName = "next_id", 
        pkColumnValue = "notification_message_line", 
        allocationSize = 1)
@Table(name = "notification_message_line")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class NotificationMessageLineDTO implements Serializable {

    private int id;
    private NotificationMessageSectionDTO notificationMessageSection;
    private String content;
    private int versionNum;

    public NotificationMessageLineDTO() {
    }

    public NotificationMessageLineDTO(int id, String content) {
        this.id = id;
        this.content = content;
    }

    public NotificationMessageLineDTO(int id,
            NotificationMessageSectionDTO notificationMessageSection,
            String content) {
        this.id = id;
        this.notificationMessageSection = notificationMessageSection;
        this.content = content;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "notification_message_line_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_section_id")
    public NotificationMessageSectionDTO getNotificationMessageSection() {
        return this.notificationMessageSection;
    }

    public void setNotificationMessageSection(
            NotificationMessageSectionDTO notificationMessageSection) {
        this.notificationMessageSection = notificationMessageSection;
    }

    @Column(name = "content", nullable = false, length = 1000)
    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }
    
    @Version
    @Column(name="OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    public String getAuditKey(Serializable id) {
        StringBuilder key = new StringBuilder();
        key.append(getNotificationMessageSection().getNotificationMessage().getEntity().getId())
                .append("-msg-")
                .append(getNotificationMessageSection().getNotificationMessage().getId())
                .append("-sctn-")
                .append(getNotificationMessageSection().getId())
                .append("-")
                .append(id);

        return key.toString();
    }
}
