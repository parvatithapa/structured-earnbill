package com.sapienter.jbilling.server.spa;

import com.sapienter.jbilling.server.metafields.db.value.BooleanMetaFieldValue;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.SearchResultString;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by pablo_galera on 14/02/17.
 */
public class DistributelTaxHelper {

    private int languageID;
    
    public String getProvince(UserDTO user) {
        AccountInformationTypeDTO serviceAddressGroupAIT = new AccountInformationTypeDAS().findByName(SpaConstants.SERVICE_ADDRESS_AIT, user.getEntity().getId(), user.getCustomer().getAccountType().getId());
        Integer groupAITId = serviceAddressGroupAIT.getId();
        CustomerAccountInfoTypeMetaField customerAITMetaField = user.getCustomer().getCurrentCustomerAccountInfoTypeMetaField(SpaConstants.SAME_AS_CUSTOMER_INFORMATION, serviceAddressGroupAIT.getId());
        boolean isSameAsContactInformation = ((null == customerAITMetaField || 
                                                null == customerAITMetaField.getMetaFieldValue() || 
                                                null == customerAITMetaField.getMetaFieldValue().getValue()) ? 
                                                    Boolean.TRUE : ((Boolean)customerAITMetaField.getMetaFieldValue().getValue()));
        if (isSameAsContactInformation) {
            groupAITId = new AccountInformationTypeDAS().findByName(SpaConstants.CONTACT_INFORMATION_AIT, user.getEntity().getId(), user.getCustomer().getAccountType().getId()).getId();
        }

        return (String)user.getCustomer().getCurrentCustomerAccountInfoTypeMetaField(SpaConstants.PROVINCE, groupAITId).getMetaFieldValue().getValue();
    }

    public List<TaxDistributel> getTaxList(UserDTO user, Date date, BigDecimal total) {
        return getTaxList(user, date, total, getProvince(user));
    }
    
    public List<TaxDistributel> getTaxList(UserDTO user, Date date, BigDecimal total, String province) {        
        if (StringUtils.isEmpty(province)) {
            return Collections.emptyList();
        }
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<List<String>> rows = getTaxes(user.getEntity().getId(), province, dateFormatter.format(DateConvertUtils.asLocalDate(date)));
        
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        
        languageID = user.getLanguage().getId();
        List<TaxDistributel> taxDistributelList = new ArrayList<>();
        List<String> row = rows.get(0);
        BigDecimal GST = new BigDecimal(row.get(SpaConstants.GST));
        BigDecimal PST = new BigDecimal(row.get(SpaConstants.PST));
        BigDecimal HST = new BigDecimal(row.get(SpaConstants.HST));

        if (GST.compareTo(BigDecimal.ZERO) > 0) {
            taxDistributelList.add(new TaxDistributel(getRegNumber(row, SpaConstants.GST), GST, Util.getPercentage(total, GST), DistributelTaxType.GTS));
        }
        if (PST.compareTo(BigDecimal.ZERO) > 0) {
            taxDistributelList.add(new TaxDistributel(getRegNumber(row, SpaConstants.PST), PST, Util.getPercentage(total, PST), DistributelTaxType.PST));
        }
        if (HST.compareTo(BigDecimal.ZERO) > 0) {
            taxDistributelList.add(new TaxDistributel(getRegNumber(row, SpaConstants.HST), HST, Util.getPercentage(total, HST), DistributelTaxType.HST));
        }
        return taxDistributelList;
    }
    
    private List<List<String>> getTaxes(int entityId, String province, String dateStr) {
        RouteDAS routeDAS = new RouteDAS();
        RouteDTO route = routeDAS.getRoute(entityId, "canadian_taxes");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setMax(1);
        criteria.setSort("date");
        criteria.setDirection(SearchCriteria.SortDirection.DESC);
        criteria.setFilters(new BasicFilter[]{
            new BasicFilter("province", Filter.FilterConstraint.EQ, province),
            new BasicFilter("date", Filter.FilterConstraint.LE, dateStr),

        });
        SearchResultString queryResult = routeDAS.searchDataTable(criteria, route);
        return queryResult.getRows();
    }

    private String getRegNumber(List<String> row, int taxName) {
        switch (taxName) {
            case SpaConstants.GST:
                return languageID == 1 ? row.get(SpaConstants.GST_REG_ENG) : row.get(SpaConstants.GST_REG_FR);
            case SpaConstants.PST:
                return languageID == 1 ? row.get(SpaConstants.PST_REG_ENG) : row.get(SpaConstants.PST_REG_FR);
            case SpaConstants.HST:
                return languageID == 1 ? row.get(SpaConstants.HST_REG_ENG) : row.get(SpaConstants.HST_REG_FR);
        }
        return null;
    }

    public static boolean isCustomerTaxExcempt(CustomerDTO customer) {
        BooleanMetaFieldValue value = (BooleanMetaFieldValue) customer.getMetaField(SpaConstants.TAX_EXEMPT);
        return value != null && value.getValue() != null && value.getValue();
    }
    
}
