package com.sapienter.jbilling.server.invoiceTemplate.report;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customer.event.UpdateDistributelCustomersInvoiceTemplateEvent;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.InvoiceTemplateFileDTO;
import com.sapienter.jbilling.server.invoice.InvoiceTemplateDAS;
import com.sapienter.jbilling.server.invoice.InvoiceTemplateDTO;
import com.sapienter.jbilling.server.invoice.InvoiceTemplateVersionDAS;
import com.sapienter.jbilling.server.invoice.InvoiceTemplateVersionDTO;
import com.sapienter.jbilling.server.invoiceSummary.db.InvoiceSummaryDAS;
import com.sapienter.jbilling.server.invoiceSummary.db.InvoiceSummaryDTO;
import com.sapienter.jbilling.server.invoiceTemplate.domain.DocDesign;
import com.sapienter.jbilling.server.invoiceTemplate.domain.SqlField;
import com.sapienter.jbilling.server.invoiceTemplate.domain.SubReportDataSource;
import com.sapienter.jbilling.server.invoiceTemplate.ui.JsonFactory;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.order.OrderServiceImpl;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskParameterDAS;
import com.sapienter.jbilling.server.report.BackgroundReportExportUtil;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.AccountTypeDAS;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.order.db.OrderProcessDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.ContactDTOEx;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.Util;

import net.sf.jasperreports.engine.data.JRBeanArrayDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignDataset;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRGraphics2DExporter;
import net.sf.jasperreports.engine.export.JRGraphics2DExporterParameter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXmlExporter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRParameter;

import org.apache.log4j.Logger;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.sapienter.jbilling.common.Util.getSysProp;
import static com.sapienter.jbilling.server.invoiceTemplate.report.ReportBuildVisitor.ASSETS_DATASET;
import static com.sapienter.jbilling.server.invoiceTemplate.report.ReportBuildVisitor.CDR_LINES_DATASET;
import static com.sapienter.jbilling.server.invoiceTemplate.report.ReportBuildVisitor.INVOICE_LINES_DATASET;
import static com.sapienter.jbilling.server.invoiceTemplate.report.ReportBuildVisitor.JBILLING_CONNECTION;
import static com.sapienter.jbilling.server.invoiceTemplate.report.ReportBuildVisitor.SINGLE_ROW_DATASET;

/**
 * @author elmot
 */
public class InvoiceTemplateBL {

    private final JasperDesign design;
    private final Map<String, JasperDesign> subReportsDesigns;
    private final Map<String, String> subDataSources;
    private final Map<String, JRDesignDataset> subDataSets;
    private final Map<String, SubReportDataSource> subReportDataSources;
    private JasperReport rootReport;
    private Map<String, JasperReport> subreports;
    private JasperPrint jasperPrint;

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(InvoiceTemplateBL.class));

    private static final String ITG_DYNAMIC_PARAMS = "itg_dynamic_parameters";

    private static final String BASE_DIR = getSysProp("base_dir");
    private static final String DESIGNS_FOLDER =  BASE_DIR + "designs/";
    private static final String ACCOUNT_CHARGES_PRODUCT_CATEGORY_ID = "Account Charges Product Category Id";
    private static final String OTHER_CHARGES_AND_CREDITS_PRODUCT_CATEGORY_ID = "Other Charges And Credits Product Category Id";

    public InvoiceTemplateBL(JasperDesign jasperDesign, Map<String, JasperDesign> subreports,
                             Map<String, String> subDataSources, Map<String, JRDesignDataset> subDataSets,
                             Map<String, SubReportDataSource> subReportDataSources) {
        this.design = jasperDesign;
        this.subReportsDesigns = subreports;
        this.subDataSources = subDataSources;
        this.subDataSets = subDataSets;
        this.subReportDataSources = subReportDataSources;
    }

    private Map<String, String> getSubDataSources() {
        return subDataSources;
    }

    private Map<String, JRDesignDataset> getSubDataSets() {
        return subDataSets;
    }

    private Map<String, SubReportDataSource> getSubReportDataSources() {
        return subReportDataSources;
    }

    public JasperPrint getJasperPrint() {
        return jasperPrint;
    }

    private static InvoiceTemplateBL buildDesign(String json, Map<Class<?>, Object> resources, Map<String, Class<?>> parameters, Map<String, String> dynamicParameters) {
        DocDesign docDesign = JsonFactory.getGson().fromJson(json, DocDesign.class);
        return buildDesign(docDesign, resources, parameters, dynamicParameters);
    }

    public static InvoiceTemplateBL buildDesign(Reader json, Map<Class<?>, Object> resources, Map<String, Class<?>> parameters, Map<String, String> dynamicParameters) {
        DocDesign docDesign = JsonFactory.getGson().fromJson(json, DocDesign.class);
        return buildDesign(docDesign, resources, parameters, dynamicParameters);
    }

    private static InvoiceTemplateBL buildDesign(DocDesign docDesign, Map<Class<?>, Object> resources, Map<String, Class<?>> parameters, Map<String, String> dynamicParameters) {
        ReportBuildVisitor visitor = new ReportBuildVisitor(resources, parameters, dynamicParameters);
        docDesign.visit(visitor);
        return visitor.createInvoiceTemplateBL();
    }

    public void exportToPng(int pageNum, OutputStream outputStream) throws JRException, IOException {
        exportToPng(jasperPrint, pageNum, outputStream);
    }

    private static void exportToPng(JasperPrint jasperPrint, int pageNum, OutputStream outputStream) throws JRException, IOException {
        int pageWidth = jasperPrint.getPageWidth();
        int pageHeight = jasperPrint.getPageHeight();
        BufferedImage bufferedImage = new BufferedImage(pageWidth, pageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = bufferedImage.createGraphics();
        JRGraphics2DExporter exporter = new JRGraphics2DExporter();
        exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
        exporter.setParameter(JRGraphics2DExporterParameter.PAGE_INDEX, pageNum);
        exporter.setParameter(JRGraphics2DExporterParameter.GRAPHICS_2D, graphics);
        exporter.exportReport();
        ImageIO.write(bufferedImage, "PNG", outputStream);
    }

    private void compile() throws JRException {
        rootReport = JasperCompileManager.compileReport(design);

        subreports = new HashMap<>();
        for (Map.Entry<String, JasperDesign> entry : subReportsDesigns.entrySet()) {

            JasperDesign subReport = entry.getValue();
            subreports.put(entry.getKey(), JasperCompileManager.compileReport(subReport));
        }
    }

    public void debugDesignPrint() throws JRException {
        JasperCompileManager.writeReportToXmlStream(design, System.out);
        for (JasperDesign jasperDesign : subReportsDesigns.values()) {
            JasperCompileManager.writeReportToXmlStream(jasperDesign, System.out);
        }
    }

    public void jrxmlExport(OutputStream outputStream) throws IOException, JRException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        zipOutputStream.putNextEntry(new ZipEntry(design.getName() + ".jrxml"));
        JasperCompileManager.writeReportToXmlStream(design, zipOutputStream);
        zipOutputStream.finish();
        for (JasperDesign subreport : subReportsDesigns.values()) {
            zipOutputStream.putNextEntry(new ZipEntry(subreport.getName() + ".jrxml"));
            JasperCompileManager.writeReportToXmlStream(subreport, System.out);
        }
        zipOutputStream.close();
    }

    private void fill(JRDataSource invoiceDataSource, JRDataSource cdrDataSource, Map<String, JRDataSource> xDataSources, Map<String, Object> parameters, Connection connection) throws JRException {
        Map<String, Object> reportParametrs = new HashMap<>(parameters);
        reportParametrs.putAll(subreports);
        reportParametrs.put(CDR_LINES_DATASET, cdrDataSource);
        reportParametrs.put(INVOICE_LINES_DATASET, invoiceDataSource);
        reportParametrs.put(JBILLING_CONNECTION, connection);
        // use 'single row dataset' to ensure, that there will be at least one row in detail section to display sub-report
        for (int i = 1; i <= subreports.size(); i++) {
            reportParametrs.put(SINGLE_ROW_DATASET + "_" + (i + 1), new JRBeanArrayDataSource(new Object[]{new Object()}));
        }
        for (Map.Entry<String, JRDataSource> xDataSourceEntry : xDataSources.entrySet()) {
            reportParametrs.put(xDataSourceEntry.getKey(), xDataSourceEntry.getValue());
        }
        jasperPrint = JasperFillManager.fillReport(rootReport, reportParametrs, new JREmptyDataSource());
    }

    private static void exportToPdf(JasperPrint jasperPrint, OutputStream fos) throws JRException {
        JRPdfExporter exporter = new JRPdfExporter();

        exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
        exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, fos);
        exporter.exportReport();
    }

    public void exportToPdf(OutputStream outputStream) throws JRException {
        exportToPdf(jasperPrint, outputStream);
    }

    public int getPageNumber() {
        return jasperPrint == null ? -1 : jasperPrint.getPages().size();
    }

    public void debugPrintPrint(OutputStream out) throws JRException {
        JRXmlExporter jrXmlExporter = new JRXmlExporter();
        jrXmlExporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
        jrXmlExporter.setParameter(JRExporterParameter.OUTPUT_STREAM, out);
        jrXmlExporter.exportReport();
    }

    public static void generateErrorReport(String title, String message, boolean image, OutputStream outputStream) {
        InputStream resourceAsStream = InvoiceTemplateBL.class.getResourceAsStream("errorReport.jrxml");

        try {
            JasperReport errorReport = JasperCompileManager.compileReport(resourceAsStream);
            HashMap<String, Object> params = new HashMap<>();
            params.put("ERROR_TITLE", title);
            params.put("ERROR_MSG", message);
            JasperPrint errorPrint = JasperFillManager.fillReport(errorReport, params, new JREmptyDataSource());
            if (image) {
                exportToPng(errorPrint, 0, outputStream);
            } else {
                exportToPdf(errorPrint, outputStream);
            }
        } catch (Exception e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }

    public static InvoiceTemplateBL createInvoiceTemplateBL(InvoiceTemplateDTO selected, InvoiceDTO invoice) throws JRException, SQLException {
        Integer entityId = invoice != null ? invoice.getBaseUser().getEntity().getId() : null;
        ArrayList<InvoiceLineEnvelope> invoiceLines = new ArrayList<>();
        Map<String, Object> invoiceParameters = invoice != null ? fillData(invoice) : new HashMap<>();
        Locale locale = (Locale) invoiceParameters.get(JRParameter.REPORT_LOCALE);
        Map<Class<?>, Object> resources = new HashMap<>();
        if (selected != null) {
            List<InvoiceTemplateFileDTO> files = (List<InvoiceTemplateFileDTO>) InvokerHelper.invokeMethod(InvoiceTemplateFileDTO.class, "findAllByTemplate", selected);

            Map<String, InvoiceTemplateFileDTO> filesMap = new HashMap<>();
            for (InvoiceTemplateFileDTO file : files) {
                filesMap.put(file.getName(), file);
            }
            resources.put(InvoiceTemplateFileDTO.class, filesMap);
        }
        Map<String, Object> sqlFieldParameters = processSqlFields(selected.getTemplateJson(), invoiceParameters);
        Map<String, Class<?>> xParameters = new HashMap<>();
        for (Map.Entry<String, Object> entry : invoiceParameters.entrySet()) {
            if (entry.getKey().startsWith("__")) {
                xParameters.put(entry.getKey(), entry.getValue().getClass());
            }
        }
        for (Map.Entry<String, Object> entry : sqlFieldParameters.entrySet()) {
            xParameters.put(entry.getKey(), entry.getValue().getClass());
        }
        invoiceParameters.putAll(sqlFieldParameters);

        Map<String, String> dynamicParameters = new HashMap<>();
        for (Map.Entry<String, Object> entry :  createAitDynamicParameters(invoice,entityId).entrySet()) {
            dynamicParameters.put(entry.getKey(), entry.getValue().toString());

        }
        if(invoiceParameters.get(JRParameter.REPORT_LOCALE)!=null) {
            dynamicParameters.put(ReportBuildVisitor.LOCALE_MONEY, invoiceParameters.get(JRParameter.REPORT_LOCALE).toString());
        }    
        InvoiceTemplateBL invoiceTemplate = InvoiceTemplateBL.buildDesign(selected.getTemplateJson(), resources, xParameters, dynamicParameters);
        BigDecimal sub_total = BigDecimal.ZERO;
        final Set<AssetEnvelope> assetSet = new HashSet<>();
        final String defaultAssetIdLabel = "Identifier"; //g.message(code: 'asset.detail.identifier').toString();

        if (invoice != null) {
            for (InvoiceLineDTO invoiceLineDTO : invoice.getInvoiceLines()) {
                if (includeInvoiceLine(invoiceLineDTO, selected.getIncludeCarriedInvoiceLines())) {                 
                    Set<AssetEnvelope> invoiceLineAssets = new TreeSet<>(Comparator.comparing(AssetEnvelope::getIdentifier));

                    for (OrderProcessDTO op : invoice.getOrderProcesses()) {
                        for (OrderLineDTO l : op.getPurchaseOrder().getLines()) {
                            if (null != l.getItem() && null != invoiceLineDTO.getItem()
                                    && l.getItem().getId() == invoiceLineDTO.getItem().getId() && l.getAssets().size() > 0) {
                                ItemTypeDTO itemType = l.getItem().findItemTypeWithAssetManagement();
                                for (AssetDTO asset : l.getAssets()) {
                                    invoiceLineAssets.add(new AssetEnvelope(AssetBL.getWS(asset), itemType));
                                }
                            }
                        }
                    }

                    invoiceLines.add(new InvoiceLineEnvelope(invoiceLineDTO, invoiceLineAssets, defaultAssetIdLabel));
                    assetSet.addAll(invoiceLineAssets);
                    sub_total = sub_total.add(invoiceLineDTO.getAmount());
                }
            }
            if(!selected.getIncludeCarriedInvoiceLines()){
                invoiceParameters.put(InvoiceParameters.TOTAL.getName(), ConvertUtils.formatMoney(invoice.getTotal().subtract(invoice.getCarriedBalance()), locale));    
            }
        }
        invoiceParameters.put("sub_total", ConvertUtils.formatMoney(sub_total, locale));
        final JRBeanCollectionDataSource beanDataSource = new JRBeanCollectionDataSource(invoiceLines, false);
        List<CdrEnvelope> cdrs = collectCdrs(invoice, locale, defaultAssetIdLabel);
        final JRBeanCollectionDataSource eventsDataSource = new JRBeanCollectionDataSource(cdrs, false);
        final Map<String, JRDataSource> xDataSources = new HashMap<>();

        for (Map.Entry<String, String> entry : invoiceTemplate.getSubDataSources().entrySet()) {
            String name = entry.getKey();
            String origin = entry.getValue();
            switch (origin) {
                case INVOICE_LINES_DATASET:
                    xDataSources.put(name, new JRBeanCollectionDataSource(beanDataSource.getData(), false));
                    break;
                case CDR_LINES_DATASET:
                    xDataSources.put(name, new JRBeanCollectionDataSource(eventsDataSource.getData(), false));
                    break;
                case ASSETS_DATASET:
                    final JRDesignDataset dataSet = invoiceTemplate.getSubDataSets().get(name);
                    final AssetsCollector assetsCollector = new AssetsCollector(assetSet, defaultAssetIdLabel);
                    List<String> fieldNames = Arrays.stream(dataSet.getFields())
                            .map(JRField::getName)
                            .collect(Collectors.toList());

                    //We will exclude the JRDesignField that already exist
                    assetsCollector.getFields(fieldNames).forEach(f -> {
                        JRDesignField field = new JRDesignField();
                        field.setName(f);
                        field.setValueClass(String.class);
                        field.setDescription(f);
                        try {
                            dataSet.addField(field);
                        } catch (JRException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    assetsCollector.ensureFields(fieldNames);
                    xDataSources.put(name, new JRMapCollectionDataSource((List) assetsCollector.getData()));
                    break;
            }
        }

        ComboPooledDataSource dataSource = Context.getBean("dataSource");
        for (Map.Entry<String, SubReportDataSource> entry : invoiceTemplate.getSubReportDataSources().entrySet()) {
            xDataSources.put(entry.getKey(), entry.getValue().toJRDataSource());
        }

        invoiceTemplate.compile();

        try (Connection connection = dataSource.getConnection()) {
            invoiceTemplate.fill(beanDataSource, eventsDataSource, xDataSources, invoiceParameters, connection);
        }

        return invoiceTemplate;
    }

    private static boolean includeInvoiceLine(InvoiceLineDTO invoiceLineDTO, boolean includeCarriedInvoiceLines) {
        if (invoiceLineDTO.getInvoiceLineType() == null || invoiceLineDTO.getInvoiceLineType().getId() == Constants.INVOICE_LINE_TYPE_TAX) {
            return false;
        }
        return !(!includeCarriedInvoiceLines && invoiceLineDTO.getInvoiceLineType().getId() == Constants.INVOICE_LINE_TYPE_DUE_INVOICE);
    }

    /**
     * Copy of com.sapienter.jbilling.server.notification.NotificationBL.generatePaperInvoiceNew
     */
    private static Map<String, Object> fillData(InvoiceDTO invoice) {
        NotificationBL notification = new NotificationBL();
        InvoiceBL invoiceBl = new InvoiceBL(invoice);
        Integer entityId = invoiceBl.getEntity().getBaseUser().
                getEntity().getId();
        // the language doesn't matter when getting a paper invoice
        MessageDTO message = notification.getInvoicePaperMessage(
                entityId, null, invoiceBl.getEntity().getBaseUser().
                        getLanguageIdField(), invoiceBl.getEntity());

        String message1 = message.getContent()[0].getContent();
        String message2 = message.getContent()[1].getContent();
        ContactBL contact = new ContactBL();
        contact.setInvoice(invoice.getId());
        ContactDTOEx to = contact.getEntity() != null ? contact.getDTO() : null;
        if (to == null) to = new ContactDTOEx();
        if (to.getUserId() == null) {
            to.setUserId(invoice.getBaseUser().getUserId());
        }
        UserDTO user = invoiceBl.getEntity().getBaseUser();
        entityId = user.getEntity().getId();
        contact.setEntity(entityId);
        ContactDTOEx from = contact.getDTO();
        Integer rootUserId =new EntityBL().getRootUser(entityId);
        if (from.getUserId() == null) {
            from.setUserId(rootUserId);
        }
        UserDTO rootUser= new UserDAS().find(rootUserId);
        Locale locale = (new UserBL(invoice.getUserId())).getLocale();
        Map<String, Object> parameters = new HashMap<>();

        IWebServicesSessionBean webServicesSession = Context.getBean("webServicesSession");
        fillMetaFields("receiver", new UserBL(invoice.getBaseUser()).getUserWS().getMetaFields(), parameters);
        if (from.getUserId() != null) {
            fillMetaFields("receiver", new UserBL(rootUser).getUserWS().getMetaFields(), parameters);
        }
        fillMetaFields("company", EntityBL.getCompanyWS(invoice.getBaseUser().getCompany()).getMetaFields(), parameters);
        Collection<MetaFieldValueWS> metaFieldValueWSes = new LinkedList<>();
        for (MetaFieldValue mfv : invoice.getMetaFields()) {
            metaFieldValueWSes.add(MetaFieldBL.getWS(mfv));
        }
        fillMetaFields("invoice", metaFieldValueWSes, parameters);

        // invoice data
        parameters.put(InvoiceParameters.INVOICE_ID.getName(), invoice.getId());
        parameters.put(InvoiceParameters.INVOICE_NUMBER.getName(), invoice.getPublicNumber());
        parameters.put(InvoiceParameters.INVOICE_USER_ID.getName(), invoice.getUserId());
        parameters.put(InvoiceParameters.INVOICE_CREATE_DATETIME.getName(), Util.formatDate(invoice.getCreateDatetime(), invoice.getUserId()));
        parameters.put(InvoiceParameters.INVOICE_DUE_DATE.getName(), Util.formatDate(invoice.getDueDate(), invoice.getUserId()));
        parameters.put(InvoiceParameters.PREVIOUS_BALANCE.getName(), getPreviousBalance(invoice));
        parameters.put(InvoiceParameters.INVOICE_STATUS.getName(), invoice.getInvoiceStatus().getDescription());
        parameters.put(InvoiceParameters.BALANCE.getName(), invoice.getBalance());
        parameters.put(InvoiceParameters.CARRIED_BALANCE.getName(), invoice.getCarriedBalance());
        InvoiceSummaryDTO invoiceSummaryDTO = new InvoiceSummaryDAS().findInvoiceSummaryByInvoice(invoice.getId());
        BigDecimal totalDueAsOfInvoiceDate = (invoiceSummaryDTO != null) ? invoiceSummaryDTO.getTotalDue().setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        parameters.put(InvoiceParameters.TOTAL_DUE_AS_OF_INVOICE_DATE.getName(), totalDueAsOfInvoiceDate);
        parameters.put(InvoiceParameters.TOTAL_PAID.getName(), invoiceBl.getTotalPaid());
        parameters.put(InvoiceParameters.TOTAL_PAID_WITH_CARRIED.getName(), invoiceBl.getTotalPaidWithCarried());

        if (invoice.getDueDate().after(TimezoneHelper.companyCurrentDate(entityId))) {
            parameters.put(InvoiceParameters.PAYMENT_DUE_IN.getName(), new Integer(Math.abs(Days.daysBetween(new LocalDate(invoice.getDueDate().getTime()), new LocalDate(TimezoneHelper.companyCurrentDate(entityId))).getDays())).toString());
        } else {
            parameters.put(InvoiceParameters.PAYMENT_DUE_IN.getName(), "0");
        }

        BillingProcessDTO bp = invoice.getBillingProcess();
        if (bp == null) {
            bp = invoice.getInvoice() == null ? null : invoice.getBillingProcess();
        }
        if (bp == null) {
            parameters.put(InvoiceParameters.BILLING_PERIOD_START_DATE.getName(), Util.formatDate(invoice.getCreateDatetime(), invoice.getUserId()));
            parameters.put(InvoiceParameters.BILLING_DATE.getName(), Util.formatDate(invoice.getCreateDatetime(), invoice.getUserId()));
            parameters.put(InvoiceParameters.BILLING_PERIOD_END_DATE.getName(),
                    Util.formatDate(getBillingPeriodDate(invoice.getCreateDatetime(), invoice.getBaseUser().getCustomer().getMainSubscription()),
                            invoice.getUserId()));
        } else {
            BillingProcessConfigurationDTO bpConf = new BillingProcessConfigurationDAS().findByEntity(bp.getEntity());
            Date cusNextInvDateMinPeriod = getBillingPeriodDate(invoice.getBaseUser().getCustomer().getNextInvoiceDate(), invoice.getBaseUser().getCustomer().getMainSubscription(),true);
            if(bpConf.getInvoiceDateProcess() == 1){
                parameters.put(InvoiceParameters.BILLING_DATE.getName(), Util.formatDate( cusNextInvDateMinPeriod, invoice.getUserId()));
            }else{
                parameters.put(InvoiceParameters.BILLING_DATE.getName(), Util.formatDate(bp.getBillingDate(), invoice.getUserId()));
            }
            parameters.put(InvoiceParameters.BILLING_PERIOD_START_DATE.getName(), Util.formatDate(cusNextInvDateMinPeriod, invoice.getUserId()));
            parameters.put(InvoiceParameters.BILLING_PERIOD_END_DATE.getName(),
                    Util.formatDate(getBillingPeriodEndDate(cusNextInvDateMinPeriod, bp.getPeriodUnit()), invoice.getUserId()));
        }

        parameters.put(InvoiceParameters.ENTITY_ID.getName(),invoice.getBaseUser().getEntity().getId());
        // owner and receiver data
        parameters.put(InvoiceParameters.OWNER_COMPANY.getName(), printable(from.getOrganizationName()));
        parameters.put(InvoiceParameters.OWNER_STREET_ADDRESS.getName(), getAddress(from));
        parameters.put(InvoiceParameters.OWNER_ZIP.getName(), printable(from.getPostalCode()));
        parameters.put(InvoiceParameters.OWNER_CITY.getName(), printable(from.getCity()));
        parameters.put(InvoiceParameters.OWNER_STATE.getName(), printable(from.getStateProvince()));
        parameters.put(InvoiceParameters.OWNER_COUNTRY.getName(), printable(from.getCountryCode()));
        parameters.put(InvoiceParameters.OWNER_PHONE.getName(), getPhoneNumber(from));
        parameters.put(InvoiceParameters.OWNER_EMAIL.getName(), printable(from.getEmail()));

        parameters.put(InvoiceParameters.RECEIVER_COMPANY.getName(), printable(to.getOrganizationName()));
        parameters.put(InvoiceParameters.RECEIVER_NAME.getName(), printable(to.getFirstName(), to.getLastName()));
        parameters.put(InvoiceParameters.RECEIVER_STREET_ADDRESS.getName(), getAddress(to));
        parameters.put(InvoiceParameters.RECEIVER_ZIP.getName(), printable(to.getPostalCode()));
        parameters.put(InvoiceParameters.RECEIVER_CITY.getName(), printable(to.getCity()));
        parameters.put(InvoiceParameters.RECEIVER_STATE.getName(), printable(to.getStateProvince()));
        parameters.put(InvoiceParameters.RECEIVER_COUNTRY.getName(), printable(to.getCountryCode()));
        parameters.put(InvoiceParameters.RECEIVER_PHONE.getName(), getPhoneNumber(to));
        parameters.put(InvoiceParameters.RECEIVER_EMAIL.getName(), printable(to.getEmail()));
        parameters.put(InvoiceParameters.RECEIVER_ID.getName(), printable(String.valueOf(to.getId())));

        // symbol of the currency
        CurrencyBL currency = new CurrencyBL(invoice.getCurrency().getId());
        String symbol = currency.getEntity().getSymbol();
        if (symbol.length() >= 4 && symbol.charAt(0) == '&' &&
                symbol.charAt(1) == '#') {
            // this is an html symbol
            // remove the first two digits
            symbol = symbol.substring(2);
            // remove the last digit (;)
            symbol = symbol.substring(0, symbol.length() - 1);
            // convert to a single char
            Character ch = (char) Integer.valueOf(symbol).intValue();
            symbol = ch.toString();
        }
        parameters.put(InvoiceParameters.CURRENCY_SYMBOL.getName(), symbol);

        // text coming from the notification parameters
        parameters.put(InvoiceParameters.MESSAGE_1.getName(), message1);
        parameters.put(InvoiceParameters.MESSAGE_2.getName(), message2);
        parameters.put(InvoiceParameters.CUSTOMER_NOTES.getName(), "HST: 884725441");
        //todo: change this static value

        // invoice notes stripped of html line breaks
        String notes = invoice.getCustomerNotes();
        if (notes != null) {
            notes = notes.replaceAll("<br/>", "\r\n");
        }
        parameters.put(InvoiceParameters.INVOICE_NOTES.getName(), notes);

        // tax calculated
        BigDecimal taxTotal = new BigDecimal(0);
        String tax_price = "";
        String tax_amount = "";
        String product_code;
        List<InvoiceLineDTO> lines = new ArrayList<>(invoice.getInvoiceLines());
        // Temp change: sort is leading to NPE
        //Collections.sort(lines, new InvoiceLineComparator());
        for (InvoiceLineDTO line : lines) {
            // process the tax, if this line is one
            if (line.getInvoiceLineType() != null && // for headers/footers
                    line.getInvoiceLineType().getId() ==
                            Constants.INVOICE_LINE_TYPE_TAX) {
                // update the total tax variable
                taxTotal = taxTotal.add(line.getAmount());
                product_code = line.getItem() != null ? line.getItem().getInternalNumber() : line.getDescription();
                tax_price += product_code + " " + line.getPrice().setScale(2, BigDecimal.ROUND_HALF_UP).toString() + " %\n";
                tax_amount += symbol +ConvertUtils.formatMoney(line.getAmount(), locale) + "\n";
            }
        }

        BigDecimal totalTaxAmount = lines.stream().filter(line -> null != line.getTaxAmount()).map(InvoiceLineDTO ::
            getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGrossAmount = lines.stream().filter(line -> null != line.getTaxAmount()).map(InvoiceLineDTO ::
            getGrossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        tax_price = (tax_price.equals("")) ? "0.00 %" : tax_price.substring(0, tax_price.lastIndexOf("\n"));
        tax_amount = (tax_amount.equals("")) ? "0.00" : tax_amount.substring(0, tax_amount.lastIndexOf("\n"));
        parameters.put(InvoiceParameters.SALES_TAX.getName(), taxTotal.setScale(2, BigDecimal.ROUND_HALF_UP));
        parameters.put(InvoiceParameters.TAX_PRICE.getName(), tax_price);
        parameters.put(InvoiceParameters.TAX_AMOUNT.getName(), tax_amount);
        parameters.put(InvoiceParameters.TOTAL_TAX_AMOUNT.getName(), symbol + "" + ConvertUtils.formatMoney(totalTaxAmount.setScale(2, BigDecimal.ROUND_HALF_UP), locale));
        parameters.put(InvoiceParameters.TOTAL_GROSS_AMOUNT.getName(), symbol + "" + ConvertUtils.formatMoney(totalGrossAmount.setScale(2, BigDecimal.ROUND_HALF_UP), locale));

        // this parameter help in filter out tax items from invoice lines
        parameters.put(InvoiceParameters.INVOICE_LINE_TAX_ID.getName(), Constants.INVOICE_LINE_TYPE_TAX);
        parameters.put(InvoiceParameters.TOTAL.getName(), ConvertUtils.formatMoney(invoice.getTotal(), locale));
        parameters.put(InvoiceParameters.NUMERIC_TOTAL.getName(), invoice.getTotal().add(invoice.getCarriedBalance().negate()));
        parameters.put(InvoiceParameters.TOTAL_WITHOUT_CARRIED.getName(), invoiceBl.getTotalWithoutCarried(invoice.getInvoiceLines()));
        
        //payment term calculated
        parameters.put(InvoiceParameters.PAYMENT_TERMS.getName(), new Long((invoice.getDueDate().getTime() - invoice.getCreateDatetime().getTime()) / (24 * 60 * 60 * 1000)).toString());

        // set report locale
        parameters.put(JRParameter.REPORT_LOCALE, locale);
        parameters.put(InvoiceParameters.FORMAT_UTIL.getName(), new FormatUtil(locale, symbol));

        parameters.putAll(createAitDynamicParameters(invoice, entityId));

        List<Integer> customers = BackgroundReportExportUtil.getCustomersInHierarchy(invoice.getUserId());
        parameters.put(InvoiceParameters.SUB_ACCOUNT_LIST.getName(),customers);

        // set the subreport directory
        String accountChargesProdCatId = new MetaFieldDAS().getComapanyLevelMetaFieldValue(ACCOUNT_CHARGES_PRODUCT_CATEGORY_ID, entityId);
        String otherChargesAndCreditsProdCatId = new MetaFieldDAS().getComapanyLevelMetaFieldValue(OTHER_CHARGES_AND_CREDITS_PRODUCT_CATEGORY_ID, entityId);

        parameters.put(InvoiceParameters.SUBREPORT_DIR.getName(), DESIGNS_FOLDER);
        parameters.put(InvoiceParameters.BASE_DIR.getName(), BASE_DIR);
        parameters.put(InvoiceParameters.ACCOUNT_CHARGES_PRODUCT_CATEGORY_ID.getName(),
                (null != accountChargesProdCatId) ? Integer.valueOf(accountChargesProdCatId) : null);
        parameters.put(InvoiceParameters.OTHER_CHARGES_AND_CREDITS_PRODUCT_CATEGORY_ID.getName(),
                (null != otherChargesAndCreditsProdCatId) ? Integer.valueOf(otherChargesAndCreditsProdCatId) : null);

        return parameters;
    }

    public static Map<String, Object> createAitDynamicParameters(InvoiceDTO invoice, Integer entityId) {
        // Set up dynamic AIT fields as parameters to send out to the ITG.
        Map<String, Object> dynamicParameters = new HashMap<>();
        if(invoice!=null) {
            Map<String, String> metaFieldsAndGroupMap = new HashMap<>();
            invoice.getBaseUser().getCustomer().getAccountType().getInformationTypes().forEach(accountInformationTypeDTO -> accountInformationTypeDTO.getMetaFields().forEach(metaField -> {
                String stringKey = (accountInformationTypeDTO.getName() + "_" + metaField.getName()).toLowerCase().replaceAll("[/!@#\\\\$%&\\\\*()\\s]", "_");
                metaFieldsAndGroupMap.put(stringKey, "");
            }));
            
            Set<CustomerAccountInfoTypeMetaField> currentCustomerAitMetaFields = new HashSet<>();
            CustomerDTO customerDTO = invoice.getBaseUser().getCustomer();
            Date invoiceDate = invoice.getCreateDatetime();
            for (AccountInformationTypeDTO ait : customerDTO.getAccountType().getInformationTypes()) {
                Date invoiceEffectiveDate = customerDTO.getEffectiveDateByGroupIdAndDate(ait.getId(), invoiceDate);
                currentCustomerAitMetaFields.addAll(customerDTO.getCustomerAccountInfoTypeMetaFields(ait.getId(), invoiceEffectiveDate));
            }

            for (CustomerAccountInfoTypeMetaField customerAitMetaField : currentCustomerAitMetaFields) {
                String parameterName = customerAitMetaField.getMetaFieldValue().getField().getName().toLowerCase().replaceAll("[/!@#\\\\$%&\\\\*()\\s]", "_");
                String parameterValue = customerAitMetaField.getMetaFieldValue().getValue() != null ? customerAitMetaField.getMetaFieldValue().getValue().toString() : "";
                dynamicParameters.put(parameterName, parameterValue);

            /*
            * Adding all meta fields again. Now with account information name as prefix. So that if account type has different addresses for billing and service
            * then those will be accessible via ait name as prefix.
            * */
                if (customerAitMetaField.getAccountInfoType() != null && customerAitMetaField.getAccountInfoType().getName() != null) {
                    String prefix = customerAitMetaField.getAccountInfoType().getName().toLowerCase().replaceAll("[/!@#\\\\$%&\\\\*()\\s]", "_");
                    dynamicParameters.put(prefix + "_" + parameterName, parameterValue);
                }
            }

            for (Map.Entry<String, String> entry : metaFieldsAndGroupMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                dynamicParameters.putIfAbsent(key, value);
            }

            if (PreferenceBL.getPreferenceValueAsBoolean(entityId, Constants.PREFERENCE_ITG_INVOICE_NOTIFICATION)) {
                ServletRequestAttributes requestAttributes = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
                if (requestAttributes != null) {
                    requestAttributes.setAttribute(ITG_DYNAMIC_PARAMS, dynamicParameters, RequestAttributes.SCOPE_SESSION);
                }
            }
        }    

        return dynamicParameters;
    }

    public static List<JbillingMediationRecord> retrieveMediationRecordLinesForInvoice(InvoiceDTO invoiceDTO) {
        MediationService mediationService = Context.getBean("mediationService");
        List<JbillingMediationRecord> eventsForInvoice = new ArrayList<>();
        if (invoiceDTO != null && invoiceDTO.getOrderProcesses() != null) {
            invoiceDTO.getOrderProcesses().stream().filter(orderProcessDTO -> orderProcessDTO.getPurchaseOrder() != null)
                    .map(orderProcessDTO -> orderProcessDTO.getPurchaseOrder().getId())
                    .forEach(orderId -> eventsForInvoice.addAll(mediationService.getMediationRecordsForOrder(orderId)));
        }
        return eventsForInvoice;
    }

    private static List<CdrEnvelope> collectCdrs(InvoiceDTO invoice, Locale locale, String defaultAssetIdLabel) {
        Map<JbillingMediationRecord, CallDataRecord> mediationRecords = new LinkedHashMap();
        for (JbillingMediationRecord record: retrieveMediationRecordLinesForInvoice(invoice)) {
            mediationRecords.put(record, OrderServiceImpl.convertJMRtoRecord(record));
        }

        return ConvertUtils.collectCdrs(mediationRecords, locale, defaultAssetIdLabel);
    }

    private static void fillMetaFields(String prefix, MetaFieldValueWS[] metaFields, Map<String, Object> parameters) {
        fillMetaFields(prefix, metaFields != null ? Arrays.asList(metaFields) : null, parameters);
    }

    private static void fillMetaFields(String prefix, Collection<MetaFieldValueWS> metaFields, Map<String, Object> parameters) {
        if (metaFields != null) {
            for (MetaFieldValueWS mfv : metaFields) {
                String name = mfv.getFieldName().replace('.', '_').replace(' ', '_');
                String value = mfv.getValue() == null ? "" : String.valueOf(mfv.getValue());
                parameters.put("__" + prefix + "__" + name, value);
            }
        }
    }

    private static String printable(String str) {
        if (str == null) {
            return "";
        }
        return str;
    }

    private static String printable(String str, String str2) {
        StringBuilder builder = new StringBuilder();

        if (str != null) builder.append(str).append(' ');
        if (str2 != null) builder.append(str2);

        return builder.toString();
    }

    private static String getPhoneNumber(ContactDTOEx contact){
        if(contact.getPhoneCountryCode()!=null && contact.getPhoneAreaCode()!=null && (contact.getPhoneNumber()!=null && !contact.getPhoneNumber().trim().equals("")))
            return  contact.getPhoneCountryCode()+"-"+contact.getPhoneAreaCode()+"-"+contact.getPhoneNumber();
        else
            return "";
    }

    private static String getAddress(ContactDTOEx contact){
        return printable(contact.getAddress1())+((contact.getAddress2()!=null && !contact.getAddress2().trim().equals(""))?("\n"+contact.getAddress2()):(""));
    }

    private static Date getBillingPeriodDate(Date startDate, MainSubscriptionDTO mainSubscription, boolean subtract){
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        if(mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId() == PeriodUnitDTO.SEMI_MONTHLY){
            cal.add(GregorianCalendar.DAY_OF_MONTH, mainSubscription.getSubscriptionPeriod().getValue() *
                    (subtract ? -15 : 15));
        } else {
            cal.add(MapPeriodToCalendar.map(mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId()),
                    mainSubscription.getSubscriptionPeriod().getValue() * (subtract ? -1 : 1));
        }

        return cal.getTime();
    }

    private static Date getBillingPeriodDate(Date startDate, MainSubscriptionDTO mainSubscription){
        return getBillingPeriodDate(startDate, mainSubscription, false);
    }

    private static Date getBillingPeriodEndDate(Date startDate, PeriodUnitDTO unitDTO){
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        if (CalendarUtils.isSemiMonthlyPeriod(unitDTO)) {
            cal.setTime(CalendarUtils.addSemiMonthyPeriod(cal.getTime()));
        } else {
            cal.add(MapPeriodToCalendar.map(unitDTO.getId()), 1);
        }

        cal.add(Calendar.DAY_OF_YEAR, -1);
        return cal.getTime();
    }

    // parse the query to find the parameters and execute the query
    private static Map<String, Object> processSqlFields(String templateJson, Map<String, Object> parameters) {
        DocDesign docDesign = JsonFactory.getGson().fromJson(templateJson, DocDesign.class);
        List<SqlField> sqlFields = docDesign.getSqlFields();
        Map<String, Object> sqlFieldParameters = new HashMap<>();
        for (SqlField sqlField : sqlFields) {
            LOG.debug("Processing the SQL field  : "+sqlField.getName());
            try{
                Object output = sqlField.setAsParameter(parameters);
                if(output!=null) sqlFieldParameters.put(sqlField.getName(),output);
            } catch (Exception exception){
                LOG.error(exception);
                throw new RuntimeException(exception);
            }
        }
        return sqlFieldParameters;
    }

    public static InvoiceTemplateDTO createNewTemplateWithVersion(InvoiceTemplateDTO newTemplate,
                                                                  String name,
                                                                  String json,
                                                                  Integer entityId,
                                                                  Integer userId){
        InvoiceTemplateDAS das = new InvoiceTemplateDAS();
        InvoiceTemplateVersionDAS versionDAS = new InvoiceTemplateVersionDAS();

        newTemplate.setName(name);
        newTemplate.setEntity(new CompanyDTO(entityId));

        newTemplate = das.save(newTemplate);
        das.flush();

        InvoiceTemplateVersionDTO versionDTO = new InvoiceTemplateVersionDTO(newTemplate.getId()+".1",
                null, TimezoneHelper.serverCurrentDate(), json);
        versionDTO.setUserId(userId);
        versionDTO = versionDAS.save(versionDTO);
        versionDAS.flush();

        versionDTO.setInvoiceTemplate(newTemplate);
        newTemplate.getInvoiceTemplateVersions().add(versionDTO);
        versionDAS.save(versionDTO);
        versionDAS.flush();
        newTemplate = das.save(newTemplate);
        UpdateDistributelCustomersInvoiceTemplateEvent event = new UpdateDistributelCustomersInvoiceTemplateEvent(name, entityId, newTemplate.getId());
        EventManager.process(event);
        return newTemplate;

    }

    private static InvoiceTemplateVersionDTO getTemplateVersionForInvoice(InvoiceTemplateDTO dto){
        LinkedList<InvoiceTemplateVersionDTO> versionDTOs = InvoiceTemplateVersionBL.sortDTOByVersionNumber(dto.getInvoiceTemplateVersions());
        return InvoiceTemplateVersionBL.getVersionForInvoice(versionDTOs);
    }

    public static void setTemplateVersionForInvoice(InvoiceTemplateDTO invoiceTemplate){

        if(invoiceTemplate == null){
            throw new SessionInternalError("Could not resolve InvoiceTemplate",
                    new String[]{"download.invoice.template.not.set"});
        }

        InvoiceTemplateVersionDTO versionDTO = InvoiceTemplateBL.getTemplateVersionForInvoice(invoiceTemplate);
        if(versionDTO != null){
            invoiceTemplate.setTemplateJson(versionDTO.getId());
        }
    }
    
    public static Integer getDefaultTemplateId(){
        return new InvoiceTemplateDAS().getDefaultTemplateId(Constants.DEFAULT_ITG);
    }

    /**
     * if template is not configured anywhere then 0 is returned
     * else 1 for AccountType, 2 for Customer and 3 for PaperInvoiceNotificationTask
     * */
    private static Integer getTemplateConfigurationPlace(InvoiceTemplateDTO templateDTO){
        return (new AccountTypeDAS().countAllByInvoiceTemplate(templateDTO.getId()) > 0) ? 1
                : ((new CustomerDAS().countAllByInvoiceTemplate(templateDTO.getId()) > 0) ? 2
                : (new PluggableTaskParameterDAS().isInvoiceTemplateConfigured(templateDTO.getId()) ? 3: 0));

    }

    public static Boolean validateTemplateForDelete(InvoiceTemplateDTO templateDTO){
        Integer configurationPlace;
        if((configurationPlace = getTemplateConfigurationPlace(templateDTO)) != 0){
            throw new SessionInternalError("Invoice Template is already being used.",
                    new String[]{"invoice.template.in.use.for."+configurationPlace+".message,"+templateDTO.getId()});
        }
        return Boolean.TRUE;
    }

    public static boolean isDuplicateInvoiceTemplate (String invoiceTemplateName, Integer entityId){
        return new InvoiceTemplateDAS().isDuplicateInvoiceTemplate(invoiceTemplateName,entityId);
    }

    private static BigDecimal getPreviousBalance(InvoiceDTO invoiceDTO) {
        return UserBL.getBalance(invoiceDTO.getBaseUser().getId()).subtract(invoiceDTO.getTotal().subtract(invoiceDTO.getCarriedBalance()));
    }




}
