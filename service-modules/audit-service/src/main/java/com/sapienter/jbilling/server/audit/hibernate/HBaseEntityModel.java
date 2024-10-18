package com.sapienter.jbilling.server.audit.hibernate;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a parsed annotated HBase POJO or JPA entity. This model
 * can be cached and re-used to avoid the overhead of the java reflection API.
 *
 * @author Brian Cowdery
 * @since 16-12-2012
 */
public class HBaseEntityModel {

    private String tableName = null;
    private String columnFamily = null;
    private Map<String, String> fieldLookup = null;
    private Map<String, String> columnFields = new HashMap<String, String>();

    private Method keyMethod = null;

    public HBaseEntityModel() {
    }

    /**
     * Returns the audit table name.
     * @return audit table name
     */
    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns a map of JavaBean field name, sand the associated database column name.
     * @return map with field names as the key, and the DB column name as the value.
     */
    public Map<String, String> getColumnFields() {
        return columnFields;
    }

    public void setColumnFields(Map<String, String> columnFields) {
        this.columnFields = columnFields;
    }

    /**
     * Returns the JavaBean field name for a given column name.
     * @param columnName column name to get
     * @return field name
     */
    public String getFieldName(String columnName) {
        _buildLookupMap();
        return this.fieldLookup.get(columnName);
    }

    private void _buildLookupMap() {
        if (fieldLookup != null)
             return;

        fieldLookup = new HashMap<String, String>();

        for (Map.Entry<String, String> entry : columnFields.entrySet()) {
            fieldLookup.put(entry.getValue(), entry.getKey());
        }
    }

    /**
     * Returns the method annotated with {@link @HBaseKey} to be used when generating a
     * unique row key for the entity.
     * @return entity method for generating a row key.
     */
    public Method getKeyMethod() {
        return keyMethod;
    }

    public void setKeyMethod(Method keyMethod) {
        this.keyMethod = keyMethod;
    }

    /**
     * Invoke the row key generation method on the target entity.
     * @param entity entity to invoke method on
     * @param id id of the entity to use when generating identifier
     * @return generated row identifier
     */
    public String invokeKeyMethod(Object entity, Serializable id) {
        if (keyMethod == null)
            throw new RuntimeException("Entity does not have annotated @HBaseKey method for generating row ids.");

        if (keyMethod.getReturnType() != String.class)
            throw new RuntimeException("@HBaseKey method must return a String.");

        try {
            return (String) keyMethod.invoke(entity, id);

        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot invoke @HBaseKey method; must be public or package default.", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Could not invoke @HBaseKey method.", e);
        }
    }

    public String getColumnFamily() {
        return columnFamily;
    }

    public void setColumnFamily(String columnFamily) {
        this.columnFamily = columnFamily;
    }
}
