package com.sapienter.jbilling.server.report.builder;

import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDTO;
import com.sapienter.jbilling.server.report.util.EnrollmentScope;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.audit.db.EventLogDAS;
import com.sapienter.jbilling.server.util.audit.db.EventLogDTO;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ReportBuilderCustomerPackages class.
 *
 * @author Leandro Bagur
 * @since 09/01/18.
 */
public class ReportBuilderCustomerPackages {

    private Integer entityId;
    private List<Integer> children;
    private String scope;
    private Date startDate;
    private Date endDate;

    private static final String[] PURPOSES = {"Discount", "Migration", "Product Class", "Report Group", "Service Provider"};

    private static final Comparator<Map<String, ?>> ROW_COMPARATOR = (Map<String, ?> o1, Map<String, ?> o2) -> {
        LocalDate spaDate1 = DateConvertUtils.asLocalDate((Date) o1.get("spa_action_date"));
        LocalDate spaDate2 = DateConvertUtils.asLocalDate((Date) o2.get("spa_action_date"));

        int result = spaDate1.compareTo(spaDate2);
        if (result == 0) {
            Integer orderId1 = (Integer) o1.get("order_id");
            Integer orderId2 = (Integer) o2.get("order_id");
            result = orderId1.compareTo(orderId2);
        }
        return result;
    };

    public ReportBuilderCustomerPackages(Integer entityId, List<Integer> children, Map<String, Object> parameters) {
        this.entityId = entityId;
        this.children = children;
        this.scope = (String) parameters.get("scope");
        this.startDate = (Date) parameters.get("start_date");
        this.endDate = (Date) parameters.get("end_date");
    }
    
    public List<OrderDTO> getOrders(LocalDateTime endLocalDate) {
        return new OrderDAS().getEnrollmentOrdersByDate(entityId, children, startDate, DateConvertUtils.asUtilDate(endLocalDate), OrderDAS.OrderDate.CREATION_DATE);
    }

    public List<Map<String, ?>> getData() {
        //The end date should include all day
        LocalDateTime endLocalDate = DateConvertUtils.asLocalDateTime(endDate);
        endLocalDate = endLocalDate.plusDays(1).minusSeconds(1);
        List<OrderDTO> orders = getOrders(endLocalDate);
        List<Map<String, ?>> data = new ArrayList<>();

        for (OrderDTO order : orders) {
            String staffName = getMetafieldValue(order.getMetaFields(), SpaConstants.MF_STAFF_IDENTIFIER);
            String enrollmentType = getMetafieldValue(order.getMetaFields(), SpaConstants.MF_ENROLLMENT_TYPE);

            if (EnrollmentScope.NEW_CUSTOMERS.toString().equals(scope) && SpaConstants.ENROLLMENT_TYPE_NEW_SERVICES.equals(enrollmentType)) {
                continue;
            }

            UserDTO user = order.getUser();
            List<OrderChangeDTO> orderChanges = new OrderChangeDAS().findByOrder(order.getId());

            orderChanges.forEach(
                    orderChange -> {
                        if (isTheCurrentlyApplicable(orderChange, orderChanges)) {
                            ItemDTO item = orderChange.getItem();
                            if (item.isPlan()) {
                                if (orderChange.getStartDate().after(new Date())) {
                                    PlanDTO plan = new PlanDAS().findPlanByItemId(item.getId());
                                    List<PlanItemDTO> planItems = plan.getPlanItems();
                                    planItems.forEach(
                                            planItem -> {
                                                if (!planItem.getItem().isPlan()) {
                                                    data.add(getRowData(orderChange, order, user, staffName, planItem.getItem()));
                                                }
                                            }
                                    );
                                }
                            } else {
                                data.add(getRowData(orderChange, order, user, staffName, item));
                            }
                        }
                    }
            );

        }
        data.sort(ROW_COMPARATOR);
        return data;
    }

    private boolean isTheCurrentlyApplicable(OrderChangeDTO orderChange, List<OrderChangeDTO> orderChanges) {
        for (OrderChangeDTO oc : orderChanges) {
            if (!oc.getId().equals(orderChange.getId()) && oc.getItem().getId() == orderChange.getItem().getId()) {
                LocalDate other = DateConvertUtils.asLocalDate(oc.getStartDate());                
                if (LocalDate.now().equals(other)) {
                    return false;
                }
                LocalDate assessed = DateConvertUtils.asLocalDate(orderChange.getStartDate());
                if (LocalDate.now().isAfter(other) &&
                        LocalDate.now().isAfter(assessed) &&
                        other.isAfter(assessed)) {
                    return false;
                }
                if (LocalDate.now().isBefore(other) &&
                        LocalDate.now().isBefore(assessed) &&
                        other.isBefore(assessed)) {
                    return false;
                }
                if (LocalDate.now().isAfter(other) &&
                        LocalDate.now().isBefore(assessed)) {
                    return false;
                }                
            }            
        }
        return true;
    }

    /**
     * Get a specific metafield value.
     *
     * @param metafields    metafields list
     * @param metafieldName metafield to search
     * @return String
     */
    static private String getMetafieldValue(List<MetaFieldValue> metafields, String metafieldName) {
        return metafields.stream()
                .filter(metafieldValue -> metafieldName.equals(metafieldValue.getField().getName()))
                .map(MetaFieldValue::getValue)
                .map(Object::toString)
                .findFirst()
                .orElse(StringUtils.EMPTY);
    }

    protected Map<String, Object> getRowData(OrderChangeDTO orderChange, OrderDTO order, UserDTO user, String staffName, ItemDTO item) {
        Map<String, Object> row = new HashMap<>();
        row.put("spa_action_date", order.getCreateDate());
        row.put("start_date", order.getActiveSince());
        row.put("agent_name", staffName);
        row.put("order_id", order.getId());
        row.put("order_status", order.getOrderStatus().getDescription(Constants.LANGUAGE_ENGLISH_ID));
        row.put("order_total", orderChange.getPrice());
        row.put("finish_date", order.getActiveUntil() != null ? order.getActiveUntil() : order.getFinishedDate());
        row.put("customer_id", user.getId());
        row.put("oldest_order_date", getOldestOrderDate(user));
        row.put("billing_cycle", order.getOrderPeriod().getDescription(Constants.LANGUAGE_ENGLISH_ID));
        row.put("language", user.getLanguage().getDescription());
        row.put("postal_code", getServiceMetafield(user, SpaConstants.POSTAL_CODE));
        row.put("province", getServiceMetafield(user, SpaConstants.PROVINCE));
        row.put("payment_method", getPaymentMethod(user));
        row.put("product_id", item.getId());
        row.put("product_name", item.getDescription(Constants.LANGUAGE_ENGLISH_ID));
        row.put("service", getService(item));
        row.put("banff_account", getBanffAccountId(user.getId(), orderChange));
        row.put("currency_symbol", Util.formatSymbolMoney(order.getBaseUserByUserId().getCompany().getCurrency().getSymbol(), false));
        return row;
    }

    /**
     * Get service value. It could be any item type that starts with "Discount", "Migration", "Product Class", "Report Group" or "Service Provider".
     *
     * @param item item
     * @return String
     */
    static private String getService(ItemDTO item) {
        String itemType = item.getItemTypes().stream()
                .map(ItemTypeDTO::getDescription)
                .filter(description -> Arrays.stream(PURPOSES).parallel().anyMatch(description::contains))
                .findFirst().orElse("");
        if (!StringUtils.isEmpty(itemType)) {
            itemType = itemType.substring(itemType.indexOf("-") + 1, itemType.length()).trim();
        }
        return itemType;
    }

    /**
     * Get all banff account id related to a specific user as comma separated string
     *
     * @param userId user id
     * @param orderChange orderChange
     * @return String
     */
    static private String getBanffAccountId(Integer userId, OrderChangeDTO orderChange) {
        List<AssetWS> assetWSSet = new AssetBL().getAllAssetsByUserId(userId, orderChange);

        Set<String> banffAccount = assetWSSet.stream()
                .flatMap(assetWS -> Arrays.stream(assetWS.getMetaFields()))
                .filter(metaFieldValueWS -> SpaConstants.DOMAIN_ID.equals(metaFieldValueWS.getFieldName()))
                .map(MetaFieldValueWS::getStringValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return StringUtils.join(banffAccount, ",");
    }

    /**
     * Get the first payment method description found for a specific user
     *
     * @param user user
     * @return String
     */
    static private String getPaymentMethod(UserDTO user) {
        List<PaymentInformationDTO> paymentInfoDTOs = user.getPaymentInstruments();
        if (!CollectionUtils.isEmpty(paymentInfoDTOs)) {
            PaymentMethodDTO paymentMethod = new PaymentMethodDAS().findNow(paymentInfoDTOs.get(0).getPaymentMethodId());
            return paymentMethod.getDescription(Constants.LANGUAGE_ENGLISH_ID);
        }
        return StringUtils.EMPTY;
    }

    /**
     * Get the oldest created order date for a specific user
     *
     * @param user user
     * @return Date
     */
    static private Date getOldestOrderDate(UserDTO user) {
        Optional<Date> optDate = user.getOrders().stream().map(OrderDTO::getActiveSince).min(Date::compareTo);
        return optDate.isPresent() ? optDate.get() : null;
    }

    /**
     * Function to get the value for the parameter metafield of the customer's service address
     *
     * @param user            user
     * @param metafieldToFind metafield to find
     * @return String
     */
    static private String getServiceMetafield(UserDTO user, String metafieldToFind) {
        CustomerAccountInfoTypeMetaField customerAccountInfo = user.getCustomer().getCustomerAccountInfoTypeMetaField(metafieldToFind);
        return customerAccountInfo == null ? null : customerAccountInfo.getMetaFieldValue().getValue().toString();
    }

    public Integer getEntityId() {
        return entityId;
    }

    public List<Integer> getChildren() {
        return children;
    }

    public Date getStartDate() {
        return startDate;
    }
}
