package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.validator.ValidationException;

import java.util.Date;
import java.util.List;

/**
 * Created by Taimoor Choudhary on 3/29/18.
 */
public class PlanPriceFileValidator {

    public static Integer validatePlanDetails(FieldSet fieldSet, int entityId) throws ValidationException {

        if (StringUtils.isBlank(fieldSet.readString(PlanImportConstants.PLAN_NUMBER_COL))){
            throw new ValidationException("Invalid Plan Number");
        }

        List<PlanDTO> planDTO = new PlanDAS().findPlanByPlanNumber(fieldSet.readString(PlanImportConstants.PLAN_NUMBER_COL).trim(), entityId);

        Date startDate = null;
        Date endDate = null;

        if(!StringUtils.isBlank(fieldSet.readString(PlanImportConstants.AVAILABILITY_START_COL))) {
            try {
                startDate = fieldSet.readDate(PlanImportConstants.AVAILABILITY_START_COL, "MM/dd/yyyy");
            } catch(Exception e) {
                throw new ValidationException("Invalid Availability Start Date");
            }
        }

        if(!StringUtils.isBlank(fieldSet.readString(PlanImportConstants.AVAILABILITY_END_COL))) {
            try {
                endDate = fieldSet.readDate(PlanImportConstants.AVAILABILITY_END_COL, "MM/dd/yyyy");
            } catch(Exception e) {
                throw new ValidationException("Invalid Availability End Date");
            }
        }

        if (startDate != null && endDate != null && startDate.compareTo(endDate) == 1) {
            throw new ValidationException("Availability End Date should be greater than Availability Start Date");
        }

        if (startDate == null && endDate != null) {
            startDate = Util.parseDate(Util.parseDate(TimezoneHelper.companyCurrentDate(entityId)));

            if (startDate.compareTo(endDate) == 1) {
                throw new ValidationException("Availability End Date should be in future");
            }
        }

        if(planDTO == null && StringUtils.isBlank(fieldSet.readString(PlanImportConstants.CURRENCY_CODE_COL))) {
            throw new ValidationException("Currency Code cannot be Empty/Blank");
        }

        if(planDTO == null && StringUtils.isBlank(fieldSet.readString(PlanImportConstants.PLAN_PERIOD_COL))) {
            throw new ValidationException("Plan Period cannot be Empty/Blank");
        }

        if (StringUtils.isNotBlank(fieldSet.readString(PlanImportConstants.PLAN_RATE_COL))) {
            if (!BulkLoaderUtility.isValidPrice(fieldSet.readString(PlanImportConstants.PLAN_RATE_COL))) {
                throw new ValidationException("Invalid Rate format for PLAN");
            }
        }

        if (StringUtils.isBlank(fieldSet.readString(PlanImportConstants.PLAN_CATEGORY_COL))) {
                throw new ValidationException("Plan Category name needs to be provided");
        }

        if (StringUtils.isBlank(fieldSet.readString(PlanImportConstants.PLAN_MF_PAYMENT_OPTION_COL))) {
            throw new ValidationException("Plan Payment Option Meta-Filed can't be empty");
        }

        if (StringUtils.isBlank(fieldSet.readString(PlanImportConstants.PLAN_MF_DURATION_COL))) {
            throw new ValidationException("Plan Duration Meta-Filed can't be empty");
        }

        return planDTO != null? planDTO.get(0).getId() : null;
    }

    public static int validatePlanItemDetails(FieldSet fieldSet, int entityId) throws ValidationException {

        if(StringUtils.isBlank(fieldSet.readString(PlanImportConstants.BUNDLE_ITEM_CODE_COL))) {
            throw new ValidationException("Bundle Product code is required");
        }

        ItemDTO itemDTO = new ItemDAS().findItemByInternalNumber(fieldSet.readString(PlanImportConstants.BUNDLE_ITEM_CODE_COL).trim(), entityId);

        if(itemDTO == null){
            throw new ValidationException("No item found for the given Product Code in the current Company");
        }

        if(StringUtils.isNotBlank(fieldSet.readString(PlanImportConstants.BUNDLE_QTY_COL))) {
            if (!NumberUtils.isNumber(fieldSet.readString(PlanImportConstants.BUNDLE_QTY_COL))) {
                throw new ValidationException("Invalid Bundle Quantity format");
            }
        }

        if(StringUtils.isBlank(fieldSet.readString(PlanImportConstants.BUNDLE_PERIOD_COL))) {
            throw new ValidationException("Bundle Item Period is required");
        }

        return itemDTO.getId();
    }

    public static void validatePlanFreeUsagePoolDetails(FieldSet fieldSet) throws ValidationException {

        // Free Usage Pool name cannot be empty
        if (StringUtils.isBlank(fieldSet.readString(PlanImportConstants.FREE_USAGE_POOL_COL))){
            throw new ValidationException("FUP name can't be empty.");
        }
    }

    public static void validatePlanRows(List<PlanImportConstants.ColumnIdentifier> attributesRead) throws ValidationException {
        if (attributesRead.contains(PlanImportConstants.ColumnIdentifier.PLAN)){
            if (!attributesRead.contains(PlanImportConstants.ColumnIdentifier.ITEM)){
                throw new ValidationException("Missing Product row from Plan");
            }

            return;
        }
        throw new ValidationException("Missing Plan row.");
    }
}
