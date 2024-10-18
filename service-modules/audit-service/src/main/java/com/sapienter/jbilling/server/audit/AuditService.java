package com.sapienter.jbilling.server.audit;

import com.sapienter.jbilling.server.audit.hibernate.AuditEvent;

import java.io.Serializable;
import java.util.List;

/**
 * Created by marcomanzicore on 23/11/15.
 */
public interface AuditService {

    String BEAN_NAME = "auditService";

    /**
     * Searches the audit table for the given Class, for rows matching the prefix. This method
     * only returns the NEWEST version of audited entities that match the criteria.
     * <p/>
     * This method is useful for finding audited entries of an associated entity. For example,
     * <p/>
     * Order 123:      row key = "1-usr-2-123"
     * Order Line 4:   row key = "1-usr-2-ord-123-4"
     * <p/>
     * find(OrderLineDTO.class, "1-usr-2-ord-123-", 10);
     * #=> Returns 10 latest versions of all associated order lines
     *
     * @param type   Hibernate mapped entity class to query audit versions.
     * @param auditKeyPrefix row prefix to search for.
     * @return list of audited entity versions
     */

    List<Audit> find(Class type, String auditKeyPrefix);

    /**
     * Returns a list of audited versions for a given Class. This will return up to the given
     * maximum number of historical versions in ascending order (newest first).
     *
     * @param type Hibernate mapped entity class to query audit versions.
     * @param auditKey row identifier of the audit entry.
     * @param versions number of historical versions to return.
     * @return list of audited entity versions
     */
    List<Audit> get(Class type, String auditKey, int versions);

    /**
     * Returns a specific audited version for a given Class and row identifier. This method
     * will return <code>null</code> if no version exists for the provided timestamp.
     *
     * @param type Hibernate mapped entity class to query audit versions.
     * @param auditKey row identifier of the audit entry.
     * @param timestamp the timestamp to fetch.
     * @return audited entity version.
     */
    Audit get(Class type, String auditKey, long timestamp);


    /**
     * Writes a record to the HBase audit tables.
     *
     * @param entity Hibernate mapped entity being logged.
     * @param id ID of the entity.
     * @param event Type of audit event being logged.
     */
    void put(Object entity, Serializable id, AuditEvent event);
}
