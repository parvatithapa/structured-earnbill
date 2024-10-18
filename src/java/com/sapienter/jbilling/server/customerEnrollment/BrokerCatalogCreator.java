package com.sapienter.jbilling.server.customerEnrollment;

import com.googlecode.jcsv.writer.CSVWriter;
import com.googlecode.jcsv.writer.internal.CSVWriterBuilder;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.customerEnrollment.csv.BrokerCatalogResponse;
import com.sapienter.jbilling.server.customerEnrollment.csv.BrokerCatalogResponseEntryConverter;
import com.sapienter.jbilling.server.customerEnrollment.csv.CustomerEnrollmentCSVColumnJoiner;
import com.sapienter.jbilling.server.customerEnrollment.csv.CustomerEnrollmentCSVStrategy;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pricing.RouteBL;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.pricing.strategy.BlockIndexRouteRateCardStrategy;
import com.sapienter.jbilling.server.pricing.strategy.TeaserPricingStrategy;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.ScrollableResults;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class BrokerCatalogCreator {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BrokerCatalogCreator.class));

    private final CustomerEnrollmentCSVStrategy csvStrategy = new CustomerEnrollmentCSVStrategy();
    private final BrokerCatalogResponseEntryConverter csvEntryConverter = new BrokerCatalogResponseEntryConverter();
    private final CustomerEnrollmentCSVColumnJoiner customerEnrollmentCSVColumnJoiner = new CustomerEnrollmentCSVColumnJoiner();
    private final PlanDAS planDAS = new PlanDAS();
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    private final String header = "\"Product Id\",\"Product Name\",\"Active Start Date\",\"Active End Date\",\"Division\",\"LDC\",\"Commodity\",\"Customer Type\",\"Billing Model\",\"Product Type\",\"Rate\",\"ETF\",\"Term\",\"Discount Period\",\"Block Size\"";

    enum ProductType {
        FP, VP, BI, TP
    }

    public File create(Integer companyId, String catalogVersion) {
        List<PlanDTO> plans = planDAS.findAllActive(companyId);
        List<BrokerCatalogResponse> brokerCatalogResponses = new ArrayList<>(plans.size());

        BrokerCatalogResponse brokerCatalogResponse;
        for (PlanDTO plan : plans) {
            brokerCatalogResponse = this.generateBrokerCatalogResponse(companyId, plan, catalogVersion);

            if (brokerCatalogResponse != null) {
                brokerCatalogResponses.add(brokerCatalogResponse);
            }
        }

        return this.generateResponseFile(brokerCatalogResponses, companyId, catalogVersion);
    }

    private BrokerCatalogResponse generateBrokerCatalogResponse(Integer companyId, PlanDTO plan, String catalogVersion) {
        BrokerCatalogResponse brokerCatalogResponse = new BrokerCatalogResponse();

        ItemDTO item = plan.getItem();
        String planInternalNumber = item.getInternalNumber();

        brokerCatalogResponse.setProductId(catalogVersion + planInternalNumber);
        brokerCatalogResponse.setProductName(item.getDescription());
        brokerCatalogResponse.setActiveStartDate(item.getActiveSince());
        brokerCatalogResponse.setActiveEndDate(item.getActiveUntil());
        brokerCatalogResponse.setDivision((String) this.getMetaFieldValue(plan, FileConstants.DIVISION));
        brokerCatalogResponse.setLdc(item.getEntity().getDescription());
        brokerCatalogResponse.setCommodity(String.valueOf(item.getDescription().charAt(0)));

        RouteDTO route = new RouteDAS().getRouteByName(companyId, "Plans");
        if (route != null) {
            ScrollableResults routeTableRows = new RouteBL(route).getRouteTableRows();
            try {
                while (routeTableRows.next()) {
                    if (routeTableRows.get(3).equals(planInternalNumber)) {
                        brokerCatalogResponse.setCustomerType(String.valueOf(((String) routeTableRows.get(5)).charAt(0)));
                        break;
                    }
                }
            } finally {
                routeTableRows.close();
            }
        }

        String billingModel = (String) this.getMetaFieldValue(plan, FileConstants.BILLING_MODEL);
        if (billingModel != null) {
            brokerCatalogResponse.setBillingModel(String.valueOf((billingModel).charAt(0)));
        }

        //Product Type
        if (!plan.getPlanItems().isEmpty()) {
            SortedMap<Date, PriceModelDTO> planPriceModels = plan.getPlanItems().get(0).getModels();
            PriceModelDTO planPriceModel = planPriceModels.get(planPriceModels.firstKey());
            String productType;
            switch (planPriceModel.getType()) {
                case FLAT:                  productType = ProductType.FP.name(); break;
                case ROUTE_BASED_RATE_CARD: productType = ProductType.VP.name(); break;
                case BLOCK_AND_INDEX:       productType = ProductType.BI.name(); break;
                case TEASER_PRICING:        productType = ProductType.TP.name(); break;
                default:                    productType = "";
            }
            brokerCatalogResponse.setProductType(productType);

            //Rate
            BigDecimal rate = null;
            if (productType.equals(ProductType.FP.name())) {
                rate = planPriceModel.getRate();
            }
            else if (productType.equals(ProductType.BI.name())) {
                String blockRate = planPriceModel.getAttributes().get(BlockIndexRouteRateCardStrategy.PARAM_BLOCK_RATE);
                rate = StringUtils.isNotEmpty(blockRate) ? new BigDecimal(blockRate) : null;
            }
            else if (productType.equals(ProductType.TP.name())) {
                String rateParam = planPriceModel.getAttributes().get(TeaserPricingStrategy.FIRST_PERIOD);
                rate = StringUtils.isNotEmpty(rateParam) ? new BigDecimal(rateParam) : null;
            }
            brokerCatalogResponse.setRate(rate);

            MetaFieldValue earlyTerminationFeeAmountMetaFieldValue = plan.getMetaField(FileConstants.EARLY_TERMINATION_FEE_AMOUNT_META_FIELD);
            brokerCatalogResponse.setEtf(earlyTerminationFeeAmountMetaFieldValue != null ? (BigDecimal) earlyTerminationFeeAmountMetaFieldValue.getValue() : null);

            MetaFieldValue durationMetaFieldValue = plan.getMetaField(FileConstants.DURATION);
            brokerCatalogResponse.setTerm(durationMetaFieldValue != null ? Integer.valueOf((String) durationMetaFieldValue.getValue()) : null);

            //Discount Period
            Integer discountPeriod = (productType.equals(ProductType.TP.name())) ? Integer.valueOf(TeaserPricingStrategy.FIRST_PERIOD) : null;
            brokerCatalogResponse.setDiscountPeriod(discountPeriod);

            //Block Size
            Integer blockSize = null;
            if (productType.equals(ProductType.BI.name())) {
                String blockQuantity = planPriceModel.getAttributes().get(BlockIndexRouteRateCardStrategy.PARAM_BLOCK_QUANTITY);
                blockSize = StringUtils.isNotEmpty(blockQuantity) ? new Integer(blockQuantity) : null;
            }
            brokerCatalogResponse.setBlockSize(blockSize);
        }

        return brokerCatalogResponse;
    }

    private Object getMetaFieldValue(CustomizedEntity entity, String name) {
        MetaFieldValue metaFieldValue = entity.getMetaField(name);
        return metaFieldValue != null ? metaFieldValue.getValue() : null;
    }

    private File generateResponseFile(List<BrokerCatalogResponse> brokerCatalogResponses, Integer companyId, String catalogVersion) {
        File file = null;

        try {
            file = Paths.get(this.getCatalogFolderPath(companyId), catalogVersion + "-" + dateFormat.format(TimezoneHelper.serverCurrentDate()) + ".csv").toFile();
            Writer writer = new FileWriter(file).append(header).append(System.lineSeparator());
            CSVWriter<BrokerCatalogResponse> csvWriter = new CSVWriterBuilder<BrokerCatalogResponse>(writer).strategy(csvStrategy).entryConverter(csvEntryConverter).columnJoiner(customerEnrollmentCSVColumnJoiner).build();
            csvWriter.writeAll(brokerCatalogResponses);
            csvWriter.close();
        }
        catch (IOException e) {
            LOG.debug(e.getMessage());
        }

        return file;
    }

    private String getCatalogFolderPath(Integer companyId) throws IOException {
        Path catalogFolderPath = Paths.get(Util.getSysProp("base_dir"), FileConstants.CUSTOMER_ENROLLMENT_FOLDER, companyId.toString(), "catalog");

        if (!Files.exists(catalogFolderPath)) {
            Files.createDirectories(catalogFolderPath);
        }

        return catalogFolderPath.toString();
    }
}