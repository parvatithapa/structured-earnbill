package com.sapienter.jbilling.server.sapphire.cis;

import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetAssignmentWS;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDAS;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.spa.operationaldashboard.OperationalDashboardRequestGenerator;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

import org.apache.commons.collections.CollectionUtils;

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

/**
 * Created by pablo on 20/06/17.
 */
public class SapphireCISHelper {
    private static OperationalDashboardRequestGenerator operationalDashboardRequestGenerator = new OperationalDashboardRequestGenerator();

    private static final Comparator<OrderChangeDTO> OrderChangesComparator = (OrderChangeDTO oc1, OrderChangeDTO oc2) -> (oc1.getStartDate().compareTo(oc2.getStartDate())); 

    public static List<SappOrderLine> getSappOrderLineList(List<OrderWS> orders) {
        List<SappOrderLine> sapphireOrders = new ArrayList<>();
        if (orders != null) {
            for (OrderWS order : orders) {
                if (order.getOrderLines().length > 0) {
                    for (OrderLineWS orderLine : order.getOrderLines()) {
                        ItemDTO item = new ItemDAS().find(orderLine.getItemId());
                        SappOrderLine sappOrderLine = new SappOrderLine();
                        sappOrderLine.setId(String.valueOf(order.getId()));
                        sappOrderLine.setDescription(orderLine.getDescription());
                        sappOrderLine.setCategory(item.getItemTypes().iterator().next().getDescription());
                        sappOrderLine.setStatus(order.getStatusStr());
                        List<OrderChangeDTO> orderChanges = new OrderChangeDAS().findByOrderLine(orderLine.getId());
                        if (!orderChanges.isEmpty()) {
                            sappOrderLine.setActiveSince(getMostCurrentStartDate(orderChanges));
                        } else {
                            sappOrderLine.setActiveSince(order.getActiveSince());
                        }
                        sappOrderLine.setActiveUntil(order.getActiveUntil());
                        sappOrderLine.setPeriod(order.getPeriodStr());

                        String currency = Util.formatSymbolMoney(new CurrencyDAS().find(order.getCurrencyId()).getSymbol(), false) +
                                          formatNumber(orderLine.getAmountAsDecimal());
                        sappOrderLine.setAmount(currency);
                        sapphireOrders.add(sappOrderLine);
                    }
                } else {
                    List<OrderChangeDTO> orderChanges = new OrderChangeDAS().findByOrder(order.getId());
                    for (OrderChangeDTO orderChange : orderChanges) {
                        PlanDTO plan = new PlanDAS().findPlanByItemId(orderChange.getItem().getId());

                        if (plan != null) {
                            for (PlanItemDTO planItem : plan.getPlanItems()) {
                                SappOrderLine sappOrderLine = new SappOrderLine();
                                sappOrderLine.setId(String.valueOf(order.getId()));
                                sappOrderLine.setDescription(planItem.getItem().getDescription());
                                sappOrderLine.setCategory(planItem.getItem().getItemTypes().iterator().next().getDescription());
                                sappOrderLine.setStatus(orderChange.getStatus().getDescription());
                                sappOrderLine.setActiveSince(orderChange.getStartDate());
                                sappOrderLine.setActiveUntil(order.getActiveUntil());
                                sappOrderLine.setPeriod(planItem.getBundle().getPeriod().getDescription());
                                String currency = Util.formatSymbolMoney(new CurrencyDAS().find(order.getCurrencyId()).getSymbol(), false) +
                                                  formatNumber(planItem.getPrice(TimezoneHelper.companyCurrentDateByUserId(order.getUserId()))
                                                                       .getRate()
                                                                       .multiply(planItem.getBundle().getQuantity()));
                                sappOrderLine.setAmount(currency);
                                sapphireOrders.add(sappOrderLine);
                            }
                        } else {
                            SappOrderLine sappOrderLine = new SappOrderLine();
                            sappOrderLine.setId(String.valueOf(order.getId()));
                            sappOrderLine.setDescription(orderChange.getDescription());
                            sappOrderLine.setCategory(orderChange.getItem().getItemTypes().iterator().next().getDescription());
                            sappOrderLine.setStatus(orderChange.getStatus().getDescription());
                            sappOrderLine.setActiveSince(order.getActiveSince());
                            sappOrderLine.setActiveUntil(order.getActiveUntil());
                            sappOrderLine.setPeriod(orderChange.getOrder().getOrderPeriod().getDescription());
                            String currency = Util.formatSymbolMoney(new CurrencyDAS().find(order.getCurrencyId()).getSymbol(), false) +
                                              formatNumber(orderChange.getPrice().multiply(orderChange.getQuantity()));
                            sappOrderLine.setAmount(currency);
                            sapphireOrders.add(sappOrderLine);
                        }
                    }
                }
            }
        }
        Collections.sort(sapphireOrders, new SappOrderLineComparator());
        return sapphireOrders;
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
                    if (LocalDate.now().equals(date)) {
                        mostCurrentDate = date;
                        break;
                    } else {
                        if (LocalDate.now().isAfter(date)) {
                            if (mostCurrentDate.isBefore(date)) {
                                mostCurrentDate = date;
                            }
                        } else {
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

    public static List<SappInvoinceLine> getSappInvoiceLineList(List<InvoiceWS> invoices) {
        List<SappInvoinceLine> sapphireInvoiceLines = new ArrayList<>();
        if (invoices != null) {
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd/YYYY");
            for (InvoiceWS invoice : invoices) {
                SappInvoinceLine firstSappInvoinceLine = null;
                BigDecimal total = BigDecimal.ZERO;
                for (InvoiceLineDTO line : invoice.getInvoiceLines()) {
                    SappInvoinceLine sappInvoinceLine = new SappInvoinceLine();
                    if (firstSappInvoinceLine == null) {
                        sappInvoinceLine.setId(invoice.getId().toString());
                        sappInvoinceLine.setCreateDatetime(dateFormat.format(DateConvertUtils.asLocalDate(invoice.getCreateDatetime())));
                        sappInvoinceLine.setStatus(invoice.getStatusDescr());
                        sappInvoinceLine.setBalance(invoice.getBalance());
                        firstSappInvoinceLine =  sappInvoinceLine;
                    }
                    sappInvoinceLine.setDescription(line.getDescription());
                    String currency = Util.formatSymbolMoney(new CurrencyDAS().find(invoice.getCurrencyId()).getSymbol(), false) +
                                      formatNumber(line.getAmountAsDecimal());
                    sappInvoinceLine.setAmount(currency);
                    sapphireInvoiceLines.add(sappInvoinceLine);
                    firstSappInvoinceLine.setTotal(firstSappInvoinceLine.getTotal());
                    total = total.add(new BigDecimal(line.getAmount()));
                }
                if (null != firstSappInvoinceLine) {
                    firstSappInvoinceLine.setTotal(total.toString());
                }
            }
        }
        return sapphireInvoiceLines;
    }

    public static List<SappPayment> getSappPaymentList(List<PaymentWS> payments) {
        List<SappPayment> sapphirePayments = new ArrayList<>();
        if (payments != null) {
            for (PaymentWS paymentWS : payments) {
                SappPayment sappPayment = new SappPayment();
                sappPayment.setId(Integer.valueOf(paymentWS.getId()).toString());
                sappPayment.setDate(paymentWS.getPaymentDate());
                sappPayment.setPaidOrRefund(paymentWS.getIsRefund() == 0 ? "P" : "R");
                String currency = Util.formatSymbolMoney(new CurrencyDAS().find(paymentWS.getCurrencyId()).getSymbol(), false) +
                                  formatNumber(paymentWS.getAmountAsDecimal());
                sappPayment.setAmount(currency);
                if (paymentWS.getPaymentInstruments().size() > 0) {
                    sappPayment.setPaymentMethod(new PaymentMethodTypeDAS().find(paymentWS.getPaymentInstruments().get(0).getPaymentMethodTypeId()).getMethodName());
                } else {
                    sappPayment.setPaymentMethod(paymentWS.getMethod());
                }
                sappPayment.setPaymentResult(new PaymentResultDAS().find(Integer.valueOf(paymentWS.getResultId())).getDescription());
                sapphirePayments.add(sappPayment);
                if (sapphirePayments.size() == 6) {
                    break;
                }
            }
        }
        Collections.sort(sapphirePayments, new SappPaymentComparator());
        return sapphirePayments;
    }

    public static List<SappAsset> getSappAssetList(List<AssetWS> assets) {
        List<SappAsset> sapphireAssets = new ArrayList<>();
        if (assets != null) {
            for (AssetWS assetWS : assets) {
                SappAsset sappAsset = new SappAsset();
                sappAsset.setId(String.valueOf(assetWS.getId()));
                sappAsset.setName(getOrderLineDescriptionFromAsset(assetWS));
                sappAsset.setOrderId(String.valueOf(getOrderIdFromAsset(assetWS)));
                sappAsset.setType(new ItemBL(assetWS.getItemId()).getEntity().findItemTypeWithAssetManagement().getDescription());
                sappAsset.setAssetIdentifier(assetWS.getIdentifier());

                for (AssetAssignmentWS assignmentWS : assetWS.getAssignments()) {
                    if (null == assignmentWS.getEndDatetime()) {
                        sappAsset.setStartDate(assignmentWS.getStartDatetime());
                    }
                }
                sappAsset.setStatus(assetWS.getStatus());
                sapphireAssets.add(sappAsset);
            }
        }

        return sapphireAssets;
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

class SappOrderLineComparator implements Comparator<SappOrderLine>, Serializable {

    @Override
    public int compare(SappOrderLine o1, SappOrderLine o2) {
        int comparison = o1.getStatusToCompare() - o2.getStatusToCompare();
        if (comparison == 0) {
            comparison = o2.getActiveSince().compareTo(o1.getActiveSince());
        }
        return comparison;
    }
}

class SappPaymentComparator implements Comparator<SappPayment>, Serializable {

    @Override
    public int compare(SappPayment p1, SappPayment p2) {
        int comparison = Integer.valueOf(p2.getId()) - Integer.valueOf(p1.getId());
        return comparison;
    }
}
