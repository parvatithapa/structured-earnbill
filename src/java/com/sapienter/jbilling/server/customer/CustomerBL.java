/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.customer;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.rowset.CachedRowSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.resources.CustomerMetaFieldValueWS;
import com.sapienter.jbilling.server.customer.report.ComplianceReportCreator;
import com.sapienter.jbilling.server.ediTransaction.task.BillingModelModificationTask;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderBillingTypeDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;

/**
 * @author Emil
 */
public final class CustomerBL extends ResultList implements CustomerSQL {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private CustomerDTO customer = null;

    public CustomerBL() {
    }

    public CustomerBL(Integer id) {
        customer = new CustomerDAS().find(id);
    }

    public CustomerBL(CustomerDTO customer) {
        this.customer = customer;
    }

    public CustomerDTO getEntity() {
        return customer;
    }

    /**
     * Searches through parent customers (including this customer) looking for a
     * customer with "invoice if child" set to true. If no parent account is explicitly
     * invoiceable, the top/root parent will be returned.
     *
     * @return invoiceable customer account
     */
    public CustomerDTO getInvoicableParent() {
        CustomerDTO parent = customer;

        while (parent.getInvoiceChild() == null || !parent.getInvoiceChild().equals(new Integer(1))) {
            if (parent.getParent() == null) {
                break;
            }
            parent = parent.getParent();
        }

        return parent;
    }

    public CachedRowSet getList(int entityID, Integer userRole,
            Integer userId)
                    throws SQLException, Exception{

        if(userRole.equals(Constants.TYPE_ROOT)) {
            prepareStatement(CustomerSQL.listRoot);
            cachedResults.setInt(1,entityID);
        } else if(userRole.equals(Constants.TYPE_CLERK)) {
            prepareStatement(CustomerSQL.listClerk);
            cachedResults.setInt(1,entityID);
        } else if(userRole.equals(Constants.TYPE_PARTNER)) {
            prepareStatement(CustomerSQL.listPartner);
            cachedResults.setInt(1, entityID);
            cachedResults.setInt(2, userId.intValue());
        } else {
            throw new Exception("The user list for the type " + userRole +
                    " is not supported");
        }

        execute();
        conn.close();
        return cachedResults;
    }

    // this is the list for the Customer menu option, where only
    // customers/partners are listed. Meant for the clients customer service
    public CachedRowSet getCustomerList(int entityID, Integer userRole,
            Integer userId)
                    throws SQLException, Exception {

        if(userRole.equals(Constants.TYPE_INTERNAL) ||
                userRole.equals(Constants.TYPE_ROOT) ||
                userRole.equals(Constants.TYPE_CLERK)) {
            prepareStatement(CustomerSQL.listCustomers);
            cachedResults.setInt(1,entityID);
        } else if(userRole.equals(Constants.TYPE_PARTNER)) {
            prepareStatement(CustomerSQL.listPartner);
            cachedResults.setInt(1, entityID);
            cachedResults.setInt(2, userId.intValue());
        } else {
            throw new Exception("The user list for the type " + userRole +
                    " is not supported");
        }

        execute();
        conn.close();
        return cachedResults;
    }

    public CachedRowSet getSubAccountsList(Integer userId)
            throws SQLException, Exception {

        // find out the customer id of this user
        UserBL user = new UserBL(userId);

        prepareStatement(CustomerSQL.listSubaccounts);
        cachedResults.setInt(1,user.getEntity().getCustomer().getId());

        execute();
        conn.close();
        return cachedResults;
    }

    /**
     * Returns a list of userIds for the descendants of the customer given
     * @param parent: top parent customer
     * @return
     */
    public List<Integer> getDescendants(CustomerDTO parent){
        List<Integer> descendants = new ArrayList<Integer>();
        if(parent != null){
            for(CustomerDTO customer: parent.getChildren()){
                if(customer.getBaseUser().getDeleted() == 0){
                    //add it as descendant
                    descendants.add(customer.getBaseUser().getId());
                    //call the same function in a recursive way to get all the descendants
                    descendants.addAll(getDescendants(customer));
                }
            }
        }
        return descendants;
    }

    public List<String> getCustomerEmails(Integer userId, Integer entityId) {
        MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
        CustomerDAS customerDas = new CustomerDAS();
        List<String> emails = new ArrayList<String>();

        Integer customerId = customerDas.getCustomerId(userId);
        if(null != customerId){
            List<Integer> emailMetaFieldIds = metaFieldDAS.getByFieldType(
                    entityId, new MetaFieldType[]{MetaFieldType.EMAIL});

            List<Integer> valueIds = metaFieldDAS.getValuesByCustomerAndFields(
                    customerId, emailMetaFieldIds, TimezoneHelper.companyCurrentDate(entityId));

            for(Integer valueId : valueIds){
                MetaFieldValue value = metaFieldDAS.getStringMetaFieldValue(valueId);
                String email = null != value.getValue() ? (String) value.getValue() : null;
                if(null != email && !email.trim().isEmpty()){
                    emails.addAll(ContactBL.getEmailList(email));
                }
            }
        }

        return emails;
    }

    /**
     * Returns the top parent of the hierarchy and all the descendants
     * @param parent
     * @return
     */
    public List<Integer> getUsersOfSameTree(CustomerDTO parent){
        List<Integer> usersInTree = new ArrayList<Integer>();

        CustomerDTO topParent = parent;
        while (topParent.getParent() != null){
            topParent = topParent.getParent();
        }

        if(topParent.getBaseUser().getDeleted() == 0){
            usersInTree.add(topParent.getBaseUser().getId());
        }

        usersInTree.addAll(getDescendants(topParent));

        return  usersInTree;
    }

    public File createRegulatoryComplianceReport(Integer entityId) {

        ComplianceReportCreator complianceReportCreator = new ComplianceReportCreator();
        File regComplianceReport = complianceReportCreator.createComplianceReport(entityId);

        return regComplianceReport;
    }

    public void processEarlyTerminationFee(UserWS user, Date terminationDate){

        OrderDAS orderDAS = new OrderDAS();
        ItemBL itemBL = new ItemBL();

        //get main recurring order for the customer
        List<OrderDTO> orders = orderDAS.findRecurringOrders(user.getId());

        if (orders.isEmpty()) {
            return;
        }

        OrderDTO mainSubscriptionOrder = orders.get(0);

        //check that the termination date is before the active until date.
        if(mainSubscriptionOrder.getActiveUntil().before(terminationDate)){
            return;
        }

        //from the main recurring order get the plan
        PlanDTO plan = BillingModelModificationTask.findPlanInOrderLines(mainSubscriptionOrder.getLines(), itemBL);

        //get the Bill Model metafield from the plan
        String billingModel = BillingModelModificationTask.getBillingModel(plan);

        if(billingModel == null){
            return;
        }

        //Check that the bill model is not Rate Ready
        if(billingModel.equals(FileConstants.BILLING_MODEL_RATE_READY)){
            return;
        }

        //If all is ok:

        //get the early termination fee amount from the plan (“Early Termination Fee Amount” metafield)
        BigDecimal feeAmount = getEarlyTerminationFeeMetaFieldValue(plan);

        if(feeAmount == null){
            return;
        }

        //create new one-time order for the customer
        createOrder(user.getUserId(), FileConstants.EARLY_TERMINATION_INTERNAL_NUMBER, feeAmount);
    }

    private void createOrder(Integer userId, String itemInternalNumber, BigDecimal price) {
        logger.debug("Started creating order.");


        UserDTO user = new UserDAS().findNow(userId);
        // create the first order for customer.
        OrderDTO order = new OrderDTO();
        OrderPeriodDTO period = new OrderPeriodDAS().find(com.sapienter.jbilling.server.util.Constants.ORDER_PERIOD_ONCE);
        order.setOrderPeriod(period);

        OrderBillingTypeDTO type = new OrderBillingTypeDTO();
        type.setId(com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID);
        order.setOrderBillingType(type);
        order.setCreateDate(Calendar.getInstance().getTime());
        order.setCurrency(user.getCurrency());
        order.setActiveSince(TimezoneHelper.companyCurrentDate(user.getEntity().getId()));


        order.setBaseUserByUserId(user);

        ItemDAS itemDAS = new ItemDAS();
        ItemDTO item = itemDAS.findItemByInternalNumber(itemInternalNumber, user.getEntity().getId());

        Integer languageId = user.getLanguageIdField();
        String description = "";
        if (item == null) {
            throw new SessionInternalError("Plan does not exist. Please create a plan");
        }
        description = item.getDescription(languageId);

        OrderLineDTO line = new OrderLineDTO();
        line.setDescription(description);
        line.setItemId(item.getId());
        line.setQuantity(1);
        line.setPrice(price);
        line.setTypeId(com.sapienter.jbilling.server.util.Constants.ORDER_LINE_TYPE_ITEM);
        line.setPurchaseOrder(order);
        order.getLines().add(line);

        OrderBL orderBL = new OrderBL();
        orderBL.set(order);

        // create the db record
        Integer orderId = orderBL.create(user.getEntity().getId(), null, order);

        logger.debug("New Order has been created with id {}", orderId);
    }

    public static BigDecimal getEarlyTerminationFeeMetaFieldValue(PlanDTO plan) {
        MetaFieldValue<BigDecimal> earlyTerminationFeeMF = plan.getMetaField(FileConstants.EARLY_TERMINATION_FEE_AMOUNT_META_FIELD);
        if(earlyTerminationFeeMF != null) {
            return earlyTerminationFeeMF.getValue();
        }
        return null;
    }

    /**
     * Updates given meta fields values on customer
     * @param userId
     * @param customerMetaFieldValues
     */
    public void updateCustomerMetaFields(Integer userId, MetaFieldValueWS[] customerMetaFieldValues) {
        UserDTO user = new UserDAS().findNow(userId);
        Integer entityId = user.getCompany().getId();
        Set<MetaField> mFToUpdate = MetaFieldHelper.validateAndGetMetaFields(EntityType.CUSTOMER,
                customerMetaFieldValues, user.getLanguageIdField(), entityId);
        logger.debug("updating {} on user {}", mFToUpdate, user.getId());
        MetaFieldHelper.fillMetaFieldsFromWS(mFToUpdate, user.getCustomer(), customerMetaFieldValues);
    }
    
    public static CustomerMetaFieldValueWS getCustomerMetaFieldValueWS(Integer userId) {
        UserDTO user = new UserDAS().findNow(userId);
        return new CustomerMetaFieldValueWS(userId, convertCustomerMetafieldsToMap(user.getCustomer()));
    }

    @SuppressWarnings("rawtypes")
    private static Map<String, String> convertCustomerMetafieldsToMap(CustomerDTO customer) {
        List<MetaFieldValue> customerMetaFields = customer.getMetaFields();
        List<MetaFieldValueWS> metaFields = new ArrayList<>();
        for (MetaFieldValue metafield : customerMetaFields) {
            MetaFieldValueWS metaFieldValueWS = MetaFieldBL.getWS(metafield);
            if (null != metaFieldValueWS) {
                metaFields.add(metaFieldValueWS);
            }
        }
        return MetaFieldBL.getMetaFieldsMap(metaFields.toArray(new MetaFieldValueWS[0]));
    }

}