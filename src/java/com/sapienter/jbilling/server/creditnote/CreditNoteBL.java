package com.sapienter.jbilling.server.creditnote;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteDAS;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteDTO;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteInvoiceMapDAS;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteInvoiceMapDTO;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteLineDAS;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteLineDTO;
import com.sapienter.jbilling.server.creditnote.db.CreditType;
import com.sapienter.jbilling.server.creditnote.event.CreditNoteBalanceChangeEvent;
import com.sapienter.jbilling.server.creditnote.event.CreditNoteCreationEvent;
import com.sapienter.jbilling.server.creditnote.event.CreditNoteDeletedEvent;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceStatusDAS;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.process.AgeingBL;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

public class CreditNoteBL {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private CreditNoteDAS creditNoteDas = null;
    private CreditNoteDTO creditNoteDto = null;
    private InvoiceDAS invoiceDAS = null;
    private CreditNoteInvoiceMapDAS mapDas = null;


    public CreditNoteBL(Integer creditNoteId) {
        init();
        set(creditNoteId);
    }

    public CreditNoteBL() {
        init();
    }

    public void set(Integer creditNoteId) {
        creditNoteDto = creditNoteDas.find(creditNoteId);
    }

    private void init() {
        creditNoteDas = new CreditNoteDAS();
        creditNoteDto = new CreditNoteDTO();
        invoiceDAS = new InvoiceDAS();
        mapDas = new CreditNoteInvoiceMapDAS();
    }

    public CreditNoteDTO getCreditNote() {
        return this.creditNoteDto;
    }

    public CreditNoteDTO getEntity() {
        return this.creditNoteDto;
    }

    public CreditNoteWS getCreditNoteWS() {
        return getWS(this.creditNoteDto);
    }

    public CreditNoteWS getWS(CreditNoteDTO creditNoteDTO) {
        if (creditNoteDTO != null) {
            CreditNoteWS creditNoteWS = new CreditNoteWS();

            creditNoteWS.setId(creditNoteDTO.getId());
            creditNoteWS.setUserId(creditNoteDTO.getCreationInvoice().getBaseUser().getId());
            creditNoteWS.setEntityId(creditNoteDTO.getEntityId());
            creditNoteWS.setBalanceAsDecimal(creditNoteDTO.getBalance());
            creditNoteWS.setAmountAsDecimal(creditNoteDTO.getAmount());
            creditNoteWS.setCreationInvoiceId(creditNoteDTO.getCreationInvoice().getId());
            creditNoteWS.setCreditNoteLineIds(creditNoteDTO.getLines().stream().map(CreditNoteLineDTO::getId).toArray(Integer[]::new));
            creditNoteWS.setCreditNoteInvoiceMapIds(creditNoteDTO.getPaidInvoices().stream().map(CreditNoteInvoiceMapDTO::getId).toArray(Integer[]::new));
            creditNoteWS.setDeleted(creditNoteDTO.getDeleted());
            creditNoteWS.setType(creditNoteDTO.getCreditType().name());
            creditNoteWS.setCreateDateTime(creditNoteDTO.getCreateDateTime());

            return creditNoteWS;
        }

        return null;
    }

    public CreditNoteDTO getDTO(CreditNoteWS creditNoteWS) {
        if (creditNoteWS != null) {
            CreditNoteDTO creditNote = new CreditNoteDTO();
            creditNote.setId(creditNoteWS.getId());
            creditNote.setEntityId(creditNoteWS.getEntityId());
            creditNote.setBalance(creditNoteWS.getBalanceAsDecimal());
            creditNote.setAmount(creditNoteWS.getAmountAsDecimal());
            creditNote.setCreationInvoice(invoiceDAS.find(creditNoteWS.getCreationInvoiceId()));

            Set<CreditNoteLineDTO> creditNoteLines = new HashSet<>();
            for (Integer creditNoteLineId : creditNoteWS.getCreditNoteLineIds()) {
                creditNoteLines.add(new CreditNoteLineDAS().find(creditNoteLineId));
            }
            creditNote.setLines(creditNoteLines);

            Set<CreditNoteInvoiceMapDTO> creditNoteInvoiceMaps = new HashSet<>();
            for (Integer creditNoteInvoiceMapId : creditNoteWS.getCreditNoteInvoiceMapIds()) {
                creditNoteInvoiceMaps.add(new CreditNoteInvoiceMapDAS().find(creditNoteInvoiceMapId));
            }
            creditNote.setPaidInvoices(creditNoteInvoiceMaps);

            creditNote.setDeleted(creditNoteWS.getDeleted());
            creditNote.setCreditType(CreditType.valueOf(creditNoteWS.getType()));
            creditNote.setCreateDateTime(creditNoteWS.getCreateDateTime());

            return creditNote;
        }

        return null;
    }

    public Integer create(InvoiceDTO invoiceDTO) {
        final CreditNoteDTO creditNote = new CreditNoteDTO();
        BigDecimal amount = invoiceDTO.getBalance();
        if( amount.compareTo(BigDecimal.ZERO) < 0 ) {
            amount = amount.negate();
        }
        creditNote.setAmount(amount);
        creditNote.setBalance(amount);
        creditNote.setCreditType(CreditType.AUTO_GENERATED);
        logger.debug("Creation Invoice Id: {} ", (invoiceDTO.getId()));
        creditNote.setCreationInvoice(invoiceDTO);
        creditNote.setDeleted(0);
        creditNote.setDescription("Credit Note ...");
        creditNote.setEntityId(invoiceDTO.getBaseUser().getEntity().getId());

        List<InvoiceLineDTO> invoiceLines = invoiceDTO.getInvoiceLines()
                .stream()
                .filter(invoiceLine -> invoiceLine.getInvoiceLineType().getId() != Constants.INVOICE_LINE_TYPE_DUE_INVOICE)
                .collect(Collectors.toList());

        creditNote.setLines(invoiceLines.stream()
                .map(invoiceLine -> buildCreditLine(invoiceLine, creditNote))
                .collect(Collectors.toSet()));

        creditNote.setCreateDateTime(TimezoneHelper.serverCurrentDate());
        invoiceDTO.setCreditNoteGenerated(creditNote);

        creditNote.setId(save(creditNote).getId());

        invoiceDTO.setTotal(BigDecimal.ZERO);
        invoiceDTO.setBalance(BigDecimal.ZERO);
        invoiceDTO.setInvoiceStatus(new InvoiceStatusDAS().find(Constants.INVOICE_STATUS_PAID));
        invoiceDAS.save(invoiceDTO);

        logger.debug("Credit Note Generated: {}", creditNote);

        CreditNoteCreationEvent event = new CreditNoteCreationEvent(
                invoiceDTO, creditNote.getEntityId());
        EventManager.process(event);

        return creditNote.getId();
    }

    public void delete(Integer creditNoteId) {
        set(creditNoteId);
        creditNoteDto.setDeleted(1);
        for (CreditNoteLineDTO creditNoteLineDTO : creditNoteDto.getLines()) {
            creditNoteLineDTO.setDeleted(1);
        }

        save(creditNoteDto);

        // since credit note is deleted, give back its balance on negative invoice
        // This is now only one place where negative balance invoice takes shape again :(
        InvoiceDTO creationInvoice = creditNoteDto.getCreationInvoice();
        BigDecimal negativeBalance = creditNoteDto.getBalance().multiply(new BigDecimal(-1));
        creationInvoice.setTotal(negativeBalance);
        creationInvoice.setBalance(negativeBalance);

        CreditNoteDeletedEvent event = new CreditNoteDeletedEvent(creditNoteDto.getEntityId(), creditNoteDto);
        EventManager.process(event);
    }

    public CreditNoteDTO save(CreditNoteDTO creditNoteDTO) {
        return creditNoteDas.save(creditNoteDTO);
    }

    public List<CreditNoteDTO> getAllCreditNotes(Integer entityId) {
        return creditNoteDas.findAllByEntityId(entityId);
    }

    public void applyCreditNote(Integer creditNoteId) {

        CreditNoteDTO creditNote = creditNoteDas.find(creditNoteId);
        if(!creditNote.hasBalance()) {
            return ;
        }
        // apply credit note to debit unpaid invoices
        for (InvoiceDTO invoice : getDebitInvoicesWithBalanceOldestFirst(creditNote.getCreationInvoice().getBaseUser())) {
            if (invoice.hasBalance()) {
                applyCreditNoteToInvoice(creditNote, invoice);
            }
        }
    }

    // Tet the invoices that have balance sorted by creation date with oldest invoices first
    private List<InvoiceDTO> getDebitInvoicesWithBalanceOldestFirst(UserDTO baseUser) {
        return invoiceDAS.findWithBalanceOldestFirstByUser(baseUser);
    }

    public void applyExistingCreditNotesToUnpaidInvoices(Integer userId) {
        List<CreditNoteDTO> creditNotes = new CreditNoteDAS().findCreditNotesWithBalanceOldestFirst(userId);
        for (CreditNoteDTO creditNote : creditNotes) {
            applyCreditNote(creditNote.getId());
        }
    }

    public void applyExistingCreditNotesToInvoice(InvoiceDTO debitInvoice) {
        if (null != debitInvoice && debitInvoice.hasBalance()) {
            List<CreditNoteDTO> creditNotes = creditNoteDas.findCreditNotesWithBalanceOldestFirst(debitInvoice.getBaseUser().getId());
            for (CreditNoteDTO creditNote : creditNotes) {
                if (creditNote.hasBalance() && debitInvoice.hasBalance()) {
                    applyCreditNoteToInvoice(creditNote, debitInvoice);
                }
            }
        }
    }

    public void applyCreditNoteToInvoice(CreditNoteDTO creditNote, InvoiceDTO debitInvoice) {

        // Before applying credit note, increment the payment attempts on the invoice
        debitInvoice.setPaymentAttempts(debitInvoice.getPaymentAttempts() + 1);

        // compare and find out amount by which to reduce the balance
        BigDecimal invoiceBalance = debitInvoice.getBalance();
        BigDecimal creditNoteBalance = creditNote.getBalance();
        BigDecimal balanceReducerAmount = invoiceBalance.compareTo(creditNoteBalance) < 0 ? invoiceBalance : creditNoteBalance;

        BigDecimal creditNoteOldBalance = creditNote.getBalance();
        // reduce the balance on credit note dto
        creditNote.setBalance(creditNoteBalance.subtract(balanceReducerAmount));
        BigDecimal creditNoteNewBalance = creditNote.getBalance();

        // reduce the balance on invoice dto
        debitInvoice.setBalance(invoiceBalance.subtract(balanceReducerAmount));

        // After balance is set, lets update the invoice status according to the balance
        if (BigDecimal.ZERO.compareTo(debitInvoice.getBalance()) == 0) {
            // update the to_process flag if the balance is 0
            debitInvoice.setToProcess(0);
        } else {
            debitInvoice.setToProcess(1);
        }

        // create a new record for credit_note_invoice_map table
        CreditNoteInvoiceMapDTO creditNoteInvoiceMap =
                new CreditNoteInvoiceMapDTO(creditNote, debitInvoice, balanceReducerAmount, TimezoneHelper.serverCurrentDate(), 1);
        new CreditNoteInvoiceMapDAS().save(creditNoteInvoiceMap);

        // if the user is in the ageing process, she should be out
        if (debitInvoice.isPaid() || !UserBL.isUserBalanceEnoughToAge(debitInvoice.getBaseUser(),null)) {
            new AgeingBL().out(debitInvoice.getBaseUser(), debitInvoice.getId(), DateConvertUtils.getNow());
        }

        CreditNoteBalanceChangeEvent event = new CreditNoteBalanceChangeEvent(creditNote.getEntityId(),
                creditNote.getId(),
                creditNoteOldBalance,
                creditNoteNewBalance);
        EventManager.process(event);
    }

    public boolean unLinkFromInvoice(Integer invoiceId) {

        InvoiceDTO invoice= invoiceDAS.find(invoiceId);
        Iterator<CreditNoteInvoiceMapDTO> it = invoice.getCreditNoteMap().iterator();
        boolean bSucceeded = false;
        while (it.hasNext()) {
            CreditNoteInvoiceMapDTO map = it.next();
            if (this.creditNoteDto.getId() == map.getCreditNote().getId()) {
                this.removeInvoiceLink(map.getId());
                bSucceeded = true;
                break;
            }
        }

        return bSucceeded;
    }

    public void removeInvoiceLink(Integer mapId) {
        try {
            // declare variables
            InvoiceDTO invoice;

            // find the map
            CreditNoteInvoiceMapDTO map = mapDas.find(mapId);
            // start returning the money to the payment's balance
            BigDecimal amount = map.getAmount();
            creditNoteDto = map.getCreditNote();
            BigDecimal creditNoteOldBalance = creditNoteDto.getBalance();
            amount = amount.add(creditNoteOldBalance);
            creditNoteDto.setBalance(amount);

            // the balace of the invoice also increases
            invoice = map.getInvoiceEntity();
            amount = map.getAmount().add(invoice.getBalance());
            invoice.setBalance(amount);

            // this invoice probably has to be paid now
            if (invoice.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                invoice.setToProcess(1);
            }

            // get rid of the map all together
            invoice.getCreditNoteMap().remove(map);
            mapDas.delete(map);

            CreditNoteBalanceChangeEvent event = new CreditNoteBalanceChangeEvent(
                    creditNoteDto.getEntityId(), creditNoteDto.getId(),
                    creditNoteOldBalance, creditNoteDto.getBalance());
            EventManager.process(event);

        } catch (EntityNotFoundException enfe) {
            logger.error("Exception removing creditNote-invoice link: EntityNotFoundException", enfe);
        } catch (Exception e) {
            logger.error("Exception removing creditNote-invoice link", e);
            throw new SessionInternalError(e);
        }
    }

    public Integer[] getLastCreditNotes(Integer userId, Integer number) {
        List<Integer> result = creditNoteDas.findIdsByUserLatestFirst(userId, number);
        return result.toArray(new Integer[result.size()]);
    }

    public BigDecimal getAvailableCreditNotesBalanceByUser(Integer userId) {
        CurrencyBL currencyBL = new CurrencyBL();
        UserDTO user = new UserDAS().find(userId);

        return new CreditNoteDAS().findAvailableCreditNotesBalanceByUser(userId)
                .stream()
                .map(creditNote -> currencyBL.convert(creditNote.getCreationInvoice().getCurrency().getId(),
                        user.getCurrency().getId(),
                        creditNote.getBalance(),
                        TimezoneHelper.serverCurrentDate(),
                        user.getEntity().getId()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CreditNoteLineDTO buildCreditLine(InvoiceLineDTO invoiceLine, CreditNoteDTO creditNote) {
        return CreditNoteLineDTO.builder()
                .creationInvoiceLine(invoiceLine)
                .creditNoteDTO(creditNote)
                .deleted(0)
                .description(invoiceLine.getDescription())
                .amount(invoiceLine.getAmount().negate())
                .build();
    }


    private void validateParameters(Date fromDate, Date toDate) {
        List<String> errorMessages = new ArrayList<>();
        if(null == fromDate) {
            errorMessages.add("Invalid paramters passed , fromDate is null!");
        }

        if(null == toDate) {
            errorMessages.add("Invalid paramters passed , toDate is null!");
        }

        if(!errorMessages.isEmpty()) {
            throw new SessionInternalError(errorMessages.toArray(new String[0]));
        }

        if(fromDate.after(toDate)) {
            throw new SessionInternalError("Invalid paramters passed , fromDate is greater than toDate!");
        }
    }

    /**
     * Returns {@link CreditNoteInvoiceMapWS} between given dates.
     * @param fromDate
     * @param toDate
     * @return
     */
    public CreditNoteInvoiceMapWS[] getCreditNoteInvoiceMaps(Date fromDate, Date toDate) {
        try {
            validateParameters(fromDate, toDate);
            CreditNoteInvoiceMapWS[] result = creditNoteDas.findCreditNoteInvoiceMapsByDate(fromDate, toDate).toArray(new CreditNoteInvoiceMapWS[0]);
            logger.debug("CreditNoteInvoiceMaps {} for Date [{} to {}] ", result, fromDate, toDate);
            return result;
        } catch(SessionInternalError error) {
            logger.error("In valid parameters passed!", error);
            throw error;
        } catch(Exception ex) {
            logger.error("Error in getCreditNoteInvoiceMaps", ex);
            throw new SessionInternalError("Error in getCreditNoteInvoiceMaps", ex);
        }
    }

    /**
     * fetches all {@link CreditNoteWS} for given userId.
     * @param userId
     * @param offset
     * @param limit
     * @return
     */
    public CreditNoteWS[] findCreditNotesByUser(Integer userId, Integer offset, Integer limit) {
        logger.debug("fetching credit notes for user {} with offset {} and limit {}", userId, offset, limit);
        return creditNoteDas.findCreditNotesByUser(userId, offset, limit)
                .stream()
                .map(this::getWS)
                .toArray(CreditNoteWS[]::new);
    }

}
