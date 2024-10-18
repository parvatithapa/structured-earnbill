package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customerEnrollment.helper.CustomerEnrollmentFileGenerationHelper;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.*;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.hibernate.ScrollableResults;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class AutoRenewalTask extends AbstractCronTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AutoRenewalTask.class));

    private static final ParameterDescription PARAM_DAYS_BEFORE_NOTIFICATION =
            new ParameterDescription("days_before_notification", true, ParameterDescription.Type.STR);

    // Initializer for pluggable params
    {
        descriptions.add(PARAM_DAYS_BEFORE_NOTIFICATION);
    }

    private CustomerDAS customerDAS = new CustomerDAS();
    private OrderDAS orderDAS = new OrderDAS();
    private ItemDAS itemDAS = new ItemDAS();
    private AccountInformationTypeDAS accountInformationTypeDAS = new AccountInformationTypeDAS();
    private final Integer AUTO_RENEWAL_ORDER_DURATION=1;

    public AutoRenewalTask() {
        setUseTransaction(true);
    }

    @Override
    public String getTaskName() {
        return "Auto Renewal task, entity Id: " + this.getEntityId() + ", task Id:" + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        _init(context);

        ScrollableResults customers = customerDAS.findAllByCompanyId(this.getEntityId());
        try {
            String daysBeforeNotificationParam = parameters.get(PARAM_DAYS_BEFORE_NOTIFICATION.getName());
            Integer daysBeforeNotification = NumberUtils.isNumber(daysBeforeNotificationParam) ? Integer.valueOf(daysBeforeNotificationParam) : 30;

            LocalDate today = DateConvertUtils.asLocalDate(executionDate);

            while (customers.next()) {
                CustomerDTO customer = (CustomerDTO) customers.get()[0];
                MetaFieldValue<String> customerDropMetaField=customer.getMetaField(FileConstants.TERMINATION_META_FIELD);

                if(customerDropMetaField!=null && customerDropMetaField.getValue()!=null && (customerDropMetaField.getValue().equals(FileConstants.DROPPED)|| customerDropMetaField.getValue().equals(FileConstants.TERMINATION_PROCESSING))){
                    continue;
                }
                UserDTO user = customer.getBaseUser();
                List<OrderDTO> orders = orderDAS.findRecurringOrders(user.getId());

                if (!orders.isEmpty()) {
                    //finding latest subscription order
                    OrderDTO mainSubscriptionOrder = orders.get(0);

                    if (mainSubscriptionOrder.getActiveUntil() != null) {
                        LocalDate contractExpiryDate = DateConvertUtils.asLocalDate(mainSubscriptionOrder.getActiveUntil());

                        //Add comment for this line. We are checking contract should be today or in past
                        if (!contractExpiryDate.isAfter(today)) {
                            // create a new subscription order which startDate=oldSubscriptionOrder.untilDate and Active Until date= Today + 1 Month
                            Date newSubscribtionEndDate = TimezoneHelper.companyCurrentDatePlusTemporalUnit(getEntityId(), AUTO_RENEWAL_ORDER_DURATION, ChronoUnit.MONTHS);

                            //If customer termination date is the past then should not renew customer subscription else subscribtion end date will be customerTerminationDate or newSubscribtionEndDate which will small
                            MetaFieldValue customerTerminationDateMetaField=user.getCustomer().getMetaField(FileConstants.CUSTOMER_TERMINATION_DATE_METAFIELD);
                            if(customerTerminationDateMetaField!=null && customerTerminationDateMetaField.getValue()!=null){
                                Date customerTerminationDate=(Date) customerTerminationDateMetaField.getValue();
                                LOG.debug("customer Termination Date : "+customerTerminationDate);
                                if(executionDate.after(customerTerminationDate)){
                                    continue;
                                }else{
                                    newSubscribtionEndDate=newSubscribtionEndDate.after(customerTerminationDate)?customerTerminationDate:newSubscribtionEndDate;
                                }
                            }

                            LOG.debug("new Subscribtion End Date + "+newSubscribtionEndDate);

                            //Creating new one month subscription order for auto renewal
                           createSubscriptionOrder(user, mainSubscriptionOrder.getActiveUntil(),newSubscribtionEndDate, mainSubscriptionOrder.getLines().get(0).getItem());

                            //Set the last enrollment meta field on the customer
                            MetaField customerLastEnrollmentMetaField = MetaFieldBL.getFieldByName(user.getEntity().getId(), new EntityType[]{EntityType.CUSTOMER},
                                    FileConstants.RENEWED_DATE);
                            customer.setMetaField(customerLastEnrollmentMetaField, DateConvertUtils.asUtilDate(today));

                            //updating customer renewal date.
                            MetaField customerRenewalDate = MetaFieldBL.getFieldByName(user.getEntity().getId(), new EntityType[]{EntityType.CUSTOMER},
                                    FileConstants.CUSTOMER_COMPLETION_DATE_METAFIELD);
                            customer.setMetaField(customerRenewalDate, newSubscribtionEndDate);
                            //Send notification
                            this.sendNotification(customer, true, null);

                        }
                        //Verify if the "contract expiry date" - "days before notification" was reached
                        else if (today.isEqual(contractExpiryDate.minusDays(daysBeforeNotification))) {
                            this.sendNotification(customer, false, daysBeforeNotification);
                        }
                        //Verify if exists a param with the customer state and if the "contract expiry date" - "state days before notification(param value)" was reached
                        else {
                            String customerBusinessAitName = customer.getAccountType().getDescription().equals(FileConstants.RESIDENTIAL_ACCOUNT_TYPE) ?
                                    FileConstants.CUSTOMER_INFORMATION_AIT : FileConstants.BUSINESS_INFORMATION_AIT;
                            Integer customerBusinessAitId = accountInformationTypeDAS.getAccountInformationTypeByName(
                                    this.getEntityId(), customer.getAccountType().getId(), customerBusinessAitName).getId();

                            MetaFieldValue stateMetaFieldValue = customer.getCustomerAccountInfoTypeMetaField(FileConstants.STATE, customerBusinessAitId).getMetaFieldValue();
                            if (stateMetaFieldValue != null) {
                                String customerState = CustomerEnrollmentFileGenerationHelper.USState.getAbbreviationForState((String) stateMetaFieldValue.getValue());

                                for (Map.Entry<String, String> parameter : parameters.entrySet()) {
                                    if (parameter.getKey().equals(customerState)) {
                                        Integer stateDaysBeforeNotification = Integer.valueOf(parameter.getValue());
                                        if (today.isEqual(contractExpiryDate.minusDays(stateDaysBeforeNotification))) {
                                            this.sendNotification(customer, false, stateDaysBeforeNotification);
                                        }
                                    }
                                }
                            }
                            else {
                                LOG.debug(String.format("%s meta field value is null. It should be not null.", FileConstants.STATE));
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            LOG.debug(e.getMessage());
        }
        finally {
            customers.close();
        }
    }

    private void sendNotification(CustomerDTO customer, boolean renewalReached, Integer daysBeforeNotification) {
        EventManager.process(new AutoRenewalEvent(this.getEntityId(), customer, renewalReached, daysBeforeNotification));
    }


    private Date getFirstDayOfMonthDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

   private Integer createSubscriptionOrder(UserDTO user, Date startDate, Date endDate, ItemDTO item){
       LOG.debug("Creating new subscription order for auto renewal");

       OrderDTO order = new OrderDTO();

       OrderBillingTypeDTO type = new OrderBillingTypeDTO();
       type.setId(com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID);
       order.setOrderBillingType(type);
       order.setCreateDate(Calendar.getInstance().getTime());
       order.setCurrency(user.getCurrency());
       order.setActiveSince(startDate);
       order.setActiveUntil(endDate);
       order.setBaseUserByUserId(user);


       Integer languageId = user.getLanguageIdField();
       if (item == null) {
           throw new SessionInternalError("Plan does not exist. Please create a plan");
       }

       //finding Monthly subscription period
       OrderPeriodDTO orderPeriodDTO=new OrderPeriodDAS().findOrderPeriod(getEntityId(),1 , 1);
       if(orderPeriodDTO==null){
           throw new SessionInternalError("No monthly period found in compnay "+getEntityId());
       }
       order.setOrderPeriod(orderPeriodDTO);

       OrderLineDTO line = new OrderLineDTO();
       line.setDescription(item.getDescription(languageId));
       line.setItemId(item.getId());
       line.setQuantity(1);
       line.setPrice(item.getPrice(companyCurrentDate(), user.getEntity().getId()).getRate());
       line.setTypeId(com.sapienter.jbilling.server.util.Constants.ORDER_LINE_TYPE_ITEM);
       line.setPurchaseOrder(order);
       order.getLines().add(line);

       OrderBL orderBL = new OrderBL();
       orderBL.set(order);

       // create the db record
      return orderBL.create(user.getEntity().getId(), null, order);
   }
}