package com.sapienter.jbilling.test.framework.builders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;

/**
 * Created by dario on 24/06/16.
 */
public class AssetBuilder extends AbstractBuilder {

    private String code;
    private Integer assetStatusId;
    private Integer itemId;
    private Integer[] entities;
    private List<MetaFieldValueWS> metaFields = new ArrayList<>();
    private boolean global = false;
    private String identifier;

    private AssetBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        super(api, testEnvironment);
    }

    public static AssetBuilder getBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        return new AssetBuilder(api, testEnvironment);
    }

    public AssetBuilder withCode(String code) {
        this.code = code;
        return this;
    }

    public AssetBuilder withMetafields(List<MetaFieldValueWS> metaFields) {
        this.metaFields = metaFields;
        return this;
    }

    public AssetBuilder withAssetStatusId (Integer assetStatusId) {
        this.assetStatusId = assetStatusId;
        return this;
    }

    public AssetBuilder withItemId (Integer itemId) {
        this.itemId = itemId;
        return this;
    }

    public AssetBuilder global (boolean global) {
        this.global = global;
        return this;
    }

    public AssetBuilder withIdentifier (String identifier) {
        this.identifier = identifier;
        return this;
    }

    public Integer build () {
        AssetWS asset = new AssetWS();
        asset.setAssetStatusId(assetStatusId);
        asset.setItemId(itemId);
        asset.setGlobal(global);
        asset.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));
        asset.setIdentifier(identifier!=null ? identifier : "Asset");
        asset.setEntities(null == entities ? Arrays.asList(api.getCallerCompanyId()) : Arrays.asList(entities));
        asset.setEntityId(api.getCallerCompanyId());

        Integer assetId = api.createAsset(asset);

        testEnvironment.add(code, assetId, code, api, TestEntityType.ASSET);

        return assetId;
    }
}
