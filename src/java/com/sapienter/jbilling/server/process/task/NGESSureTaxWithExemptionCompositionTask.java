package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.client.metafield.MetaFieldBindHelper;
import com.sapienter.jbilling.client.suretax.request.LineItem;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineTypeDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.*;

/**
 * Created by aman on 22/4/16.
 */


/*
* This plugin is extended version of Sure tax plugin. If taxline is defined on customer level then
 * it used customer level taxline else calculate taxline by suretax. It provides additional functionality
* of tax exemption. basically twp types of tax exemption :
* 1.Tax Exemption Code : Tax exemption can be customer specific according to NGES requirement.
*       Currently tax exemption is defined at product level but it should be defined at customer level too.
*       So, while enrolling customer, CSR will define the tax exemption code for the customer
*       and it should be used for tax calculation. That meta field name will be provided as plugin parameter
*       for TAX_EXEMPTION_CODE_FIELD_NAME
* 2.Percentage Based Tax Exemption : A particular tax exemption code is used for specific type of tax exemption.
*       It will exempt that type of tax for that product completely i.e. tax free.
*       For NGES, customer can get discount in any particular tax type. So, it means, there will be tax but amount would be less
*       as per discount percentage. Again, percentage will be defined at customer level. That meta field name will be
*       provided as plugin parameter for PERCENTAGE_EXEMPTION_FIELD_NAME. And percentage code for discount will be in
*       plugin parameter TAX_EXEMPTION_CODE_FOR_DISCOUNT_VALUE.
*
*       For example : Invoice Line amount is $100 and acc. to Sure tax, Sales tax would be $10 for this amount(10% sales tax on amount).
*       But NGES is giving 15% discount is sales tax to this customer. So, total amount should be $100+(10-1.5)=$108.5
*
*       So, percentage is defined at customer level in AIT meta field. We will send two request line for single invoice line.
*       First request line for sure tax : 100-15%=85. So, tax will be $85+$8.5=$93.5
*       Second request line for sure tax : $15 with tax exemption code for sales tax(Defined at plugin level). Means no tax on it.
*                                           Total amount for this line : $15.
*       Total amount : 93.5+15=$108.5
* */
public class NGESSureTaxWithExemptionCompositionTask extends SureTaxCompositionTask {

    private static final FormatLogger LOG = new FormatLogger(
            Logger.getLogger(NGESSureTaxWithExemptionCompositionTask.class));

    public static final String PERCENTAGE_EXEMPTION_FIELD_NAME = "Percentage Exemption Field Name";
    public static final String TAX_EXEMPTION_CODE_FOR_DISCOUNT_VALUE = "Tax Exemption For Discount";

    private Integer exemptionPercentage;
    private Integer exemptionPercentageCode;
    private Integer commodityId;

    public NGESSureTaxWithExemptionCompositionTask() {
        super();
        descriptions.add(new ParameterDescription(PERCENTAGE_EXEMPTION_FIELD_NAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(TAX_EXEMPTION_CODE_FOR_DISCOUNT_VALUE, false, ParameterDescription.Type.INT));
    }

    public void apply(NewInvoiceContext invoice, Integer userId) throws TaskException {

        UserDTO invoiceToUser = new UserDAS().find(userId);

        //if calcualte tax manually is true then calculate tax by customer's taxLine
        MetaFieldValue<Boolean> isCalculateTaxManually=invoiceToUser.getCustomer().getMetaField(FileConstants.CUSTOMER_CALCULATE_TAX_MANUALLY);
        LOG.debug("isCalculateTaxManually : " +isCalculateTaxManually);
        if(isCalculateTaxManually!=null && isCalculateTaxManually.getValue()!=null && isCalculateTaxManually.getValue()){
            LOG.debug("Calculating tax manually");
            calculateTaxManually(invoice, userId);
            return;
        }

        LOG.debug("Calculating tax by sure tax");
        // Check meta fields passed as parameter
        final String percentageExemptionFieldName = getParameter(PERCENTAGE_EXEMPTION_FIELD_NAME, "");
        try {
            exemptionPercentageCode = getParameter(TAX_EXEMPTION_CODE_FOR_DISCOUNT_VALUE, 0);
        } catch (PluggableTaskException e) {
            throw new TaskException("Plugin configuration issue. Parameter : " + TAX_EXEMPTION_CODE_FOR_DISCOUNT_VALUE + " has issue : %s" + e.getMessage());
        }

        LOG.debug("Percentage Exemption Field Name : %s, Tax Exemption For Discount : %d", percentageExemptionFieldName, exemptionPercentageCode);

        // Get customer specific tax exemption
        Map<String, MetaFieldValue> customerMetaFieldValues = getMetaFieldValues(invoiceToUser, new LinkedList<String>() {{
            add(percentageExemptionFieldName);
            add(FileConstants.COMMODITY);
        }});

        MetaFieldValue<Integer> percentageExemption = customerMetaFieldValues.get(percentageExemptionFieldName);
        exemptionPercentage = (percentageExemption != null) ? percentageExemption.getValue() : null;

        LOG.debug("Percentage Exemption value defined at customer AIT level : %d",  exemptionPercentage);

        // NGES customer always got subscribed to plan having a product(commodity) in it. Find it.
        // While creating invoice, check if that product is in invoice line then apply exemption logic on it.
        MetaFieldValue<String> commodityValue = customerMetaFieldValues.get(FileConstants.COMMODITY);
        String itemName = commodityValue != null ? commodityValue.getValue() : null;
        LOG.debug("Customer subscribed to product : %s", itemName);

        ItemDTO commodityObject = new ItemDAS().findItemByInternalNumber(itemName, getEntityId());
        commodityId = commodityObject != null ? commodityObject.getId() : null;

        LOG.debug("Customer subscribed product id : %d", commodityId);

        // Call Sure tax plugin
        super.apply(invoice, userId);
    }

    protected List<LineItem> buildLineItem(Integer itemId, InvoiceLineDTO invoiceLine,
                                           String uniqueTrackingCode, PlanDTO plan, String lineNr)
            throws TaskException {
        List<LineItem> newList = super.buildLineItem(invoiceLine.getItem().getId(), invoiceLine, uniqueTrackingCode, null, lineNr);
        List<LineItem> newListWithExemption = null;
        // Check if item is commodity. If it is then check for tax exemption logic.
        if (itemId != null && commodityId != null && itemId.equals(commodityId)) {
            LOG.debug("Invoice line has commodity product : %d", commodityId);

            //Pass new list to store LineItem object created for this invoice line
            if (newList.size() == 1) {
                LineItem commodityTaxLine = newList.get(0);
                if (commodityTaxLine != null) {
                    //Check if tax exemption
                    newListWithExemption = processExemption(commodityTaxLine, invoiceLine);
                }
            }
        }

        //If new request line is added then return that else return usual request line created by sure tax plugin.
        if (newListWithExemption == null || newListWithExemption.size() < 1) {
            return newList;
        } else {
            return newListWithExemption;
        }
    }

    private List<LineItem> processExemption(LineItem requestLine, InvoiceLineDTO invoiceLine) {
        List<LineItem> newList = new LinkedList<LineItem>();
        if (exemptionPercentage != null && exemptionPercentage < 100 && exemptionPercentageCode > 0) {
            LOG.debug("Percentage discount in tax : %d", exemptionPercentage);
            LOG.debug("Percentage discount code : %d", exemptionPercentageCode);
            float amount = invoiceLine.getAmount().floatValue();
            float taxFreeAmount = amount * exemptionPercentage / 100;
            float taxableAmount = amount - taxFreeAmount;

            // Request line will be divided into two. One for tax free amount and other one for taxable amount

            // Taxable Amount Request line
            LOG.debug("First request line with amount : %f", taxableAmount);
            requestLine.setRevenue(taxableAmount);
            newList.add(requestLine);

            // Non taxable line. Set tax exemption code for percentage exemption
            LineItem newLine = LineItem.getCopy(requestLine);
            LOG.debug("Second request line with amount : %f", taxFreeAmount);
            newLine.setRevenue(taxFreeAmount);
            addExemptionCode(newLine, "" + exemptionPercentageCode);
            newList.add(newLine);
        }
        return newList;
    }

    private static void addExemptionCode(LineItem requestLine, final String exemptionCode) {
        if (exemptionCode != null && !exemptionCode.isEmpty()) {
            if (requestLine.getTaxExemptionCodeList() != null) {
                requestLine.getTaxExemptionCodeList().add(exemptionCode);
            } else {
                requestLine.setTaxExemptionCodeList(new LinkedList<String>() {{
                    add(exemptionCode);
                }});
            }
        }
    }

    /* Calculating tax according to the customer tax lines*/
    private void calculateTaxManually(NewInvoiceContext invoice,Integer userId){
        UserDTO userDTO=new UserDAS().findByUserId(userId, invoice.getEntityId());
        invoice.getResultLines().addAll(createTaxLine(invoice, userDTO));
    }


    private List<InvoiceLineDTO> createTaxLine(NewInvoiceContext invoiceContext, UserDTO user){
        List<InvoiceLineDTO> lineDTOList=new ArrayList<>();

        MetaFieldValue<String> metaFieldValue=user.getCustomer().getMetaField(FileConstants.CUSTOMER_TAX_METAFIELD);
        LOG.debug("customer tax-rate : "+metaFieldValue);
        if(metaFieldValue==null || metaFieldValue.getValue()==null){
            return lineDTOList;
        }

        BigDecimal amount=BigDecimal.ZERO;

        for(InvoiceLineDTO invoiceLineDTO:invoiceContext.getResultLines()){
            amount=amount.add(invoiceLineDTO.getAmount());
        }

        LOG.debug("Total Amount : "+amount);

        Map<String, BigDecimal> taxRates=getCustomerTaxRates(metaFieldValue.getValue());
        LOG.debug("Tax Rates : "+taxRates);

        for(String tax:taxRates.keySet()){
            InvoiceLineTypeDTO lineType = new InvoiceLineTypeDTO(
                    Constants.INVOICE_LINE_TYPE_TAX);
            InvoiceLineDTO invoiceLineDTO=new InvoiceLineDTO();
            BigDecimal percentageRate=taxRates.get(tax);
            BigDecimal taxAmount=amount.multiply(percentageRate).divide(new BigDecimal(100));
            invoiceLineDTO.setAmount(taxAmount);
            invoiceLineDTO.setDescription(tax);
            invoiceLineDTO.setInvoiceLineType(lineType);
            invoiceLineDTO.setIsPercentage(1);
            invoiceLineDTO.setQuantity(1);
            invoiceLineDTO.setPrice(percentageRate);
            invoiceLineDTO.setInvoice(invoiceContext);
            lineDTOList.add(invoiceLineDTO);
        }

        return lineDTOList;
    }


    /*This method parse the customer tax rate to usable format. The current format of the customer taxline is (taxDescription1=percentageTax1;taxDescription2=percentageTax2; ) */
    private Map<String, BigDecimal> getCustomerTaxRates(String taxes){
        Map<String, BigDecimal> result=new HashMap<>();

        Map<String, String> taxLineMap= MetaFieldBindHelper.convertJSONStringToMap(taxes);
        for(String taxLine:taxLineMap.keySet()){
           String taxPersentage= taxLineMap.get(taxLine);
            if(StringUtils.isNotBlank(taxPersentage)){
                result.put(taxLine, new BigDecimal(taxPersentage));
            }else{
                result.put(taxLine, BigDecimal.ZERO);
            }
        }
        return result;
    }

    @Override
    protected Map<String, MetaFieldValue> getMetaFieldValues(UserDTO user, List<String> metaFieldNames) {
        Map<String, MetaFieldValue> metaFieldValues = new HashMap<String, MetaFieldValue>();
        Set<CustomerAccountInfoTypeMetaField> list = user.getCustomer().getCustomerAccountInfoTypeMetaFields();
        for (CustomerAccountInfoTypeMetaField customerAccountInfoTypeMetaField : list) {
            String aitName=customerAccountInfoTypeMetaField.getAccountInfoType().getName();
            //we have two aits having zipcode metafield. So for calculate tax use  "Customer Information" or "Business Information" ait's zipcode value.
            List<String> aits=Arrays.asList(FileConstants.CUSTOMER_INFORMATION_AIT, FileConstants.BUSINESS_INFORMATION_AIT, FileConstants.ACCOUNT_INFORMATION_AIT);
            if(aits.contains(aitName)){
                MetaFieldValue metaField = customerAccountInfoTypeMetaField.getMetaFieldValue();
                if (metaFieldNames.contains(metaField.getField().getName())) {
                    if (metaFieldNames.remove(metaField.getField().getName()))
                        metaFieldValues.put(metaField.getField().getName(), metaField);
                }
            }
        }
        return metaFieldValues;
    }
}
