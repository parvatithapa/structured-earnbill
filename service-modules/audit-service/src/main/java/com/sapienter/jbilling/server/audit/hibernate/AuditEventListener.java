/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.audit.hibernate;

import com.sapienter.jbilling.server.audit.AuditService;
import org.apache.log4j.Logger;
import org.hibernate.event.spi.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.Serializable;

public class AuditEventListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private static final Logger LOG = Logger.getLogger(AuditEventListener.class);

    @Autowired
    private AuditService auditService;

    public void setAuditService (AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void onPostDelete (PostDeleteEvent hibernateEvent) {
        doAudit(hibernateEvent.getEntity(), hibernateEvent.getId(), AuditEvent.DELETE);
    }

    @Override
    public void onPostUpdate (PostUpdateEvent hibernateEvent) {
        doAudit(hibernateEvent.getEntity(), hibernateEvent.getId(), AuditEvent.UPDATE);
    }

    @Override
    public void onPostInsert (PostInsertEvent hibernateEvent) {
        doAudit(hibernateEvent.getEntity(), hibernateEvent.getId(), AuditEvent.CREATE);
    }

    @Override
    public boolean requiresPostCommitHanding(org.hibernate.persister.entity.EntityPersister ep) {
        return false;
    }

    private void doAudit (final Object entity, final Serializable id, final AuditEvent event) {
        if (entity == null) {
            return;
        }
        try {
            auditService.put(entity, id, event);
        } catch (Exception e) {
            LOG.error("Exception when adding [{" + event+  "}] audit log record for entity[{" + entity.getClass().getSimpleName() + "}].", e);
        }
    }
}
