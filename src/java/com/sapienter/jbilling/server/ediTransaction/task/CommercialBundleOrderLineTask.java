package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.event.NewOrderEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pricing.RouteBeanFactory;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.search.SearchResult;
import jbilling.RouteService;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;

import java.math.BigDecimal;
import java.util.*;


public class CommercialBundleOrderLineTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CommercialBundleOrderLineTask.class));
    /*Constant Values*/
    private RouteService routeService = Context.getBean(Context.Name.ROUTE_SERVICE);
    public static final ParameterDescription COC_PRODUCT_NAME  =
            new ParameterDescription("COC product name ", false, ParameterDescription.Type.STR);

    public static final ParameterDescription TOC_PRODUCT_NAME  =
            new ParameterDescription("TOC product name ", false, ParameterDescription.Type.STR);
    public static final ParameterDescription COC_DATA_TABLE  =
            new ParameterDescription("COC Data Table", false, ParameterDescription.Type.STR);
    public static final ParameterDescription TOC_DATA_TABLE  =
            new ParameterDescription("TOC Data Table", false, ParameterDescription.Type.STR);


    private static final Class<Event> events[] = new Class[]{
            NewOrderEvent.class
    };

    //initializer for pluggable params
    {
        descriptions.add(COC_PRODUCT_NAME);
        descriptions.add(TOC_PRODUCT_NAME);
        descriptions.add(COC_DATA_TABLE);
        descriptions.add(TOC_DATA_TABLE);
    }




    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {

        if(!(event instanceof  NewOrderEvent)){
            return;
        }

        NewOrderEvent meterReadOrderCreationEvent=(NewOrderEvent)event;
        OrderDTO orderDTO=meterReadOrderCreationEvent.getOrder();

                /*run this plugin only one time order which did not have plan*/
        if(orderDTO.getOrderPeriod().getValue()!=null){
            return;
        }

        //if order containing plan then will not include obligation charges
        Optional<OrderLineDTO> orderLine=orderDTO.getLines().stream().filter((OrderLineDTO orderLineDTO)-> orderLineDTO.getItem()!=null && orderLineDTO.getItem().isPlan()).findFirst();
        if(orderLine.isPresent()){
            return;
        }

        List<OrderDTO> subscriptions = new OrderDAS()
                .findByUserSubscriptions(meterReadOrderCreationEvent.getOrder().getUser().getId());

        String productCode = subscriptions.get(0).getLines().get(0).getItem().getInternalNumber();
        if (!parameters.containsValue(productCode)) {
            return;
        }

        try{
            calculateObligationCharges(orderDTO);
        }catch (SessionInternalError e){
            orderDTO.setNotes("Unable to add Obligation charges : "+e.getMessage());
        }

        new OrderDAS().save(orderDTO);

    }

    private void calculateObligationCharges(OrderDTO orderDTO){

        MetaFieldValue zoneMetaField=orderDTO.getUser().getCustomer().getMetaField(FileConstants.CUSTOMER_ZONE_META_FIELD_NAME);
        if(zoneMetaField==null || zoneMetaField.getValue()==null){
            throw new SessionInternalError("Customer should belongs to a Zone");
        }

        String capacityObligationProductName=parameters.get(COC_PRODUCT_NAME.getName());
        String transmissionObligationProductName=parameters.get(TOC_PRODUCT_NAME.getName());
        LOG.debug("COC ProductName "+capacityObligationProductName);
        LOG.debug("TOC ProductName "+transmissionObligationProductName);


        MetaFieldValue capacityObligationMetaField=orderDTO.getUser().getCustomer().getMetaField(FileConstants.CUSTOMER_PEAK_LOAD_CONTRIBUTION_META_FIELD_NAME);
        if(capacityObligationMetaField!=null && capacityObligationMetaField.getValue()!=null && capacityObligationProductName !=null){
            LOG.debug("Calculating capacity obligation charge ");

            BigDecimal capacityObligationCharge=calculateCapacityObligationCharge(orderDTO, zoneMetaField.getValue().toString());
            createOrderLine(orderDTO, capacityObligationProductName, capacityObligationCharge);
        }

        MetaFieldValue transmissionObligationMetaField=orderDTO.getUser().getCustomer().getMetaField(FileConstants.CUSTOMER_PEAK_LOAD_CONTRIBUTION_META_FIELD_NAME);
        if(transmissionObligationMetaField !=null && transmissionObligationMetaField.getValue()!=null && transmissionObligationProductName !=null){
            LOG.debug("Calculating transmission obligation charge ");
            BigDecimal transmissionObligationChange=calculateTransmissionObligationCharge(orderDTO, zoneMetaField.getValue().toString());
            createOrderLine(orderDTO, transmissionObligationProductName, transmissionObligationChange);
        }
    }

    private BigDecimal calculateCapacityObligationCharge(OrderDTO orderDTO, String zone){
        MetaFieldValue capacityObligationMetaField=orderDTO.getUser().getCustomer().getMetaField(FileConstants.CUSTOMER_PEAK_LOAD_CONTRIBUTION_META_FIELD_NAME);

        LOG.debug("Capacity Obligation meta-field : "+capacityObligationMetaField);

        if(capacityObligationMetaField==null){
            throw new SessionInternalError("Customer should have "+FileConstants.CUSTOMER_PEAK_LOAD_CONTRIBUTION_META_FIELD_NAME);
        }
        BigDecimal capacityObligation=(BigDecimal)capacityObligationMetaField.getValue();
        BigDecimal capacityObligationCharge=BigDecimal.ZERO;
        capacityObligationCharge=calculateCOC(orderDTO.getActiveSince(), orderDTO.getActiveUntil(), capacityObligationCharge, zone, capacityObligation);
        LOG.debug("Total Capacity obligation charge : " + capacityObligationCharge);
        return capacityObligationCharge;
    }

    private BigDecimal calculateTransmissionObligationCharge(OrderDTO orderDTO, String zone){
        MetaFieldValue transmissionContributionMetaField=orderDTO.getUser().getCustomer().getMetaField(FileConstants.CUSTOMER_TRANSMISSION_CONTRIBUTION_META_FIELD_NAME);
        LOG.debug("Customer transmission Obligation "+transmissionContributionMetaField);
        if(transmissionContributionMetaField==null){
            LOG.error("Customer should have transmission obligation");
            throw new SessionInternalError("Customer should have "+FileConstants.CUSTOMER_TRANSMISSION_CONTRIBUTION_META_FIELD_NAME);
        }
        BigDecimal transmissionContribution=(BigDecimal)transmissionContributionMetaField.getValue();
        BigDecimal transmissionObligationChange=BigDecimal.ZERO;
        transmissionObligationChange=calculateTOC(orderDTO.getActiveSince(), orderDTO.getActiveUntil(), transmissionObligationChange, zone, transmissionContribution);
        LOG.debug("Total Transmission obligation charge : " + transmissionObligationChange);
        return transmissionObligationChange;
    }


    private void createOrderLine(OrderDTO order,String productCode, BigDecimal amount){
        ItemDTO item =new ItemDAS().findItemByInternalNumber(productCode, getEntityId());
        if(item==null){
            LOG.error("No product find for product code : "+productCode);
            throw new SessionInternalError("No product find for product code : "+productCode);
        }
        OrderLineDTO line = new OrderLineDTO();
        line.setDescription(item.getDescription());
        line.setItemId(item.getId());
        line.setQuantity(1);
        line.setPrice(amount);
        line.setAmount(amount);
        line.setTypeId(com.sapienter.jbilling.server.util.Constants.ORDER_LINE_TYPE_ITEM);
        line.setPurchaseOrder(order);
        line.setCreateDatetime(TimezoneHelper.serverCurrentDate());
        order.getLines().add(line);
    }

    /*
    * this method return the total Transmission obligation charge
    * */
    private BigDecimal calculateTOC(Date startDate, Date endDate, BigDecimal transmissionObligationChange, String zone, BigDecimal transmissionContribution){
        DateTime startDateTime = new DateTime(startDate);
        DateTime endDateTime = new DateTime(endDate);
        String monthAndYear=findMonth(startDateTime);

        String tocDataTable=parameters.get(TOC_DATA_TABLE.getName());

        BigDecimal transmissionPriceValue=new BigDecimal(getValueFromDataTable(getEntityId(), monthAndYear, tocDataTable, "transmission_price", zone));

        if(startDateTime.getMonthOfYear()!=endDateTime.getMonthOfYear()){
            DateTime firstDayOfNextMonth=startDateTime.plusMonths(1).withDayOfMonth(1);
            BigDecimal days=new BigDecimal(Days.daysBetween(startDateTime.toLocalDate(), firstDayOfNextMonth.toLocalDate()).getDays());

            transmissionObligationChange=transmissionObligationChange.add(transmissionPriceValue.multiply(days).multiply(transmissionContribution));
            transmissionObligationChange=calculateTOC(firstDayOfNextMonth.toDate(), endDateTime.toDate(), transmissionObligationChange, zone, transmissionContribution);
        }else{
            BigDecimal days=new BigDecimal(Days.daysBetween(startDateTime.toLocalDate(), endDateTime.toLocalDate()).getDays()+1);
            transmissionObligationChange=transmissionObligationChange.add(transmissionPriceValue.multiply(days).multiply(transmissionContribution));
        }
        return transmissionObligationChange;
    }

    private String findMonth(DateTime startDateTime){
        String month=startDateTime.getMonthOfYear()<10 ? "0"+startDateTime.getMonthOfYear():startDateTime.getMonthOfYear()+"";
        return startDateTime.getYear()+"-"+month;
    }

    /*
    * this method return the total capacity obligation charge
    * */
    private BigDecimal calculateCOC(Date startDate, Date endDate, BigDecimal capacityObligationCharge, String zone, BigDecimal capacityObligation){
        DateTime startDateTime = new DateTime(startDate);
        DateTime endDateTime = new DateTime(endDate);
        String monthAndYear=findMonth(startDateTime);

        LOG.debug("Finding data for month "+monthAndYear);

        String cocDataTable=parameters.get(COC_DATA_TABLE.getName());
        BigDecimal zsfValue=new BigDecimal(getValueFromDataTable(getEntityId(), monthAndYear, cocDataTable, "zsf", zone));
        BigDecimal almValue=new BigDecimal(getValueFromDataTable(getEntityId(), monthAndYear, cocDataTable, "alm", zone));
        BigDecimal fprValue=new BigDecimal(getValueFromDataTable(getEntityId(), monthAndYear, cocDataTable, "fpr", zone));
        BigDecimal capacityPriceValue=new BigDecimal(getValueFromDataTable(getEntityId(), monthAndYear, cocDataTable, "capacity_price", zone));


        if(startDateTime.getMonthOfYear()!=endDateTime.getMonthOfYear()){
            DateTime firstDayOfNextMonth=startDateTime.plusMonths(1).withDayOfMonth(1);
            BigDecimal days=new BigDecimal(Days.daysBetween(startDateTime.toLocalDate(), firstDayOfNextMonth.toLocalDate()).getDays());
            capacityObligationCharge=capacityObligationCharge.add(zsfValue.multiply(almValue).multiply(fprValue).multiply(capacityPriceValue).multiply(days).multiply(capacityObligation));
            capacityObligationCharge=calculateCOC(firstDayOfNextMonth.toDate(), endDateTime.toDate(), capacityObligationCharge, zone, capacityObligation);
        }else{
            BigDecimal days=new BigDecimal(Days.daysBetween(startDateTime.toLocalDate(), endDateTime.toLocalDate()).getDays()+1);
            capacityObligationCharge=capacityObligationCharge.add(zsfValue.multiply(almValue).multiply(fprValue).multiply(capacityPriceValue).multiply(days).multiply(capacityObligation));
        }
        return capacityObligationCharge;
    }

    private String getValueFromDataTable(Integer companyId, String month, String tableName, String columnName, String zone){


        RouteDTO routeDTO=new RouteDAS().getRoute(companyId, tableName);

        if(routeDTO==null){
            throw new SessionInternalError("Configuration Issue: No data table configured with name "+tableName);
        }

        RouteBeanFactory factory = new RouteBeanFactory(routeDTO);
        List<String> columnNames = factory.getTableDescriptorInstance().getColumnsNames();

        Map<String, String> map = new HashMap<>();
        map.put("Month",month);
        map.put("Zone", zone+"");

        SearchResult<String> result = routeService.getFilteredRecords(routeDTO.getId(), map);

        if(result.getRows().size()==0){
            throw new SessionInternalError("No record found for Month( "+month+") and zone ("+zone+") in data table "+tableName );
        }

        Integer searchNameIdx = columnNames.indexOf(columnName);

        if(searchNameIdx.equals(-1)){
            throw new SessionInternalError("Date Table "+tableName +" have not "+columnName+" column ");
        }

        return result.getRows().get(0).get(searchNameIdx);
    }



}
