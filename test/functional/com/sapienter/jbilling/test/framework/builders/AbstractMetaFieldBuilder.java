package com.sapienter.jbilling.test.framework.builders;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by aman on 15/3/16.
 */
public abstract class AbstractMetaFieldBuilder<T extends AbstractMetaFieldBuilder> {
    private List<MetaFieldTool> metaFields = new LinkedList<MetaFieldTool>();

    public T withMetaField(String metaFieldName, Object metaFieldValue) {
        metaFields.add(new MetaFieldTool(metaFieldName, metaFieldValue));
        return (T) this;
    }

    public T withMetaField(String metaFieldName, Object metaFieldValue, Integer groupId) {
        metaFields.add(new MetaFieldTool(metaFieldName, metaFieldValue, groupId));
        return (T) this;
    }

    public MetaFieldValueWS[] buildMetaField() {

        MetaFieldValueWS[] values = metaFields.stream().map(mft -> {
            MetaFieldValueWS value = new MetaFieldValueWS();
            value.setFieldName(mft.mfName);
            value.setValue(mft.value);
            if (mft.groupId != null) value.setGroupId(mft.groupId);
            return value;
        }).toArray(MetaFieldValueWS[]::new);

        if (values != null && values.length > 0) return values;
        return null;
    }

    private static class MetaFieldTool {
        String mfName;
        Object value;
        Integer groupId;

        public MetaFieldTool(String mfName, Object value) {
            this.mfName = mfName;
            this.value = value;
        }

        public MetaFieldTool(String mfName, Object value, Integer groupId) {
            this.mfName = mfName;
            this.value = value;
            this.groupId = groupId;
        }
    }
}
