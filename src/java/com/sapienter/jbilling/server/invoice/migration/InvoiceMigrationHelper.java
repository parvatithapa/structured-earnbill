package com.sapienter.jbilling.server.invoice.migration;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;

public class InvoiceMigrationHelper {

    private final static String 		Test 							=  "Test";
    private static final int 			XLSX_SHEET_NUMBER 				=  0;
    private static Logger 				logger 							=  LoggerFactory.getLogger(InvoiceMigrationHelper.class);
    private static final Integer		CUSTOM 							=  -1;
    private static boolean 				printOnConsole 					=  true;
    private static final Integer 		ENTERED 						=  4;
    public 	static final Integer 		AUS_DOLLAR 						=  11;


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static List<Map<Integer, SPCInvoice>> readXlsxSheet(final String inputFileName) {
        List<Map<Integer, SPCInvoice>> spcInvoiceMapList = new ArrayList<Map<Integer,SPCInvoice>>();
        Map<Integer, SPCInvoice> spcInvoiceMapOne = new HashMap();
        Map<Integer, SPCInvoice> spcInvoiceMapTwo = new HashMap();
        Map<Integer, SPCInvoice> spcInvoiceMapThree = new HashMap();

        try (Workbook spcInvoiceWorkbook = WorkbookFactory.create(new File(inputFileName)))
        {
            List<SPCInvoice> spcCustomerList = new ArrayList();

            int numberOfSubSheets = spcInvoiceWorkbook.getNumberOfSheets();
            if (printOnConsole )
                logger.debug("Total Number of Sheets : {} reading sheet number : {} ", numberOfSubSheets,XLSX_SHEET_NUMBER);
            Sheet spcDatatypeSheet = spcInvoiceWorkbook.getSheetAt(XLSX_SHEET_NUMBER);
            Iterator<Row> spcDataIterator = spcDatatypeSheet.iterator();
            while (spcDataIterator.hasNext()) {
                Row currentRow = spcDataIterator.next();
                SPCInvoice spcInvoiceOne = new SPCInvoice();
                SPCInvoice spcInvoiceTwo = new SPCInvoice();
                SPCInvoice spcInvoiceThree = new SPCInvoice();
                String crmNumber = getCellValue(currentRow.getCell(2));
                if (null != crmNumber && !crmNumber.isEmpty() && !crmNumber.equalsIgnoreCase("crmAccountNumber")) {
                    spcInvoiceOne.setCrmAccountNumber(crmNumber);
                    spcInvoiceOne.setBillRunTiming(currentRow.getCell(1).toString());
                    spcInvoiceOne.setUserId(Integer.valueOf(getCellValue(currentRow.getCell(3))));
                    spcInvoiceOne.setOpeningBalance(new BigDecimal(getCellValue(currentRow.getCell(5))));
                    spcInvoiceOne.setPayments((new BigDecimal(getCellValue(currentRow.getCell(6)))));
                    spcInvoiceOne.setAdjustments(new BigDecimal(getCellValue(currentRow.getCell(7))));
                    spcInvoiceOne.setNewCharges((new BigDecimal(getCellValue(currentRow.getCell(8)))));
                    spcInvoiceOne.setBalance(new BigDecimal(getCellValue(currentRow.getCell(9))));
                    spcInvoiceOne.setCheckBalance(new BigDecimal(getCellValue(currentRow.getCell(10))));
                    spcCustomerList.add(spcInvoiceOne);
                    spcInvoiceMapOne.put(spcInvoiceOne.getUserId(), spcInvoiceOne);
                    System.out.println(spcInvoiceOne);
                }

                if (null != crmNumber && !crmNumber.isEmpty() && !crmNumber.equalsIgnoreCase("crmAccountNumber")) {
                    spcInvoiceTwo.setCrmAccountNumber(crmNumber);
                    spcInvoiceTwo.setBillRunTiming(currentRow.getCell(1).toString());
                    spcInvoiceTwo.setUserId(Integer.valueOf(getCellValue(currentRow.getCell(3))));
                    spcInvoiceTwo.setOpeningBalance(new BigDecimal(getCellValue(currentRow.getCell(12))));
                    spcInvoiceTwo.setPayments((new BigDecimal(getCellValue(currentRow.getCell(13)))));
                    spcInvoiceTwo.setAdjustments(new BigDecimal(getCellValue(currentRow.getCell(14))));
                    spcInvoiceTwo.setNewCharges((new BigDecimal(getCellValue(currentRow.getCell(15)))));
                    spcInvoiceTwo.setBalance(new BigDecimal(getCellValue(currentRow.getCell(16))));
                    spcInvoiceTwo.setCheckBalance(new BigDecimal(getCellValue(currentRow.getCell(17))));
                    spcCustomerList.add(spcInvoiceTwo);
                    spcInvoiceMapTwo.put(spcInvoiceTwo.getUserId(), spcInvoiceTwo);
                    System.out.println(spcInvoiceTwo);
                }

                if (null != crmNumber && !crmNumber.isEmpty() && !crmNumber.equalsIgnoreCase("crmAccountNumber")) {
                spcInvoiceThree.setCrmAccountNumber(crmNumber);
                spcInvoiceThree.setBillRunTiming(currentRow.getCell(1).toString());
                spcInvoiceThree.setUserId(Integer.valueOf(getCellValue(currentRow.getCell(3))));
                spcInvoiceThree.setOpeningBalance(new BigDecimal(getCellValue(currentRow.getCell(19))));
                spcInvoiceThree.setPayments((new BigDecimal(getCellValue(currentRow.getCell(20)))));
                spcInvoiceThree.setAdjustments(new BigDecimal(getCellValue(currentRow.getCell(21))));
                spcInvoiceThree.setNewCharges((new BigDecimal(getCellValue(currentRow.getCell(22)))));
                spcInvoiceThree.setBalance(new BigDecimal(getCellValue(currentRow.getCell(23))));
                spcInvoiceThree.setCheckBalance(new BigDecimal(getCellValue(currentRow.getCell(24))));
                spcCustomerList.add(spcInvoiceThree);
                spcInvoiceMapThree.put(spcInvoiceThree.getUserId(), spcInvoiceThree);
                System.out.println(spcInvoiceThree);
                }
            }
            spcInvoiceMapList.add(spcInvoiceMapOne);
            spcInvoiceMapList.add(spcInvoiceMapTwo);
            spcInvoiceMapList.add(spcInvoiceMapThree);
        } catch (IOException | EncryptedDocumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return spcInvoiceMapList;
    }

    private static String getCellValue(Cell currentCell) {
        String returnValue = null;

        if (null != currentCell && currentCell.getCellType() == CellType.FORMULA) {
            System.out.println("Formula is::" + currentCell.getCellFormula());
            switch (currentCell.getCachedFormulaResultType()) {
                case NUMERIC:
                returnValue = currentCell.getNumericCellValue() + "";
                System.out.println("Last numeric value evaluated as: " + returnValue);
                break;
                case STRING:
                returnValue = currentCell.getRichStringCellValue() + "";
                System.out.println("Last string evaluated as \"" + returnValue + "\"");
                break;
            }
        } else {
            DataFormatter formatter = new DataFormatter();
            returnValue = formatter.formatCellValue(currentCell).trim().contains(" ") ? formatter.formatCellValue(currentCell).trim()
                    .replace(" ", "")
                    : formatter.formatCellValue(currentCell).trim();
                    if (printOnConsole)
                        System.out.println("ApachePOIExcelRead.getCellValue()-------------------------" + returnValue + "-------"
                                + ((null != currentCell) ? currentCell.getColumnIndex() : 0));
        }
        return returnValue;
    }


    public static InvoiceLineDTO buildInvoiceLine(Integer userId, Integer itemId) {

        InvoiceLineDTO invoiceLineDTO = new InvoiceLineDTO();
        invoiceLineDTO.setDescription("line desc");
        invoiceLineDTO.setItemId(itemId);
        invoiceLineDTO.setPercentage(0); // should not be percentage product
        invoiceLineDTO.setQuantity(BigDecimal.ONE);
        invoiceLineDTO.setSourceUserId(userId);
        return invoiceLineDTO;
    }

    public static PaymentInformationWS createPaymentInstrument(String Test, Integer paymentMethodId) {
        PaymentInformationWS cc = new PaymentInformationWS();
        cc.setPaymentMethodTypeId(paymentMethodId);
        cc.setProcessingOrder(1);
        cc.setPaymentMethodId(-1);

        List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
        addMetaField(metaFields, Test, false, true, DataType.CHAR, 1, Test.toCharArray());

        cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

        return cc;
    }

    private static void addMetaField(List<MetaFieldValueWS> metaFields,
            String fieldName, boolean disabled, boolean mandatory,
            DataType dataType, Integer displayOrder, Object value) {
        MetaFieldValueWS ws = new MetaFieldValueWS();
        ws.setFieldName(fieldName);
        ws.getMetaField().setDisabled(disabled);
        ws.getMetaField().setMandatory(mandatory);
        ws.getMetaField().setDataType(dataType);
        ws.getMetaField().setDisplayOrder(displayOrder);
        ws.setValue(value);

        metaFields.add(ws);
    }

    public static PaymentWS getPaymentForInvoice(Integer userId, SPCInvoice spcInvoice,Integer paymentMethodId) {
        PaymentWS paymentWS = new PaymentWS();
        if (spcInvoice.getPayments().compareTo(BigDecimal.ZERO) < 0) {
            paymentWS.setAmount(spcInvoice.getPayments().negate());	
        }else {
            paymentWS.setAmount(spcInvoice.getPayments());
        }
        System.out.println("Payments :  "+spcInvoice.getPayments());
        paymentWS.setBalance(BigDecimal.ZERO);
        paymentWS.setIsRefund(0);
        paymentWS.setMethodId(CUSTOM);
        paymentWS.setResultId(ENTERED);
        paymentWS.setCurrencyId(AUS_DOLLAR);
        paymentWS.setUserId(userId);
        paymentWS.setPaymentNotes("Notes");
        paymentWS.setPaymentPeriod(1);
        paymentWS.getPaymentInstruments().add(InvoiceMigrationHelper.createPaymentInstrument("Test",paymentMethodId));
        return paymentWS;
    }

    public static <T> List<List<T>> partition(List<T> list, int batchSize) {
        List<List<T>> parts = new ArrayList<List<T>>();
        int size = list.size();
        for (int i = 0; i < size; i += batchSize) {
            parts.add(new ArrayList<T>(list.subList(i, Math.min(size, i + batchSize))));
        }
        return parts;
    }
}
