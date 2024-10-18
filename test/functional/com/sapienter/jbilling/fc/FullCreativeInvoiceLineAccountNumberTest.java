package com.sapienter.jbilling.fc;

import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangePlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.server.util.search.SearchCriteria;

@Test(groups = {"fullcreative"}, testName = "FullCreativeInvoiceLineAccountNumberTest")
public class FullCreativeInvoiceLineAccountNumberTest {

	private static final Logger logger = LoggerFactory.getLogger(FullCreativeInvoiceLineAccountNumberTest.class);
    private  JbillingAPI api;
    private int userId;
    final private static int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    final private static int orderPeriod = 2;
    private Integer inboundUsageId ;
    private Integer chatUsageId ;
    private Integer activeResponseUsageId ;
    private Integer planId ;
    private int product8XXTollFreeId;
    private int invoiceCompositionPlugInId;
    private Integer basicItemManagerPlugInId;

    @BeforeClass
    protected void setUp() throws Exception {
		api = JbillingAPIFactory.getAPI();
		inboundUsageId = FullCreativeTestConstants.INBOUND_USAGE_PRODUCT_ID;
		chatUsageId = FullCreativeTestConstants.CHAT_USAGE_PRODUCT_ID;
		activeResponseUsageId = FullCreativeTestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID;
		planId = FullCreativeTestConstants.AF_BEST_VALUE_PLAN_ID;
		product8XXTollFreeId = FullCreativeTestConstants.TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID;
		invoiceCompositionPlugInId= FullCreativeTestConstants.INVOICE_COMPOSITION_PLUGIN_ID;
		PluggableTaskTypeWS type = api.getPluginTypeWSByClassName(FullCreativeTestConstants.ORDER_LINE_BASED_COMPOSITION_TASK_NAME);
		try {
			PluggableTaskWS plugin = api.getPluginWS(invoiceCompositionPlugInId);
			logger.debug("Plugin {}", plugin);
			plugin.setTypeId(type.getId());
			api.updatePlugin(plugin);
	
			basicItemManagerPlugInId = FullCreativeTestConstants.BASIC_ITEM_MANAGER_PLUGIN_ID;
			FullCreativeUtil.updatePlugin(basicItemManagerPlugInId, FullCreativeTestConstants.TELCO_USAGE_MANAGER_TASK_NAME, api);
		} catch(Exception ex) {
			PluggableTaskWS plugIn = new PluggableTaskWS();
			plugIn.setNotes("Test -Plugin");
			plugIn.setOwningEntityId(api.getCallerCompanyId());
			plugIn.setProcessingOrder(123);
			plugIn.setTypeId(type.getId());
			invoiceCompositionPlugInId = api.createPlugin(plugIn);
		}
    }

    @Test
    public void testacInvoiceLineAccountNumber() {
		try {
			this.api = JbillingAPIFactory.getAPI();
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.YEAR, 2014);
			calendar.set(Calendar.MONTH, 1);
			calendar.set(Calendar.DAY_OF_MONTH, 1);
	
			UserWS user = FullCreativeUtil.createUser(calendar.getTime());
			logger.debug("User id : {}", user.getId());
	
			userId=user.getId();
			logger.debug("Creating order...");
			OrderWS order1 = new OrderWS();
			order1.setUserId(userId);
			order1.setPeriod(orderPeriod);
			order1.setBillingTypeId(Integer.valueOf(1));
			order1.setActiveSince(new Date());
			order1.setCurrencyId(new Integer(1));
	
			PlanWS planWS = api.getPlanWS(planId); // AF Best Value Plan
			assertNotNull("Plan Not Found for id 603.", planWS);
	
			OrderLineWS subscriptionLine = new OrderLineWS();
			subscriptionLine.setItemId(Integer.valueOf(planWS.getItemId()));
			subscriptionLine.setAmount("225.00");
			subscriptionLine.setPrice("225.00");
			subscriptionLine.setTypeId(Integer.valueOf(1));
			subscriptionLine.setDescription("AF Best Value Plan");
			subscriptionLine.setQuantity("1");
	
			order1.setOrderLines(new OrderLineWS[]{subscriptionLine});
	
			BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
			SearchCriteria criteria = new SearchCriteria();
			criteria.setMax(1);
			criteria.setOffset(1);
			criteria.setSort("id");
			criteria.setTotal(-1);
			criteria.setFilters(new BasicFilter[]{basicFilter});
	
			AssetSearchResult result = api.findProductAssetsByStatus(product8XXTollFreeId, criteria);
			assertNotNull("No available asset found", result);
			AssetWS[] availableAssets = result.getObjects();
			assertTrue("No assets found for product 603.", null != availableAssets && availableAssets.length != 0);
	
	
			OrderChangeWS orderChanges[] = OrderChangeBL.buildFromOrder(order1, ORDER_CHANGE_STATUS_APPLY_ID);
			for (OrderChangeWS ws : orderChanges) {
				if (ws.getItemId().intValue() == planWS.getItemId().intValue()) {
					OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
					orderChangePlanItem.setItemId(product8XXTollFreeId);
					orderChangePlanItem.setId(0);
					orderChangePlanItem.setOptlock(0);
					orderChangePlanItem.setBundledQuantity(1);
					orderChangePlanItem.setDescription("DID-8XX");
					orderChangePlanItem.setMetaFields(new MetaFieldValueWS[0]);
					orderChangePlanItem.setAssetIds(new int[]{availableAssets[0].getId()});
					ws.setOrderChangePlanItems(new OrderChangePlanItemWS[]{orderChangePlanItem});
					break;
				}
			}
	
			Integer orderId	= api.createOrder(order1, orderChanges);
			assertNotNull("Order 1 not created", orderId);
			logger.debug("Order Created with asset : {}", orderId);
	
			OrderWS order2 = new OrderWS();
			order2.setUserId(userId);
			order2.setPeriod(1);
			order2.setBillingTypeId(Integer.valueOf(1));
			order2.setActiveSince(new Date());
			order2.setCurrencyId(new Integer(1));
	
			MetaFieldWS[] meta = api.getMetaFieldsForEntity(EntityType.ORDER_LINE.name());
			MetaFieldWS inbound = null;
			MetaFieldWS chat = null;
			MetaFieldWS activeresponse = null;
			for (MetaFieldWS metaFieldWS: meta){
				if (metaFieldWS.getName().equals(Constants.PHONE_META_FIELD)){
					inbound = metaFieldWS;
				} else if (metaFieldWS.getName().equals(Constants.CHAT_IDENTIFIER)){
					chat = metaFieldWS;
				} else if (metaFieldWS.getName().equals(Constants.ACTIVE_RESPONSE_IDENTIFIER)){
					activeresponse=metaFieldWS;
				}
			}
	
			OrderLineWS line1 = new OrderLineWS();
			line1.setItemId(inboundUsageId);
			line1.setAmount("0.95");
			line1.setPrice("0.95");
			line1.setTypeId(Integer.valueOf(1));
			line1.setDescription("Inbound");
			line1.setQuantity("1");
	
			MetaFieldValueWS inboundCallsIdentifier1 = new MetaFieldValueWS();
			inboundCallsIdentifier1.setFieldName(inbound.getName());
			inboundCallsIdentifier1.setStringValue("8888991646");
			inboundCallsIdentifier1.getMetaField().setDataType(inbound.getDataType());
			line1.setMetaFields(new MetaFieldValueWS[] { inboundCallsIdentifier1 });
	
			OrderLineWS line2 = new OrderLineWS();
			line2.setItemId(inboundUsageId);
			line2.setAmount("0.95");
			line2.setPrice("0.95");
			line2.setTypeId(Integer.valueOf(1));
			line2.setDescription("Inbound");
			line2.setQuantity("1");
	
			MetaFieldValueWS inboundCallsIdentifier2 = new MetaFieldValueWS();
			inboundCallsIdentifier2.setFieldName(inbound.getName());
			inboundCallsIdentifier2.setStringValue("8888991646");
			inboundCallsIdentifier2.getMetaField().setDataType(inbound.getDataType());
			line2.setMetaFields(new MetaFieldValueWS[] { inboundCallsIdentifier2 });
	
			OrderLineWS line3 = new OrderLineWS();
			line3.setItemId(inboundUsageId);
			line3.setAmount("0.95");
			line3.setPrice("0.95");
			line3.setTypeId(Integer.valueOf(1));
			line3.setDescription("Inbound");
			line3.setQuantity("1");
	
			MetaFieldValueWS inboundCallsIdentifier3 = new MetaFieldValueWS();
			inboundCallsIdentifier3.setFieldName(inbound.getName());
			inboundCallsIdentifier3.setStringValue("8888980699");
			inboundCallsIdentifier3.getMetaField().setDataType(inbound.getDataType());
			line3.setMetaFields(new MetaFieldValueWS[] { inboundCallsIdentifier3 });
	
			OrderLineWS line4 = new OrderLineWS();
			line4.setItemId(inboundUsageId);
			line4.setAmount("0.95");
			line4.setPrice("0.95");
			line4.setTypeId(Integer.valueOf(1));
			line4.setDescription("Inbound");
			line4.setQuantity("1");                     
	
			MetaFieldValueWS inboundCallsIdentifier4 = new MetaFieldValueWS();
			inboundCallsIdentifier4.setFieldName(inbound.getName());
			inboundCallsIdentifier4.setStringValue("8888782737");
			inboundCallsIdentifier4.getMetaField().setDataType(inbound.getDataType());
			line4.setMetaFields(new MetaFieldValueWS[] { inboundCallsIdentifier4 });
	
			OrderLineWS line5 = new OrderLineWS();
			line5.setItemId(chatUsageId);
			line5.setAmount("0.95");
			line5.setPrice("0.95");
			line5.setTypeId(Integer.valueOf(1));
			line5.setDescription("Chat");
			line5.setQuantity("1");
	
			MetaFieldValueWS chatIdentifier1 = new MetaFieldValueWS();
			chatIdentifier1.setFieldName(chat.getName());
			chatIdentifier1.setStringValue("8774009503");
			chatIdentifier1.getMetaField().setDataType(chat.getDataType());
			line5.setMetaFields(new MetaFieldValueWS[] { chatIdentifier1 });
	
			OrderLineWS line6 = new OrderLineWS();
			line6.setItemId(chatUsageId);
			line6.setAmount("0.95");
			line6.setPrice("0.95");
			line6.setTypeId(Integer.valueOf(1));
			line6.setDescription("Chat");
			line6.setQuantity("1");
	
			MetaFieldValueWS chatIdentifier2 = new MetaFieldValueWS();
			chatIdentifier2.setFieldName(chat.getName());
			chatIdentifier2.setStringValue("8774009503");
			chatIdentifier2.getMetaField().setDataType(chat.getDataType());
			line6.setMetaFields(new MetaFieldValueWS[] { chatIdentifier2 });
	
			OrderLineWS line7 = new OrderLineWS();
			line7.setItemId(chatUsageId);
			line7.setAmount("0.95");
			line7.setPrice("0.95");
			line7.setTypeId(Integer.valueOf(1));
			line7.setDescription("Chat");
			line7.setQuantity("1");
	
			MetaFieldValueWS chatIdentifier3 = new MetaFieldValueWS();
			chatIdentifier3.setFieldName(chat.getName());
			chatIdentifier3.setStringValue("8774009503");
			chatIdentifier3.getMetaField().setDataType(chat.getDataType());
			line7.setMetaFields(new MetaFieldValueWS[] { chatIdentifier3 });
	
			OrderLineWS line8 = new OrderLineWS();
			line8.setItemId(activeResponseUsageId);
			line8.setAmount("0.95");
			line8.setPrice("0.95");
			line8.setTypeId(Integer.valueOf(1));
			line8.setDescription("Active Response");
			line8.setQuantity("1");
	
			MetaFieldValueWS activerrsponseidentifier1 = new MetaFieldValueWS();
			activerrsponseidentifier1.setFieldName(activeresponse.getName());
			activerrsponseidentifier1.setStringValue("1210000171");
			activerrsponseidentifier1.getMetaField().setDataType(activeresponse.getDataType());
			line8.setMetaFields(new MetaFieldValueWS[] { activerrsponseidentifier1 });
	
			OrderLineWS line9 = new OrderLineWS();
			line9.setItemId(activeResponseUsageId);
			line9.setAmount("0.95");
			line9.setPrice("0.95");
			line9.setTypeId(Integer.valueOf(1));
			line9.setDescription("Active Response");
			line9.setQuantity("1");
	
			MetaFieldValueWS activerrsponseidentifier2 = new MetaFieldValueWS();
			activerrsponseidentifier2.setFieldName(activeresponse.getName());
			activerrsponseidentifier2.setStringValue("1210000200");
			activerrsponseidentifier2.getMetaField().setDataType(activeresponse.getDataType());
			line9.setMetaFields(new MetaFieldValueWS[] { activerrsponseidentifier2 });
	
			OrderLineWS line10 = new OrderLineWS();
			line10.setItemId(activeResponseUsageId);
			line10.setAmount("0.95");
			line10.setPrice("0.95");
			line10.setTypeId(Integer.valueOf(1));
			line10.setDescription("Active Response");
			line10.setQuantity("1");
	
			MetaFieldValueWS activerrsponseidentifier3 = new MetaFieldValueWS();
			activerrsponseidentifier3.setFieldName(activeresponse.getName());
			activerrsponseidentifier3.setStringValue("1210000200");
			activerrsponseidentifier3.getMetaField().setDataType(activeresponse.getDataType());
			line10.setMetaFields(new MetaFieldValueWS[] { activerrsponseidentifier3 });
	
			order2.setOrderLines(new OrderLineWS[] {line1, line2, line3, line4, line5, line6, line7, line8, line9, line10});
	
			Integer orderId2 = api.createOrder(order2, OrderChangeBL.buildFromOrder(order2, ORDER_CHANGE_STATUS_APPLY_ID));
			assertNotNull("Order 2 is not created", orderId2);
			logger.debug("Order Created With Id: {}", orderId2);
	
			int invoiceId = api.createInvoiceFromOrder(orderId2, null);
			logger.debug("Invoice Created With Id: {}",invoiceId);
			assertNotNull("Invoice Id should not be null", invoiceId);
	
			InvoiceWS invoiceWS=api.getInvoiceWS(invoiceId);
			assertNotNull("Invoice Not Found", invoiceWS);
			InvoiceLineDTO invoiceLines[] = invoiceWS.getInvoiceLines();
			assertNotNull("Invoice Lines array is null", invoiceLines);
			assertEquals("Number of lines on invoice Should be ", 10, invoiceLines.length);
	
			Integer invoiceLineIdentifierCount = null;
			Integer orderLineIdentifierCount = null;
			HashMap<String, Integer> invoiceLineIdentifierCountMap = new HashMap<String, Integer>();
			HashMap<String, Integer> orderLineIdentifierCountMap = new HashMap<String, Integer>();    
			OrderWS orderWs=api.getOrder(orderId2);
			OrderLineWS orderLineWS[]= orderWs.getOrderLines();
	
			for (OrderLineWS lineWS:orderLineWS) {
	
				for (MetaFieldValueWS identifier : lineWS.getMetaFields()) {
		
					assertNotNull("Meta field for Call Identifier exists " +
						"on order line but has not been populated!", identifier);
		
					assertNotNull("Meta field for Call Identifier exists " +
						"on order line but has not been populated!", identifier.getValue());
		
					String identifierValue = String.valueOf(identifier.getValue());
		
					if (Constants.PHONE_META_FIELD.equals(identifier.getFieldName()) || 
						Constants.CHAT_IDENTIFIER.equals(identifier.getFieldName()) || 
						Constants.ACTIVE_RESPONSE_IDENTIFIER.equals(identifier.getFieldName())) {
			
						orderLineIdentifierCount = orderLineIdentifierCountMap.get(identifierValue);
						if (null == orderLineIdentifierCount) {
							orderLineIdentifierCount = 0;
						}
					orderLineIdentifierCountMap.put(identifierValue, ++orderLineIdentifierCount);
					}
				}
			}
	
			assertTrue("There are no call identifiers populated on order lines.", !orderLineIdentifierCountMap.isEmpty());
			logger.debug("orderLineIdentifierCountMap: {}", orderLineIdentifierCountMap);
	
			for (InvoiceLineDTO invoiceLine : invoiceLines) {
	
				if (null != invoiceLine.getCallIdentifier()) {
					invoiceLineIdentifierCount = invoiceLineIdentifierCountMap.get(invoiceLine.getCallIdentifier());
					if (invoiceLineIdentifierCount == null) {
					invoiceLineIdentifierCount = 0;
					}
					invoiceLineIdentifierCountMap.put(invoiceLine.getCallIdentifier(), ++invoiceLineIdentifierCount);
				} else if (null != invoiceLine.getCallIdentifier()) {
					invoiceLineIdentifierCount = invoiceLineIdentifierCountMap.get(invoiceLine.getCallIdentifier());
					if (invoiceLineIdentifierCount == null) {
					invoiceLineIdentifierCount = 0;
					}
					invoiceLineIdentifierCountMap.put(invoiceLine.getCallIdentifier(), ++invoiceLineIdentifierCount);
				} else if (null != invoiceLine.getCallIdentifier()) {
					invoiceLineIdentifierCount = invoiceLineIdentifierCountMap.get(invoiceLine.getCallIdentifier());
					if (invoiceLineIdentifierCount == null) {
					invoiceLineIdentifierCount = 0;
					}
					invoiceLineIdentifierCountMap.put(invoiceLine.getCallIdentifier(), ++invoiceLineIdentifierCount);
				} else {
					// if you are coming here, means none of the identifiers are populated on invoice line, hence fail the test
					assertTrue("At least one of the identifiers for inbound, chat or active response has to be set.", 1 != 1);
				}
			}
	
			assertTrue("There are no call identifiers populated on invoice lines.", !invoiceLineIdentifierCountMap.isEmpty());
			logger.debug("invoiceLineIdentifierCountMap: {}", invoiceLineIdentifierCountMap);
	
			List<Entry<String, Integer>> orderLineIdentifierCountList = 
				this.sortMap(new ArrayList<Entry<String, Integer>>(orderLineIdentifierCountMap.entrySet()));
	
			List<Entry<String, Integer>> invoiceLineIdentifierCountList = 
				this.sortMap( new ArrayList<Entry<String, Integer>>(invoiceLineIdentifierCountMap.entrySet()));
	
			Iterator<Entry<String, Integer>> invoiceLineIdentifierIterator = invoiceLineIdentifierCountList.iterator();
			Iterator<Entry<String, Integer>> orderLineIdentifierIterator = orderLineIdentifierCountList.iterator();
	
			logger.debug("orderLineIdentifierCountList: {}", orderLineIdentifierCountList);
			logger.debug("invoiceLineIdentifierCountList: {}", invoiceLineIdentifierCountList);
	
			while (invoiceLineIdentifierIterator.hasNext() && orderLineIdentifierIterator.hasNext()) {
	
			Entry<String, Integer> orderLineIdentifierEntry = orderLineIdentifierIterator.next();
			Entry<String, Integer> invoiceLineIdentifierEntry = invoiceLineIdentifierIterator.next();
	
			assertEquals("Invoice Line Phone Number Should Be euqal to Order Line Phone Number",
				orderLineIdentifierEntry.getKey(), invoiceLineIdentifierEntry.getKey());
	
			assertEquals("Invoice Line Identifier Count Should Be euqal to Order Line Identifier Count",
				orderLineIdentifierEntry.getValue(), invoiceLineIdentifierEntry.getValue());
			}
	
		} catch (Exception ex) {
			logger.error("Execption during the execution of the test testacInvoiceLineAccountNumber", ex);
		}
    }

    @org.testng.annotations.AfterClass
    protected void cleanUp() {
		if(invoiceCompositionPlugInId!=FullCreativeTestConstants.INVOICE_COMPOSITION_PLUGIN_ID) {
			api.deletePlugin(invoiceCompositionPlugInId);
			return ;
		} 
		PluggableTaskWS plugin = api.getPluginWS(invoiceCompositionPlugInId);
		plugin.setTypeId(api.getPluginTypeWSByClassName(FullCreativeTestConstants.ORDER_CHANGE_BASED_COMPOSITION_TASK_NAME).getId());
		api.updatePlugin(plugin);
	
		FullCreativeUtil.updatePlugin(basicItemManagerPlugInId, FullCreativeTestConstants.BASIC_ITEM_MANAGER_TASK_NAME, api);
    }

    private List<Entry<String, Integer>> sortMap(List<Entry<String, Integer>> list){
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2 ) {
			return (Long.valueOf(o1.getKey())).compareTo(Long.valueOf(o2.getKey()));
			}
		});
		return list;
    }
}
