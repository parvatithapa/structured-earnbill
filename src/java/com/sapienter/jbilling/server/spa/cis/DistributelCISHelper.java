package com.sapienter.jbilling.server.spa.cis;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDAS;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.spa.operationaldashboard.OperationalDashboardMode;
import com.sapienter.jbilling.server.spa.operationaldashboard.OperationalDashboardRequestGenerator;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

/**
 * Created by pablo on 20/06/17.
 */
public class DistributelCISHelper {
    private static OperationalDashboardRequestGenerator operationalDashboardRequestGenerator = new OperationalDashboardRequestGenerator();

    private static final Comparator<OrderChangeDTO> OrderChangesComparator = (OrderChangeDTO oc1, OrderChangeDTO oc2) -> (oc1.getStartDate().compareTo(oc2.getStartDate())); 

    public static List<DistOrderLine> getDistOrderLineList(List<OrderWS> orders) {
        List<DistOrderLine> distributelOrders = new ArrayList<>();
        if (orders != null) {
            for (OrderWS order : orders) {
                if (order.getOrderLines().length > 0) {
                    for (OrderLineWS orderLine : order.getOrderLines()) {
                        ItemDTO item = new ItemDAS().find(orderLine.getItemId());
                        if (!item.isPlan()) {
                            DistOrderLine distOrderLine = new DistOrderLine();
                            distOrderLine.setId(String.valueOf(order.getId()));
                            distOrderLine.setDescription(orderLine.getDescription());
                            distOrderLine.setCategory(item.getItemTypes().iterator().next().getDescription());
                            distOrderLine.setStatus(order.getStatusStr());
                            List<OrderChangeDTO> orderChanges = new OrderChangeDAS().findByOrderLine(orderLine.getId());
                            if (!orderChanges.isEmpty()) {                                
                                distOrderLine.setActiveSince(getMostCurrentStartDate(orderChanges));
                            } else {
                                distOrderLine.setActiveSince(order.getActiveSince());
                            }
                            distOrderLine.setActiveUntil(order.getActiveUntil());
                            distOrderLine.setPeriod(order.getPeriodStr());
                            String amount = Util.formatSymbolMoney(new CurrencyDAS().find(order.getCurrencyId()).getSymbol(), false) +
                                    formatNumber(orderLine.getAmountAsDecimal());
                            distOrderLine.setAmount(amount);
                            distributelOrders.add(distOrderLine);
                        }
                    }
                } else {
                    List<OrderChangeDTO> orderChanges = new OrderChangeDAS().findByOrder(order.getId());
                    for (OrderChangeDTO orderChange : orderChanges) {
                        PlanDTO plan = new PlanDAS().findPlanByItemId(orderChange.getItem().getId());

                        if (plan != null) {
                            for (PlanItemDTO planItem : plan.getPlanItems()) {
                                DistOrderLine distOrderLine = new DistOrderLine();
                                distOrderLine.setId(String.valueOf(order.getId()));
                                distOrderLine.setDescription(planItem.getItem().getDescription());
                                distOrderLine.setCategory(planItem.getItem().getItemTypes().iterator().next().getDescription());
                                distOrderLine.setStatus(orderChange.getStatus().getDescription());
                                distOrderLine.setActiveSince(orderChange.getStartDate());
                                distOrderLine.setActiveUntil(order.getActiveUntil());
                                distOrderLine.setPeriod(planItem.getBundle().getPeriod().getDescription());
                                String amount = Util.formatSymbolMoney(new CurrencyDAS().find(order.getCurrencyId()).getSymbol(), false) +
                                        formatNumber(planItem.getPrice(TimezoneHelper.companyCurrentDateByUserId(order.getUserId()))
                                                             .getRate()
                                                             .multiply(planItem.getBundle().getQuantity()));
                                distOrderLine.setAmount(amount);
                                distributelOrders.add(distOrderLine);
                            }
                        } else {
                            DistOrderLine distOrderLine = new DistOrderLine();
                            distOrderLine.setId(String.valueOf(order.getId()));
                            distOrderLine.setDescription(orderChange.getDescription());
                            distOrderLine.setCategory(orderChange.getItem().getItemTypes().iterator().next().getDescription());
                            distOrderLine.setStatus(orderChange.getStatus().getDescription());
                            distOrderLine.setActiveSince(order.getActiveSince());
                            distOrderLine.setActiveUntil(order.getActiveUntil());
                            distOrderLine.setPeriod(orderChange.getOrder().getOrderPeriod().getDescription());
                            String amount = Util.formatSymbolMoney(new CurrencyDAS().find(order.getCurrencyId()).getSymbol(), false) +
                                    formatNumber(orderChange.getPrice().multiply(orderChange.getQuantity()));
                            distOrderLine.setAmount(amount);
                            distributelOrders.add(distOrderLine);
                        }
                    }
                }
            }
        }
        Collections.sort(distributelOrders, new DistOrderLineComparator());
        return distributelOrders;
    }

    private static Date getMostCurrentStartDate(List<OrderChangeDTO> orderChanges) {
        Collections.sort(orderChanges, OrderChangesComparator);
        if (orderChanges.size() == 1) {
            return orderChanges.get(0).getStartDate();
        } else {            
            LocalDate mostCurrentDate = DateConvertUtils.asLocalDate(orderChanges.get(0).getStartDate());
            for (OrderChangeDTO orderChange: orderChanges) {
                LocalDate date = DateConvertUtils.asLocalDate(orderChange.getStartDate());
                if (!mostCurrentDate.equals(date)) {
                    if (LocalDate.now().equals(date)) { // is today
                        mostCurrentDate = date;
                        break;
                    } else { // is past date
                        if (LocalDate.now().isAfter(date)) {
                            if (mostCurrentDate.isBefore(date)) {
                                mostCurrentDate = date;
                            }
                        } else { //is future date
                            if (mostCurrentDate.isAfter(date)) {
                                mostCurrentDate = date;
                            }
                        }
                    }
                }
            }
            return DateConvertUtils.asUtilDate(mostCurrentDate);
        }        
    }

    public static List<DistInvoinceLine> getDistInvoiceLineList(List<InvoiceWS> invoices) {
        List<DistInvoinceLine> distributelInvoiceLines = new ArrayList<>();
        String invoiceTotal;
        if (invoices != null) {
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd/YYYY");
            for (InvoiceWS invoice : invoices) {
                DistInvoinceLine firstDistInvoinceLine = null;
                BigDecimal total = BigDecimal.ZERO;
                for (InvoiceLineDTO line : invoice.getInvoiceLines()) {
                    DistInvoinceLine distInvoinceLine = new DistInvoinceLine();
                    if (firstDistInvoinceLine == null) {                        
                        distInvoinceLine.setId(invoice.getId().toString());
                        distInvoinceLine.setCreateDatetime(dateFormat.format(DateConvertUtils.asLocalDate(invoice.getCreateDatetime())));
                        distInvoinceLine.setStatus(invoice.getStatusDescr());
                        String balance = Util.formatSymbolMoney(new CurrencyDAS().find(invoice.getCurrencyId()).getSymbol(), false) +
                                formatNumber(invoice.getBalance());
                        distInvoinceLine.setBalance(balance);
                        firstDistInvoinceLine =  distInvoinceLine;
                    }
                    distInvoinceLine.setDescription(line.getDescription());
                    String amount = Util.formatSymbolMoney(new CurrencyDAS().find(invoice.getCurrencyId()).getSymbol(), false) +
                            formatNumber(line.getAmountAsDecimal());
                    distInvoinceLine.setAmount(amount);
                    distributelInvoiceLines.add(distInvoinceLine);
                    invoiceTotal = Util.formatSymbolMoney(new CurrencyDAS().find(invoice.getCurrencyId()).getSymbol(), false) +
                            formatNumber(firstDistInvoinceLine.getTotal());
                    firstDistInvoinceLine.setTotal(invoiceTotal);
                    total = total.add(new BigDecimal(line.getAmount()));
                }
                if (null != firstDistInvoinceLine) {
                    invoiceTotal = Util.formatSymbolMoney(new CurrencyDAS().find(invoice.getCurrencyId()).getSymbol(), false) +
                            formatNumber(total);
                    firstDistInvoinceLine.setTotal(invoiceTotal);
                }
            }
        }
        return distributelInvoiceLines;
    }

    public static List<DistPayment> getDistPaymentList(List<PaymentWS> payments) {
        List<DistPayment> distributelPayments = new ArrayList<>();
        if (payments != null) {
            for (PaymentWS paymentWS : payments) {
                DistPayment distPayment = new DistPayment();
                distPayment.setId(Integer.valueOf(paymentWS.getId()).toString());
                distPayment.setDate(paymentWS.getPaymentDate());
                distPayment.setPaidOrRefund(paymentWS.getIsRefund() == 0 ? "P" : "R");
                String amount = Util.formatSymbolMoney(new CurrencyDAS().find(paymentWS.getCurrencyId()).getSymbol(), false) +
                        formatNumber(paymentWS.getAmountAsDecimal());
                distPayment.setAmount(amount);
                if (paymentWS.getPaymentInstruments().size() > 0) {
                    distPayment.setPaymentMethod(new PaymentMethodTypeDAS().find(paymentWS.getPaymentInstruments().get(0).getPaymentMethodTypeId()).getMethodName());
                } else {
                    distPayment.setPaymentMethod(paymentWS.getMethod());
                }
                distPayment.setPaymentResult(new PaymentResultDAS().find(Integer.valueOf(paymentWS.getResultId())).getDescription());
                distributelPayments.add(distPayment);
                if (distributelPayments.size() == 6) {
                    break;
                }
            }
        }
        Collections.sort(distributelPayments, new DistPaymentComparator());
        return distributelPayments;
    }

    public static List<DistAsset> getDistAssetList(List<AssetWS> assets, Integer userId) {
        List<DistAsset> distributelAssets = new ArrayList<>();
        UserDTO userDTO = new UserDAS().find(userId);
        if (assets != null) {
            for (AssetWS assetWS : assets) {
                DistAsset distAsset = new DistAsset();
                distAsset.setId(String.valueOf(assetWS.getId()));
                distAsset.setName(getOrderLineDescriptionFromAsset(assetWS));
                distAsset.setOrderId(String.valueOf(getOrderIdFromAsset(assetWS)));
                distAsset.setType(new ItemBL(assetWS.getItemId()).getEntity().findItemTypeWithAssetManagement().getDescription());
                MetaFieldValue fieldValue = new ItemDAS().find(assetWS.getItemId()).getMetaField(SpaConstants.HARDWARE_SKU);
                if (fieldValue != null && fieldValue.getValue() != null) {
                    distAsset.setMetaField(String.valueOf(fieldValue.getValue()));
                }else{
                    OrderDTO order = new OrderDAS().find(getOrderIdFromAsset(assetWS));
                    if (null != order){
                        order.getLines().stream().forEach(line -> {
                            if(line.getItem().isPlan()){
                                PlanDTO plan = new PlanDAS().findPlanByItemId(line.getItemId());
                                MetaFieldValue planFieldValue = MetaFieldHelper.getMetaField(plan, SpaConstants.HARDWARE_SKU);
                                if (planFieldValue != null && planFieldValue.getValue() != null) {
                                    distAsset.setMetaField(String.valueOf(planFieldValue.getValue()));
                                }
                            }
                        });
                    }
                }

                ItemBL itemBL = new ItemBL(assetWS.getItemId());
                if (itemBL.getEntity() != null) {
                    String requestURL = operationalDashboardRequestGenerator
                            .getRequestURL(itemBL.getEntity().findItemTypeWithAssetManagement().getDescription(), userDTO, OperationalDashboardMode.SHOW, assetWS.getId());
                    distAsset.setDashboard(requestURL);
                } else {
                    distAsset.setDashboard(null);
                }
                distributelAssets.add(distAsset);
            }
        }

        return distributelAssets;
    }

    private static Integer getOrderIdFromAsset(AssetWS assetWS) {
        OrderChangeDAS orderChangeDAS = new OrderChangeDAS();
        List<OrderChangeDTO> orderChangeDTOList = orderChangeDAS.findByOrderLine(assetWS.getOrderLineId());
        if (CollectionUtils.isNotEmpty(orderChangeDTOList)) {
            return orderChangeDTOList.get(0).getOrder().getId();
        } else {
            OrderChangeDTO orderChangeDTO = orderChangeDAS.findByOrderChangeByAssetIdInPlanItems(assetWS.getId());
            if (orderChangeDTO != null) {
                return orderChangeDTO.getOrder().getId();
            } else {
                Integer orderChangeId = new OrderChangeDAS().findOrderChangeIdsByAssetId(assetWS.getId()).get(0);                
                return new OrderChangeDAS().findNow(orderChangeId).getOrder().getId();                
            }
        }
    }

    private static String getOrderLineDescriptionFromAsset(AssetWS assetWS) {
        OrderLineDAS orderLineDAS = new OrderLineDAS();
        if (assetWS.getOrderLineId() != null) {
            return orderLineDAS.find(assetWS.getOrderLineId()).getDescription();
        }
        OrderChangeDAS orderChangeDAS = new OrderChangeDAS();
        OrderChangeDTO orderChangeDTO = orderChangeDAS.findByOrderChangeByAssetIdInPlanItems(assetWS.getId());
        if (orderChangeDTO != null) {
            return orderChangeDTO.getDescription();
        } else {
            Integer orderChangeId = orderChangeDAS.findOrderChangeIdsByAssetId(assetWS.getId()).get(0);
            return orderChangeDAS.findNow(orderChangeId).getDescription();
        }
    }

    private static Object formatNumber(Object o) {
        String number = "";
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(3);
        nf.setMinimumFractionDigits(2);
        if (o instanceof Number) {
            if (o instanceof Double || o instanceof Float || o instanceof BigDecimal) {
                number = nf.format(o);
            } else {
                number = o.toString();
            }
        } else if (o instanceof String) {
            try {
                number = nf.format(nf.parseObject((String) o));
            } catch (Exception e) {
            }
        }
        return number;
    }
}

class DistOrderLineComparator implements Comparator<DistOrderLine>, Serializable {

    @Override
    public int compare(DistOrderLine o1, DistOrderLine o2) {
        int comparison = o1.getStatusToCompare() - o2.getStatusToCompare();
        if (comparison == 0) {
            comparison = o2.getActiveSince().compareTo(o1.getActiveSince());
        }
        return comparison;
    }
}

class DistPaymentComparator implements Comparator<DistPayment>, Serializable {

    @Override
    public int compare(DistPayment p1, DistPayment p2) {
        int comparison = Integer.valueOf(p2.getId()) - Integer.valueOf(p1.getId());
        return comparison;
    }
}
