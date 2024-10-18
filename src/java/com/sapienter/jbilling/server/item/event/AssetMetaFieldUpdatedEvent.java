package com.sapienter.jbilling.server.item.event;

import java.util.Map;

import lombok.ToString;

import org.springframework.util.Assert;

import com.google.common.collect.MapDifference.ValueDifference;
import com.sapienter.jbilling.server.system.event.Event;

@ToString
public class AssetMetaFieldUpdatedEvent implements Event {

    private Integer assetId;
    private Integer entityId;
    private Map<String, ValueDifference<Object>> assetMetaFieldDiff;

    public AssetMetaFieldUpdatedEvent(Integer assetId, Integer entityId,
            Map<String, ValueDifference<Object>> assetMetaFieldDiff) {
        this.assetId = assetId;
        this.entityId = entityId;
        this.assetMetaFieldDiff = assetMetaFieldDiff;
    }

    public Integer getAssetId() {
        return assetId;
    }

    public Map<String, ValueDifference<Object>> getAssetMetaFieldDiff() {
        return assetMetaFieldDiff;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public String getName() {
        return "AssetMetaFieldUpdatedEvent";
    }

    public ValueDifference<Object> findMetaFieldValueDiffForName(String metaFieldName) {
        Assert.hasText(metaFieldName, "provide metafield name!");
        return assetMetaFieldDiff.get(metaFieldName);
    }
}
