package com.sapienter.jbilling.server.util.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.einvoice.db.EInvoiceLogDAS;
import com.sapienter.jbilling.einvoice.db.EInvoiceLogDTO;
import com.sapienter.jbilling.einvoice.plugin.IEInvoiceProvider;
import com.sapienter.jbilling.gst.Datum;
import com.sapienter.jbilling.gst.Doc;
import com.sapienter.jbilling.gst.DocDet;
import com.sapienter.jbilling.gst.DocIssue;
import com.sapienter.jbilling.gst.Exp;
import com.sapienter.jbilling.gst.GSTReturn;
import com.sapienter.jbilling.gst.Hsn;
import com.sapienter.jbilling.gst.Inv;
import com.sapienter.jbilling.gst.Itm;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryDAS;
import com.sapienter.jbilling.server.pricing.db.DataTableQueryDAS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.JDBCException;

public class GSTR1JSONMapper {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String EINVOICE_PLUGIN_INTERFACE_NAME = "com.sapienter.jbilling.einvoice.plugin.IEInvoiceProvider";
    private final PluggableTaskTypeCategoryDAS pluggableTaskTypeCategoryDAS = new PluggableTaskTypeCategoryDAS();
    private final DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private String formattedJsonString = "null";
    private static final String ANERROR = "An error occurred: {}";
    private static final String CHECKSUM = " checksum ";
    private Map<Integer, String> sacMap = null;
    private Map<Integer, String> iNumMap = null;
    private Map<Integer, String> ackDateMap = null;

    public String getGSTR1Json(List<InvoiceWS> invoiceWSList, List<OrderWS> orderWSList) throws Exception {

        String tableName = null;
        try {
            UserWS userWS = new UserBL(invoiceWSList.get(0).getUserId()).getUserWS();
            Integer entityId = userWS.getEntityId();// For all seller gstin

            // Table name format in configuration document(custom invoice) is (route_60_sac_code)
            //Here 60 is company ID

            tableName = "route_" + entityId + "_sac_code";
            prepareData(invoiceWSList, orderWSList);

            List<List<Doc>> docListList = new ArrayList<>();
            for (InvoiceWS invoiceWS : invoiceWSList) {
                Integer invoiceId = invoiceWS.getId();
                List<Doc> docList = new ArrayList<>();
                Doc doc = new Doc();
                doc.setCancel(0);
                doc.setNum(1);
                doc.setTotnum(1);
                doc.setFrom(iNumMap.get(invoiceId));
                doc.setTo(iNumMap.get(invoiceId));
                doc.setNetIssue(1);
                docList.add(doc);
                docListList.add(docList);
            }

            List<List<DocDet>> docDetListList = new ArrayList<>();
            List<DocDet> docDetList = new ArrayList<>();
            int docDetListSize = 0;
            for (int i = 0; i < 12; i++) {
                DocDet docDet = new DocDet();
                if (invoiceWSList.size() > i) {
                    docDet.setDocs(docListList.get(i));
                    ++docDetListSize;
                }
                docDet.setDocNum(i + 1);
                docDetList.add(docDet);
            }
            docDetListList.add(docDetList);

            StringBuilder builderDocIssue = new StringBuilder();
            DocIssue docIssue = new DocIssue();
            docIssue.setFlag("N");
            docIssue.setDocDet(docDetListList.get(0));

            for (int i = 0; i < docDetListSize; i++) {
                getDocDetails(docDetList, i, builderDocIssue);
            }
            docIssue.setChksum(generateChecksum(builderDocIssue.toString()));
            logger.debug(builderDocIssue.toString() + CHECKSUM + docIssue.getChksum());

            List<List<Datum>> datumListList = new ArrayList<>();
            List<Datum> datumList = getDatumList(invoiceWSList, tableName);
            datumListList.add(datumList);
            StringBuilder builderHsn = new StringBuilder();
            Hsn hsn = getHsn(datumListList, datumList, builderHsn);
            hsn.setChksum(generateChecksum(builderHsn.toString()));
            logger.debug(builderHsn.toString() + CHECKSUM + hsn.getChksum());

            List<List<Itm>> itmsListList = getItemsList(invoiceWSList);

            List<List<Inv>> invListList = new ArrayList<>();
            List<Inv> invList = new ArrayList<>();
            for (int i = 0; i < invoiceWSList.size(); i++) {
                InvoiceLineDTO[] invoiceLineDTOS = invoiceWSList.get(i).getInvoiceLines();
                Integer invoiceId = invoiceWSList.get(i).getId();
                Inv inv = new Inv();
                BigDecimal totalAmountOfInvoice = BigDecimal.ZERO;
                for (InvoiceLineDTO invoiceLineDTO : invoiceLineDTOS) {
                    totalAmountOfInvoice = (totalAmountOfInvoice.add(invoiceLineDTO.getGrossAmount()));
                }
                inv.setVal(totalAmountOfInvoice.setScale(2, BigDecimal.ROUND_HALF_UP));
                inv.setItms(itmsListList.get(i));
                inv.setFlag("N");
                inv.setIrn(invoiceWSList.get(i).getIrn());
                inv.setSrctyp("E-Invoice");
                inv.setIdt(formatter.format(invoiceWSList.get(i).getCreateDatetime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()));
                inv.setIrngendate(ackDateMap.get(invoiceId));
                inv.setInum(iNumMap.get(invoiceId));

                List<Itm> listItm = itmsListList.get(i);
                StringBuilder builder = new StringBuilder();

                builder.append("idt=");
                builder.append(inv.getIdt());
                builder.append(",");

                builder.append("inum=");
                builder.append(inv.getInum());
                builder.append(",");

                for (int j = 0; j < invoiceLineDTOS.length; j++) {
                    if (!listItm.isEmpty()) {
                        builder.append("itms=num=");
                        builder.append(j + 1);
                        builder.append(",");

                        builder.append("csamt=");
                        builder.append(listItm.get(0).getCsamt());
                        builder.append(",");

                        builder.append("iamt=");
                        builder.append(listItm.get(0).getIamt());
                        builder.append(",");

                        builder.append("rt=");
                        builder.append(listItm.get(0).getRt());
                        builder.append(",");

                        builder.append("txval=");
                        builder.append(listItm.get(0).getTxval());
                        builder.append(",");
                    }
                }
                builder.append("val=");
                builder.append(inv.getVal());

                inv.setChksum(generateChecksum(builder.toString()));
                logger.debug(builder.toString() + CHECKSUM + inv.getChksum());
                invList.add(inv);
                invListList.add(invList);
            }

            List<Exp> expList = new ArrayList<>();
            Exp exp = new Exp();
            for (int i = 0; i < invoiceWSList.size(); i++) {
                exp.setInv(invListList.get(i));
            }
            exp.setExpTyp("WOPAY");
            expList.add(exp);

            GSTReturn gstReturn = new GSTReturn();
            gstReturn.setGstin(getSellerGstin(entityId));
            gstReturn.setFp(DateTimeFormatter.ofPattern("MMyyyy")
                    .format(invoiceWSList.get(0)
                            .getCreateDatetime()
                            .toInstant().atZone(ZoneId.systemDefault())
                            .toLocalDate()));
            gstReturn.setFilingTyp("M");
            gstReturn.setGt(0);
            gstReturn.setCurGt(0);
            gstReturn.setExp(expList);
            gstReturn.setHsn(hsn);
            gstReturn.setDocIssue(docIssue);
            gstReturn.setFilDt(formatter.format(LocalDate.now()));

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            formattedJsonString = objectMapper.writeValueAsString(gstReturn);
        } catch (JDBCException jdbcException) {
            logger.error("[ " + tableName + " does not exist in database ]", jdbcException);
            throw new SessionInternalError(new String[]{"Entry is required for sac code in Data Tables section. [" + tableName + " does not exist]"});
        } catch (Exception exception) {
            logger.error(ANERROR, exception.getMessage(), exception);
        }
        return formattedJsonString;
    }

    private List<List<Itm>> getItemsList(List<InvoiceWS> invoiceWSList) {
        List<List<Itm>> itmsListList = new ArrayList<>();
        for (InvoiceWS invoiceWS : invoiceWSList) {
            Integer invoiceId = invoiceWS.getId();
            List<Itm> itmsList = new ArrayList<>();
            InvoiceLineDTO[] invoiceLineDTOS = invoiceWS.getInvoiceLines();
            BigDecimal totalTxValOfOneInv = BigDecimal.ZERO;
            Itm itm = new Itm();
            for (InvoiceLineDTO invoiceLineDTO : invoiceLineDTOS) {
                totalTxValOfOneInv = totalTxValOfOneInv.add(new BigDecimal(invoiceLineDTO.getAmount()));
            }
            itm.setCsamt(0);// need to work
            itm.setRt(0);
            itm.setTxval(totalTxValOfOneInv.setScale(2, BigDecimal.ROUND_HALF_UP));
            itm.setIamt(0F);
            itmsList.add(itm);
            itmsListList.add(itmsList);
        }
        return itmsListList;
    }

    private static Hsn getHsn(List<List<Datum>> datumListList, List<Datum> datumList, StringBuilder builderHsn) {
        Hsn hsn = new Hsn();
        hsn.setFlag("N");
        hsn.setData(datumListList.get(0));

        for (int i = 0; i < datumList.size(); i++) {
            Datum data = datumList.get(i);
            if (data != null) {
                builderHsn.append("data=camt=");
                builderHsn.append(data.getCamt());
                builderHsn.append(",");

                builderHsn.append("desc=");
                builderHsn.append(data.getDesc());
                builderHsn.append(",");

                builderHsn.append("hsn_sc=");
                builderHsn.append(data.getHsnSc());
                builderHsn.append(",");

                builderHsn.append("iamt=");
                builderHsn.append(data.getIamt());
                builderHsn.append(",");

                builderHsn.append("num=");
                builderHsn.append(data.getNum());
                builderHsn.append(",");

                builderHsn.append("qty=");
                builderHsn.append(data.getQty());
                builderHsn.append(",");

                builderHsn.append("txval=");
                builderHsn.append(data.getTxval());
                builderHsn.append(",");

                builderHsn.append("uqc=");
                builderHsn.append(data.getUqc());
            }
            if (datumList.size() - 1 != i) builderHsn.append(",");
        }
        return hsn;
    }

    private List<Datum> getDatumList(List<InvoiceWS> invoiceWSList, String tableName) {
        List<Datum> datumList = new ArrayList<>();
        for (InvoiceWS invoiceWS : invoiceWSList) {
            Datum datum = new Datum();
            Integer invoiceId = invoiceWS.getId();
            List<Float> taxAmt = getTaxAmount(invoiceId);
            InvoiceLineDTO[] invoiceLineDTOS = invoiceWS.getInvoiceLines();
            BigDecimal quantity = BigDecimal.ZERO;
            BigDecimal totalTxValOfOneInvDatum = BigDecimal.ZERO;
            for (InvoiceLineDTO invoiceLineDTO : invoiceLineDTOS) {
                quantity = (quantity.add(new BigDecimal(invoiceLineDTO.getQuantity())));
                totalTxValOfOneInvDatum = totalTxValOfOneInvDatum.add(new BigDecimal(invoiceLineDTO.getAmount()));
            }
            datum.setSamt(taxAmt.get(1));
            datum.setRt(0);
            datum.setUqc("NA");
            datum.setQty(0F);
            datum.setNum(1);
            datum.setTxval(totalTxValOfOneInvDatum.setScale(2, BigDecimal.ROUND_HALF_UP));
            datum.setCamt(taxAmt.get(0));
            datum.setHsnSc(sacMap.get(invoiceId));
            datum.setIamt(taxAmt.get(2));
            datum.setDesc(new DataTableQueryDAS().getColumnValueBySacCode(tableName, sacMap.get(invoiceId), "description"));
            datumList.add(datum);
        }
        return datumList;
    }

    private static void getDocDetails(List<DocDet> docDetList, int i, StringBuilder builderDocIssue) {
        DocDet docDet = docDetList.get(i);
        List<Doc> doclist = docDet.getDocs();
        Doc doc = doclist.get(0);

        builderDocIssue.append("doc_det=doc_num=");
        builderDocIssue.append(i + 1);
        builderDocIssue.append(",");
        if (doc != null) {
            builderDocIssue.append("docs=cancle=");
            builderDocIssue.append(doc.getCancel());
            builderDocIssue.append(",");

            builderDocIssue.append("from=");
            builderDocIssue.append(doc.getFrom());
            builderDocIssue.append(",");

            builderDocIssue.append("net_issue=");
            builderDocIssue.append(doc.getNetIssue());
            builderDocIssue.append(",");

            builderDocIssue.append("num=");
            builderDocIssue.append(doc.getNum());
            builderDocIssue.append(",");

            builderDocIssue.append("to=");
            builderDocIssue.append(doc.getTo());
            builderDocIssue.append(",");

            builderDocIssue.append("totnum=");
            builderDocIssue.append(doc.getTotnum());
        }
        if (docDetList.size() - 1 != i)
            builderDocIssue.append(",");
    }

    //Generate Checksum
    public static String generateChecksum(String data) {
        try {
            // Create a MessageDigest instance for SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Get the byte representation of the data
            byte[] bytes = data.getBytes(StandardCharsets.UTF_8);

            // Update the digest with the data
            byte[] hash = digest.digest(bytes);

            // Convert the hash to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.error(ANERROR, e.getMessage(), e);
            return null;
        }
    }

    public String getSellerGstin(Integer entityId) throws Exception {
        Integer eInvoiceProviderPluginTypeId = pluggableTaskTypeCategoryDAS.findByInterfaceName(EINVOICE_PLUGIN_INTERFACE_NAME).getId();
        PluggableTaskManager<IEInvoiceProvider> taskManager = new PluggableTaskManager<>(entityId, eInvoiceProviderPluginTypeId);
        IEInvoiceProvider task = taskManager.getNextClass();
        return task.getGSTIn();
    }

    private List<Float> getTaxAmount(Integer invoiceId) {
        List<Float> taxAmount = new ArrayList<>();
        try {
            EInvoiceLogDTO eInvoiceLog = new EInvoiceLogDAS().findByInvoiceId(invoiceId);
            if (eInvoiceLog != null) {
                String invoicePayload = eInvoiceLog.geteInvoiceRequestpayload();
                if (!invoicePayload.isEmpty()) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode rootNode = objectMapper.readTree(invoicePayload);

                    //cgstAmt[0]
                    taxAmount.add(rootNode.at("/ValDtls/CgstVal").floatValue());
                    //sgatAmt[1]
                    taxAmount.add(rootNode.at("/ValDtls/SgstVal").floatValue());
                    //igatAmt[2]]
                    taxAmount.add(rootNode.at("/ValDtls/IgstVal").floatValue());
                }
            } else {
                //cgstAmt[0]
                taxAmount.add(0F);
                //sgatAmt[1]
                taxAmount.add(0F);
                //igatAmt[2]]
                taxAmount.add(0F);
            }

        } catch (Exception exception) {
            logger.error(ANERROR, exception.getMessage(), exception);
        }
        return taxAmount;
    }

    private void prepareData(List<InvoiceWS> invoiceWSList, List<OrderWS> orderWSList) throws Exception {
        sacMap = new HashMap<>();
        iNumMap = new HashMap<>();
        ackDateMap = new HashMap<>();
        for (int i = 0; i < invoiceWSList.size(); i++) {
            Integer invoiceId = invoiceWSList.get(i).getId();
            OrderWS orderWS = orderWSList.get(i);
            boolean flag = false;
            MetaFieldValueWS[] valueWS = orderWS.getMetaFields();
            for (MetaFieldValueWS value : valueWS) {
                if (((value.getFieldName().equals("SAC 1")) || (value.getFieldName().equals("SAC 2")) || (value.getFieldName().equals("SAC 3")))
                        && (value.getStringValue() != null && !flag)) {
                    sacMap.put(invoiceId, value.getStringValue());
                    flag = true;
                }
                if (value.getFieldName().equals("Custom Invoice Number")) {
                    iNumMap.put(invoiceId, value.getStringValue());
                }
            }
            if (Boolean.FALSE.equals(flag)) {
                sacMap.put(invoiceId, null);
            }

            getAckDateMap(invoiceId);
        }

    }

    private void getAckDateMap(Integer invoiceId) throws Exception {
        EInvoiceLogDTO eInvoiceLog = new EInvoiceLogDAS().findByInvoiceId(invoiceId);
        if ((eInvoiceLog != null) && (!(eInvoiceLog.geteInvoiceResponse().isEmpty()))) {

            String invoiceResponse = eInvoiceLog.geteInvoiceResponse();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(invoiceResponse);
            String ackDtInput = root.at("/AckDt").asText();
            LocalDateTime dateTime = LocalDateTime.parse(ackDtInput, inputFormatter);
            String ackDtOutput = dateTime.format(formatter);
            ackDateMap.put(invoiceId, ackDtOutput);
        } else {
            ackDateMap.put(invoiceId, null);
        }
    }

    public Float iGSTAmountAsPerInvoice(int rt, Float amtWithTax, Integer invoiceId) {

        try {
            EInvoiceLogDTO eInvoiceLog = new EInvoiceLogDAS().findByInvoiceId(invoiceId);
            if (eInvoiceLog != null) {
                String invoicePayload = eInvoiceLog.geteInvoiceRequestpayload();
                if (!invoicePayload.isEmpty()) {
                    JsonNode rootNode = new ObjectMapper().readTree(invoicePayload);
                    JsonNode itemListNode = rootNode.path("ItemList");
                    if (itemListNode.isArray()) {
                        for (JsonNode item : itemListNode) {
                            // Access individual properties with each item
                            int gstRt = item.path("GstRt").asInt();
                            float txVal = item.path("TotItemVal").floatValue();
                            if (gstRt == rt && txVal == amtWithTax) return item.path("IgstAmt").floatValue();
                        }
                    }
                }
            }
        } catch (Exception exception) {
            logger.error(ANERROR, exception.getMessage(), exception);
        }
        return 0F;
    }
}
