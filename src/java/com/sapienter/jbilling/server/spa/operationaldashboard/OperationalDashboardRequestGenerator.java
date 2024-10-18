package com.sapienter.jbilling.server.spa.operationaldashboard;

import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.spa.SpaImportHelper;
import com.sapienter.jbilling.server.user.UserHelperDisplayerDistributel;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.SearchResultString;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Matías Cabezas on 02/10/17.
 */
public class OperationalDashboardRequestGenerator {
    /* Standard fields*/
    private static final String EQUALS_SIGN = "=";
    private static final String AMP = "&";
    private static final String MODE = "_mode";
    private static final String RETURNMODE ="_returnmode";
    private static final String RETURNMODE_PLAINTEXT ="plaintext";
    private static final String RETURNMODE_HTML ="html";
    private static final String DISPLAYMODE ="_displaymode";
    private static final String DISPLAYMODE_FULL ="full";
    private static final String ASSET_ID = "assetid";
    private static final String NULL_STRING = "NULL";
    public static final String TRANSACTION_TAG = "_transactiontag";

    /* Customer fields*/
    private static final String CID = "cid";
    private static final String EMAIL = "email";
    private static final String NAME = "name";
    private static final String LANGUAGE = "language";
    private static final String PHONE_1 = "phone1";
    private static final String PHONE_2 = "phone2";
    private static final String FIRST_NAME = "firstname";
    private static final String LAST_NAME = "lastname";
    
    public static final String PARAM_NAME_SEPARATOR = "_";

    public String getRequestURL(String assetType, UserDTO userDTO, OperationalDashboardMode mode, Integer assetId) {
        Map<String, String> parameters = new HashMap<>();
        AssetDTO assetDTO = new AssetDAS().find(assetId);
        
        /* Prefix is none by default*/
        generateCustomerParameters(OperationalDashboardPrefix.NONE.getValue(), parameters, userDTO, mode, assetDTO);
        generateAssetMetafieldParameters(parameters, assetDTO, OperationalDashboardPrefix.NONE.getValue());

        String assetTypeURL = getAssetTypeURL(assetType, userDTO);
        
        return (StringUtils.isEmpty(assetTypeURL)) ? null : assetTypeURL + pairParametersWithURL(parameters);
    }

    public String getAssetTypeURL(String assetType, UserDTO userDTO) {
        Integer companyId = userDTO.getCompany().getId();
        StringBuilder requestURL = new StringBuilder("");

        RouteDAS routeDAS = new RouteDAS();
        RouteDTO routeDTO = routeDAS.getRouteByName(companyId, "operational_dashboard");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setMax(20);
        criteria.setFilters(new BasicFilter[]{});

        SearchResultString queryResult = routeDAS.searchDataTable(criteria, routeDTO);
        List<List<String>> rows = queryResult.getStringRows();
        if (rows.size() > 0) {
            rowFor:
            for (List<String> row : rows) {
                for (int i = 0; i < row.size(); i++) {
                    if (assetType.contains(row.get(1))) {
                        requestURL.append(row.get(2));
                        break rowFor;
                    }
                }
            }
        }

        return requestURL.toString();
    }

    public void generateCustomerParameters(String prefix, Map<String, String> parameters, UserDTO userDTO, OperationalDashboardMode mode, AssetDTO assetDTO) {
        parameters.put(prefix + CID, String.valueOf(userDTO.getId()));
        CustomerAccountInfoTypeMetaField emailMetaField = userDTO.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.EMAIL_ADDRESS);
        String emailId = (null != emailMetaField &&
                null != emailMetaField.getMetaFieldValue() &&
                null != emailMetaField.getMetaFieldValue().getValue() ? emailMetaField.getMetaFieldValue().getValue().toString() : NULL_STRING);
        parameters.put(prefix + EMAIL, emailId);
        String name = UserHelperDisplayerDistributel.getInstance().getDisplayName(userDTO);
        parameters.put(prefix + NAME, name);
        parameters.put(prefix + LANGUAGE, SpaImportHelper.getLanguageByCode(userDTO.getLanguage().getCode()));

        CustomerAccountInfoTypeMetaField phoneNumber1MetaField = userDTO.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.PHONE_NUMBER_1);
        String phoneNumber1 = (null != phoneNumber1MetaField &&
                null != phoneNumber1MetaField.getMetaFieldValue() &&
                null != phoneNumber1MetaField.getMetaFieldValue().getValue()) ? phoneNumber1MetaField.getMetaFieldValue().getValue().toString() : NULL_STRING;

        parameters.put(prefix + PHONE_1, phoneNumber1);

        CustomerAccountInfoTypeMetaField phoneNumber2MetaField = userDTO.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.PHONE_NUMBER_2);
        String phoneNumber2 = (null != phoneNumber2MetaField &&
                null != phoneNumber2MetaField.getMetaFieldValue() &&
                null != phoneNumber2MetaField.getMetaFieldValue().getValue()) ? phoneNumber2MetaField.getMetaFieldValue().getValue().toString() : NULL_STRING;

        parameters.put(prefix + PHONE_2, phoneNumber2);

        int aux = name.indexOf(" ");
        parameters.put(prefix + FIRST_NAME, (aux != -1) ? name.substring(0, aux) : name);
        parameters.put(prefix + LAST_NAME, (aux != -1) ? name.substring(aux) : "");
        parameters.put(prefix + ASSET_ID, assetDTO.getIdentifier());
        parameters.put(MODE, mode.toString().toLowerCase());
        parameters.put(RETURNMODE, OperationalDashboardMode.SHOW.equals(mode) ? RETURNMODE_HTML : RETURNMODE_PLAINTEXT);
        parameters.put(DISPLAYMODE, DISPLAYMODE_FULL);
        parameters.put(TRANSACTION_TAG, UUID.randomUUID().toString());
        
    }

    private String pairParametersWithURL(Map<String, String> parameters) {
        StringBuilder requestURL = new StringBuilder();
        requestURL.append("?");
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey().replace(" ", PARAM_NAME_SEPARATOR) : null;
            String value = entry.getValue();
            requestURL.append(key).append(EQUALS_SIGN).append(value).append(AMP);
        }        
        /* Remove last AMP character*/
        requestURL.setLength(requestURL.length() - 1);
        return requestURL.toString();
    }

    public void generateAssetMetafieldParameters(Map<String, String> parameters, AssetDTO asset, String prefix) {
        List<MetaFieldValue> assetMetaFields = asset.getMetaFields();
        Set<MetaField> categoryMetaFields = asset.getItem().findItemTypeWithAssetManagement().getAssetMetaFields();
        categoryMetaFields.stream().forEach(metaField -> 
            parameters.put(prefix + metaField.getName(), getValueForMetaField(metaField.getName(), assetMetaFields))
        );
    }

    private String getValueForMetaField(String mfName, List<MetaFieldValue> assetMetaFields) {
        MetaFieldValue mf = assetMetaFields.stream().filter(metaFieldValue -> metaFieldValue.getField().getName().equals(mfName)).findAny().orElse(null);
        if (mf != null && mf.getValue() != null) {
            return mf.getValue().toString();
        }
        return StringUtils.EMPTY;
    }

    public void generateAssetMetafieldParameters(Map<String, String> parameters, Integer assetId, String prefix) {
        AssetDTO assetDTO = new AssetDAS().find(assetId);
        generateAssetMetafieldParameters(parameters, assetDTO, prefix);
    }

    public void generateAssetMetafieldOldParameters(Map<String, String> parameters, Map<String, Object> oldMetaFieldValues, Set<MetaField> categoryMetaFields) {
        categoryMetaFields.stream().forEach(metaField -> {
                String name = metaField.getName();
                String value = oldMetaFieldValues.get(name) != null ? oldMetaFieldValues.get(name).toString() : StringUtils.EMPTY; 
                parameters.put(OperationalDashboardPrefix.OLD.getValue() + name, value);
        });
    }
}