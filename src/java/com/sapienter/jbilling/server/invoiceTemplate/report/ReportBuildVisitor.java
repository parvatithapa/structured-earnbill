package com.sapienter.jbilling.server.invoiceTemplate.report;

import com.sapienter.jbilling.server.invoiceTemplate.domain.DocDesign;
import com.sapienter.jbilling.server.invoiceTemplate.domain.DocElement;
import com.sapienter.jbilling.server.invoiceTemplate.domain.EventLines;
import com.sapienter.jbilling.server.invoiceTemplate.domain.Font;
import com.sapienter.jbilling.server.invoiceTemplate.domain.FontFace;
import com.sapienter.jbilling.server.invoiceTemplate.domain.Image;
import com.sapienter.jbilling.server.invoiceTemplate.domain.InvoiceLines;
import com.sapienter.jbilling.server.invoiceTemplate.domain.Section;
import com.sapienter.jbilling.server.invoiceTemplate.domain.SubReport;
import com.sapienter.jbilling.server.invoiceTemplate.domain.SubReportDataSource;
import com.sapienter.jbilling.server.invoiceTemplate.domain.SubReportDataSourceType;
import com.sapienter.jbilling.server.invoiceTemplate.domain.TableLines;
import com.sapienter.jbilling.server.invoiceTemplate.domain.Text;
import com.sapienter.jbilling.server.invoiceTemplate.domain.TextBox;
import com.sapienter.jbilling.server.invoiceTemplate.domain.Visitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import net.sf.jasperreports.components.list.DesignListContents;
import net.sf.jasperreports.components.list.ListContents;
import net.sf.jasperreports.components.list.StandardListComponent;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRElement;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRLineBox;
import net.sf.jasperreports.engine.JRPen;
import net.sf.jasperreports.engine.JRSortField;
import net.sf.jasperreports.engine.JRQuery;
import net.sf.jasperreports.engine.JRVariable;
import net.sf.jasperreports.engine.base.JRBoxPen;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignComponentElement;
import net.sf.jasperreports.engine.design.JRDesignDatasetRun;
import net.sf.jasperreports.engine.design.JRDesignElement;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignFrame;
import net.sf.jasperreports.engine.design.JRDesignGroup;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignRectangle;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignSortField;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignSubreport;
import net.sf.jasperreports.engine.design.JRDesignSubreportParameter;
import net.sf.jasperreports.engine.design.JRDesignSubreportReturnValue;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JRDesignVariable;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.design.JRDesignDataset;
import net.sf.jasperreports.engine.design.JRDesignTextElement;
import net.sf.jasperreports.engine.type.CalculationEnum;
import net.sf.jasperreports.engine.type.EvaluationTimeEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.SortFieldTypeEnum;
import net.sf.jasperreports.engine.type.SortOrderEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.xml.JRXmlWriter;

import static com.sapienter.jbilling.server.invoiceTemplate.report.ConvertUtils.convertColor;
import static com.sapienter.jbilling.server.invoiceTemplate.report.FieldType.Field;
import static java.awt.Color.WHITE;
import static java.util.Arrays.asList;
import static net.sf.jasperreports.engine.type.EvaluationTimeEnum.AUTO;
import static net.sf.jasperreports.engine.type.EvaluationTimeEnum.NOW;
import static net.sf.jasperreports.engine.type.ModeEnum.OPAQUE;
import static net.sf.jasperreports.engine.type.ModeEnum.TRANSPARENT;
import static net.sf.jasperreports.engine.type.PositionTypeEnum.FLOAT;
import static net.sf.jasperreports.engine.type.SplitTypeEnum.PREVENT;
import static net.sf.jasperreports.engine.type.SplitTypeEnum.STRETCH;
import static net.sf.jasperreports.engine.type.StretchTypeEnum.NO_STRETCH;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author elmot
 */
public class ReportBuildVisitor implements Visitor {

    public static final Pattern JR_EXPRESSION_PATTERN = Pattern.compile("\\$[FPV].*");
    public static final Pattern CUSTOM_EXPRESSION_PATTERN = Pattern.compile("(\\$E\\{.*\\})");
    public static final Pattern JR_PARAMETER_PATTERN = Pattern.compile("\\$P\\{[A-z0-9_]+\\}");
    public static final Pattern JR_FIELD_PATTERN = Pattern.compile("\\$F\\{[A-z0-9_]+\\}");

    public static final String INVOICE_LINES_DATASET = "INVOICE_LINES_DATASET";
    public static final String CDR_LINES_DATASET = "CDR_LINES_DATASET";
    public static final String ASSETS_DATASET = "ASSETS_DATASET";
    public static final String SINGLE_ROW_DATASET = "SINGLE_ROW_DATASET";
    public static final String JBILLING_CONNECTION = "JBILLING_CONNECTION";
    public static final String  LOCALE_MONEY= "LOCALE_MONEY";

    private static final String PAGE_NUMBER = "PAGE_NUMBER";
    private static final String REPORT_PARAMETERS_MAP = "REPORT_PARAMETERS_MAP";
    private static final String SUBREPORT = "SUBREPORT_";
    private static final String SECTION = "SECTION_";

    private static final String REPORT_PATH = com.sapienter.jbilling.common.Util.getSysProp("base_dir") + "designs" + File.separator;

    private final JasperDesign rootDesign;
    private final Map<String, JasperDesign> subReports;
    // [Requirements #9214 2014-08-08 igor.poteryaev@jbilling.com]
    // Was added for correct numbering of subreports. Simplifies referencing of subreport variables.
    private final Map<String, JasperDesign> sections;
    private final Map<Class<?>, Object> resources;
    private final Map<String, Class<?>> parameters;
    private final Map<String, String> dynamicParameters;
    private final Map<String, Class<?>> xFields;

    private final Map<String, String> subDataSources = new HashMap<>();
    private final Map<String, JRDesignDataset> subDataSets = new HashMap<>();

    private final Map<String, SubReportDataSource> subReportDataSources = new HashMap<>();
    List<JRDesignImage> backgroundImages = new LinkedList<>();
    private JasperDesign currentSubReport;
    private JasperDesign previousSubReport;
    private JRDesignBand currentBand;
    private JRDesignSubreport currentSubreportPlaceHolder;
    private JRDesignSubreport previousSubreportPlaceHolder;

    private int columnWidth;
    private JRDesignBand pageHeader;
    private JRDesignBand pageFooter;

    private Section firstSection;
    private Section lastSection;
    private boolean isInDetailSection;

    private int currentXOffset = 0;
    private int currentYOffset = 0;

    public ReportBuildVisitor(Map<Class<?>, Object> resources, Map<String, Class<?>> parameters, Map<String, String> dynamicParameters) {
        this.resources = new HashMap<>(resources);
        this.parameters = new HashMap<>(parameters);
        this.dynamicParameters = new HashMap<>(dynamicParameters);
        this.xFields = new HashMap<>();
        rootDesign = createJasperDesign();
        rootDesign.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);

        subReports = new HashMap<>();
        sections = new HashMap<>();
        currentBand = createBand(STRETCH);
        ((JRDesignSection) rootDesign.getDetailSection()).addBand(currentBand);
    }

    private void attachDataSet(TableLines lines, String dataSetName) {
        currentSubreportPlaceHolder.setDataSourceExpression(createExpression(JRDataSource.class, "$P{" + dataSetName + "}"));
        setupDataset(lines.getColumns(), currentSubReport.getMainDesignDataset());
    }

    private void setupDataset(ColumnSettings[] columns, JRDesignDataset dataSet) {
        if (columns != null) {
            for (ColumnSettings cs : columns) {
                FieldSetup field = cs.field;
                if (FieldType.Field.equals(field.getType())) {
                    setupDatasetField(dataSet, field);
                }
            }
        }
    }

    private static void setupDatasetField(JRDesignDataset mainDataset, FieldSetup fieldSetup) {
        String name = fieldSetup.getName();
        Class<?> valueClass = fieldSetup.getValueClass();
        Map fieldsMap = mainDataset.getFieldsMap();
        if (fieldsMap.containsKey(name)) {
            Class definedValueClass = ((JRField) fieldsMap.get(name)).getValueClass();
            if (!valueClass.isAssignableFrom(definedValueClass)) {
                throw new IllegalStateException("Attempt to register data set field " + name +
                        " which is already defined using not matching class: defined type is " + definedValueClass +
                        ", but provided type is " + valueClass);
            }
        } else {
            JRDesignField field = new JRDesignField();
            field.setName(name);
            field.setValueClass(valueClass);
            field.setDescription(fieldSetup.getDescription());
            try {
                mainDataset.addField(field);
            } catch (JRException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void accept(DocDesign docDesign) {
        rootDesign.setName(docDesign.getName());
        putPageParameters(docDesign);
        List<Section> sections = docDesign.getSections();
        firstSection = sections.get(0);
        lastSection = sections.get(sections.size() - 1);
        for (Section docElement : sections) {
            docElement.visit(this);
        }
        // Add backgrounds images to DocDesign band.
        processBackGroundImages(rootDesign);
    }

    @Override
    public void accept(com.sapienter.jbilling.server.invoiceTemplate.domain.List list) {

        int thickness = list.getThickness();

        JRDesignFrame listFrame = new JRDesignFrame();
        listFrame.setX(thickness / 2);
        listFrame.setY(thickness / 2);
        listFrame.setWidth(list.getWidth());
        listFrame.setHeight(list.getHeight());
        listFrame.setBackcolor(convertColor(list.getBgColor()));
        listFrame.setMode(list.isTransparent() ? TRANSPARENT : OPAQUE);
        listFrame.setStretchType(NO_STRETCH);

        JRLineBox lineBox = listFrame.getLineBox();
        for (JRPen linePen : asList(lineBox.getBottomPen(), lineBox.getLeftPen(), lineBox.getRightPen(), lineBox.getTopPen())) {
            linePen.setLineColor(convertColor(list.getBorderColor()));
            linePen.setLineWidth((float) thickness);
        }
        lineBox.setPadding(list.getPadding());

        JRDesignDatasetRun listDataSetRun = createListDataSetRun(list);
        JRDesignDataset listDataSet = subDataSets.get(listDataSetRun.getDatasetName());

        StandardListComponent listComponent = new StandardListComponent();
        listComponent.setDatasetRun(listDataSetRun);
        listComponent.setIgnoreWidth(list.isIgnoreWidth());
        listComponent.setPrintOrderValue(list.getOrientation().getPrintOrder());
        listComponent.setContents(createListContents(list, listDataSet));

        JRDesignComponentElement listComponentElement = new JRDesignComponentElement();
        listComponentElement.setComponent(listComponent);
        listComponentElement.setComponentKey(com.sapienter.jbilling.server.invoiceTemplate.domain.List.COMPONENT_KEY);
        listComponentElement.setPositionType(FLOAT);
        listComponentElement.setHeight(list.getHeight());
        listComponentElement.setStretchType(NO_STRETCH);

        Text noDataText = list.getNoDataText();
        if (noDataText != null) {

            noDataText.setTop(0);
            noDataText.setLeft(0);
            noDataText.setWidth(list.getWidth());
            noDataText.setHeight(list.getHeight());

            JRDesignTextElement noDataTextElement = createTextElement(noDataText, NOW);
            if (noDataTextElement instanceof JRDesignTextField) {
                ((JRDesignTextField) noDataTextElement).setStretchWithOverflow(true);
            }

            listFrame.addElement(noDataTextElement);
        }

        listFrame.addElement(listComponentElement);

        JRDesignFrame wrappingFrame = new JRDesignFrame();
        wrappingFrame.setX(list.getLeft());
        wrappingFrame.setY(list.getTop());
        wrappingFrame.setWidth(list.getWidth() + thickness);
        wrappingFrame.setHeight(list.getHeight() + thickness);
        wrappingFrame.setMode(TRANSPARENT);
        wrappingFrame.setStretchType(NO_STRETCH);
        wrappingFrame.addElement(listFrame);

        addElementToCurrentBand(wrappingFrame);
    }

    private JRDesignDatasetRun createListDataSetRun(com.sapienter.jbilling.server.invoiceTemplate.domain.List list) {
        String dataSetName;
        Set<FieldSetup> fields = new HashSet<>();
        switch (list.getSource()) {
            case CDREvents:
                dataSetName = CDR_LINES_DATASET;
                fields.addAll(FieldSetup.CDR_EVENTS_FIELDS);
                break;
            case InvoiceLines:
                dataSetName = INVOICE_LINES_DATASET;
                fields.addAll(FieldSetup.INVOICE_LINES_FIELDS);
                break;
            case Assets:
                dataSetName = ASSETS_DATASET;
                fields.addAll(AssetsCollector.COMMON_FIELDS);
                break;
            default:
                throw new IllegalArgumentException("Unsupported list data source: " + list.getSource());
        }

        String listDataSetName = currentSubReport.getMainDataset().getName() + "_LIST_" + dataSetName + "_" + UUID.randomUUID().toString().replace("-", "_").toUpperCase();

        FieldSetup[] sortCriteria = list.getSortCriteria();
        Collections.addAll(fields, sortCriteria);

        JRDesignDataset listDataSet = new JRDesignDataset(false);
        listDataSet.setName(listDataSetName);
        for (FieldSetup field : fields) {
            setupDatasetField(listDataSet, field);
        }

        setupParameters(new ParameterAggregator(listDataSet));

        for (FieldSetup fs : sortCriteria) {
            try {
                addSortField(fs, listDataSet);
            } catch (JRException e) {
                throw new RuntimeException(e);
            }
        }

        String filterExpr = list.getFilterExpr();
        if (filterExpr != null && !filterExpr.isEmpty()) {
            listDataSet.setFilterExpression(createExpression(Boolean.class, filterExpr));
        }

        try {
            currentSubReport.addDataset(listDataSet);
        } catch (JRException e) {
            throw new RuntimeException(e);
        }

        String listDataSourceName = listDataSourceName(listDataSetName);

        new ParameterAggregator(currentSubReport)
                .addParameter(listDataSourceName, JRDataSource.class);

        subDataSources.put(listDataSourceName, dataSetName);
        subDataSets.put(listDataSourceName, listDataSet);

        JRDesignDatasetRun result = new JRDesignDatasetRun();
        result.setDatasetName(listDataSetName);
        result.setDataSourceExpression(createExpression(JRDataSource.class, "$P{" + listDataSourceName + "}"));
        return result;
    }

    private static String listDataSourceName(String dataSetName) {
        return dataSetName + "_DATA_SRC";
    }

    private ListContents createListContents(com.sapienter.jbilling.server.invoiceTemplate.domain.List list, JRDesignDataset listDataSet) {

        DesignListContents result = new DesignListContents();
        result.setHeight(list.getHeight());
        result.setWidth(list.getWidth());

        JRDesignRectangle rectangle = new JRDesignRectangle();
        rectangle.setMode(OPAQUE);
        rectangle.setBackcolor(list.isTransparent() ? WHITE : convertColor(list.getBgColor()));
        rectangle.setX(0);
        rectangle.setY(0);
        rectangle.setWidth(list.getWidth());
        rectangle.setHeight(list.getHeight());
        rectangle.getLinePen().setLineWidth(0f);
        result.addElement(rectangle);

        for (DocElement docElement : list.getElements()) {
            if (docElement instanceof TextBox) {
                TextBox textBox = (TextBox) docElement;

                if (!textBox.isTransparent() || textBox.getThickness() != 0) {
                    result.addElement(createTextBox(textBox));
                }

                JRDesignTextElement textElement = createTextElement(textBox, textBox.getEvaluationTime());
                if (textElement instanceof JRDesignTextField) {
                    String expression = ((JRDesignTextField) textElement).getExpression().getText();
                    ensureFields(expression, listDataSet);
                }
                result.addElement(textElement);
            }
        }
        return result;
    }

    @Override
    public void accept(Section section) {
        if (section.isPageBreakBefore() && previousSubReport != null) {
            previousSubreportPlaceHolder.setRunToBottom(true);
        }

        String sectionName = SECTION + (sections.size() + 1);
        sections.put(sectionName, null);
        new ParameterAggregator(rootDesign).addParameter(sectionName, JasperReport.class);

        JasperDesign savedSubReport = currentSubReport;
        JRDesignSubreport savedSubreportPlaceHolder = currentSubreportPlaceHolder;
        currentSubreportPlaceHolder = new JRDesignSubreport(rootDesign);
        currentSubreportPlaceHolder.setPositionType(FLOAT);
        currentSubreportPlaceHolder.setX(0);
        currentSubreportPlaceHolder.setY(currentBand.getHeight());
        currentSubreportPlaceHolder.setHeight(0);
        currentSubreportPlaceHolder.setExpression(createExpression(JasperReport.class, "$P{" + sectionName + "}"));
        currentSubreportPlaceHolder.setParametersMapExpression(createExpression(Map.class, "$P{" + REPORT_PARAMETERS_MAP + "}"));
        currentSubreportPlaceHolder.setWidth(columnWidth);
        currentSubreportPlaceHolder.setRunToBottom(section == lastSection);

        addElementToCurrentBand(currentSubreportPlaceHolder);

        currentSubReport = createJasperDesign();

        currentSubReport.setColumnWidth(columnWidth);
        currentSubReport.setPageWidth(columnWidth);
        currentSubReport.setTopMargin(0);
        currentSubReport.setBottomMargin(0);
        currentSubReport.setLeftMargin(0);
        currentSubReport.setRightMargin(0);
        currentSubReport.setName(sectionName);

        JRDesignBand title = runBand(section.getHeader().getElements(), STRETCH);
        currentSubReport.setTitle(title);
        currentSubReport.setSummaryWithPageHeaderAndFooter(true);
        currentSubReport.setSummary(runBand(section.getFooter().getElements(), STRETCH));

        DocElement detailElement = section.detailElement();

        if (detailElement != null) {
            JRDesignBand savedBand = currentBand;
            currentBand = createBand(STRETCH);
            isInDetailSection = true;
            detailElement.visit(this);
            isInDetailSection = false;
            ((JRDesignSection) currentSubReport.getDetailSection()).addBand(currentBand);
            currentBand = savedBand;
        }

        sections.put(sectionName, currentSubReport);
        previousSubReport = currentSubReport;
        previousSubreportPlaceHolder = currentSubreportPlaceHolder;
        currentSubReport = savedSubReport;
        currentSubreportPlaceHolder = savedSubreportPlaceHolder;
    }

    private void convertToJrxml(String sourcePath, String destinationPath) {
        try {
            File file = new File(sourcePath);
            File fileDestinantion = new File(destinationPath);
            JasperReport report = (JasperReport) JRLoader.loadObject(file);
            JRXmlWriter.writeReport(report, destinationPath, "UTF-8");
        } catch (JRException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void accept(SubReport subReport) {

        String subreportName = SUBREPORT + (subReports.size() + 1);
        subReports.put(subreportName, null);

        // we place inline subreport into current report's data set, instead of the root design
        if (null != currentSubReport) {
            new ParameterAggregator(currentSubReport).addParameter(subreportName, JasperReport.class);
        } else {
            new ParameterAggregator(rootDesign).addParameter(subreportName, JasperReport.class);
        }

        // use 'single row dataset' to ensure, that there will be at least one row in detail section to display subreport
        new ParameterAggregator(rootDesign).addParameter(SINGLE_ROW_DATASET + "_" + (subReports.size() + 1), JRDataSource.class);

        if (isInDetailSection) {
            currentSubreportPlaceHolder.setDataSourceExpression(createExpression(JRDataSource.class, "$P{" + SINGLE_ROW_DATASET + "_" + (subReports.size() + 1) + "}"));
        }

        JasperDesign savedSubReport = currentSubReport;
        JRDesignSubreport savedSubreportPlaceHolder = currentSubreportPlaceHolder;
        currentSubreportPlaceHolder = new JRDesignSubreport(rootDesign);
        currentSubreportPlaceHolder.setPositionType(FLOAT);
        currentSubreportPlaceHolder.setMode(OPAQUE);
        currentSubreportPlaceHolder.setX(subReport.getLeft());
        currentSubreportPlaceHolder.setY(subReport.getTop());
        currentSubreportPlaceHolder.setExpression(createExpression(JasperReport.class, "$P{" + subreportName + "}"));
        currentSubreportPlaceHolder.setParametersMapExpression(createExpression(Map.class, "$P{" + REPORT_PARAMETERS_MAP + "}"));

        addElementToCurrentBand(currentSubreportPlaceHolder);

        currentSubReport = createJasperDesign();

        try {
            String path = REPORT_PATH + subReport.fileName;
            convertToJrxml(path + ".jasper", path + ".jrxml");
            currentSubReport = JRXmlLoader.load(path + ".jrxml");
            createJasperDesign(currentSubReport);
        } catch (JRException e) {
            e.printStackTrace();
        }

        currentSubReport.setColumnWidth(columnWidth);
        currentSubReport.setPageWidth(columnWidth);
        currentSubReport.setTopMargin(0);
        currentSubReport.setBottomMargin(0);
        currentSubReport.setLeftMargin(0);
        currentSubReport.setRightMargin(0);
        currentSubReport.setName(subreportName);
        currentSubReport.setSummaryWithPageHeaderAndFooter(false);
        bindReturnValuesFromSubReport((null != savedSubReport) ? savedSubReport : rootDesign, currentSubReport, currentSubreportPlaceHolder);

        String subReportDataSourceName = "SUB_REPORT_DATA_SOURCE_" + subReportDataSources.size();
        String dataSourcePath = subReport.getSubReportDataSourcePath();
        if (isEmpty(dataSourcePath)) {
            JRQuery query = currentSubReport.getQuery();
            if (query != null) {
                dataSourcePath = query.getText();
            }
        }
        SubReportDataSource srds = new SubReportDataSource(subReport.getSubReportDataSourceType(), dataSourcePath);
        subReportDataSources.put(subReportDataSourceName, srds);

        if (SubReportDataSourceType.JBILLING == subReport.getSubReportDataSourceType()) {
            currentSubreportPlaceHolder.setConnectionExpression(createExpression(Connection.class, "$P{" + JBILLING_CONNECTION + "}"));
        } else {
            currentSubreportPlaceHolder.setDataSourceExpression(createExpression(JRDataSource.class, "$P{" + subReportDataSourceName + "}"));
        }

        JasperDesign designToModify = (null != savedSubReport) ? savedSubReport : rootDesign;
        new ParameterAggregator(designToModify).addParameter(subReportDataSourceName, JRDataSource.class);
        if(designToModify.getParametersMap().get(JBILLING_CONNECTION) == null){
            new ParameterAggregator(designToModify).addParameter(JBILLING_CONNECTION, Connection.class);
        }

        subReports.put(subreportName, currentSubReport);
        previousSubReport = currentSubReport;
        previousSubreportPlaceHolder = currentSubreportPlaceHolder;
        currentSubReport = savedSubReport;
        currentSubreportPlaceHolder = savedSubreportPlaceHolder;
    }

    private void bindReturnValuesFromSubReport (JasperDesign report, JasperDesign subReport, JRDesignSubreport subReportPlaceHolder) {
        Map<String, JRVariable> reportVariables = report.getVariablesMap();
        Map<String, JRVariable> subReportVariables = subReport.getVariablesMap();

        for (Map.Entry<String, JRVariable> variableEntry: subReportVariables.entrySet())
            if (!reportVariables.containsKey(variableEntry.getKey())) {
                JRVariable subReportVariable = variableEntry.getValue();
                JRDesignVariable newVariable = new JRDesignVariable();
                newVariable.setName(subReport.getName() + "_" + subReportVariable.getName());
                newVariable.setValueClass(subReportVariable.getValueClass());
                newVariable.setCalculation(CalculationEnum.SYSTEM);

                JRDesignSubreportReturnValue returnValue = new JRDesignSubreportReturnValue();
                returnValue.setSubreportVariable(subReportVariable.getName());
                returnValue.setToVariable(newVariable.getName());
                returnValue.setCalculation(CalculationEnum.NOTHING);
                try {
                    report.addVariable(newVariable);
                    subReportPlaceHolder.addReturnValue(returnValue);
                } catch (JRException e) {
                    e.printStackTrace();
                }
            }
    }

    private TextBox createNoDataTextBox(String noData) {
        TextBox noDataBox = new TextBox();
        noDataBox.setLeft(0);
        noDataBox.setTop(0);
        noDataBox.setWidth(columnWidth);
        noDataBox.setHeight(120);

        noDataBox.setName(currentSubReport.getName() + "_NO_DATA_TEXT_BOX");

        Font noDataFont = new Font();
        noDataFont.setFace(FontFace.DEFAULT);
        noDataFont.setBold(false);
        noDataFont.setColor("#000000");
        noDataFont.setItalic(false);
        noDataFont.setSize(12);

        noDataBox.setFont(noDataFont);

        noDataBox.setRoundCornerRadius(0);
        noDataBox.setPadding(0);
        noDataBox.setExpr(noData);

        return noDataBox;
    }

    private static void addSubreportParameter(JRDesignSubreport jasperDesign, String name, Class<?> clazz) {
        addSubreportParameter(jasperDesign, name, clazz, "$P{" + name + "}");
    }

    private static void addSubreportParameter(JRDesignSubreport jasperDesign, String name, Class<?> clazz, String expression) {
        if (jasperDesign != null) {
            JRDesignSubreportParameter parameter = new JRDesignSubreportParameter();
            parameter.setName(name);
            parameter.setExpression(createExpression(clazz, expression));
            try {
                jasperDesign.addParameter(parameter);
            } catch (JRException e) {
                e.printStackTrace();
            }
        }
    }

    private static JRDesignExpression createExpression(Class<?> clazz, String expt) {
        JRDesignExpression subreportExpr = new JRDesignExpression();
        subreportExpr.setText(expt);
        subreportExpr.setValueClass(clazz);
        return subreportExpr;
    }

    private JasperDesign createJasperDesign() {
        JasperDesign newDesign = new JasperDesign();
        newDesign.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
        setupParameters(new ParameterAggregator(newDesign));
        return newDesign;
    }

    private void createJasperDesign(JasperDesign newDesign) {
        newDesign.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
        setupParameters(new ParameterAggregator(newDesign));
    }

    private void setupParameters(ParameterAggregator aggregator) {
        aggregator.addParameters(parameters);

        for (InvoiceParameters parameter : InvoiceParameters.values()) {
            try {
                aggregator.addParameter(parameter.getName(), parameter.getTypeClass());
            } catch (RuntimeException e) {
                // ignore it for now
            }
        }

        // Add dynamic parameters.
        for (String key : dynamicParameters.keySet()) {
            aggregator.addParameter(key, String.class);
        }
    }

    private JRDesignBand runBand(List<DocElement> elements, SplitTypeEnum stretch) {
        return runBand(elements, createBand(stretch));
    }

    private JRDesignBand runBand(List<DocElement> elements, JRDesignBand band) {
        JRDesignBand savedBand = currentBand;
        currentBand = band;
        for (DocElement element : elements) {
            element.visit(this);
        }
        currentBand = savedBand;
        return band;
    }

    @Override
    public void accept(EventLines eventLines) {
        attachDataSet(eventLines, CDR_LINES_DATASET);

        JRDesignBand columnHeader = createBand(STRETCH);
        JRDesignBand jrDesignBand = createBand(PREVENT);
        ((JRDesignSection) currentSubReport.getDetailSection()).addBand(jrDesignBand);
        ColumnSettings.setupLines(eventLines, columnHeader, jrDesignBand, columnWidth, null);
        JRElement[] elements = columnHeader.getElements();
        int hdrWidth;
        if (elements.length > 0) {
            int last = elements.length - 1;
            hdrWidth = elements[last].getX() + elements[last].getWidth();
        } else {
            hdrWidth = columnWidth;
        }

        currentSubReport.setColumnHeader(columnHeader);
        try {
            addTableLinesGrouping(eventLines, hdrWidth);
            FieldSetup sortCriteria = eventLines.getSortCriterion();
            if (sortCriteria != null) {
                addSortField(sortCriteria);
            }
        } catch (JRException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void accept(Text text) {
        acceptText(text, AUTO);
    }

    @Override
    public void accept(TextBox textBox) {
        if (!textBox.isTransparent() || textBox.getThickness() != 0) {
            acceptBox(textBox);
        }
        acceptText(textBox);
    }

    public void acceptBox(TextBox textBox) {
        addElementToCurrentBand(createTextBox(textBox));
    }

    private JRDesignRectangle createTextBox(TextBox textBox) {
        JRDesignRectangle rectangle = new JRDesignRectangle();
        rectangle.setMode(textBox.isTransparent() ? TRANSPARENT : OPAQUE);
        rectangle.setBackcolor(convertColor(textBox.getBgColor()));
        ConvertUtils.setupRectangle(textBox, rectangle, columnWidth);
        rectangle.setRadius(textBox.getRoundCornerRadius());
        JRPen linePen = rectangle.getLinePen();
        linePen.setLineColor(convertColor(textBox.getBorderColor()));
        linePen.setLineWidth((float)textBox.getThickness());
        return rectangle;
    }

    private void addElementToCurrentBand(JRDesignElement jrElement) {
        addElementToBand(currentBand, jrElement);
    }

    private void addElementToBand(JRDesignBand jrBand, JRDesignElement jrElement) {
        jrElement.setX(jrElement.getX() + currentXOffset);
        jrElement.setY(jrElement.getY() + currentYOffset);
        jrBand.addElement(jrElement);
        jrBand.setHeight(Math.max(jrBand.getHeight(), jrElement.getY() + jrElement.getHeight()));
    }

    public void acceptText(TextBox text) {
        addElementToCurrentBand(createTextElement(text, text.getEvaluationTime()));
    }

    public void acceptText(Text text, EvaluationTimeEnum evaluationTime) {
        addElementToCurrentBand(createTextElement(text, evaluationTime));
    }

    private JRDesignTextElement createTextElement(Text textBox, EvaluationTimeEnum evaluationTime) {
        String expression = textBox.getExpr() == null ? "" : textBox.getExpr();
        JRDesignTextElement designText = createTextElement(expression, evaluationTime, this);
        designText.setHorizontalTextAlign(textBox.getAlignment());
        ConvertUtils.setupFont(textBox.getFont(), designText);
        JRLineBox lineBox = designText.getLineBox();
        lineBox.setPadding(textBox.getPadding());
        ConvertUtils.setupRectangle(textBox, designText, columnWidth);
        // Text boxes are accompanied by rectangles, which handle this
        if (!(textBox instanceof TextBox)) {

            designText.setMode(textBox.isTransparent() ? TRANSPARENT : OPAQUE);
            designText.setBackcolor(convertColor(textBox.getBgColor()));

            JRBoxPen pen = lineBox.getPen();
            pen.setLineColor(convertColor(textBox.getBorderColor()));
            pen.setLineWidth((float)textBox.getThickness());
        }
        return designText;
    }

    public static JRDesignTextElement createTextElement(String expression) {
        return createTextElement(expression, EvaluationTimeEnum.AUTO);
    }

    public static JRDesignTextElement createTextElement(String expression, EvaluationTimeEnum evaluationTime) {
        return createTextElement(expression, evaluationTime, null);
    }

    public static JRDesignTextElement createTextElement(String expression, EvaluationTimeEnum evaluationTime, ReportBuildVisitor visitor) {
        if (JR_EXPRESSION_PATTERN.matcher(expression).find() || CUSTOM_EXPRESSION_PATTERN.matcher(expression).find()) {
            JRDesignTextField textField = new JRDesignTextField();
            if (visitor == null) {
                textField.setExpression(createExpression(String.class, "String.valueOf(" + wrapExpression(expression) + ")"));
            } else {
                textField.setExpression(createExpression(String.class, "String.valueOf(" + visitor.ensureParameters(wrapExpression(expression)) + ")"));
            }
            textField.setEvaluationTime(evaluationTime);
            return textField;
        } else {
            JRDesignStaticText text = new JRDesignStaticText();
            text.setText(expression);
            return text;
        }
    }

    /**
     * Ensures that all parameters mentioned in the expression provided
     * exist in parameters map and excludes them if it do not exist, since some parameters
     * are dynamic and optional, and if they are missed, that could cause Jasper exception
     *
     * @param expression expression to check
     * @return new expression referencing only existing properties
     */
    private String ensureParameters(String expression) {
        Matcher matcher = JR_PARAMETER_PATTERN.matcher(expression);
        while (matcher.find()) {
            String parameterExpression = matcher.group();
            String parameterName = parameterExpression.substring(3, parameterExpression.length() - 1);
            if (parameterName.startsWith("__") && !parameters.containsKey(parameterName)) { // <-- we check only dynamic optional parameters having '__' prefix
                expression = expression.replace(parameterExpression, "\"\"");
            }
        }
        return expression;
    }

    private String ensureFields(String expression, JRDesignDataset dataSet) {
        Matcher matcher = JR_FIELD_PATTERN.matcher(expression);
        while (matcher.find()) {
            String fieldExpression = matcher.group();
            String fieldName = fieldExpression.substring(3, fieldExpression.length() - 1);
            if (fieldName.startsWith("__")) { // <-- we check only dynamic optional fields having '__' prefix
                setupDatasetField(dataSet, new FieldSetup(fieldName, fieldName, Field, String.class));
            }
        }
        return expression;
    }

    private static String wrapExpression(String expression) {
        if (!CUSTOM_EXPRESSION_PATTERN.matcher(expression).find()) {
            return expression;
        }
        int start = 0;
        StringBuilder sb = new StringBuilder();
        Matcher startExprMatcher = Pattern.compile("\\$E\\{").matcher(expression);
        while (startExprMatcher.find(start)) {
            int rs = startExprMatcher.start();
            if (rs > start) {
                sb.append(expression.substring(start, rs));
            }
            start = extractExpression(expression, rs, sb);
        }
        sb.append(expression.substring(start));
        return sb.length() == 0 ? "\"\"" : sb.toString();
    }

    private static int extractExpression(String expression, int start, StringBuilder sb) {
        Stack<Integer> stack = new Stack<>();
        int exprStart = start + 3;
        int end = expression.length();
        for (int i = exprStart; i < end; i++) {
            char c = expression.charAt(i);
            switch (c) {
                case '{':
                    stack.push(i);
                    break;
                case '}':
                    if (stack.empty()) {
                        sb.append("String.valueOf(").append(expression.substring(exprStart, i)).append(")");
                        return i + 1;
                    } else {
                        stack.pop();
                    }
            }
        }
        return end;
    }

    @Override
    public void accept(InvoiceLines invoiceLines) {

        //TODO double minimalTotal = -Double.MAX_VALUE;
        attachDataSet(invoiceLines, INVOICE_LINES_DATASET);

        JRDesignBand columnHeader = createBand(STRETCH);
        JRDesignBand jrDesignBand = createBand(PREVENT);
        ((JRDesignSection) currentSubReport.getDetailSection()).addBand(jrDesignBand);
        ColumnSettings.setupLines(invoiceLines, columnHeader, jrDesignBand, columnWidth, dynamicParameters.get(LOCALE_MONEY));
        JRElement[] elements = columnHeader.getElements();
        int hdrWidth;
        if (elements.length > 0) {
            int last = elements.length - 1;
            hdrWidth = (elements[last].getX() + elements[last].getWidth());
        } else {
            hdrWidth = columnWidth;
        }

        currentSubReport.setColumnHeader(columnHeader);
        try {
            addTableLinesGrouping(invoiceLines, hdrWidth);
            addSorting(invoiceLines);
        } catch (JRException e) {
            throw new RuntimeException(e);
        }

    }

    private void addSorting(InvoiceLines invoiceLines) throws JRException {
        FieldSetup sortCriteria = invoiceLines.getSortCriterion();
        if (sortCriteria != null) {
            addSortField(sortCriteria);
        }
    }

    private void addTableLinesGrouping(TableLines lines, int width) throws JRException {
        FieldSetup groupCriteria = lines.getGroupCriteria();
        if (groupCriteria != null) {
            addGroup(lines, lines.findColumn(groupCriteria), width, true);
        }
        FieldSetup addGroupCriteria = lines.getAddGroupCriteria();
        if (addGroupCriteria != null) {
            addGroup(lines, lines.findColumn(addGroupCriteria), width, false);
        }
    }

    private void addSortField(FieldSetup fieldToSort) throws JRException {
        addSortField(fieldToSort, currentSubReport.getMainDesignDataset());
    }

    private static void addSortField(FieldSetup fieldToSort, JRDesignDataset dataset) throws JRException {
        JRSortField[] sortFields = dataset.getSortFields();
        for (JRSortField sortField : sortFields) {
            if (sortField.getName().equals(fieldToSort.getName())) {
                return;
            }
        }
        JRDesignSortField sortField = new JRDesignSortField(fieldToSort.getName(), SortFieldTypeEnum.FIELD, SortOrderEnum.ASCENDING);
        dataset.addSortField(sortField);
    }

    private void addGroup(TableLines lines, ColumnSettings cs, int tableWidth, boolean isMainGroup) throws JRException {
        addSortField(cs.field);
        currentSubReport.addGroup(createGroup(lines, cs.field, cs.formatter, tableWidth, isMainGroup));
    }

    private static JRDesignGroup createGroup(TableLines lines, FieldSetup fieldSetup, String formatter, int width, boolean isMainGroup) throws JRException {

        JRDesignExpression expression = fieldSetup.createExpression(lines, formatter, null);

        JRDesignGroup jrDesignGroup = new JRDesignGroup();
        jrDesignGroup.setName(fieldSetup.getName());
        jrDesignGroup.setExpression(expression);

        JRDesignSection groupHeaderSection = (JRDesignSection) jrDesignGroup.getGroupHeaderSection();

        Font headerFont;
        String bgColor;
        if (isMainGroup) {
            headerFont = lines.getGroupHeaderFont();
            bgColor = lines.getGroupBgColor();
        } else {
            headerFont = lines.getAddGroupHeaderFont();
            bgColor = lines.getAddGroupBgColor();
        }

        JRDesignTextField headerStripe = new JRDesignTextField();
        int height = headerFont.getSize() * 5 / 2;
        headerStripe.setHeight(height);
        headerStripe.setX(0);
        headerStripe.setY(0);
        headerStripe.setWidth(width);
        headerStripe.setMode(OPAQUE);
        headerStripe.getLineBox().setPadding(height / 5);
        headerStripe.setBackcolor(convertColor(bgColor));
        headerStripe.setExpression(expression);
        headerStripe.setBlankWhenNull(true);
        headerStripe.setStretchWithOverflow(true);
        ConvertUtils.setupFont(headerFont, headerStripe);
        JRDesignBand band = createBand(STRETCH);
        band.setHeight(height);
        groupHeaderSection.addBand(band);
        band.addElement(headerStripe);

        return jrDesignGroup;
    }

    private static JRDesignBand createBand(SplitTypeEnum stretch) {
        JRDesignBand band = new JRDesignBand();
        band.setSplitType(stretch);
        return band;
    }

    private static String resolveImageExtension(String base64) {
        if (base64.contains("gif")) {
            return ".gif";
        } else if (base64.contains("jpeg")) {
            return ".jpg";
        } else if (base64.contains("png")) {
            return ".png";
        } else if (base64.contains("tiff")) {
            return ".tiff";
        } else {
            return "";
        }
    }

    @Override
    public void accept(com.sapienter.jbilling.server.invoiceTemplate.domain.Image image) {
        String imageUrl = image.getImageUrl();
        if (imageUrl == null || imageUrl.isEmpty()) {
            return; // <-- we won't draw empty images since URL is required property
        }
        JRDesignImage designImage = new JRDesignImage(rootDesign);
        designImage.setPositionType(PositionTypeEnum.FIX_RELATIVE_TO_TOP);
        JRDesignExpression jrImageExpression = new JRDesignExpression();
        if (image.getImageSource() == Image.Source.File) { // we need to create temp file with base64 image parsed
            String[] parts = imageUrl.split(","); // data:<mime-type>;base54,<base64Data>
            byte[] imageData = DatatypeConverter.parseBase64Binary(parts[1]);

        /*if (image.getImageSource() == Image.Source.File) {
            @SuppressWarnings("unchecked")
            Map<String, InvoiceTemplateFileDTO> files = (Map<String, InvoiceTemplateFileDTO>) resources.get(InvoiceTemplateFileDTO.class);
            InvoiceTemplateFileDTO file = files.get(imageUrl);
            byte[] imageData = file.getData();*/

            File tempDir = com.google.common.io.Files.createTempDir();
            String tempDirPath = tempDir.getAbsolutePath();
            File imageFile = new File(tempDirPath + File.separator + "image");
            try {
                if (imageFile.createNewFile()) {
                    FileOutputStream outputStream = null;
                    try {
                        outputStream = new FileOutputStream(imageFile);
                        outputStream.write(imageData);
                    } finally {
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    }
                    imageUrl = imageFile.getCanonicalPath();
                    if (File.separator.equals("\\")) { // replace Windows separator
                        imageUrl = imageUrl.replace("\\", "\\\\");
                    }
                }
            } catch (IOException e) {
                imageUrl = "";
            }
        }
        jrImageExpression.setText("\"" + imageUrl + "\"");
        jrImageExpression.setValueClass(String.class);
        designImage.setExpression(jrImageExpression);
        ConvertUtils.setupRectangle(image, designImage, columnWidth);
        /**
         * If image is marked as background than add it in list which in the end add it to Root design.
         * Else it will go to current band.
         * */
        if (image.isBackground()) {
            backgroundImages.add(designImage);
        } else {
            addElementToCurrentBand(designImage);
        }
    }

    private void putPageParameters(DocDesign docDesign) {

        rootDesign.setOrientation(docDesign.orientation());
        rootDesign.setPageWidth(docDesign.pageWidth());
        rootDesign.setPageHeight(docDesign.pageHeight());

        rootDesign.setTopMargin(docDesign.getMarginTop());
        rootDesign.setBottomMargin(docDesign.getMarginBottom());
        rootDesign.setLeftMargin(docDesign.getMarginLeft());
        rootDesign.setRightMargin(docDesign.getMarginRight());
        rootDesign.setColumnWidth(columnWidth = docDesign.columnWidth());

        pageHeader = runBand(docDesign.getPageHeader().getElements(), PREVENT);
        pageFooter = runBand(docDesign.getPageFooter().getElements(), PREVENT);

        rootDesign.setPageHeader(pageHeader);
        rootDesign.setPageFooter(pageFooter);
    }

    public InvoiceTemplateBL createInvoiceTemplateBL() {
        Map<String, JasperDesign> allSubreports = new HashMap<>(sections);
        allSubreports.putAll(subReports);
        return new InvoiceTemplateBL(rootDesign, allSubreports, subDataSources, subDataSets, subReportDataSources);
    }

    /**
     * Process images marked as Background.Jasper has special band to display at background.
     * */
    private void processBackGroundImages(JasperDesign jasperDesign){
        JRDesignBand band = createBand(STRETCH);
        // Set maximum height for background band.
        band.setHeight(jasperDesign.getPageHeight() - jasperDesign.getTopMargin() - jasperDesign.getBottomMargin());
        // Iterate over all images and add it to background band.
        backgroundImages.stream().forEach(image ->
                addElementToBand(band, image)
        );
        //Set band as background
        jasperDesign.setBackground(band);
    }
}
