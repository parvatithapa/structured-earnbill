package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.validator.ValidationException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Wajeeha Ahmed on 12/11/17.
 */
public class ProductPriceFileValidator {

    private ItemDTO itemDTO = null;

    public enum PriceType {
        DEFAULT,
        ACCOUNT_TYPE,
        CUSTOMER,
        DEPENDENCY
    }

    /**
     * Validates Product row fields
     * @param fieldSet
     * @param entityId
     * @throws ValidationException
     */
    public void validateProduct(FieldSet fieldSet, int entityId) throws ValidationException {

        if (StringUtils.isBlank(fieldSet.readString(ProductImportConstants.PROD_CODE_COL))){
            throw new ValidationException("Invalid Product Code");
        }

        itemDTO = new ItemDAS().findItemByInternalNumber(fieldSet.readString(ProductImportConstants.PROD_CODE_COL).trim(), entityId);

        if (null == itemDTO && (StringUtils.isBlank(fieldSet.readString(ProductImportConstants.PROD_DESC_COL)))){
            throw new ValidationException("Invalid Product Description");
        }

        if (null == itemDTO && StringUtils.isBlank((ProductImportConstants.PROD_CAT_COL))){
            throw new ValidationException("Invalid Categories");
        }

        Date startDate = null;
        Date endDate = null;

        if(!StringUtils.isBlank(fieldSet.readString(ProductImportConstants.AVAILABILITY_START_COL))) {
            try {
                startDate = fieldSet.readDate(ProductImportConstants.AVAILABILITY_START_COL, "MM/dd/yyyy");
            } catch(Exception e) {
                throw new ValidationException("Invalid Availability Start Date");
            }
        }

        if(!StringUtils.isBlank(fieldSet.readString(ProductImportConstants.AVAILABILITY_END_COL))) {
            try {
                endDate = fieldSet.readDate(ProductImportConstants.AVAILABILITY_END_COL, "MM/dd/yyyy");
            } catch(Exception e) {
                throw new ValidationException("Invalid Availability End Date");
            }
        }

        if(!isAnUpdateRequest()) {
            if (startDate != null && endDate != null && startDate.compareTo(endDate) == 1) {
                throw new ValidationException("Availability End Date should be greater than Availability Start Date");
            }

            if (startDate == null && endDate != null) {
                startDate = Util.parseDate(Util.parseDate(new Date()));

                if (startDate.compareTo(endDate) == 1) {
                    throw new ValidationException("Availability End Date should be in future");
                }
            }
        }
    }

    /**
     * Validates Default Product Price row fields
     * @param fieldSet
     * @param entityId
     * @throws ValidationException
     */
    public void validatePrice(FieldSet fieldSet, int entityId) throws ValidationException {

        if(!StringUtils.isBlank(fieldSet.readString(ProductImportConstants.AVAILABILITY_START_COL))) {
            try {
                fieldSet.readDate(ProductImportConstants.AVAILABILITY_START_COL, "MM/dd/yyyy");
            } catch(Exception e) {
                throw new ValidationException("Invalid Effective Date");
            }
        }

        if(null == itemDTO && (StringUtils.isBlank(fieldSet.readString(ProductImportConstants.CURRENCY_CODE_COL)))) {
            throw new ValidationException("Invalid Currency Code");
        }
    }

    /**
     * Validates Account Type Price row fields
     * @param fieldSet
     * @throws ValidationException
     */
    public void validateAccountTypePrice(FieldSet fieldSet) throws ValidationException {

        // PRODUCT CODE Shouldn't be empty or a blank space
        if (StringUtils.isBlank(fieldSet.readString(ProductImportConstants.PROD_CODE_COL))) {
            throw new ValidationException("Product Code is required to set Account Type Price");
        }

        // ACCOUNT ID Shouldn't be empty or a blank space
        if (StringUtils.isBlank(fieldSet.readString(ProductImportConstants.ACCOUNT_TYPE_ID_COL))) {
            throw new ValidationException("Invalid Account Type ID");
        } else {
            if (!NumberUtils.isDigits(fieldSet.readString(ProductImportConstants.ACCOUNT_TYPE_ID_COL))) {
                throw new ValidationException("Invalid Account Type ID format");
            }
        }

        // IF Effective Date column is not empty or blank space it should be in given format
        Date startDate = null;
        Date endDate = null;

        if(!StringUtils.isBlank(fieldSet.readString(ProductImportConstants.START_DATE_COL))) {
            try {
                startDate = fieldSet.readDate(ProductImportConstants.START_DATE_COL, "MM/dd/yyyy");
            } catch(Exception e) {
                throw new ValidationException("Invalid Availability Start Date");
            }
        }

        if(!StringUtils.isBlank(fieldSet.readString(ProductImportConstants.EXPIRY_DATE_COL))) {
            try {
                endDate = fieldSet.readDate(ProductImportConstants.EXPIRY_DATE_COL, "MM/dd/yyyy");
            } catch(Exception e) {
                throw new ValidationException("Invalid Availability End Date");
            }
        }

        if (startDate != null && endDate != null && startDate.compareTo(endDate) == 1) {
            throw new ValidationException("Availability End Date should be greater than Availability Start Date");
        }

        if (startDate == null && endDate != null) {
            startDate = Util.parseDate(Util.parseDate(new Date()));

            if (startDate.compareTo(endDate) == 1) {
                throw new ValidationException("Availability End Date should be in future");
            }
        }
        // IF CHAINED column is not empty or blank space it should be in given format
        if(!StringUtils.isBlank(fieldSet.readString(ProductImportConstants.PROD_CHAINED))) {
            try {
                fieldSet.readBoolean(ProductImportConstants.PROD_CHAINED);
            } catch(Exception e) {
                throw new ValidationException("Invalid chained value");
            }
        }
    }

    /**
     * Validates Customer Level Price row fields
     * @param fieldSet
     * @throws ValidationException
     */
    public void validateCustomerPrice(FieldSet fieldSet) throws ValidationException {

        if (StringUtils.isBlank(fieldSet.readString(ProductImportConstants.PROD_CODE_COL))) {
            throw new ValidationException("Product Code is required to set Customer Level Price");
        }

        if (StringUtils.isBlank(fieldSet.readString(ProductImportConstants.CUSTOMER_ID_COL))) {
            throw new ValidationException("Customer Identifier can't be empty");
        }

        Date startDate = null;
        Date endDate = null;

        if(!StringUtils.isBlank(fieldSet.readString(ProductImportConstants.START_DATE_COL))) {
            try {
                startDate = fieldSet.readDate(ProductImportConstants.START_DATE_COL, "MM/dd/yyyy");
            } catch(Exception e) {
                throw new ValidationException("Invalid Availability Start Date");
            }
        }

        if(!StringUtils.isBlank(fieldSet.readString(ProductImportConstants.EXPIRY_DATE_COL))) {
            try {
                endDate = fieldSet.readDate(ProductImportConstants.EXPIRY_DATE_COL, "MM/dd/yyyy");
            } catch(Exception e) {
                throw new ValidationException("Invalid Availability End Date");
            }
        }

        if (startDate != null && endDate != null && startDate.compareTo(endDate) == 1) {
            throw new ValidationException("Availability End Date should be greater than Availability Start Date");
        }

        if (startDate == null && endDate != null) {
            startDate = Util.parseDate(Util.parseDate(new Date()));

            if (startDate.compareTo(endDate) == 1) {
                throw new ValidationException("Availability End Date should be in future");
            }
        }

        // IF CHAINED column is not empty or blank space it should be in given format
        if(!StringUtils.isBlank((fieldSet.readString(ProductImportConstants.PROD_CHAINED)))) {
            try {
                fieldSet.readBoolean(ProductImportConstants.PROD_CHAINED);
            } catch(Exception e) {
                throw new ValidationException("Invalid chained value");
            }
        }
    }

    /**
     * Validates FLAT Price Model row fields
     * @param fieldSet
     * @throws ValidationException
     */
    public void validateFlatPriceModel(FieldSet fieldSet) throws ValidationException {

        if (StringUtils.isBlank(fieldSet.readString(ProductImportConstants.FLAT_RATE_COL))) {
            throw new ValidationException("Invalid price for FLAT model");
        }else if (!BulkLoaderUtility.isValidPrice(fieldSet.readString(ProductImportConstants.FLAT_RATE_COL))) {
            throw new ValidationException("Invalid Rate format for FLAT price model");
        }
    }

    /**
     * Validates Product for required rows i.e. PROD,PRICE,*MODEL
     * @param attributesRead
     */
    public void validateProductRowsForDefaultPrices(List<ProductImportConstants.ColumnIdentifier> attributesRead) throws ValidationException {

        if (attributesRead.contains(ProductImportConstants.ColumnIdentifier.PRODUCT)) {
            if (attributesRead.contains(ProductImportConstants.ColumnIdentifier.PRICE)) {
                if (null == itemDTO && !validatePriceModel(attributesRead)) {
                    throw new ValidationException("Missing Price Model from Product");
                }

                return;
            }

            throw new ValidationException("Missing Price row from Product");
        }
        throw new ValidationException("Missing Product row");
    }


    /**
     * Validates Product for required rows i.e. PRICE,*MODEL
     * @param attributesRead
     */
    public void validateProductRows(List<ProductImportConstants.ColumnIdentifier> attributesRead) throws ValidationException {

        if (attributesRead.contains(ProductImportConstants.ColumnIdentifier.PRICE)){
            if (!validatePriceModel(attributesRead)){
                throw new ValidationException("Missing Price Model from Product");
            }

            return;
        }
        throw new ValidationException("Missing PRICE row for the Product");
    }

    /**
     * Validates supported Price Models
     * @param attributesRead
     * @return
     */
    private boolean validatePriceModel(List<ProductImportConstants.ColumnIdentifier> attributesRead){

        List<ProductImportConstants.ColumnIdentifier> supportedPriceModels = new ArrayList<>();

        supportedPriceModels.add(ProductImportConstants.ColumnIdentifier.PRICE_MODEL_FLAT);
        supportedPriceModels.add(ProductImportConstants.ColumnIdentifier.PRICE_MODEL_TIERED);

        return CollectionUtils.containsAny(attributesRead, supportedPriceModels);
    }

    public void validateChainInPricingModel(PriceModelWS prices, PriceModelWS newPriceModel){

        if(null == prices || null == newPriceModel)
            return;

        if(prices.getType().equals(PriceModelStrategy.FLAT) &&
                newPriceModel.getType().equals(PriceModelStrategy.FLAT)){
            throw new ValidationException("Invalid pricing model. Cannot add two Flat pricing model in a chain");
        }
    }

    /**
     * Validates TIERED Price Model row fields
     * @param fieldSet
     * @throws ValidationException
     */
    public void validateTieredPriceModel(FieldSet fieldSet) throws ValidationException {

        if (StringUtils.isBlank(fieldSet.readString(ProductImportConstants.QTY_FROM_COL))) {
            throw new ValidationException("Invalid Quantity value for TIERED");
        } else {
            try {
                int quantity = Integer.parseInt(fieldSet.readString(ProductImportConstants.QTY_FROM_COL).trim());
            } catch (NumberFormatException e) {
                throw new ValidationException("Invalid Quantity format for TIERED");
            }
        }

        if (StringUtils.isBlank(fieldSet.readString(ProductImportConstants.TIER_RATE_COL))) {
            throw new ValidationException("Invalid Rate for TIERED");
        } else if (!BulkLoaderUtility.isValidPrice(fieldSet.readString(ProductImportConstants.TIER_RATE_COL))) {
            throw new ValidationException("Invalid Rate format for TIERED");
        }
    }

    public boolean isAnUpdateRequest(){
        if(null == itemDTO)
            return false;
        return true;
    }

    public void validateRatingUnitScheme(FieldSet fieldSet) throws ValidationException{

        // Date is mandatory while adding Rating Unit/Scheme
        if(StringUtils.isBlank(fieldSet.readString(ProductImportConstants.AVAILABILITY_START_COL))) {
            throw new ValidationException("Date is required while adding or updating Rating Unit/Scheme");
        }else{
            try {
                fieldSet.readDate(ProductImportConstants.AVAILABILITY_START_COL, "MM/dd/yyyy");
            } catch(Exception e) {
                throw new ValidationException("Invalid Effective Date for Rating Unit/Scheme");
            }
        }
    }

    public void validatePricingModelStartDate(ProductFileItem fileItem, Date priceModelDate, Date productActiveSinceDate) {

        boolean validDateExists = fileItem.getPriceModelTimeLine().keySet().stream().anyMatch(existingDate -> existingDate.compareTo(productActiveSinceDate) <= 0);

        // Check Persisted Pricing Models for Start Date
        if (!validDateExists && isAnUpdateRequest()) {
            ItemDTOEx itemDTOEx = ItemBL.getItemDTOEx(this.itemDTO);

            validDateExists = itemDTOEx.getDefaultPrices().keySet().stream().anyMatch(existingDate -> existingDate.compareTo(productActiveSinceDate) <= 0);
        }

        if (!validDateExists) {
            if (priceModelDate.compareTo(productActiveSinceDate) == 1) {
                throw new ValidationException("Availability Start Date cannot be less than Price Model Start date");
            }
        }

    }
}
