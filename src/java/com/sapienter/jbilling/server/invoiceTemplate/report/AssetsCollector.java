package com.sapienter.jbilling.server.invoiceTemplate.report;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.sapienter.jbilling.server.invoiceTemplate.report.FieldType.Field;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

/**
 * @author Klim
 */
public class AssetsCollector {

    // default common fields
    public static final Set<String> COMMON_FIELD_NAMES = new TreeSet<String>(asList("id", "assetStatusId", "createDatetime", "deleted", "entityId",
            "groupId", "identifier", "itemId", "notes", "orderLineId", "status", "assetType"));

    public static final List<FieldSetup> COMMON_FIELDS = new LinkedList<FieldSetup>();

    static {
        for (String name : COMMON_FIELD_NAMES) {
            COMMON_FIELDS.add(new FieldSetup(name, name, Field, String.class));
        }
    }

    private final Set<String> fields = new TreeSet<String>(COMMON_FIELD_NAMES);

    final List<Map<String, String>> data = new LinkedList<Map<String, String>>();

    public AssetsCollector(Collection<? extends AssetEnvelope> source, String defaultAssetIdLabel) {

        for (AssetEnvelope a : source) {

            Map<String, String> dataMap = new TreeMap<String, String>();

            dataMap.put("id", value(a.getId()));
            dataMap.put("assetStatusId", value(a.getAssetStatusId()));
            dataMap.put("createDatetime", value(a.getCreateDatetime()));
            dataMap.put("deleted", value(a.getDeleted()));
            dataMap.put("entityId", value(a.getEntityId()));
            dataMap.put("groupId", value(a.getGroupId()));
            dataMap.put("identifier", value(a.getIdentifier()));
            dataMap.put("itemId", value(a.getItemId()));
            dataMap.put("notes", value(a.getNotes()));
            dataMap.put("orderLineId", value(a.getOrderLineId()));
            dataMap.put("status", value(a.getStatus()));
            dataMap.put("assetType", value(a.getItemType().getDescription()));
            dataMap.put("identifierLabel", value(a.getItemType().getAssetIdentifierLabel(), defaultAssetIdLabel));

            MetaFieldValueWS[] metaFields = a.getMetaFields();
            if (metaFields != null) {
                for (MetaFieldValueWS mf : metaFields) {
                    dataMap.put(String.valueOf("__" + name(mf.getFieldName())), value(mf.getValue()));
                }
            }

            data.add(dataMap);
            fields.addAll(dataMap.keySet());
        }

        for (String f : fields) {
            for (Map<String, String> m : data) {
                if (!m.containsKey(f)) {
                    m.put(f, "");
                }
            }
        }
    }

    public List<Map<String, String>> getData() {
        return unmodifiableList(data);
    }

    public Set<String> getFields() {
        return unmodifiableSet(fields);
    }

    public Set<String> getFields(List<String> fieldList) {
        if (CollectionUtils.isEmpty(fieldList)) {
            getFields();
        }

        return unmodifiableSet(fields.stream()
                                     .filter(f -> !fieldList.contains(f))
                                     .collect(Collectors.toSet()));
    }

    public void ensureFields(Collection<String> fieldNames) {
        fields.addAll(fieldNames);
        for (String f : fields) {
            for (Map<String, String> m : data) {
                if (!m.containsKey(f)) {
                    m.put(f, "");
                }
            }
        }
    }

    private static String name(String n) {
        return n.replace(".", "_").replace(" ", "_");
    }

    private static String value(Object v) {
        return value(v, "");
    }

    private static String value(Object v, String whenNull) {
        return v == null ? whenNull : String.valueOf(v);
    }
}
