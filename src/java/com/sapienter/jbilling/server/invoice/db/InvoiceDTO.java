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
package com.sapienter.jbilling.server.invoice.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

import com.sapienter.jbilling.server.creditnote.db.CreditNoteDTO;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteInvoiceMapDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderProcessDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.process.db.PaperInvoiceBatchDTO;
import com.sapienter.jbilling.server.user.db.InvoiceExportableWrapper;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.csv.Exportable;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;

@SuppressWarnings("serial")
@Entity
@TableGenerator(
        name            = "invoice_GEN",
        table           = "jbilling_seqs",
        pkColumnName    = "name",
        valueColumnName = "next_id",
        pkColumnValue   = "invoice",
        allocationSize  = 1)
@Table(name = "invoice")
public class InvoiceDTO extends CustomizedEntity implements Serializable, Exportable {

    private static final int PROCESS = 1;
    public static final int DO_NOT_PROCESS = 0;

    private int id;
    private BillingProcessDTO billingProcessDTO;
    private UserDTO baseUser;
    private CurrencyDTO currencyDTO;
    private InvoiceDTO invoice;
    private PaperInvoiceBatchDTO paperInvoiceBatch;
    private Date createDatetime;
    private Date dueDate;
    private BigDecimal total;
    private int paymentAttempts;
    private InvoiceStatusDTO invoiceStatus;
    private BigDecimal balance;
    private BigDecimal carriedBalance;
    private int inProcessPayment;
    private Integer isReview;
    private Integer deleted;
    private String customerNotes;
    private String publicNumber;
    private Date lastReminder;
    private Integer overdueStep;
    private Date createTimestamp;
    private Set<InvoiceLineDTO> invoiceLines = new HashSet<>();
    private Set<InvoiceDTO> invoices = new HashSet<>();
    private Set<OrderProcessDTO> orderProcesses = new HashSet<>(0);
    private Collection<PaymentInvoiceMapDTO> paymentMap = new HashSet<>(0);
    private Collection<CreditNoteInvoiceMapDTO> creditNoteMap = new HashSet<>(0);
    private int versionNum;
    private boolean isOrderLineTier = false;

    // for transition to JPA
    private String currencyName;
    private String currencySymbol;
    private CreditNoteDTO creditNoteGenerated;

    private InvoiceExportableWrapper invoiceExportableWrapper;

    public InvoiceDTO() {
    }

    public InvoiceDTO(InvoiceDTO invoice) {
        this.setBalance(invoice.getBalance());
        this.setBaseUser(invoice.getBaseUser());
        this.setBillingProcess(invoice.getBillingProcess());
        this.setCarriedBalance(invoice.getCarriedBalance());
        this.setCreateDatetime(invoice.getCreateDatetime());
        this.setCreateTimestamp(invoice.getCreateTimestamp());
        this.setCurrency(invoice.getCurrency());
        this.setCustomerNotes(invoice.getCustomerNotes());
        this.setDeleted(invoice.getDeleted());
        this.setDueDate(invoice.getDueDate());
        this.setId(invoice.getId());
        this.setInProcessPayment(invoice.getInProcessPayment());
        this.setInvoice(invoice.getInvoice());
        this.setInvoiceLines(invoice.getInvoiceLines());
        this.setInvoices(invoice.getInvoices());
        this.setIsReview(invoice.getIsReview());
        this.setLastReminder(invoice.getLastReminder());
        this.setOrderProcesses(invoice.getOrderProcesses());
        this.setOverdueStep(invoice.getOverdueStep());
        this.setPaperInvoiceBatch(invoice.getPaperInvoiceBatch());
        this.setPaymentAttempts(invoice.getPaymentAttempts());
        this.setPaymentMap(invoice.getPaymentMap());
        this.setCreditNoteMap(invoice.getCreditNoteMap());
        this.setPublicNumber(invoice.getPublicNumber());
        this.setInvoiceStatus(invoice.getInvoiceStatus());
        this.setTotal(invoice.getTotal());
        setInvoiceLines(new HashSet<>(invoice.getInvoiceLines()));
        setInvoices(new HashSet<>(invoice.getInvoices()));
        setOrderProcesses(new HashSet<>(invoice.getOrderProcesses()));
        setPaymentMap(new ArrayList<>(invoice.getPaymentMap()));
        setCreditNoteMap(new ArrayList<>(invoice.getCreditNoteMap()));
        setMetaFields(new ArrayList<>(invoice.getMetaFields()));
    }

    public InvoiceDTO(int id, CurrencyDTO currencyDTO, Date createDatetime,
            Date dueDate, BigDecimal total, int paymentAttempts, InvoiceStatusDTO invoiceStatus,
            BigDecimal carriedBalance, int inProcessPayment, int isReview,
            Integer deleted, Date createTimestamp) {
        this.id = id;
        this.currencyDTO = currencyDTO;
        this.createDatetime = createDatetime;
        this.dueDate = dueDate;
        this.total = total;
        this.paymentAttempts = paymentAttempts;
        this.invoiceStatus = invoiceStatus;
        this.carriedBalance = carriedBalance;
        this.inProcessPayment = inProcessPayment;
        this.isReview = isReview;
        this.deleted = deleted;
        this.createTimestamp = createTimestamp;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "invoice_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_process_id")
    public BillingProcessDTO getBillingProcess() {
        return this.billingProcessDTO;
    }

    public void setBillingProcess(BillingProcessDTO billingProcessDTO) {
        this.billingProcessDTO = billingProcessDTO;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserDTO getBaseUser() {
        return this.baseUser;
    }

    public void setBaseUser(UserDTO baseUser) {
        this.baseUser = baseUser;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = false)
    public CurrencyDTO getCurrency() {
        return this.currencyDTO;
    }

    public void setCurrency(CurrencyDTO currencyDTO) {
        this.currencyDTO = currencyDTO;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegated_invoice_id")
    public InvoiceDTO getInvoice() {
        return this.invoice;
    }

    public void setInvoice(InvoiceDTO invoice) {
        this.invoice = invoice;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_invoice_batch_id")
    public PaperInvoiceBatchDTO getPaperInvoiceBatch() {
        return this.paperInvoiceBatch;
    }

    public void setPaperInvoiceBatch(PaperInvoiceBatchDTO paperInvoiceBatch) {
        this.paperInvoiceBatch = paperInvoiceBatch;
    }

    @Column(name = "create_datetime", nullable = false, length = 29)
    public Date getCreateDatetime() {
        return this.createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    @Column(name = "due_date", nullable = false, length = 13)
    public Date getDueDate() {
        return this.dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * Sum total of the invoice lines for the current period. This amount will not change
     * when the invoice is paid, and will always show the dollar value of this invoice for
     * historical purposes.
     *
     * Since a carried invoice balance is added as an invoice line, this total automatically
     * includes the total for the current month plus the carried balances of old un-paid invoices.
     *
     * @return sum total of the invoice lines
     */
    @Column(name = "total", nullable = false, precision = 17, scale = 17)
    public BigDecimal getTotal() {
        return this.total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @Column(name = "payment_attempts", nullable = false)
    public int getPaymentAttempts() {
        return this.paymentAttempts;
    }

    public void setPaymentAttempts(int paymentAttempts) {
        this.paymentAttempts = paymentAttempts;
    }
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id", nullable = false)
    public InvoiceStatusDTO getInvoiceStatus() {
        return invoiceStatus;
    }

    public void setInvoiceStatus(InvoiceStatusDTO invoiceStatus) {
        this.invoiceStatus = invoiceStatus;
    }

    @OneToOne(fetch = FetchType.LAZY, mappedBy="creationInvoice")
    public CreditNoteDTO getCreditNoteGenerated() {
        return creditNoteGenerated;
    }

    public void setCreditNoteGenerated(CreditNoteDTO creditNoteGenerated) {
        this.creditNoteGenerated = creditNoteGenerated;
    }

    /**
     * Returns 1 if this invoice is to be processed as part of the current billing
     * cycle. If the invoice has already been paid or it's balance was carried over
     * to another invoice this method will return 0, and should not be processed.
     *
     * @return returns 1 if this invoice is to be processed, 0 if not
     */
    @Transient
    public Integer getToProcess() {
        if (getInvoiceStatus() != null) {
            if (Constants.INVOICE_STATUS_PAID.equals(getInvoiceStatus().getId())) {
                return DO_NOT_PROCESS;
            }

            if (Constants.INVOICE_STATUS_UNPAID.equals(getInvoiceStatus().getId())) {
                return PROCESS;
            }

            if (Constants.INVOICE_STATUS_UNPAID_AND_CARRIED.equals(getInvoiceStatus().getId())) {
                return DO_NOT_PROCESS;
            }
        }

        return PROCESS;
    }

    public void setToProcess(Integer toProcess) {
        if (toProcess == null) {
            setInvoiceStatus(null);
        } else {
            Integer status;
            if(toProcess == DO_NOT_PROCESS){
                status = Constants.INVOICE_STATUS_PAID;
            } else {
                if(getInvoiceStatus()!=null
                        && Constants.INVOICE_STATUS_UNPAID_AND_CARRIED==getInvoiceStatus().getId()){
                    // set the original status as it is only in case of carried
                    // because we don't want to change carried status to paid/unpaid (#5958)
                    status = getInvoiceStatus().getId();
                } else {
                    //else stick to the original rule, processing invoice to unpaid status
                    status = Constants.INVOICE_STATUS_UNPAID;
                }
            }

            setInvoiceStatus(new InvoiceStatusDAS().find(status));
        }
    }

    /**
     * The total amount owing that must be paid for this single invoice to be brought to zero and marked
     * paid. The initial balance is calculated as the invoice total {@link #getTotal()} - carried
     * balance {@link #getCarriedBalance()}. The initial balance of the invoice <strong>represents the
     * current periods charges only</strong>, not including any carried balances.
     *
     * As payments are applied, this balance will be reduced until it reaches zero and the invoice
     * is marked as paid.
     *
     * @return total owing balance
     */
    @Column(name = "balance", precision = 17, scale = 17)
    public BigDecimal getBalance() {
        return this.balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    /**
     * The sum total of all remaining un-paid or partially paid invoice balances. This represents
     * the portion of the invoice total amount {@link #getTotal()} that was brought forward from
     * an old un-paid invoice.
     *
     * @return portion of the invoice total that was carried forward.
     */
    @Column(name = "carried_balance", nullable = false, precision = 17, scale = 17)
    public BigDecimal getCarriedBalance() {
        return this.carriedBalance;
    }

    public void setCarriedBalance(BigDecimal carriedBalance) {
        this.carriedBalance = carriedBalance;
    }

    @Column(name = "in_process_payment", nullable = false)
    public int getInProcessPayment() {
        return this.inProcessPayment;
    }

    public void setInProcessPayment(int inProcessPayment) {
        this.inProcessPayment = inProcessPayment;
    }

    @Column(name = "is_review", nullable = false)
    public Integer getIsReview() {
        return this.isReview;
    }

    public void setIsReview(Integer isReview) {
        this.isReview = isReview;
    }

    @Column(name = "deleted", nullable = false)
    public Integer getDeleted() {
        return this.deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    @Column(name = "customer_notes", length = 1000)
    public String getCustomerNotes() {
        return this.customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public void appendCustomerNote (String note) {
        if (! StringUtils.isBlank(note)) {
            StringBuilder newNote = new StringBuilder();
            if (this.customerNotes != null) {
                newNote.append(this.customerNotes).append(" ");
            }
            setCustomerNotes(newNote.append(note).toString());
        }
    }

    @Column(name = "public_number", length = 40)
    public String getPublicNumber() {
        return this.publicNumber;
    }

    public void setPublicNumber(String publicNumber) {
        this.publicNumber = publicNumber;
    }

    @Column(name = "last_reminder", length = 29)
    public Date getLastReminder() {
        return this.lastReminder;
    }

    public void setLastReminder(Date lastReminder) {
        this.lastReminder = lastReminder;
    }

    @Column(name = "overdue_step")
    public Integer getOverdueStep() {
        return this.overdueStep;
    }

    public void setOverdueStep(Integer overdueStep) {
        this.overdueStep = overdueStep;
    }

    @Column(name = "create_timestamp", nullable = false, length = 29)
    public Date getCreateTimestamp() {
        return this.createTimestamp;
    }

    public void setCreateTimestamp(Date createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "invoice")
    public Set<InvoiceLineDTO> getInvoiceLines() {
        return this.invoiceLines;
    }

    public void setInvoiceLines(Set<InvoiceLineDTO> invoiceLines) {
        this.invoiceLines = invoiceLines;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "invoice")
    public Set<InvoiceDTO> getInvoices() {
        return this.invoices;
    }

    public void setInvoices(Set<InvoiceDTO> invoices) {
        this.invoices = invoices;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "invoice")
    @Fetch(FetchMode.SUBSELECT)
    public Set<OrderProcessDTO> getOrderProcesses() {
        return this.orderProcesses;
    }

    public void setOrderProcesses(Set<OrderProcessDTO> orderProcesses) {
        this.orderProcesses = orderProcesses;
    }

    public void setPaymentMap(Collection<PaymentInvoiceMapDTO> paymentMap) {
        this.paymentMap = paymentMap;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "invoiceEntity")
    public Collection<PaymentInvoiceMapDTO> getPaymentMap() {
        return paymentMap;
    }

    @Version
    @Column(name = "OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    @Override
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "invoice_meta_field_map",
            joinColumns = @JoinColumn(name = "invoice_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
            )
    @Sort(type = SortType.COMPARATOR, comparator = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    public List<MetaFieldValue> getMetaFields() {
        return getMetaFieldsList();
    }

    @Override
    @Transient
    public EntityType[] getCustomizedEntityType() {
        return new EntityType[] { EntityType.INVOICE };
    }

    // Helpers, for JPA migration
    @Transient
    public Integer getDelegatedInvoiceId() {
        if (getInvoice() != null) {
            return getInvoice().getId();
        }

        return null;
    }

    @Transient
    public String getCurrencyName() {
        return currencyName;
    }

    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    @Transient
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    @Transient
    public Integer getUserId() {
        return getBaseUser().getId();
    }

    @Transient
    public boolean hasSubAccounts() {
        for(InvoiceLineDTO line: getInvoiceLines()) {
            if (line.getInvoiceLineType().getId() == Constants.INVOICE_LINE_TYPE_SUB_ACCOUNT) {
                return true;
            }
        }
        return false;
    }

    @Transient
    public String getNumber() {
        return getPublicNumber();
    }

    @Transient
    public boolean isReviewInvoice() {
        return ( isReview != null && isReview.intValue() == 1 );
    }

    public void setCreditNoteMap(Collection<CreditNoteInvoiceMapDTO> creditNoteMap) {
        this.creditNoteMap = creditNoteMap;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "invoiceEntity")
    public Collection<CreditNoteInvoiceMapDTO> getCreditNoteMap() {
        return creditNoteMap;
    }

    /**
     * This flag is used to find out is generated invoice is credit invoice or not.
     * It sum up all invoice lines excluding carried lines of invoice (Due Invoice lines)
     * and if total of lines is negative, then it concludes that invoice is a credit invoice.
     */
    @Transient
    public boolean isCreditInvoice() {
        BigDecimal totalAmount = getInvoiceLines().stream()
                .filter(line -> (line.getDeleted() == 0 && line.getInvoiceLineType().getId() != Constants.INVOICE_LINE_TYPE_DUE_INVOICE))
                .map(InvoiceLineDTO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        return (totalAmount.compareTo(BigDecimal.ZERO) < 0) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Transient
    public boolean isPaid() {
        InvoiceStatusDTO status = getInvoiceStatus();
        return status == null ? balance.compareTo(BigDecimal.ZERO) == 0 :
            status.getId() == Constants.INVOICE_STATUS_PAID;
    }

    /**
     * Touch this InvoiceDTO and initialize all lazy-loaded fields.
     */
    public void touch() {
        getBillingProcess();
        getBaseUser();
        getCurrency();
        getInvoice();
        getPaperInvoiceBatch();

        if (getInvoiceLines() != null) {
            getInvoiceLines().size();
        }
        if (getInvoices() != null) {
            getInvoices().size();
        }
        if (getPaymentMap() != null) {
            getPaymentMap().size();
        }
        if (getOrderProcesses() != null) {
            for (OrderProcessDTO process : getOrderProcesses()) {
                process.getPurchaseOrder().touch();
            }
        }
    }

    public String getAuditKey(Serializable id) {
        StringBuilder key = new StringBuilder();
        key.append(getBaseUser().getCompany().getId())
        .append("-usr-")
        .append(getBaseUser().getId())
        .append("-")
        .append(id);

        return key.toString();
    }

    @Override
    @Transient
    public String[] getFieldNames() {
        return getInvoiceExportableWrapper().getFieldNames();
    }

    @Override
    @Transient
    public Object[][] getFieldValues() {
        return getInvoiceExportableWrapper().getFieldValues();
    }

    @Transient
    public InvoiceExportableWrapper getInvoiceExportableWrapper() {
        if(null == invoiceExportableWrapper) {
            invoiceExportableWrapper = new InvoiceExportableWrapper(getId());
        }
        return invoiceExportableWrapper;
    }

    public void setIsOrderLineTier(boolean isOrderLineTier){
        this.isOrderLineTier=isOrderLineTier;
    }

    @Transient
    public boolean getIsOrderLineTier(){
        return isOrderLineTier;
    }

    @Transient
    public boolean hasBalance() {
        return this.balance.compareTo(BigDecimal.ZERO) > 0;
    }

    @Transient
    public List<InvoiceLineDTO> getInvoiceLinesByOrderId(Integer orderId) {
        return this.invoiceLines
                .stream()
                .filter(invoiceLine -> null != invoiceLine.getOrder())
                .filter(invoiceLine -> invoiceLine.getOrder().getId().equals(orderId))
                .collect(Collectors.toList());
    }

    @Transient
    public OrderProcessDTO getOrderProcessForOrder(Integer orderId) {
        for(OrderProcessDTO orderProcess : getOrderProcesses()) {
            if(orderProcess.getPurchaseOrder().getId().equals(orderId)) {
                return orderProcess;
            }
        }
        return null;
    }
}
