/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.tools;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.*;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;

public class UploadOrders {

    private JbillingAPI service;

    //global settings
    private String entityName;
    private Integer entityLanguage;
    private Integer entityCurrency;
    private DateTimeFormatter dateFormat;

    //settings specific for UploadOrders;

    private String importFile;

    //indexed for fields
    private int user_id = -1;
    private int first_name = -1;
    private int last_name = -1;
    private int billing_period = -1;
    private int billing_type = -1;
    private int status = -1;
    private int active_since = -1;
    private int active_until = -1;
    private int product_id = -1;
    private int product_description = -1;
    private int price = -1;
    private int quantity = -1;
    private int own_invoice = -1;
    private int use_default_pricing = -1;

    // local cache
    Map<String, Integer> usernameUserIdMap = new HashMap<String, Integer>();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Upload properties file required");
            System.exit(1);
            return;
        }
        (new UploadOrders()).upload(args);
    }

    public void upload(String[] args){
        usernameUserIdMap.clear();

        //opens the API service
        service = createAPIService();
        if (null == service) {
            System.err.println("Aborting Upload of Orders");
            System.exit(1);
            return;
        }

        String uploadPropertyFilePath = args[0];

        //read the upload properties file
        Properties properties = readProperties(uploadPropertyFilePath);
        if (null == properties) {
            System.err.println("Problem reading the properties file");
            System.exit(1);
            return;
        }

        //initialize global settings and upload-order specific settings
        initGlobalSettings(properties);
        initSpecificSettings(properties);

        //check if the data file should be overriden
        //it helps with unit testing
        if(args.length > 1){
            String overrideDataFile = args[1];
            if(null != overrideDataFile &&
                    StringUtils.isNotBlank(overrideDataFile)){
                importFile = overrideDataFile;
            }
        }

        //do the processing
        processDataFile();
    }

    private void processDataFile(){
        BufferedReader importFileReader = null;
        try {
            importFileReader = new BufferedReader(new FileReader(importFile));
            resolveColumnIndices(importFileReader);
            processRecords(importFileReader);
            importFileReader.close();
        } catch (Exception e) {
            System.err.println("Exception while processing indices or processing records. Aborting.");
            e.printStackTrace();
            //make sure the file is closed and then return
            try {
                importFileReader.close();
            } catch (IOException ioe) {}
            return;
        }
    }

    private Properties readProperties(String propertyFileName){
        Properties properties = new Properties();
        InputStream inputStream = null;
        try {
            File propertyFile = new File(propertyFileName);
            inputStream = new FileInputStream(propertyFile);
            properties.load(inputStream);
        } catch (FileNotFoundException fnf) {
            System.out.println("Properties file not found exception. Aborting");
            return null;
        } catch (IOException io) {
            System.out.println("IO Exception while trying to read properties file. Aborting.");
            return null;
        } finally {
            //make sure the input stream is closed
            try{
                inputStream.close();
            } catch (IOException ioe){}
        }
        return properties;
    }

    private void initGlobalSettings(Properties properties) {
        entityName = properties.getProperty("entity_name");
        entityLanguage = Integer.valueOf((String) properties.getProperty("entity_language"));
        entityCurrency = Integer.valueOf((String) properties.getProperty("entity_currency"));
        dateFormat = DateTimeFormat.forPattern(properties.getProperty("date_format").trim());
    }

    private void initSpecificSettings(Properties properties) {
        importFile = properties.getProperty("order_import_file");
    }

    private JbillingAPI createAPIService() {
        try{
            return JbillingAPIFactory.getAPI();
        } catch (JbillingAPIException jbapiex) {
            System.err.println("jb api error:");
            jbapiex.printStackTrace();
        } catch (IOException ioe) {
            System.err.println("IO error:");
            ioe.printStackTrace();
        }
        return null;
    }

    private void resolveColumnIndices(BufferedReader reader) throws IOException {
        String header = reader.readLine();
        String columns[] = header.split(",");
        for (int f = 0; f < columns.length; f++) {
            String columnName = columns[f].trim();
            // scan for the columns
            if (columnName.equalsIgnoreCase("userName")) {
                user_id = f;
            } else if (columnName.equalsIgnoreCase("FirstName")) {
                first_name = f;
            } else if (columnName.equalsIgnoreCase("LastName")) {
                last_name = f;
            } else if (columnName.equalsIgnoreCase("BillingPeriod")) {
                billing_period = f;
            } else if (columnName.equalsIgnoreCase("BillingType")) {
                billing_type = f;
            } else if (columnName.equalsIgnoreCase("Status")) {
                status = f;
            } else if (columnName.equalsIgnoreCase("ActiveSince")) {
                active_since = f;
            } else if (columnName.equalsIgnoreCase("ActiveUntill")) {
                active_until = f;
            } else if (columnName.equalsIgnoreCase("ProductId")) {
                product_id = f;
            } else if (columnName.equalsIgnoreCase("ProductDescription")) {
                product_description = f;
            } else if (columnName.equalsIgnoreCase("Price")) {
                price = f;
            } else if (columnName.equalsIgnoreCase("Quantity")) {
                quantity = f;
            } else if (columnName.equalsIgnoreCase("UseDefaultPricing")) {
                use_default_pricing = f;
            } else if (columnName.equalsIgnoreCase("OwnInvoice")) {
                own_invoice= f;
            }
        }
    }

    private void processRecords(BufferedReader reader) throws IOException {
        String record = null;
        int rowCount;
        int ordersCreated;
        for (rowCount = 0, ordersCreated = 0; (record = reader.readLine()) != null; rowCount++) {
            System.out.println(rowCount + ". Processing row: " + record);

            try {
                if (!record.trim().isEmpty()) {

                    String fields[] = record.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                    // define the username
                    String username = "";
                    if (user_id >= 0) {
                        username += fields[user_id].trim();
                    } else {
                        // build from first/last name
                    }

                    //resolve the user id from username
                    Integer userId = usernameUserIdMap.get(username);
                    if (null == userId && null != username && !username.trim().isEmpty()) {
                        try {
                            userId = service.getUserId(username);
                            usernameUserIdMap.put(username, userId);
                        } catch (SessionInternalError userNotExist) {
                            System.out.println(rowCount + ". Error retrieving the user id from the username");
                        }
                    }

                    if (userId != null) {
                        // user exists create the order
                        OrderWS newOrder = new OrderWS();
                        newOrder.setUserId(userId);

                        if (billing_type >=0) {
                            newOrder.setBillingTypeId(parseInteger(fields[billing_type].trim(), "billing_type"));
                        }

                        if (billing_period >= 0) {
                            newOrder.setPeriod(parseInteger(fields[billing_period].trim(), "billing_period"));
                        }
                        
                        //WebServicesSessionSpringBean.create() we handling the status
                        if (status >= 0) {
                            //newOrder.setStatusId(parseInteger(fields[status].trim(), "status"));
                        	OrderStatusWS orderStatusWS = new OrderStatusWS();
                        	orderStatusWS.setId(parseInteger(fields[status].trim(), "status"));
                        	newOrder.setOrderStatusWS(orderStatusWS);
                        }

                        if (active_since >= 0) {
                            newOrder.setActiveSince(parseDate(dateFormat, fields[active_since].trim(), "active_since"));
                        }
                        if (active_until >= 0) {
                            newOrder.setActiveUntil(parseDate(dateFormat, fields[active_until].trim(), "active_until"));
                        }
                        if (own_invoice >= 0) {
                            newOrder.setOwnInvoice(parseInteger(fields[own_invoice].trim(), "own_invoice"));
                        }

                        newOrder.setCurrencyId(entityCurrency);

                        Integer itemId = null;
                        if (product_id >= 0) {
                            itemId = parseInteger(fields[product_id].trim(), "product_id");
                        }

                        ItemDTOEx item = null;

                        if (item == null) {
                            try {
                                item = service.getItem(itemId, userId, null);
                            } catch (SessionInternalError itemNotExist) {
                                System.err.print(rowCount + ". Problem finding the requested item. Item id: "+itemId+". Skipping record.");
                                continue;
                            }
                        }

                        OrderLineWS lines[] = new OrderLineWS[1];
                        OrderLineWS line;

                        line = new OrderLineWS();
                        line.setTypeId(com.sapienter.jbilling.server.util.Constants.ORDER_LINE_TYPE_ITEM);
                        if (quantity >= 0) {
                            line.setQuantity(parseInteger(fields[quantity].trim(),"quantity"));
                        }
                        if (product_id >= 0) {
                            line.setItemId(parseInteger(fields[product_id].trim(), "product_id"));
                        }
                        if (use_default_pricing >= 0) {
                            boolean useDefaultPricing = parseInteger(fields[use_default_pricing].trim(), "use_default_pricing") != 0;
                            if (useDefaultPricing) {
                                line.setUseItem(Boolean.TRUE);
                            } else {
                                if (price >= 0) {
                                    line.setPrice(new BigDecimal(fields[price].trim()));
                                    line.setAmount(line.getPriceAsDecimal().multiply(line.getQuantityAsDecimal()));
                                }
                                if (product_description >= 0) {
                                    line.setDescription(StringUtils.isNotBlank(fields[product_description].trim())
                                            ? fields[product_description].trim() : item.getDescription());
                                }
                                line.setUseItem(Boolean.FALSE);
                            }
                        } else {
                            line.setUseItem(Boolean.TRUE);
                        }


                        lines[0] = line;
                        newOrder.setOrderLines(lines);
                        List<OrderChangeStatusWS> changeStatuses = Arrays.asList(service.getOrderChangeStatusesForCompany());
                        OrderChangeStatusWS applyStatus = null;
                        for (OrderChangeStatusWS status : changeStatuses) {
                            if (status.getApplyToOrder().equals(ApplyToOrder.YES)) {
                                applyStatus = status;
                                break;
                            }
                        }
                        try {

                            Integer orderId = service.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, applyStatus != null ? applyStatus.getId() : null));
                            ordersCreated++;
                            System.out.println(rowCount + ". order created with id:" + orderId);
                        } catch (Exception internalError) {
                            System.out.print(rowCount + ". Ignoring record: \"" + record + "\" due to internal error: ");
                            Throwable error = internalError;
                            while (null != error) {
                                System.err.print(error.getMessage() + " -> ");
                                error = error.getCause();
                            }
                            System.err.println();
                        }

                    }
                } else {
                    System.out.println(rowCount + ". Ignoring record ");
                } //if (record not empty)
            } catch(Exception e){
                System.err.println(rowCount + ". Error processing record #"+ rowCount +". Message: " + e.getMessage());
            }
        } // for all records
        System.out.println("Processed rows: " + rowCount +", orders created: " + ordersCreated);
    }

    private Date parseDate(DateTimeFormatter dateFormat, String value, String fieldName)
            throws ParseException {
        try {
            return StringUtils.isNotBlank(value) ? dateFormat.parseDateTime(value).toDate() : null;
        } catch (IllegalArgumentException e) {
           System.err.println("date parse exception while trying to parse value:"
                   + value + ", for field name:" + fieldName);
            throw e;
        }
    }

    private Integer parseInteger(String value, String fieldName)
            throws NumberFormatException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe){
            System.err.println("number format exception while trying to parse value:"
                    + value + ", for field name:"+ fieldName);
            throw nfe;
        }
    }

}