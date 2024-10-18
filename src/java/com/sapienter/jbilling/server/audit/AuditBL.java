package com.sapienter.jbilling.server.audit;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.audit.hibernate.HBaseEntityModel;
import com.sapienter.jbilling.server.audit.hibernate.HibernateAnnotationUtils;
import com.sapienter.jbilling.server.audit.mapper.DateConverter;
import com.sapienter.jbilling.server.audit.mapper.StringConverter;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Entity auditing for all JPA/Hibernate mapped entities in jBilling.
 *
 * Note that all retrieval methods will return an empty list {@link Collections#emptyList()} if audit
 * logging has been disabled.
 *
 * TODO Modularization: This class should go in the audit service. Is still here because in the #deserialize method it
 * need to have access to classes that are still in the grails src code -> from services we can't access them and it fail
 *
 * @author Marco Manzi
 * @since 24-11-2015
 */
public class AuditBL {

    static {
        // allow nulls without throwing exceptions and use null for default values (where possible)
        BeanUtilsBean.getInstance().getConvertUtils().register(false, true, 0);

        // additional format converters for apache BeanUtils that impose some sane defaults
        ConvertUtils.register(new DateConverter(), Date.class);
        ConvertUtils.register(new StringConverter(), String.class);
    }

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AuditBL.class));

    private boolean isHbaseAuditEnabled = Util.getSysPropBooleanTrue("hbase.audit.logging");

    @Autowired
    private AuditService auditService;


    public void setHbaseAuditEnabled (boolean isHbaseAuditEnabled) {
        this.isHbaseAuditEnabled = isHbaseAuditEnabled;
    }

    public void setAuditService (AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Returns a list of audited versions for an entity type. This will return up to the given
     * maximum number of historical versions in ascending order (newest first).
     *
     * @param type Hibernate mapped entity class to query audit versions.
     * @param rowKey row identifier of the audit entry.
     * @param versions number of historical versions to return.
     * @return list of audited entity versions.
     */
    public List<Audit> get(Class type, String rowKey, int versions) {
        if (! isHbaseAuditEnabled) return Collections.emptyList();
        return fillWithColumnValues(auditService.get(type, rowKey, versions));
    }

    /**
     * Returns an audited entity version at a specific point in time.
     *
     * @param type Hibernate mapped entity class to query audit versions.
     * @param rowKey row identifier of the audit entry.
     * @param timestamp timestamp of the version to fetch.
     * @return audited entity version.
     */
    public Audit get(Class type, String rowKey, long timestamp) {
        if (! isHbaseAuditEnabled) return null;
        return fillWithColumnValues(auditService.get(type, rowKey, timestamp));
    }

    /**
     * Searches the audit tables for records that match the given prefix, and returns the
     * NEWEST logged version of all records with a matching row identifier.
     *
     * @param type Hibernate mapped entity class to query audit versions.
     * @param prefix row identifier prefix to search for.
     * @return list of audited entity versions.
     */
    public List<Audit> find(Class type, String prefix) {
        if (! isHbaseAuditEnabled) return Collections.emptyList();
        return fillWithColumnValues(auditService.find(type, prefix));
    }


    /**
     * Restores a historical version of an audited entity at at a specific point in time.
     *
     * This is a convenience variant of {@link #restore(Class, Audit, Object)} that removes
     * the need to fetch the audited version from HBase for restore.
     *
     * @param entity the target object to overwrite.
     * @param id identifier of the Hibernate entity.
     * @param timestamp timestamp of the version to restore.
     */
    public void restore(Object entity, Serializable id, long timestamp) {
        if (Auditable.class.isAssignableFrom(entity.getClass())) {
            Audit version = get(entity.getClass(), ((Auditable)entity).getAuditKey(id), timestamp);
            restore(entity.getClass(), version, entity);
        }
    }

    /**
     * Restores a historical version of an audited entity's column values. This only
     * affects the given object and will NOT restore any associated objects or foreign key
     * relationships.
     *
     * @param version historical version to restore
     * @param target target object to overwrite
     */
    public void restore(Class type, Audit version, Object target) {
        HBaseEntityModel model = HibernateAnnotationUtils.buildEntityModel(type);

        Map<String, String> columns = model.getColumnFields();
        for (Map.Entry<String, String> entry : columns.entrySet()) {
            String fieldName = entry.getKey();
            String columnName = entry.getValue();

            if (version.getColumns().containsKey(columnName)) {

                // get historical value to populate
                String value = version.getColumns().get(columnName);
                if (value == null || value.isEmpty()) value = null;

                // populate the target property with the historical value
                try {
                    BeanUtils.setProperty(target, fieldName, value);
                } catch (InvocationTargetException e) {
                    LOG.error("Unhandled exception occurred when setting historical value.", e);
                } catch (IllegalAccessException e) {
                    LOG.error("Could not set historical value, setter method is not accessible.", e);
                }
            }
        }

    }


    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    private List<Audit> fillWithColumnValues(List<Audit> audits) {
        return audits.stream().map(audit -> fillWithColumnValues(audit))
                .collect(Collectors.toList());
    }

    public Audit fillWithColumnValues(Audit audit) {
        try {
            audit.setColumns(getColumnValues(deserialize(audit.getObject())));
        } catch (IOException e) {
            LOG.error("Unhandled exception occurred when setting historical value.", e);
        } catch (ClassNotFoundException e) {
            LOG.error("Unhandled exception occurred when setting historical value.", e);
        }
        return audit;
    }

    /**
     * Parses the given object and extracts a map of values from member fields annotated
     * with the {@link javax.persistence.Column} annotation.
     *
     * @param entity entity object with mapped columns.
     * @return map of column names and values.
     */
    public Map<String, String> getColumnValues(Object entity) {
        if (entity == null) return Collections.emptyMap();
        HBaseEntityModel model = HibernateAnnotationUtils.getHibernateEntityModel(entity.getClass());
        if (model.getColumnFields().isEmpty()) {
            LOG.warn("Entity has no auditable columns, associated objects will be audited separately.");
            return null;
        }

        Map<String, String> columns = model.getColumnFields();
        Map<String, String> values = new TreeMap<>();

        for (Map.Entry<String, String> entry : columns.entrySet()) {
            String fieldName = entry.getKey();
            String columnName = entry.getValue();

            try {
                values.put(columnName, BeanUtils.getProperty(entity, fieldName));

            } catch (InvocationTargetException e) {
                LOG.error("Unhandled exception occurred when getting parameter value.", e);
            } catch (NoSuchMethodException e) {
                LOG.error("Could not get parameter value, getter method does not exist.", e);
            } catch (IllegalAccessException e) {
                LOG.error("Could not get parameter value, getter method is not accessible.", e);
            }
        }

        return values;
    }
}
