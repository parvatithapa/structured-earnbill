package com.sapienter.jbilling.server.dt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.validator.ValidationException;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.item.RatingConfigurationWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.RatingUnitBL;
import com.sapienter.jbilling.server.pricing.RatingUnitWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.db.RatingUnitDAS;
import com.sapienter.jbilling.server.pricing.db.RatingUnitDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usageratingscheme.UsageRatingSchemeWS;
import com.sapienter.jbilling.server.usageratingscheme.domain.entity.UsageRatingSchemeDTO;
import com.sapienter.jbilling.server.usageratingscheme.domain.repository.UsageRatingSchemeDAS;
import com.sapienter.jbilling.server.usageratingscheme.service.UsageRatingSchemeBL;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;

/**
 * Created by Wajeeha Ahmed on 11/27/17.
 */
public class ProductUploadFileItemProcessor {

    static ProductPriceFileValidator Validator;

    /**
     * Process column values for Product details row
     * @param fieldSet
     */
    public static void processProductFields(FieldSet fieldSet, ProductFileItem fileItem, int entityId) {

        // Validate required Product fields
        Validator.validateProduct(fieldSet, entityId);

        fileItem.setProductCode(fieldSet.readString(ProductImportConstants.PROD_CODE_COL));

        //Manage list of multi-language descriptions
        setMultiLanguageDescription(fieldSet, fileItem);

        String[] categories = fieldSet.readString(ProductImportConstants.PROD_CAT_COL).split(",");
        ArrayList<String> categoryList = new ArrayList<>();

        for(String category : categories){
            if(StringUtils.isNotBlank(category)){
                categoryList.add(category);
            }
        }

        if (!Validator.isAnUpdateRequest() && CollectionUtils.isEmpty(categoryList)) {
            throw new ValidationException("Product Category/ies are required; ");
        }

        // Set Product Categories
        fileItem.setCategories(categoryList.toArray(new String[categoryList.size()]));

        // Set Availability Start Date
        if (!fieldSet.readString(ProductImportConstants.AVAILABILITY_START_COL).trim().isEmpty()) {
            fileItem.setActiveSince(fieldSet.readDate(ProductImportConstants.AVAILABILITY_START_COL, "MM/dd/yyyy"));
        }

        // Set Availability End Date
        if (!fieldSet.readString(ProductImportConstants.AVAILABILITY_END_COL).trim().trim().isEmpty()) {
            fileItem.setActiveUntil(fieldSet.readDate(ProductImportConstants.AVAILABILITY_END_COL, "MM/dd/yyyy"));
        }

        fileItem.setAllowDecimalQuantity(Boolean.parseBoolean(fieldSet.readString(ProductImportConstants.PROD_ALLOW_DECIMAL_QUANTITY_COL)));
        fileItem.setCompany(fieldSet.readString(ProductImportConstants.PROD_COMPANY));
    }

    /**
     * Process column values for Product Price row
     * @param fieldSet
     * @param userId
     */
    public static void processPrice(FieldSet fieldSet, ProductFileItem fileItem, int entityId, int userId) {

        // Validate required Price fields
        Validator.validatePrice(fieldSet, entityId);

        Price price = new Price();
        price.setDate(getLastPricingModelDate(fieldSet, fileItem, entityId));
        price.setCompany(fieldSet.readString(ProductImportConstants.PROD_COMPANY));

        Integer currencyId = getCurrencyId(fieldSet.readString(ProductImportConstants.CURRENCY_CODE_COL), entityId, userId);

        if(null != currencyId){
            price.setCurrencyId(currencyId);
        }

        fileItem.getPrices().add(price);

    }

    /**
     * Process column values for Account Type Price row
     * @param fieldSet
     */
    public static Integer processAccountTypePrice(FieldSet fieldSet, ProductFileItem fileItem, int entityId, int userId) {

        // Validate required Account Type Price fields
        Validator.validateAccountTypePrice(fieldSet);

        if (!fieldSet.readString(ProductImportConstants.PROD_CODE_COL).trim().isEmpty()) {
            fileItem.setProductCode(fieldSet.readString(ProductImportConstants.PROD_CODE_COL));
        }

        fileItem.setAccountTypeId(new Integer(fieldSet.readString(ProductImportConstants.ACCOUNT_TYPE_ID_COL)));

        Date date = Util.parseDate(Util.parseDate(new Date()));
        if (!fieldSet.readString(ProductImportConstants.START_DATE_COL).trim().isEmpty()) {
            date = fieldSet.readDate(ProductImportConstants.START_DATE_COL, "MM/dd/yyyy");
        }

        if (!fieldSet.readString(ProductImportConstants.EXPIRY_DATE_COL).trim().isEmpty()) {
            fileItem.setPriceExpiryDate(fieldSet.readDate(ProductImportConstants.EXPIRY_DATE_COL, "MM/dd/yyyy"));
        }

        fileItem.setPriceEffectiveDate(date);
        fileItem.setChained(fieldSet.readBoolean(ProductImportConstants.PROD_CHAINED));

        Integer currencyId = getCurrencyId(fieldSet.readString(ProductImportConstants.CURRENCY_CODE_COL), entityId, userId);

        return currencyId;
    }

    /**
     * Process column values for Customer Level Price row
     * @param fieldSet
     */
    public static Integer processCustomerTypePrice(FieldSet fieldSet, ProductFileItem fileItem, int entityId, int userId) {

        // Validate required Customer Level Price fields
        Validator.validateCustomerPrice(fieldSet);

        if (!fieldSet.readString(ProductImportConstants.PROD_CODE_COL).trim().isEmpty()) {
            fileItem.setProductCode(fieldSet.readString(ProductImportConstants.PROD_CODE_COL));
        }

        fileItem.setCustomerIdentifier(fieldSet.readString(ProductImportConstants.CUSTOMER_ID_COL));

        Date date = Util.parseDate(Util.parseDate(new Date()));
        if (!fieldSet.readString(ProductImportConstants.START_DATE_COL).trim().isEmpty()) {
            date = fieldSet.readDate(ProductImportConstants.START_DATE_COL, "MM/dd/yyyy");
        }

        if (!fieldSet.readString(ProductImportConstants.EXPIRY_DATE_COL).trim().isEmpty()) {
            fileItem.setPriceExpiryDate(fieldSet.readDate(ProductImportConstants.EXPIRY_DATE_COL, "MM/dd/yyyy"));
        }

        fileItem.setPriceEffectiveDate(date);
        fileItem.setChained(fieldSet.readBoolean(ProductImportConstants.PROD_CHAINED));

        Integer currencyId = getCurrencyId(fieldSet.readString(ProductImportConstants.CURRENCY_CODE_COL), entityId, userId);

        return currencyId;
    }

    /**
     * Process column values for Flat pricing model
     * @param fieldSet
     */
    public static void processPriceModelFlat(FieldSet fieldSet, PriceModelWS priceModel){
        // Validate required FLAT price model fields
        Validator.validateFlatPriceModel(fieldSet);

        priceModel.setType(PriceModelStrategy.FLAT.name());
        priceModel.setRate(new BigDecimal(fieldSet.readString(ProductImportConstants.FLAT_RATE_COL)));
    }

    /**
     * Returns Currency ID for the given currency code, if it doesn't exists returns currency id for the Entity
     * @param currencyCode
     * @param entityId
     * @return
     */
    private static Integer getCurrencyId(String currencyCode, int entityId, int userId){
        Integer currencyId = null;

        if(!StringUtils.isEmpty(currencyCode)) {

            CurrencyDTO currencyDTO = new CurrencyDAS().findCurrencyByCode(currencyCode);
            try {
                if(CurrencyBL.entityHasCurrency(entityId, currencyDTO.getId()))
                {
                    currencyId = currencyDTO.getId();
                }
            } catch (Exception exception) {
                throw new ValidationException("Invalid Currency Code." + exception);
            }
        }

        if (null == currencyId) {
            // If the currency is not specified use the default company currency
            throw new ValidationException("Currency Code does not exist for the provided Company");
        }

        return currencyId;
    }

    public static void addChain(PriceModelWS prices, PriceModelWS newPriceModelWS){
        if(prices == null){
            prices = newPriceModelWS;

        }else if (prices.getNext() != null){
            addChain(prices.getNext(), newPriceModelWS);
        }
        else{
            prices.setNext(newPriceModelWS);
        }
    }

    public static void createUpdatePriceTimeLine(ProductFileItem item, Date date, PriceModelWS newPriceModelWS){

        SortedMap<Date, PriceModel> timeLine = item.getPriceModelTimeLine();
        PriceModel priceModel = timeLine.get(date);

        if(!priceModel.isChained()){
            priceModel.setPriceModelWS(newPriceModelWS);

        }else {
            Validator.validateChainInPricingModel(priceModel.getPriceModelWS(), newPriceModelWS);

            PriceModelWS priceModelWS = priceModel.getPriceModelWS();

            while (true){
                if (priceModelWS == null) {
                    priceModel.setPriceModelWS(newPriceModelWS);
                    break;
                }
                else {
                    priceModelWS = priceModelWS.getNext();
                }
            }
        }
    }

    public static Date getLastPricingModelDate(FieldSet fieldSet, ProductFileItem fileItem, Integer entityId){

        Date date = null;

        boolean isAnUpdateRequest = Validator.isAnUpdateRequest();

        if(fieldSet.readString(ProductImportConstants.AVAILABILITY_START_COL).trim().isEmpty()){

            date = TimezoneHelper.companyCurrentDate(entityId);
            if(!isAnUpdateRequest) {
                fileItem.EnablePriceModelForEpochDate(true);
            }else{
                fileItem.EnablePriceModelForEpochDate(false);
            }
        }
        else if(StringUtils.isNotBlank(fieldSet.readString(ProductImportConstants.AVAILABILITY_START_COL))){
            Date priceModelDate = fieldSet.readDate(ProductImportConstants.AVAILABILITY_START_COL, "MM/dd/yyyy");

            if(!isAnUpdateRequest) {

                if (priceModelDate.equals(CommonConstants.EPOCH_DATE)) {
                    fileItem.EnablePriceModelForEpochDate(false);
                } else {
                    fileItem.EnablePriceModelForEpochDate(true);
                }
            }

            return priceModelDate;

        }

        return date;
    }

    /**
     * Process column values for Tiered pricing model
     * @param fieldSet
     */
    public static void processPriceModelTiered(FieldSet fieldSet, PriceModelWS priceModel){
        // Validate required TIERED price model fields
        Validator.validateTieredPriceModel(fieldSet);

        if(null == priceModel.getType()) {
            priceModel.setType(BulkLoaderUtility.checkPriceStrategy(PriceModelStrategy.TIERED.name()));
        }

        priceModel.addAttribute(fieldSet.readString(ProductImportConstants.QTY_FROM_COL), fieldSet.readString(ProductImportConstants.TIER_RATE_COL));
    }

    public static void setChainedValueInPriceModel(PriceModel priceModel, FieldSet fieldSet){
        priceModel.setChained(Boolean.parseBoolean(fieldSet.readString(ProductImportConstants.PROD_CHAINED)));
    }

    private static void setMultiLanguageDescription(FieldSet fieldSet, ProductFileItem fileItem){
        try{

            String descriptions[] = fieldSet.readString(ProductImportConstants.PROD_DESC_COL).split(",");

            fileItem.getDescriptions().addAll(getInternationalDescriptionWS(descriptions));

        }catch (ValidationException validationException){
            throw validationException;
        }catch (Exception exception){
            throw new ValidationException("Invalid detail in product description");
        }
    }

    public static void processRatingUnit(FieldSet fieldSet, ProductFileItem fileItem, int entityId){

        // Validate incoming Rating Unit
        Validator.validateRatingUnitScheme(fieldSet);

        Date ratingConfigurationDate = fieldSet.readDate(ProductImportConstants.AVAILABILITY_START_COL, "MM/dd/yyyy");

        RatingUnitWS ratingUnitWS = null;
        UsageRatingSchemeWS ratingSchemeWS = null;

        // Process Rating Unit if the incoming value is not Blank
        if (!StringUtils.isBlank(fieldSet.readString(ProductImportConstants.RATING_UNIT_NAME))) {

            // Verify if Incoming Rating Unit is applicable
            List<RatingUnitDTO> ratingUnitDTOList = new RatingUnitDAS().findByName(fieldSet.readString(ProductImportConstants.RATING_UNIT_NAME).trim(), entityId);

            if(CollectionUtils.isEmpty(ratingUnitDTOList)){

                throw new ValidationException(String.format("Given RatingUnit: %s is not available for Entity: %s",
                        fieldSet.readString(ProductImportConstants.RATING_UNIT_NAME), entityId ));

            } else if(ratingUnitDTOList.size() > 1){

                throw new ValidationException(String.format("More than 1 Rating Unit found for given name: %s",
                        fieldSet.readString(ProductImportConstants.RATING_UNIT_NAME)));
            }

            ratingUnitWS = RatingUnitBL.getWS(ratingUnitDTOList.get(0));
        }

        // Process Rating Scheme if the incoming value is not Blank
        if (!StringUtils.isBlank(fieldSet.readString(ProductImportConstants.RATING_SCHEME_NAME))) {

            // Verify if Incoming Rating Scheme is applicable
            UsageRatingSchemeDTO ratingSchemeDTO = new UsageRatingSchemeDAS()
                    .getByRatingSchemeCode(fieldSet.readString(ProductImportConstants.RATING_SCHEME_NAME).trim(), entityId);

            if (ratingSchemeDTO == null) {
                throw new ValidationException(String.format("Given Rating Scheme Code: %s is not available for Entity: %s",
                        fieldSet.readString(ProductImportConstants.RATING_SCHEME_NAME), entityId));
            }

            ratingSchemeWS = UsageRatingSchemeBL.getWS(ratingSchemeDTO);
        }

        RatingConfigurationWS ratingConfigurationWS = new RatingConfigurationWS();

        if(ratingUnitWS != null) {
            ratingConfigurationWS.setRatingUnit(ratingUnitWS);
        }

        if(ratingSchemeWS != null){
            ratingConfigurationWS.setUsageRatingScheme(ratingSchemeWS);
        }

        if(StringUtils.isNotBlank(fieldSet.readString(ProductImportConstants.PRICE_UNIT))){


            String pricingUnitDescriptions[] = fieldSet.readString(ProductImportConstants.PRICE_UNIT).trim().split(",");

            try {
                ratingConfigurationWS.setPricingUnit(getInternationalDescriptionWS(pricingUnitDescriptions));
            }catch (Exception exception){
                throw new ValidationException("Invalid detail in pricing unit description");
            }
        }

        // Setting Rating Configuration to NULL so that it can be used to remove the time-line if it exists
        if(ratingConfigurationWS.getRatingUnit() == null && ratingConfigurationWS.getUsageRatingScheme() == null
                && CollectionUtils.isEmpty(ratingConfigurationWS.getPricingUnit())){
            ratingConfigurationWS = null;
       }

        // Add Rating Configuration to the time-line
        fileItem.getRatingConfigurationTimeLine().put(ratingConfigurationDate, ratingConfigurationWS);


    }

    public static List<InternationalDescriptionWS> getInternationalDescriptionWS(String descriptions[]){

        List<InternationalDescriptionWS> internationalDescriptions=new ArrayList<>();

        // NOTE: Temporary code keep the current format in working state
        if(descriptions.length == 1){

            String descriptionDetails[] = descriptions[0].split(":");

            InternationalDescriptionWS internationalDescriptionWS = new InternationalDescriptionWS();

            try {

                if(descriptionDetails.length >= 2 && StringUtils.isNumeric(descriptionDetails[0])) {
                    int languageId = Integer.parseInt(descriptionDetails[0].trim());

                    internationalDescriptionWS.setLanguageId(languageId);
                    String content =  descriptions[0].substring(descriptionDetails[0].length()+1, descriptions[0].length());

                    internationalDescriptionWS.setContent(content.trim());
                }else {
                    // Set default language English
                    internationalDescriptionWS.setLanguageId(1);
                    internationalDescriptionWS.setContent(descriptions[0].trim());
                }

            } catch (NumberFormatException e) {
                throw new ValidationException("Language ID should be a valid INTEGER - " + descriptionDetails);
            }

            internationalDescriptions.add(internationalDescriptionWS);
            return internationalDescriptions;
        }

        for(String description: descriptions){

            String descriptionDetails[] = description.split(":");

            if(descriptionDetails.length < 2){
                throw new ValidationException("Language ID or Description missing - " + description);
            }

            try {

                InternationalDescriptionWS internationalDescriptionWS = new InternationalDescriptionWS();
                int languageId = Integer.parseInt(descriptionDetails[0].trim());

                internationalDescriptionWS.setLanguageId(languageId);
                String content =  description.substring(descriptionDetails[0].length()+1, description.length());

                internationalDescriptionWS.setContent(content.trim());
                internationalDescriptions.add(internationalDescriptionWS);
            } catch (NumberFormatException e) {
                throw new ValidationException("Language ID should be a valid INTEGER - " + description);
            }
        }

        return internationalDescriptions;
    }

    public static void processMetaFields(FieldSet fieldSet, ProductFileItem fileItem, int entityId){

        // Using Array incase more MetaField requirements come in
        MetaFieldValueWS[] metaFieldValueWSArray = new MetaFieldValueWS[1];

        String metaFieldFeatures = fieldSet.readString(ProductImportConstants.META_FIELD_FEATURES).trim();

        // Find MetaField details
        MetaField metaField = new MetaFieldDAS().getFieldByName(entityId, new EntityType[]{EntityType.PRODUCT},
                com.sapienter.jbilling.server.integration.Constants.PRODUCT_FEATURES_MF);

        if(metaField != null) {

            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(com.sapienter.jbilling.server.integration.Constants.PRODUCT_FEATURES_MF);
            metaFieldValueWS.setDataType(metaField.getDataType());
            metaFieldValueWS.setValue(metaFieldFeatures);

            metaFieldValueWSArray[0] = metaFieldValueWS;

        }else{
            throw new ValidationException(String.format("Given MetaField: %s is not available for Entity: %s",
                    com.sapienter.jbilling.server.integration.Constants.PRODUCT_FEATURES_MF, entityId));
        }

        fileItem.setMetaFields(metaFieldValueWSArray);
    }
}
