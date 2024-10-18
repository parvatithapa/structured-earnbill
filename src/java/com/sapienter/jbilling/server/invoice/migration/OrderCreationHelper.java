package com.sapienter.jbilling.server.invoice.migration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

public class OrderCreationHelper {


	private static final String CREDIT_REASON_CATEGORY = "Credit Reason Category";
	private static final String CREDIT_REASON_SUB_CATEGORY = "Credit Reason Sub-Category";
	private static final String REASON = "Reason";
	private static final String MIGRATED_ORDER = "Migrated Order";
	private static final String CSC_AGENT_NAME= "CSC Agent Name";

	private static Integer ORDER_CHANGE_STATUS_APPLY_ID;

	public static Integer getOrCreateOrderChangeStatusApply(IWebServicesSessionBean api) {
	    
	    try {
	        OrderChangeStatusWS[] statuses = api.getOrderChangeStatusesForCompany();
	        for (OrderChangeStatusWS status : statuses) {
	            if (status.getApplyToOrder().equals(ApplyToOrder.YES)) {
	                return status.getId();
	            }
	        }
	        //there is no APPLY status in db so create one
	        OrderChangeStatusWS apply = new OrderChangeStatusWS();
	        String status1Name = "APPLY: " + System.currentTimeMillis();
	        OrderChangeStatusWS status1 = new OrderChangeStatusWS();
	        status1.setApplyToOrder(ApplyToOrder.YES);
	        status1.setDeleted(0);
	        status1.setOrder(1);
	        status1.addDescription(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, status1Name));
	         Integer createOrderChangeStatus = api.createOrderChangeStatus(apply);
	         return createOrderChangeStatus;
        } catch (Exception e) {
            // TODO: handle exception
        }
		return null;
	}

	private static Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api, int months){
	    
	    try  {
		OrderPeriodWS[] periods = api.getOrderPeriods();
		for(OrderPeriodWS period : periods){
			if(months == period.getValue().intValue() &&
					PeriodUnitDTO.MONTH == period.getPeriodUnitId().intValue()){
				return period.getId();
			}
		}
		//there is no monthly order period so create one
		OrderPeriodWS monthly = new OrderPeriodWS();
		monthly.setEntityId(api.getCallerCompanyId());
		monthly.setPeriodUnitId(1);//monthly
		monthly.setValue(months);
		monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "DSC:MONTHLY:"+months)));
		 Integer createOrderPeriod = api.createOrderPeriod(monthly);
		 return createOrderPeriod;
	    }
		 catch (Exception e) {
            e.printStackTrace();
        }
		 return null;
	}
	public static OrderWS getOrderForAdjustments( SPCInvoice spcInvoice,Date activeSince, 
			Integer itemId1, IWebServicesSessionBean spcTargetApi, boolean useOpeningBalance) {
		// need an order for it
		//	Integer ONE_MONTHLY_ORDER_PERIOD = getOrCreateMonthlyOrderPeriod(spcTargetApi, 1);
		OrderWS newOrder = new OrderWS();

		BigDecimal amount = useOpeningBalance ? spcInvoice.getOpeningBalance():spcInvoice.getAdjustments(); 

		newOrder.setUserId(spcInvoice.getUserId()); // it does not matter, the user will be created
		newOrder.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		newOrder.setPeriod(1); // one time order
		newOrder.setCurrencyId(11);
		newOrder.setActiveSince(DateUtils.addDays(activeSince,-1));

		// now add some lines
		OrderLineWS lines[] = new OrderLineWS[1];
		OrderLineWS orderLineWS;

		orderLineWS = new OrderLineWS();
		orderLineWS.setPrice(amount);
		orderLineWS.setTypeId(7);
		orderLineWS.setQuantity(Integer.valueOf(1));
		orderLineWS.setAmount(amount);
		orderLineWS.setDescription("Migration Order" );

		orderLineWS.setUseItem(false);
		orderLineWS.setItemId(itemId1);

		List<MetaFieldValueWS> metaFieldList = new ArrayList<>();
		MetaFieldValueWS creditReason = new MetaFieldValueWS();
		creditReason.setFieldName(CREDIT_REASON_CATEGORY);
		creditReason.setValue(MIGRATED_ORDER);
		metaFieldList.add(creditReason);


		MetaFieldValueWS creditReasonSubCat = new MetaFieldValueWS();
		creditReasonSubCat.setFieldName(CREDIT_REASON_SUB_CATEGORY);
		creditReasonSubCat.setValue(MIGRATED_ORDER);
		metaFieldList.add(creditReasonSubCat);

		MetaFieldValueWS reason = new MetaFieldValueWS();
		reason.setFieldName(REASON);
		reason.setValue(MIGRATED_ORDER);
		metaFieldList.add(reason);

		MetaFieldValueWS cscAgentName = new MetaFieldValueWS();
		cscAgentName.setFieldName(CSC_AGENT_NAME);
		cscAgentName.setValue(MIGRATED_ORDER);
		metaFieldList.add(cscAgentName);

		MetaFieldValueWS[] metaFields = metaFieldList.toArray(new MetaFieldValueWS[0]);
		orderLineWS.setMetaFields(metaFields);
		lines[0] = orderLineWS;

		newOrder.setOrderLines(lines);

		return newOrder;
	}

	public static OrderWS getOrderForNigativeNewCharges( SPCInvoice spcInvoice,Date activeSince,
			Integer itemId1, IWebServicesSessionBean spcTargetApi, boolean useOpeningBalance) {
		// need an order for it
		//	Integer ONE_MONTHLY_ORDER_PERIOD = getOrCreateMonthlyOrderPeriod(spcTargetApi, 1);
		OrderWS newOrder = new OrderWS();

		BigDecimal amount = useOpeningBalance ? spcInvoice.getOpeningBalance().add(spcInvoice.getNewCharges()).add(spcInvoice.getPayments()):spcInvoice.getNewCharges(); 

		newOrder.setUserId(spcInvoice.getUserId()); // it does not matter, the user will be created
		newOrder.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		newOrder.setPeriod(1); // one time order
		newOrder.setCurrencyId(11);
		newOrder.setActiveSince(DateUtils.addDays(activeSince,-1));
		System.out.println("Order Active Since Date: {}"+ activeSince);

		// now add some lines
		OrderLineWS lines[] = new OrderLineWS[1];
		OrderLineWS orderLineWS;

		orderLineWS = new OrderLineWS();
		orderLineWS.setPrice(amount);
		orderLineWS.setTypeId(7);
		orderLineWS.setQuantity(Integer.valueOf(1));
		orderLineWS.setAmount(amount);
		orderLineWS.setDescription("Migration Adjustment" );

		orderLineWS.setUseItem(false);
		orderLineWS.setItemId(itemId1);

		List<MetaFieldValueWS> metaFieldList = new ArrayList<>();
		MetaFieldValueWS creditReason = new MetaFieldValueWS();
		creditReason.setFieldName(CREDIT_REASON_CATEGORY);
		creditReason.setValue(MIGRATED_ORDER);
		metaFieldList.add(creditReason);


		MetaFieldValueWS creditReasonSubCat = new MetaFieldValueWS();
		creditReasonSubCat.setFieldName(CREDIT_REASON_SUB_CATEGORY);
		creditReasonSubCat.setValue(MIGRATED_ORDER);
		metaFieldList.add(creditReasonSubCat);

		MetaFieldValueWS reason = new MetaFieldValueWS();
		reason.setFieldName(REASON);
		reason.setValue(MIGRATED_ORDER);
		metaFieldList.add(reason);

		MetaFieldValueWS cscAgentName = new MetaFieldValueWS();
		cscAgentName.setFieldName(CSC_AGENT_NAME);
		cscAgentName.setValue(MIGRATED_ORDER);
		metaFieldList.add(cscAgentName);

		MetaFieldValueWS[] metaFields = metaFieldList.toArray(new MetaFieldValueWS[0]);
		orderLineWS.setMetaFields(metaFields);
		lines[0] = orderLineWS;

		newOrder.setOrderLines(lines);

		return newOrder;
	}
}
