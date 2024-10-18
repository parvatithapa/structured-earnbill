package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.PlanItemDAS;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * Business logic for account type pricing.
 *
 * This class manages the logic of applying different prices for different account types
 *
 * @author Panche Isajeski
 * @since 05/14/2013
 */
public class AccountTypePriceBL {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AccountTypePriceBL.class));

    private AccountTypePriceDAS accountTypePriceDAS;

    private AccountTypeDAS accountTypeDAS;
    private Integer accountTypeId;

    private AccountTypeDTO accountType;
    private AccountTypePriceDTO price;

    public AccountTypePriceBL() {
        _init();
    }

    public AccountTypePriceBL(Integer accountTypeId) {
        try{
            _init();
            this.accountType = accountTypeDAS.find(accountTypeId);
            this.accountTypeId = accountTypeId;
        } catch (Exception e){
            throw new SessionInternalError("Setting account type price", AccountTypePriceBL.class, e);
        }
    }

    public AccountTypePriceBL(AccountTypeDTO accountType) {
        _init();
        this.accountType = accountType;
        this.accountTypeId = accountType.getId();
    }

    public AccountTypePriceBL(Integer accountTypeId, Integer planItemId) {
        this(accountTypeId);
        setAccountTypePrice(planItemId);
    }

    public AccountTypePriceBL(AccountTypeDTO accountType, Integer planItemId) {
        this(accountType);
        setAccountTypePrice(planItemId);
    }

    private void _init() {
        accountTypePriceDAS = new AccountTypePriceDAS();
        accountTypeDAS = new AccountTypeDAS();
    }

    public void setAccountTypeId(Integer accountTypeId) {
        this.accountType = accountTypeDAS.find(accountTypeId);
        this.accountTypeId = accountTypeId;
    }

    public void setAccountTypePrice(Integer planItemId) {
        this.price = accountTypePriceDAS.find(accountTypeId, planItemId);
    }

    public Integer getAccountTypeId() {
        return accountTypeId;
    }

    public AccountTypeDTO getAccountType() {
        return accountType;
    }

    public AccountTypePriceDTO getPrice() {
        return price;
    }

    public AccountTypePriceDTO addPrice(PlanItemDTO planItem) {
        return create(planItem);
    }

    /**
     *  Create a planItem and a account type pricing linked to that plan item
     *
     * @param planItem
     * @return account price created
     */
    public AccountTypePriceDTO create(PlanItemDTO planItem) {
        AccountTypePriceDTO dto = new AccountTypePriceDTO();
        dto.setAccountType(accountType);

        planItem = new PlanItemDAS().save(planItem);
        dto.setPlanItem(planItem);

        this.price = accountTypePriceDAS.save(dto);
        return this.price;
    }

    /**
     *  Update the plan item and the mapping account type pricing
     *
     * @param planItem
     */
    public void update(PlanItemDTO planItem) {
        // TODO (pai) FIX - the update is not working as expected
        if (price != null) {
            planItem = new PlanItemDAS().save(planItem);
            price.setPlanItem(planItem);

            accountTypePriceDAS.save(price);
        } else {

            LOG.error("Cannot update, AccountTypePriceDTO not found or not set!");
        }
    }

    /**
     *  Deletes the account type price
     *
     */
    public void delete() {
        if (price != null) {
            // TODO (pai) Check if the plan item should be deleted as well
            accountTypePriceDAS.delete(price);
        } else {
            LOG.error("Cannot delete, AccountTypePriceDTO not found or not set!");
        }
    }

    /**
     *  Retrieves the price (PlanItemDTO) for the specified account type and provided itemId (product)
     *
     * @param itemId
     * @return Account type price
     */
    public PlanItemDTO getPrice(Integer itemId) {
        return accountTypePriceDAS.findPriceByItem(accountTypeId, itemId);
    }

    /**
     *  Retrieves all the account type prices for the specified account type
     *
     * @return Account Type Prices
     */
    public List<PlanItemDTO> getAccountTypePrices() {
        return accountTypePriceDAS.findAllAccountTypePrices(accountTypeId);
    }

    /**
     *  Retrieves all the account type prices for the provided itemId (product)
     *
     * @return Account Type Prices
     */
    public List<PlanItemDTO> getAccountTypePrices(Integer itemId) {
        return accountTypePriceDAS.findAllAccountTypePricesByItem(accountTypeId, itemId);
    }

    public List<PlanItemDTO> getPricesForItemAndPricingDate(Integer itemId, Date pricingDate) {
        return accountTypePriceDAS.findAllAccountTypePrices(accountTypeId, itemId, pricingDate);
    }

}
