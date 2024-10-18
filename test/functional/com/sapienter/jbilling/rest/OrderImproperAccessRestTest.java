package com.sapienter.jbilling.rest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.fail;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.resources.OrderInfo;
import com.sapienter.jbilling.rest.RestConfig;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderHelper;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;


/**
 * @author amey.pelapkar
 * @since 25th JUN 2021
 *
 */
@Test(groups = {"rest"}, testName = "OrderImproperAccessRestTest")
public class OrderImproperAccessRestTest extends BaseRestImproperAccessTest {

	private static final boolean ENABLED_TEST = true;
	private static final String CONTEXT_STRING = "orders";
	
	private static final Integer ORDER_ID_COMPANY_1_CUSTOMER_2 = Integer.valueOf(15);
	private static final Integer ORDER_ID_COMPANY_1_CUSTOMER_53 = Integer.valueOf(35);
	private static final Integer ORDER_ID_COMPANY_2_CUSTOMER_13 = Integer.valueOf(5);
    
	@Override
	@Test(enabled = ENABLED_TEST)
	public void testCreate() {
		
		// Login as admin : Cross Company, create order for another company  -- company2AdminApi	-- mordor;2
		try {
        	OrderWS orderWS = getOrderWS(company1AdminApi , ORDER_ID_COMPANY_1_CUSTOMER_2);
        	orderWS.setId(0);
        	createOrderWS(company2AdminApi, createOrderInfo(orderWS));
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ORDER_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }		
		
		// Login as customer : create order for another company	-- company1Customer2Api		-- french-speaker;1
		try {
        	OrderWS orderWS = getOrderWS(company2AdminApi , ORDER_ID_COMPANY_2_CUSTOMER_13);
        	orderWS.setId(0);
        	createOrderWS(company1Customer2Api, createOrderInfo(orderWS));
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ORDER_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }		
		
		// Login as customer : create order in same company	-- company1Customer3Api	--	pendunsus1;1
		try {
			OrderWS orderWS = getOrderWS(company1AdminApi , ORDER_ID_COMPANY_1_CUSTOMER_2);
			orderWS.setId(0);
        	createOrderWS(company1Customer3Api, createOrderInfo(orderWS));
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ORDER_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, 2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }
		
		// Login as admin(child company) : create order for parent company	-- parent1Company3AdminApi	--	admin;3
		try {
			OrderWS orderWS = getOrderWS(company1AdminApi , ORDER_ID_COMPANY_1_CUSTOMER_2);
			orderWS.setId(0);
        	createOrderWS(parent1Company3AdminApi, createOrderInfo(orderWS));
	        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ORDER_ID_COMPANY_1_CUSTOMER_2));
		   } catch (RestClientResponseException responseError){
		       	
		       	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
		   }		
	}

	@Override
	@Test(enabled = ENABLED_TEST)
	public void testRead() {
		
		// Login as admin : Cross Company, get order for another company  -- company2AdminApi	-- mordor;2		
		try {
        	getOrderWS(company2AdminApi, ORDER_ID_COMPANY_1_CUSTOMER_2);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ORDER_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }
		
		// Login as customer : get order for another company	-- company1Customer2Api		-- french-speaker;1			
		try {
        	getOrderWS(company1Customer2Api, ORDER_ID_COMPANY_2_CUSTOMER_13);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ORDER_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }
		
		// Login as customer : get order of another customer in same company	-- company1Customer3Api	--	pendunsus1;1		
		try {
        	getOrderWS(company1Customer3Api, ORDER_ID_COMPANY_1_CUSTOMER_2);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ORDER_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, 2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }
		
		// Login as admin(child company) : get order for parent company	-- parent1Company3AdminApi	--	admin;3			
		try {
        	getOrderWS(parent1Company3AdminApi, ORDER_ID_COMPANY_1_CUSTOMER_2);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ORDER_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
	}

	@Override
	@Test(enabled = ENABLED_TEST)
	public void testUpdate() {
		
		// Login as admin : Cross Company, update order for another company  -- company2AdminApi	-- mordor;2
		try {
        	OrderWS orderWS = getOrderWS(company1AdminApi , ORDER_ID_COMPANY_1_CUSTOMER_2);
        	updateOrderWS(company2AdminApi, createOrderInfo(orderWS));        	
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ORDER_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }		
		
		// Login as customer : update order for another company	-- company1Customer2Api		-- french-speaker;1
		try {
        	OrderWS orderWS = getOrderWS(company2AdminApi , ORDER_ID_COMPANY_2_CUSTOMER_13);
        	updateOrderWS(company1Customer2Api, createOrderInfo(orderWS));
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ORDER_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }		
		
		// Login as customer : update order in same company	-- company1Customer3Api	--	pendunsus1;1
		try {
			OrderWS orderWS = getOrderWS(company1AdminApi , ORDER_ID_COMPANY_1_CUSTOMER_2);
			updateOrderWS(company1Customer3Api, createOrderInfo(orderWS));
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ORDER_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, 2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }
		
		// Login as admin(child company) : update order for parent company	-- parent1Company3AdminApi	--	admin;3
		try {
			OrderWS orderWS = getOrderWS(company1AdminApi , ORDER_ID_COMPANY_1_CUSTOMER_2);
			updateOrderWS(parent1Company3AdminApi, createOrderInfo(orderWS));
	        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ORDER_ID_COMPANY_1_CUSTOMER_2));
		   } catch (RestClientResponseException responseError){
		       	
		       	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
		   }		
	}

	@Override
	@Test(enabled = ENABLED_TEST)
	public void testDelete() {
		
		// Login as admin : Cross Company, delete order for another company  -- company2AdminApi	-- mordor;2
		try {
        	OrderWS orderWS = getOrderWS(company1AdminApi , ORDER_ID_COMPANY_1_CUSTOMER_2);
        	deleteOrderWS(company2AdminApi, createOrderInfo(orderWS));        	
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ORDER_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }		
		
		// Login as customer : delete order for another company	-- company1Customer2Api		-- french-speaker;1
		try {
        	OrderWS orderWS = getOrderWS(company2AdminApi , ORDER_ID_COMPANY_2_CUSTOMER_13);
        	deleteOrderWS(company1Customer2Api, createOrderInfo(orderWS));
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ORDER_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }		
		
		// Login as customer : delete order in same company	-- company1Customer3Api	--	pendunsus1;1
		try {
			OrderWS orderWS = getOrderWS(company1AdminApi , ORDER_ID_COMPANY_1_CUSTOMER_2);
			deleteOrderWS(company1Customer3Api, createOrderInfo(orderWS));
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ORDER_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, 2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }
		
		// Login as admin(child company) : delete order for parent company	-- parent1Company3AdminApi	--	admin;3
		try {
			OrderWS orderWS = getOrderWS(company1AdminApi , ORDER_ID_COMPANY_1_CUSTOMER_2);
			deleteOrderWS(parent1Company3AdminApi, createOrderInfo(orderWS));
	        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ORDER_ID_COMPANY_1_CUSTOMER_2));
		   } catch (RestClientResponseException responseError){
		       	
		       	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
		   }		
	}
	
	
	private OrderWS getOrderWS(RestConfig restConfig,
			Integer orderId) {
    	ResponseEntity<OrderWS> response = restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, Integer.toString(orderId)), HttpMethod.GET,
    			getAuthHeaders(restConfig, true, false), null, OrderWS.class);
        return response.getBody();
    }
	
	
	private void createOrderWS(RestConfig restConfig, OrderInfo orderInfo) {
    	restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, null), HttpMethod.POST, getAuthHeaders(restConfig, true, false), orderInfo, Integer.class);
    }
	
	private void updateOrderWS(RestConfig restConfig, OrderInfo orderInfo) {
    	restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, Integer.valueOf(orderInfo.getOrder().getId()).toString()), HttpMethod.PUT, getAuthHeaders(restConfig, true, false), orderInfo, Integer.class);
    }
	
	
	private void deleteOrderWS(RestConfig restConfig, OrderInfo orderInfo) {
    	restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, Integer.valueOf(orderInfo.getOrder().getId()).toString()), HttpMethod.DELETE, getAuthHeaders(restConfig, true, false), orderInfo, Integer.class);
    }
	
	public static OrderChangeWS[] buildFromOrder(OrderWS order, Integer statusId) {
        List<OrderChangeWS> orderChanges = new LinkedList();
        Map<OrderLineWS, OrderChangeWS> lineToChangeMap = new HashMap();
        OrderWS rootOrder = OrderHelper.findRootOrderIfPossible(order);
        for (OrderLineWS line : rootOrder.getOrderLines()) {
            OrderChangeWS change = buildFromLine(line, rootOrder, statusId);
            orderChanges.add(change);
            lineToChangeMap.put(line, change);
        }
        for (OrderWS childOrder : OrderHelper.findAllChildren(rootOrder)) {
            for (OrderLineWS line : childOrder.getOrderLines()) {
                OrderChangeWS change = buildFromLine(line, childOrder, statusId);
                orderChanges.add(change);
                lineToChangeMap.put(line, change);
            }
        }
        for (OrderLineWS line : lineToChangeMap.keySet()) {
            if (line.getParentLine() != null) {
                OrderChangeWS change = lineToChangeMap.get(line);
                if (line.getParentLine().getId() > 0) {
                    change.setParentOrderLineId(line.getParentLine().getId());
                } else {
                    OrderChangeWS parentChange = lineToChangeMap.get(line.getParentLine());
                    change.setParentOrderChange(parentChange);
                }
            }
        }
        return orderChanges.toArray(new OrderChangeWS[orderChanges.size()]);
    }

    public static final OrderChangeWS buildFromLine(OrderLineWS line, OrderWS order, Integer statusId) {
        OrderChangeWS ws = new OrderChangeWS();
        ws.setOptLock(1);
        ws.setOrderChangeTypeId(Constants.ORDER_CHANGE_TYPE_DEFAULT);
        ws.setUserAssignedStatusId(statusId);
        ws.setStartDate(new Date());
        if (line.getOrderId() != null && line.getOrderId() > 0) {
            ws.setOrderId(line.getOrderId());
        } else {
            ws.setOrderWS(order);
        }
        if (line.getId() > 0) {
            ws.setOrderLineId(line.getId());
        } else {
            // new line
            ws.setUseItem(line.getUseItem() ? 1 : 0);
        }
        if (line.getParentLine() != null && line.getParentLine().getId() > 0) {
            ws.setParentOrderLineId(line.getParentLine().getId());
        }
        ws.setDescription(line.getDescription());
        ws.setItemId(line.getItemId());
        ws.setAssetIds(line.getAssetIds());
        ws.setPrice(line.getPriceAsDecimal());
        if (line.getDeleted() == 0) {
            if (line.getId() > 0) {
                ws.setQuantity(BigDecimal.ZERO);
            } else {
                ws.setQuantity(line.getQuantityAsDecimal());
            }
        } else {
            ws.setQuantity(line.getQuantityAsDecimal().negate());
        }

        ws.setRemoval(line.getDeleted());
        if (order != null) {
            ws.setNextBillableDate(order.getNextBillableDay());
        }
        ws.setPercentage(line.isPercentage());
        ws.setMetaFields(MetaFieldHelper.copy(line.getMetaFields(), true));
        return ws;
    }
    
    /**
	 * @param orderWS
	 * @return
	 */
	private OrderInfo createOrderInfo(OrderWS orderWS) {
		OrderChangeWS[] orderChanges = buildFromOrder(orderWS, 3);
		OrderInfo orderInfo = new OrderInfo(orderWS, orderChanges);
		return orderInfo;
	}
}
