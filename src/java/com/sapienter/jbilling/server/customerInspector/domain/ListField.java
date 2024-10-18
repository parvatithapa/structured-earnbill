package com.sapienter.jbilling.server.customerInspector.domain;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.sapphire.cis.SapphireCISHelper;
import com.sapienter.jbilling.server.spa.cis.DistributelCISHelper;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.*;

@XmlRootElement(name = "list")
@XmlAccessorType(XmlAccessType.NONE)
public class ListField extends AbstractField {

    private static final FormatLogger LOG = new FormatLogger(ListField.class);
    
    @XmlAttribute
    private Type name;

    @XmlAttribute(required = true)
    private Type type;

    @XmlAttribute(required = true)
    private String properties;

    @XmlAttribute
    private Integer limit;

    @XmlAttribute
    private String sort;

    @XmlAttribute
    private Order order;

    @XmlAttribute
    private String labels;

    @XmlAttribute
    private String widths;

    @XmlAttribute
    private String title;

    @XmlAttribute
    private String comment;

    @XmlAttribute
    private String moneyProperties;

    @XmlAttribute
    private String links;

    private final static Integer LIMIT = 10;

    final static String SPLIT_CHARACTER = ",";
    public Type getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public String[] getProperties() {
        return null!=properties ? properties.split(SPLIT_CHARACTER) : null;
    }

    public Integer getLimit() {
        return limit != null ? limit : LIMIT;
    }

    public String getSort() {
        return sort;
    }

    public Order getOrder() {
        return order;
    }

    public String getMoneyProperties() {
        return moneyProperties;
    }
    
    public String getLinks() {
        return links;
    }

    public String[] getLabels() {
        String[] labelsArray = null;
        if(null!=this.labels) {
            labelsArray = this.labels.split(SPLIT_CHARACTER);
            labelsArray = labelsArray.length==this.getProperties().length ? labelsArray : null;
        }
        return labelsArray;
    }

    public String[] getWidths() {
        if(this.widths != null) {
            String[] widthsArray = this.widths.split(SPLIT_CHARACTER);
            return widthsArray.length==this.getProperties().length ? widthsArray : ArrayUtils.EMPTY_STRING_ARRAY;
        }
        return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    public String getTitle() {
        return title;
    }
    
    public String getComment() {
        return comment;
    }

    @Override
    public Object getValue(Integer userId) {
        if (this.type != null && !this.type.toString().trim().isEmpty()) {

            ListField.Order ordering = (null!=this.order && Arrays.asList(ListField.Order.values()).contains(this.order)) ? this.order : Order.ASC;

            Integer limit = (null!=this.limit && this.limit>0) ? this.limit : LIMIT;
            List<Object> entities = new ArrayList<>();
            if (type == Type.ORDER) {
                String sortAttribute = this.getPropertyName(OrderWS.class);
                List<OrderWS> orders = new OrderBL().findOrdersByUserPagedSortedByAttribute(userId, limit, 0, sortAttribute, ordering, this.getApi().getCallerLanguageId());
                this.applyTimezoneToOrders(orders, userId);
                entities.addAll(orders);
                return entities;
            }
            else if (type == Type.INVOICE) {
                String sortAttribute = this.getPropertyName(InvoiceWS.class);
                List<InvoiceWS> invoices = new InvoiceBL().findInvoicesByUserPagedSortedByAttribute(userId, limit, 0, sortAttribute, ordering, this.getApi().getCallerLanguageId());
                this.applyTimezoneToInvoices(invoices, userId);
                entities.addAll(invoices);
                return entities;
            }
            else if (type == Type.PAYMENT) {
                String sortAttribute = this.getPropertyName(PaymentWS.class);
                List<PaymentWS> payments = new PaymentBL().findPaymentsByUserPagedSortedByAttribute(userId, limit, 0, sortAttribute, ordering, this.getApi().getCallerLanguageId());
                this.applyTimezoneToPayments(payments, userId);
                entities.addAll(payments);
                return entities;
            }
            else if (type == Type.DIST_ORDER_LINE) {
                String sortAttribute = this.getPropertyName(OrderWS.class);
                List<OrderWS> orders = new OrderBL().findOrdersByUserPagedSortedByAttribute(userId, limit, 0, sortAttribute, ordering, this.getApi().getCallerLanguageId());                
                try {
                    entities.addAll(DistributelCISHelper.getDistOrderLineList(orders));    
                } catch (Exception e) {
                    LOG.error("Error getting Distributel order line list", e);
                }
                return entities;                
            }
            else if (type == Type.DIST_INVOICE_LINE) {
                String sortAttribute = this.getPropertyName(InvoiceWS.class);
                List<InvoiceWS> invoices = new InvoiceBL().findInvoicesByUserPagedSortedByAttribute(userId, limit , 0, sortAttribute, ordering, this.getApi().getCallerLanguageId());
                try {
                    entities.addAll(DistributelCISHelper.getDistInvoiceLineList(invoices));    
                } catch (Exception e) {
                    LOG.error("Error getting Distributel invoice line list", e);
                }
                return entities;
            }
            else if (type == Type.DIST_PAYMENT) {
                String sortAttribute = this.getPropertyName(PaymentWS.class);
                List<PaymentWS> payments = new PaymentBL().findPaymentsByUserPagedSortedByAttribute(userId, limit, 0, sortAttribute, ordering, this.getApi().getCallerLanguageId());
                try {
                    entities.addAll(DistributelCISHelper.getDistPaymentList(payments));
                } catch (Exception e) {
                    LOG.error("Error getting Distributel payment list", e);
                }
                return entities;
            }
            else if (type == Type.DIST_ASSET) {
                List<AssetWS> assets = new AssetBL().getAllAssetsByUserId(userId);
                try {
                    entities.addAll(DistributelCISHelper.getDistAssetList(assets, userId));
                } catch (Exception e) {
                    LOG.error("Error getting Distributel asset list", e);
                }
                return entities;
            }
            else if (type == Type.SAPP_ORDER_LINE) {
                String sortAttribute = this.getPropertyName(OrderWS.class);
                List<OrderWS> orders = new OrderBL().findOrdersByUserPagedSortedByAttribute(userId, limit, 0, sortAttribute, ordering, this.getApi().getCallerLanguageId());
                try {
                    entities.addAll(SapphireCISHelper.getSappOrderLineList(orders));
                } catch (Exception e) {
                    LOG.error("Error getting Sapphire order line list", e);
                }
                return entities;
            }
            else if (type == Type.SAPP_INVOICE_LINE) {
                String sortAttribute = this.getPropertyName(InvoiceWS.class);
                List<InvoiceWS> invoices = new InvoiceBL().findInvoicesByUserPagedSortedByAttribute(userId, limit , 0, sortAttribute, ordering, this.getApi().getCallerLanguageId());
                try {
                    entities.addAll(SapphireCISHelper.getSappInvoiceLineList(invoices));
                } catch (Exception e) {
                    LOG.error("Error getting Sapphire invoice line list", e);
                }
                return entities;
            }
            else if (type == Type.SAPP_PAYMENT) {
                String sortAttribute = this.getPropertyName(PaymentWS.class);
                List<PaymentWS> payments = new PaymentBL().findPaymentsByUserPagedSortedByAttribute(userId, limit, 0, sortAttribute, ordering, this.getApi().getCallerLanguageId());
                try {
                    entities.addAll(SapphireCISHelper.getSappPaymentList(payments));
                } catch (Exception e) {
                    LOG.error("Error getting Sapphire payment list", e);
                }
                return entities;
            }
            else if (type == Type.SAPP_ASSET) {
                List<AssetWS> assets = new AssetBL().getAllAssetsByUserId(userId);
                try {
                    entities.addAll(SapphireCISHelper.getSappAssetList(assets));
                } catch (Exception e) {
                    LOG.error("Error getting Sapphire asset list", e);
                }
                return entities;
            }
        }
        return null;
    }

    public enum Type {
        ORDER,
        INVOICE,
        PAYMENT,
        DIST_ORDER_LINE,
        DIST_INVOICE_LINE,
        DIST_PAYMENT,
        DIST_ASSET,
        SAPP_ORDER_LINE,
        SAPP_INVOICE_LINE,
        SAPP_PAYMENT,
        SAPP_ASSET
    }

    public enum Order {
        ASC,
        DESC
    }

    private String getPropertyName(Class c) {
        String propertyName = "";
        try {
            if (null!=this.sort && this.sort.trim().length()>0) {
                    propertyName = c.getDeclaredField(String.valueOf(this.sort)).getName();
            }
            else {
                propertyName = (c.equals(OrderWS.class)) ? "createDate" : "createDatetime";
            }
        } catch (Exception e) {
            propertyName = (c.equals(OrderWS.class)) ? "createDate" : "createDatetime";
        }
        return propertyName;
    }

    public boolean isValidProperty(String property, Class c) {
        boolean valid = false;
        if (null!=property && property.trim().length()>0) {
            try {
                valid = null!=c.getDeclaredField(String.valueOf(property)).getName();
            } catch (NoSuchFieldException e) {
                valid = false;
            }
        }
        else {
            valid = false;
        }
        return valid;
    }

    @Override
    public boolean isMoneyProperty(String property) {
        boolean isMoney = false;
        if(null!=this.moneyProperties && null!=property) {
            isMoney = Arrays.asList(this.moneyProperties.split(SPLIT_CHARACTER)).contains(property);
        }
        return isMoney;
    }

    private void applyTimezoneToOrders(List<OrderWS> orders, Integer userId) {
        if (!orders.isEmpty()) {
            String timezone = new UserBL(userId).getEntity().getCompany().getTimezone();
            for (OrderWS order : orders) {
                order.setCreateDate(TimezoneHelper.convertToTimezone(order.getCreateDate(), timezone));
            }
        }
    }

    public void applyTimezoneToPayments(List<PaymentWS> payments, Integer userId) {
        if (!payments.isEmpty()) {
            String timezone = new UserBL(userId).getEntity().getCompany().getTimezone();
            for (PaymentWS payment : payments) {
                payment.setCreateDatetime(TimezoneHelper.convertToTimezone(payment.getCreateDatetime(), timezone));
            }
        }
    }

    public void applyTimezoneToInvoices(List<InvoiceWS> invoices, Integer userId) {
        if (!invoices.isEmpty()) {
            String timezone = new UserBL(userId).getEntity().getCompany().getTimezone();
            for (InvoiceWS invoice : invoices) {
                invoice.setCreateTimeStamp(TimezoneHelper.convertToTimezone(invoice.getCreateTimeStamp(), timezone));
            }
        }
    }

}