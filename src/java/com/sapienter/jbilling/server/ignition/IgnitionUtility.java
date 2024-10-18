package com.sapienter.jbilling.server.ignition;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationBL;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.pricing.RouteRecordWS;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.NameValueString;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.SearchResultString;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.tools.JArrays;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTimeComparator;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by taimoor on 8/5/17.
 */
public class IgnitionUtility {

    private static final FormatLogger logger = new FormatLogger(IgnitionUtility.class);

    public static OrderWS getOrder(UserWS userWS, Integer orderId) {

        return getOrder(userWS.getLanguageId(), orderId);
    }

    public static OrderWS getOrder(Integer languageId, Integer orderId) {

        // now get the order. Avoid the proxy since this is for the client
        OrderDAS das = new OrderDAS();
        OrderDTO order = das.findNow(orderId);
        if (order == null) { // not found
            return null;
        }

        OrderBL bl = new OrderBL(order);

        return bl.getWS(languageId);
    }

    public static ItemDTOEx getItem(Integer itemId, Integer userId, Integer entityId, UserWS userWS) {
        PricingField[] fields = PricingField.getPricingFieldsValue(null);

        ItemBL helper = new ItemBL(itemId);
        List<PricingField> f = JArrays.toArrayList(fields);
        helper.setPricingFields(f);

        Integer languageId = userWS.getLanguageId();

        // use the currency of the given user if provided, otherwise
        // default to the currency of the caller (admin user)
        Integer currencyId = userWS.getCurrencyId();

        ItemDTOEx retValue = helper.getWS(helper.getDTO(languageId, userId, entityId, currencyId));

        return retValue;
    }

    private static ServiceProfile getServiceProfile(List<String> row, List<String> columnNames){

        ServiceProfile serviceProfile = new ServiceProfile();

        for(int i = 1; i < row.size(); i++){
            switch (columnNames.get(i)){
                case ServiceProfile.Names.ACB_USER_CODE:
                    serviceProfile.setACBUserCode(row.get(i));
                    break;
                case ServiceProfile.Names.BANK:
                    serviceProfile.setBank(row.get(i));
                    break;
                case ServiceProfile.Names.BANK_ACCOUNT_BRANCH:
                    serviceProfile.setBankAccountBranch(row.get(i));
                    break;
                case ServiceProfile.Names.BANK_ACCOUNT_NAME:
                    serviceProfile.setBankAccountName(row.get(i));
                    break;
                case ServiceProfile.Names.BANK_ACCOUNT_NUMBER:
                    serviceProfile.setBankAccountNumber(row.get(i));
                    break;
                case ServiceProfile.Names.BANK_ACCOUNT_TYPE:
                    serviceProfile.setBankAccountType(row.get(i));
                    break;
                case ServiceProfile.Names.BANK_DETAILS:
                    serviceProfile.setBankDetails(row.get(i));
                    break;
                case ServiceProfile.Names.CODE:

                    String code = row.get(i) == null ? StringUtils.EMPTY : row.get(i);
                    if(code.length() > 5){
                        code = code.substring(code.length() - 5);
                    }
                    serviceProfile.setCode(code);
                    break;
                case ServiceProfile.Names.CUTOFF_TIME:
                    serviceProfile.setCutOffTime(row.get(i));
                    break;
                case ServiceProfile.Names.BRAND_NAME:
                    serviceProfile.setBrandName(row.get(i));
                    break;
                case ServiceProfile.Names.ENTITY_NAME:
                    serviceProfile.setEntityName(row.get(i));
                    break;
                case ServiceProfile.Names.FILE_SEQUENCE_NO:
                    serviceProfile.setFileSequenceNo(Integer.parseInt(row.get(i)));
                    break;
                case ServiceProfile.Names.FROM_FI_FOLDER_LOCATION:
                    serviceProfile.setFromFIFolderLocation(row.get(i));
                    break;
                case ServiceProfile.Names.GENERATION_NO:
                    serviceProfile.setGenerationNo(row.get(i));
                    break;
                case ServiceProfile.Names.ISLIVE:
                    serviceProfile.setLive(Boolean.parseBoolean(row.get(i)));
                    break;
                case ServiceProfile.Names.SERVICE_PROFILE:
                    serviceProfile.setName(row.get(i));
                    break;
                case ServiceProfile.Names.SERVICE_PROVIDER:
                    serviceProfile.setServiceProvider(row.get(i));
                    break;
                case ServiceProfile.Names.SERVICES:
                    serviceProfile.setServices(row.get(i));
                    break;
                case ServiceProfile.Names.SHORT_NAME:
                    serviceProfile.setShortName(row.get(i));
                    break;
                case ServiceProfile.Names.TO_FI_FOLDER_LOCATION:
                    serviceProfile.setToFIFolderLocation(row.get(i));
                    break;
                case ServiceProfile.Names.TRANSACTION_NO:
                    serviceProfile.setTransactionNo(row.get(i));
                    break;
                case ServiceProfile.Names.TYPES_OF_DEBIT_SERVICES:
                    serviceProfile.setTypesOfDebitServices(row.get(i));
                    break;
                case ServiceProfile.Names.USER_NAME:
                    serviceProfile.setUsername(row.get(i));
                    break;

                default:
                    break;
            }
        }

        return serviceProfile;
    }

    public static void updateServiceProfile(String serviceProfileCode, Integer entityId, String sequenceNumberColumnName, String generationNumberColumnName) {
        RouteDAS routeDAS = new RouteDAS();
        RouteDTO route = routeDAS.getRoute(entityId, IgnitionConstants.DATA_TABLE_NAME);
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSort("id");
        criteria.setMax(1);
        criteria.setDirection(SearchCriteria.SortDirection.DESC);
        criteria.setFilters(new BasicFilter[]{
                new BasicFilter(ServiceProfile.Names.CODE, com.sapienter.jbilling.server.util.search.Filter.FilterConstraint.EQ, serviceProfileCode),

        });
        SearchResultString queryResult = routeDAS.searchDataTable(criteria, route);

        List<List<String>> rows = queryResult.getRows();
        List<String> columnNames = queryResult.getColumnNames();

        if(rows.isEmpty()){

            logger.debug("No Service Profile Row found in the data table for Code: " + serviceProfileCode);
            return;
        }

        List<String> row = rows.get(0);
        NameValueString[] attributes = new NameValueString[columnNames.size()-1];

        for(int i=1;i<row.size();i++){
            if(columnNames.get(i).equals(sequenceNumberColumnName) ||
                    columnNames.get(i).equals(generationNumberColumnName)){
                Integer number = Integer.parseInt(row.get(i));
                number = number+1;
                attributes[i-1] = new NameValueString(columnNames.get(i), number.toString());
            }
            else{
                attributes[i-1] = new NameValueString(columnNames.get(i),row.get(i));
            }
        }

        RouteRecordWS record = new RouteRecordWS();
        record.setAttributes(attributes);
        record.setId(Integer.parseInt(row.get(0)));

        IWebServicesSessionBean session = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        session.updateRouteRecord(record,route.getId());

    }

    public static Map<String, ServiceProfile> getServiceProfilesForGivenColumn(String dataTableName, String columnName, String columnValue, Integer entityId){

        Map<String, ServiceProfile> serviceProfiles = new HashMap<>();

        RouteDAS routeDAS = new RouteDAS();
        RouteDTO route = routeDAS.getRoute(entityId, dataTableName);
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSort("id");
        criteria.setMax(999);
        criteria.setDirection(SearchCriteria.SortDirection.ASC);

        if(columnValue != null) {
            criteria.setFilters(new BasicFilter[]{
                    new BasicFilter(columnName, com.sapienter.jbilling.server.util.search.Filter.FilterConstraint.EQ, columnValue),

            });
        }else{
            criteria.setFilters(new BasicFilter[]{ });
        }

        SearchResultString queryResult = routeDAS.searchDataTable(criteria, route);

        List<List<String>> rows = queryResult.getRows();
        List<String> columnNames = queryResult.getColumnNames();

        for(List<String> row : rows){

            ServiceProfile serviceProfile = getServiceProfile(row,columnNames);

            serviceProfiles.put(serviceProfile.getServiceProvider(), serviceProfile);
        }

        return serviceProfiles;
    }

    public static Pair<String, String> getBrandAndServiceProviderFromOrder(Integer entityId, UserWS userWS, Integer orderId){
        // Get Order, Item and Item Category
        OrderWS order = getOrder(userWS, orderId);
        Pair<String, String> brandAndServiceProvider = getBrandAndServiceProviderFromItem(entityId, userWS, order, null);
        return brandAndServiceProvider;
    }

    public static Map<String, ServiceProfile> getAllServiceProfilesForGivenEntity(String dataTableName, Integer entityId){

        Map<String, ServiceProfile> serviceProfiles = new HashMap<>();

        RouteDAS routeDAS = new RouteDAS();
        RouteDTO route = routeDAS.getRoute(entityId, dataTableName);
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSort("id");
        criteria.setMax(999);
        criteria.setDirection(SearchCriteria.SortDirection.ASC);

        criteria.setFilters(new BasicFilter[]{ });

        SearchResultString queryResult = routeDAS.searchDataTable(criteria, route);

        List<List<String>> rows = queryResult.getRows();
        List<String> columnNames = queryResult.getColumnNames();

        for(List<String> row : rows){

            ServiceProfile serviceProfile = getServiceProfile(row,columnNames);

            // Concatenate Service Provider AND Brand Name for unique KEY
            serviceProfiles.put(serviceProfile.getServiceProvider() + "." + serviceProfile.getBrandName(), serviceProfile);
        }

        return serviceProfiles;
    }

    // Gets Action Date according to the provided holidays
    public static Calendar getActionDateForWorkingDays(Calendar actionDateCalendar, List<Date> holidays) {

        if (actionDateCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {

            actionDateCalendar.add(Calendar.DATE, 2);
        } else if (actionDateCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {

            actionDateCalendar.add(Calendar.DATE, 1);
        }

        if (!CollectionUtils.isEmpty(holidays)) {

            List<Date> holidaysAscending = new ArrayList<>(holidays);

            // Put the collection in Ascending order as it is created in Descending
            Collections.sort(holidaysAscending, Collections.reverseOrder());

            for (Date holiday : holidaysAscending) {

                if (DateTimeComparator.getDateOnlyInstance().compare(actionDateCalendar.getTime(), holiday) == 0) {

                    actionDateCalendar.add(Calendar.DATE, 1);

                    if (actionDateCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                        actionDateCalendar.add(Calendar.DATE, 2);
                    } else if (actionDateCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        actionDateCalendar.add(Calendar.DATE, 1);
                    }
                }
            }
        }

        return actionDateCalendar;
    }

    public static PaymentAuthorizationDTO buildPaymentAuthorization(String responseCode, String responseMessage, String processor) {

        PaymentAuthorizationDTO paymentAuthDTO = new PaymentAuthorizationDTO();
        paymentAuthDTO.setProcessor(processor);

        if (responseMessage != null) {
            paymentAuthDTO.setResponseMessage(responseMessage);
        }
        paymentAuthDTO.setCode1(responseCode);

        return paymentAuthDTO;
    }

    public static boolean updatePaymentWithErrorCodeAndMetaField(Integer paymentId, String errorCode, String errorDescription,
                                                          IWebServicesSessionBean webServicesSessionBean, Integer entityId, String bankName){
        try {
            PaymentWS paymentWS = webServicesSessionBean.getPayment(paymentId);
            PaymentBL paymentBL = new PaymentBL(paymentId);

            if (paymentWS.getAuthorizationId() != null) {
                logger.debug("Payment Authorization already exists for payment id : " + paymentId);
                return true;
            } else {
                PaymentAuthorizationDTO paymentAuthorization = IgnitionUtility.buildPaymentAuthorization(errorCode,
                        errorDescription, bankName);
                PaymentAuthorizationBL paymentAuthorizationBL = new PaymentAuthorizationBL();
                paymentAuthorizationBL.create(paymentAuthorization, paymentId);
                paymentBL.getEntity().getPaymentAuthorizations().add(paymentAuthorizationBL.getEntity());
            }

            List<MetaFieldValueWS> metaFieldValueWSList = new ArrayList<>();
            metaFieldValueWSList.addAll(Arrays.asList(paymentWS.getMetaFields()));

            MetaField metaField = new MetaFieldDAS().getFieldByName(entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_DATE);
            Calendar calendar = Calendar.getInstance();
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");

            if (metaField != null) {
                Boolean metaFieldExists = false;

                for (MetaFieldValueWS metaFieldValueWS : paymentWS.getMetaFields()) {
                    if (metaFieldValueWS.getFieldName().equals(IgnitionConstants.PAYMENT_DATE)) {
                        metaFieldValueWS.setStringValue(dateFormat.format(DateConvertUtils.asLocalDateTime(calendar.getTime())));
                        metaFieldExists = true;
                    }
                }

                if (!metaFieldExists) {
                    MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
                    metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_DATE);
                    metaFieldValueWS.setStringValue(dateFormat.format(DateConvertUtils.asLocalDateTime(calendar.getTime())));
                    metaFieldValueWSList.add(metaFieldValueWS);
                }
            }

            MetaFieldValueWS[] updatedMetaFieldValueWSArray = new MetaFieldValueWS[metaFieldValueWSList.size()];
            metaFieldValueWSList.toArray(updatedMetaFieldValueWSArray);

            paymentWS.setMetaFields(updatedMetaFieldValueWSArray);
            paymentWS.setResultId(CommonConstants.PAYMENT_RESULT_FAILED);

            updateInvoiceAndPaymentWithFailedStatus(paymentWS,webServicesSessionBean);
        }catch (Exception exception){
            logger.debug("Exception: " +exception);
            return false;
        }

        return true;
    }

    public static List<String> appendCarriageFeed(List<String> lines){

        List<String> appendedLines = new ArrayList<>();

        lines.forEach(line -> appendedLines.add(line + "\r"));

        lines.clear();

        return appendedLines;
    }

    /**
     * Gets Action Date with respect to the Cut Off Time
     * @param cutOffTime
     * @param currentDate
     * @return
     * @throws ParseException
     */
    public static Date getActionDate(String cutOffTime, Date currentDate) throws ParseException {

        Date actionDate = currentDate;

        if(isCutOffTimeReached(cutOffTime, currentDate)){
            Calendar actionDateCal = Calendar.getInstance();
            actionDateCal.setTime(currentDate);
            actionDateCal.add(Calendar.DATE, 1);

            actionDate = actionDateCal.getTime();
        }

        logger.debug("Calculated Action date for incoming Date: %s and Cut-Off-Time: %s is: %s", currentDate, cutOffTime, actionDate);

        return actionDate;
    }

    /**
     * Checks if cut-off time is reached
     * @param cutOffTime
     * @param currentDate
     * @return
     * @throws ParseException
     */
    public static boolean isCutOffTimeReached(String cutOffTime, Date currentDate) throws ParseException {

        String dateFormat = IgnitionConstants.ABSA_TIME_FORMAT;
        String dateString = cutOffTime+" 01/01/1970";
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                .ofPattern(dateFormat);

        Date cutOffDate = DateConvertUtils.asUtilDate(LocalDateTime.parse(dateString, dateTimeFormatter));

        Calendar cutOffCal = Calendar.getInstance();
        cutOffCal.setTime(cutOffDate);

        Calendar currentCal = Calendar.getInstance();
        currentCal.setTime(currentDate);
        currentCal.set(Calendar.DAY_OF_MONTH, cutOffCal.get(Calendar.DAY_OF_MONTH));
        currentCal.set(Calendar.MONTH, cutOffCal.get(Calendar.MONTH));
        currentCal.set(Calendar.YEAR, cutOffCal.get(Calendar.YEAR));

        return (currentCal.getTimeInMillis() - cutOffCal.getTimeInMillis()) > 0;
    }

    public static void updateColumnInServiceProfile(String serviceProfileCode, Integer entityId, String columnName, String value) {
        RouteDAS routeDAS = new RouteDAS();
        RouteDTO route = routeDAS.getRoute(entityId, IgnitionConstants.DATA_TABLE_NAME);
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSort("id");
        criteria.setMax(1);
        criteria.setDirection(SearchCriteria.SortDirection.DESC);
        criteria.setFilters(new BasicFilter[]{
                new BasicFilter(ServiceProfile.Names.CODE, com.sapienter.jbilling.server.util.search.Filter.FilterConstraint.EQ, serviceProfileCode),

        });
        SearchResultString queryResult = routeDAS.searchDataTable(criteria, route);

        List<List<String>> rows = queryResult.getRows();
        List<String> columnNames = queryResult.getColumnNames();

        if(rows.isEmpty()){

            logger.debug("No Service Profile Row found in the data table for Code: " + serviceProfileCode);
            return;
        }

        List<String> row = rows.get(0);
        NameValueString[] attributes = new NameValueString[columnNames.size()-1];

        for(int i=1;i<row.size();i++){
            if(columnNames.get(i).equals(columnName)){
                attributes[i-1] = new NameValueString(columnNames.get(i), value);
            }
            else{
                attributes[i-1] = new NameValueString(columnNames.get(i),row.get(i));
            }
        }

        RouteRecordWS record = new RouteRecordWS();
        record.setAttributes(attributes);
        record.setId(Integer.parseInt(row.get(0)));

        IWebServicesSessionBean session = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        session.updateRouteRecord(record,route.getId());
    }

    public static Pair<String, String> getBrandAndServiceProviderFromItem(Integer entityId, UserWS userWS, OrderWS order, Collection<OrderChangeDTO> orderChanges){

        IWebServicesSessionBean session = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        Integer itemId = null;

        if(!ArrayUtils.isEmpty(order.getOrderLines())){
            itemId = order.getOrderLines()[0].getItemId();
        }else {
            if(!orderChanges.isEmpty()){
                itemId = orderChanges.iterator().next().getItem().getId();
            }
        }

        if(itemId ==  null){
            logger.error("Unable to find Product ID from the incoming order: " + order.getId());
            return null;
        }
        ItemDTOEx item = getItem(itemId, userWS.getId(), entityId, userWS);
        ItemTypeWS itemTypeWS = session.getItemCategoryById(item.getTypes()[0]);
        // Get Bank Name from User Payment Instrument
        String serviceProfileName = Arrays.stream(userWS.getPaymentInstruments().get(0).getMetaFields()).filter(
                metaFieldValueWS -> metaFieldValueWS.getFieldName().equals(IgnitionConstants.PAYMENT_BANK_NAME)).map(
                metaFieldValueWS -> metaFieldValueWS.getStringValue()).findFirst().orElse(IgnitionConstants.SERVICE_PROVIDER_ABSA);


        if(serviceProfileName.toLowerCase().contains("standard")){
            serviceProfileName = IgnitionConstants.SERVICE_PROVIDER_STANDARD_BANK;
        }else{
            serviceProfileName = IgnitionConstants.SERVICE_PROVIDER_ABSA;
        }

        // Get Brand Name/ Category Name
        String divisionName = itemTypeWS.getDescription();
        return Pair.of(divisionName, serviceProfileName);
    }

    public static ServiceProfile getServiceProfileForBrand(Pair<String, String> brandAndServiceProvider, Map<String, Map<String, ServiceProfile>> allServiceProfiles){

        if(null == brandAndServiceProvider)
            return null;

        String brandName = brandAndServiceProvider.getLeft();
        String serviceProfileName = brandAndServiceProvider.getRight();
        // Get service profiles for the given brand
        // service_profiles = getServiceProfilesForGivenColumn(IgnitionConstants.DATA_TABLE_NAME, ServiceProfile.Names.BRAND_NAME, divisionName, entityId);
        Map<String, ServiceProfile> serviceProfiles = allServiceProfiles.get(brandName);

        if(MapUtils.isEmpty(serviceProfiles)){
            logger.error("Unable to find any Service Profile for Brand: " + brandName);
            return null;
        }

        // Get the first service profile to be used as default
        ServiceProfile serviceProfileFirst = serviceProfiles.entrySet().iterator().next().getValue();

        // Creating an effectively final service profile name variable to be used in lambda expression
        String finalServiceProfileName = serviceProfileName;

        // Get service profile for the given User Bank from Brand Service Profiles
        ServiceProfile serviceProfile = serviceProfiles.entrySet().stream().filter(
                entry -> entry.getKey().equals(finalServiceProfileName)).map(entry -> entry.getValue()).findFirst().orElse(serviceProfileFirst);

        return serviceProfile;
    }

    public static ServiceProfile getServiceProfileFromItemForBrand(Integer entityId, UserWS userWS, OrderWS order, Collection<OrderChangeDTO> orderChanges){

        Pair<String, String> brandAndServiceProvider = getBrandAndServiceProviderFromItem(entityId, userWS, order, orderChanges);

        String brandName = brandAndServiceProvider.getLeft();
        String serviceProfileName = brandAndServiceProvider.getRight();

        // Get service profiles for the given brand
        Map<String, ServiceProfile> serviceProfiles = getServiceProfilesForGivenColumn(IgnitionConstants.DATA_TABLE_NAME,
                ServiceProfile.Names.BRAND_NAME, brandName, entityId);

        if(MapUtils.isEmpty(serviceProfiles)){
            logger.error("Unable to find any Service Profile for Brand: " + brandName);
            return null;
        }

        // Get the first service profile to be used as default
        ServiceProfile serviceProfileFirst = serviceProfiles.entrySet().iterator().next().getValue();

        // Creating an effectively final service profile name variable to be used in lambda expression
        String finalServiceProfileName = serviceProfileName;

        // Get service profile for the given User Bank from Brand Service Profiles
        ServiceProfile serviceProfile = serviceProfiles.entrySet().stream().filter(
                entry -> entry.getKey().equals(finalServiceProfileName)).map(entry -> entry.getValue()).findFirst().orElse(serviceProfileFirst);

        return serviceProfile;
    }

    // Gets Action Date according to the given Service Profile
    public static Date getUserActionDateForCurrentService(Date nextPaymentDate, String serviceType, List<Date> holidays) throws ParseException {

        Calendar currentCal = Calendar.getInstance();
        currentCal.setTime(nextPaymentDate);

        currentCal.set(Calendar.HOUR_OF_DAY, 0);
        currentCal.set(Calendar.MINUTE, 0);
        currentCal.set(Calendar.SECOND, 0);
        currentCal.set(Calendar.MILLISECOND, 0);

        int daysToAdd = 0;
        if (serviceType.toLowerCase().equals(IgnitionConstants.ServiceType.TWO_DAY.toString().toLowerCase())) {
            daysToAdd = 5;
        }

        // Subtracts given number of working days from the calender, weekends/holidays are not counted
        currentCal = subtractWorkingDays(currentCal, holidays, daysToAdd);

        Date currentCalTime = currentCal.getTime();

        logger.debug("Calculated Action date for incoming Date: %s and ServiceType: %s is: %s", nextPaymentDate, serviceType, currentCalTime);

        return currentCalTime;
    }

    // Gets Action Date according to the provided holidays
    public static Calendar getUserActionDateForWorkingDays(Calendar actionDateCalendar, List<Date> holidays) {

        if (actionDateCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {

            actionDateCalendar.add(Calendar.DATE, -2);
        } else if (actionDateCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {

            actionDateCalendar.add(Calendar.DATE, -1);
        }

        Date actionDate = actionDateCalendar.getTime();

        if (!CollectionUtils.isEmpty(holidays)) {

            for (Date holiday : holidays) {

                if (DateTimeComparator.getDateOnlyInstance().compare(actionDate, holiday) == 0) {

                    actionDateCalendar.add(Calendar.DATE, -1);

                    if (actionDateCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        actionDateCalendar.add(Calendar.DATE, -2);
                    } else if (actionDateCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                        actionDateCalendar.add(Calendar.DATE, -1);
                    }

                    actionDate = actionDateCalendar.getTime();
                }
            }

        }

        return actionDateCalendar;
    }

    public static Calendar getNextPaymentDateForCustomer(Calendar currentPaymentDate, Integer debitDate, List<Date> holidays, Map<Date, List<Date>> debitDateHolidays){

        if(currentPaymentDate == null || debitDate == null){
            logger.error("Both incoming date and Debit Date should not be NULL");
            return currentPaymentDate;
        }

        logger.debug("Current Next Payment Date is %tD",currentPaymentDate.getTime());

        currentPaymentDate.set(Calendar.HOUR_OF_DAY, 0);
        currentPaymentDate.set(Calendar.MINUTE, 0);
        currentPaymentDate.set(Calendar.SECOND, 0);
        currentPaymentDate.set(Calendar.MILLISECOND, 0);

        currentPaymentDate.add(Calendar.MONTH, 1);

        if(debitDate > 0 && debitDate <= 31) {
            if (currentPaymentDate.getActualMaximum(Calendar.DAY_OF_MONTH) < debitDate) {
                currentPaymentDate.set(Calendar.DATE, currentPaymentDate.getActualMaximum(Calendar.DAY_OF_MONTH));
            } else {
                currentPaymentDate.set(Calendar.DATE, debitDate);
            }
        }else{
            logger.debug("Debit Day provided is not withing the range: %s", debitDate);
        }

        // Update the Next payment Date in case of Holidays
        currentPaymentDate = getUserActionDateForWorkingDays(currentPaymentDate, holidays);

        // Update the Next Payment Date in case of Debit Date Holidays
        currentPaymentDate = getNextPaymentDateForCustomerForDebitDateHolidays(currentPaymentDate, debitDateHolidays);

        // Update the Next payment Date in case of Weekend
        if (currentPaymentDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            currentPaymentDate.add(Calendar.DATE, -2);
        }else if (currentPaymentDate.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            currentPaymentDate.add(Calendar.DATE, -1);
        }

        logger.debug("Calculated Next Payment Date is %tD",currentPaymentDate.getTime());

        return currentPaymentDate;
    }

    public static Calendar getNextPaymentDateForCustomerForDebitDateHolidays(Calendar currentPaymentDate, Map<Date, List<Date>> debitDateHolidays){

        if (MapUtils.isEmpty(debitDateHolidays)) {
            logger.debug("No Debit Date Holidays found.");
            return currentPaymentDate;
        }

        logger.debug("Current Next Payment Date is %tD before Debit Date holiday calculation.",currentPaymentDate.getTime());

        for(Map.Entry<Date, List<Date>> iterator : debitDateHolidays.entrySet()){
            for(Date holiday : iterator.getValue()) {

                if(DateTimeComparator.getDateOnlyInstance().compare(currentPaymentDate.getTime(), holiday) == 0){

                    currentPaymentDate.setTime(iterator.getKey());
                    break;
                }
            }
        }

        logger.debug("Calculated Next Payment Date is %tD after Debit Date holiday calculation.",currentPaymentDate.getTime());

        return currentPaymentDate;
    }

    public static void calculateDebitDateHolidays(String parameter, Map<Date, List<Date>> result){

        String[] rangeValues = parameter.split("-|;");
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);

        if(rangeValues.length == 3){

            Date rangeStartDate = DateConvertUtils.asUtilDate(LocalDate.parse(rangeValues[0].trim(), dateFormat));
            Date rangeEndDate = DateConvertUtils.asUtilDate(LocalDate.parse(rangeValues[1].trim(), dateFormat));
            Date debitDate = DateConvertUtils.asUtilDate(LocalDate.parse(rangeValues[2].trim(), dateFormat));

            List<Date> rangeList;

            if(result.containsKey(debitDate)){
                rangeList = result.get(debitDate);
            } else {
                rangeList  = new ArrayList<>();
            }

            // Save starting
            rangeList.add(rangeStartDate);

            Calendar calenderInstance = Calendar.getInstance();
            calenderInstance.setTime(rangeStartDate);

            calenderInstance.set(Calendar.HOUR_OF_DAY, 0);
            calenderInstance.set(Calendar.MINUTE, 0);
            calenderInstance.set(Calendar.SECOND, 0);
            calenderInstance.set(Calendar.MILLISECOND, 0);

            // Save dates between the range
            while (DateTimeComparator.getDateOnlyInstance().compare(calenderInstance.getTime(), rangeEndDate) != 0) {

                calenderInstance.add(Calendar.DATE, 1);
                rangeList.add(calenderInstance.getTime());
            }

            Collections.sort(rangeList, Date::compareTo);

            // Add/Update values in the Map
            result.put(debitDate, rangeList);

        }else{
            logger.error("Debit Date Update parameter: %s, doesn't have 3 values", parameter);
        }
    }

    public static int getWorkingDayDifference(Calendar currentCalendar, Calendar nextPaymentDateCalendar, List<Date> holidays){

        int dayDifference = 0;

        List<Date> holidaysAscending = new ArrayList<>(holidays);

        // Put the collection in Ascending order as it is created in Descending
        Collections.sort(holidaysAscending, Collections.reverseOrder());

        outerLoop:
        while(DateTimeComparator.getDateOnlyInstance().compare(currentCalendar.getTime(), nextPaymentDateCalendar.getTime()) < 0){

            currentCalendar.add(Calendar.DATE, 1);

            if (currentCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                currentCalendar.add(Calendar.DATE, 1);
            } else if (currentCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                currentCalendar.add(Calendar.DATE, 2);
            }

            if (!CollectionUtils.isEmpty(holidaysAscending)) {

                for (Date holiday : holidaysAscending) {

                    if (DateTimeComparator.getDateOnlyInstance().compare(currentCalendar.getTime(), holiday) == 0) {
                        currentCalendar.add(Calendar.DATE, 1);

                        if (currentCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                            currentCalendar.add(Calendar.DATE, 1);
                        } else if (currentCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                            currentCalendar.add(Calendar.DATE, 2);
                        }
                    }

                    if(DateTimeComparator.getDateOnlyInstance().compare(currentCalendar.getTime(), nextPaymentDateCalendar.getTime()) > 0){
                        break outerLoop;
                    }
                }

            }

            dayDifference++;
        }

        return dayDifference;
    }

    public static Calendar subtractWorkingDays(Calendar givenCalender, List<Date> holidays, int daysToAdd){

        for(int iterator = 0; iterator <= daysToAdd; iterator++) {

            givenCalender.add(Calendar.DATE, - (iterator == 0 ? 0 : 1));

            givenCalender = getUserActionDateForWorkingDays(givenCalender, holidays);
        }

        return givenCalender;
    }

    public static Map<String, Map<String, ServiceProfile>> getAllServiceProfilesGroupedByBrand(String dataTableName, Integer entityId){

        Map<String, Map<String, ServiceProfile>> serviceProfiles = new HashMap<>();

        RouteDAS routeDAS = new RouteDAS();
        RouteDTO route = routeDAS.getRoute(entityId, dataTableName);
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSort("id");
        criteria.setMax(999);
        criteria.setDirection(SearchCriteria.SortDirection.ASC);

        criteria.setFilters(new BasicFilter[]{ });

        SearchResultString queryResult = routeDAS.searchDataTable(criteria, route);

        List<List<String>> rows = queryResult.getRows();
        List<String> columnNames = queryResult.getColumnNames();

        rows.forEach(row ->{

            ServiceProfile serviceProfile = getServiceProfile(row,columnNames);

            Map<String, ServiceProfile> brandProfiles;

            brandProfiles = serviceProfiles.get(serviceProfile.getBrandName());

            if(brandProfiles == null){
                brandProfiles = new HashMap<>();

                serviceProfiles.put(serviceProfile.getBrandName(), brandProfiles);
            }

            brandProfiles.put(serviceProfile.getServiceProvider(), serviceProfile);
        });

        return serviceProfiles;
    }

    @Transactional
    public static void updateInvoiceAndPaymentWithFailedStatus(PaymentWS paymentWS, IWebServicesSessionBean webServicesSessionBean){
        if(ArrayUtils.isNotEmpty(paymentWS.getInvoiceIds())) {
            InvoiceBL invoiceBL = new InvoiceBL(paymentWS.getInvoiceIds()[0]);
            invoiceBL.updateInvoiceForFailedPayments(paymentWS);
        }
        else {
            logger.debug("No invoice attached to this payment");
        }

        webServicesSessionBean.updatePayment(paymentWS);
    }

    public static Calendar getOriginalNextPaymentDateForCustomer(Calendar currentPaymentDate, Integer debitDate) {

        if(currentPaymentDate == null || debitDate == null){
            logger.error("Both incoming date and Debit Date should not be NULL");
            return currentPaymentDate;
        }

        logger.debug("Current Original Next Payment Date is %tD",currentPaymentDate.getTime());

        currentPaymentDate.set(Calendar.HOUR_OF_DAY, 0);
        currentPaymentDate.set(Calendar.MINUTE, 0);
        currentPaymentDate.set(Calendar.SECOND, 0);
        currentPaymentDate.set(Calendar.MILLISECOND, 0);

        currentPaymentDate.add(Calendar.MONTH, 1);

        if(debitDate > 0 && debitDate <= 31) {
            if (currentPaymentDate.getActualMaximum(Calendar.DAY_OF_MONTH) < debitDate) {
                currentPaymentDate.set(Calendar.DATE, currentPaymentDate.getActualMaximum(Calendar.DAY_OF_MONTH));
            } else {
                currentPaymentDate.set(Calendar.DATE, debitDate);
            }
        }else{
            logger.debug("Debit Day provided is not withing the range: %s", debitDate);
        }

        logger.debug("Calculated Original Next Payment Date is %tD",currentPaymentDate.getTime());

        return currentPaymentDate;
    }
}
