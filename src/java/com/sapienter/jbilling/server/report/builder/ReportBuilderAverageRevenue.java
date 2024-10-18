package com.sapienter.jbilling.server.report.builder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
	

public class ReportBuilderAverageRevenue {
	
	private static final Logger logger = LoggerFactory.getLogger(ReportBuilderAverageRevenue.class);
	// these are the fields defined in the report
	private static final String CUSTOMER_ID     = "customer_id";
	private static final String CUSTOMER_NAME   = "customer_name";
	private static final String ACCOUNT_NAME    = "account_name";
	private static final String ACCOUNT_STATUS  = "account_status";
	private static final String INVOICE_DATE    = "invoice_date";
	private static final String MONTHLY_REVENUE = "monthly_revenue"; // customer contribution to the month revenue
	
	 /**
     * Process invoices list
     * @param dataList revenue invoices
     */
    public List<Map<String, ?>> getData(List<AverageRevenueData> dataList) {
    	logger.debug("ENTER << getData");
    	if(dataList.isEmpty()){
    		logger.debug("The list of data to calculate average revenue is empty");
    		return new ArrayList<>();
    	}
    	//
    	ArrayList<Map<String, ?>> reportRecordList = new ArrayList<>(dataList.size()/2);
    	AverageRevenueData currentCustomerData =  dataList.get(0);
        int currentInvoiceId = currentCustomerData.getInvoiceId();
        BigDecimal customerRevenue = currentCustomerData.getInvoiceAmount();
        for (AverageRevenueData data : dataList) {
        	int invoiceId = data.getInvoiceId();
        	if(invoiceId!=currentInvoiceId){ // it's a new invoice
        		reportRecordList.add(toReportRecord(currentCustomerData, customerRevenue));
        		//
        		currentCustomerData = data;
        		customerRevenue = currentCustomerData.getInvoiceAmount();
        		currentInvoiceId = currentCustomerData.getInvoiceId();
        	}//if
            
        	if(isTaxLine(data)){
        		customerRevenue = customerRevenue.subtract(data.getInvoiceLineAmount());
        	}
        }//for
		// add the last customer-revenue
		reportRecordList.add(toReportRecord(currentCustomerData, customerRevenue));
        //
        reportRecordList.trimToSize();
        sortRecords(reportRecordList);
        //
        logger.debug("There are: {} records for the average-revenue report", reportRecordList.size());
        
        logger.debug("EXIT >>> getData");
        return reportRecordList;
    }
	//------------------------------------------------------------------------------------
    
	private Map<String, ?> toReportRecord(AverageRevenueData src, BigDecimal customerRevenue){
		Map<String, Object> record = new HashMap<>(6);
		record.put(CUSTOMER_ID, src.getUserId());
		record.put(CUSTOMER_NAME, formatCustomerName(src.getCustomerName()));
		record.put(ACCOUNT_NAME,  src.getCustomerAccountName());
		record.put(ACCOUNT_STATUS, src.getCustomerAccountStatus());
		record.put(INVOICE_DATE, src.getInvoiceDate());
		record.put(MONTHLY_REVENUE, customerRevenue);
		
		return record;	
	}
	//------------------------------------------------------------------------------------
     
    private String formatCustomerName(String userName){
    	if(StringUtils.isBlank(userName)){
    		return userName;
    	}
    	return userName.replaceAll("(-|_)", " ");
    }
    //-------------------------------------------------------------------------------------
    
    private boolean isTaxLine(AverageRevenueData data){
    	String invoiceLineType = data.getInvoiceLineType();
    	invoiceLineType = invoiceLineType.toLowerCase();
        if(StringUtils.isNotBlank(invoiceLineType) && invoiceLineType.toLowerCase().contains("tax")){
        	return true;
        }	
    	return false;	
    }	
    //-------------------------------------------------------------------------------------
    /**
     * Sort the give list of maps where each represents a record in the target report
     * 
     * @param srcList
     * @return the given list sorted with respect to account-names in descending order, and customer-IDs in ascending order
     */
    private void sortRecords(List<Map<String,?>> srcList){
    	Comparator<Map<String, ?>> reportRecordComparator = new Comparator<Map<String, ?>>(){
			@Override
			public int compare(Map<String, ?> o1, Map<String, ?> o2) {
				String accountName1 = (String) o1.get(ACCOUNT_NAME);
				String accountName2 = (String) o2.get(ACCOUNT_NAME);
				int comparedAccounts = accountName1.compareTo(accountName2);
				if(comparedAccounts==0){
					Integer customerId1 = (Integer) o1.get(CUSTOMER_ID);
					Integer customerId2 = (Integer) o2.get(CUSTOMER_ID);
					return customerId1.compareTo(customerId2);
				}
				
				return comparedAccounts * -1;
			}
    			
    	};
    	
    	srcList.sort(reportRecordComparator);
        
    }
    //-------------------------------------------------------------------------------------
    
    
}
