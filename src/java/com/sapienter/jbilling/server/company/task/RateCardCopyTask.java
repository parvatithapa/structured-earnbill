package com.sapienter.jbilling.server.company.task;

import au.com.bytecode.opencsv.CSVWriter;
import com.sapienter.jbilling.server.pricing.RateCardBL;
import com.sapienter.jbilling.server.pricing.RateCardWS;
import com.sapienter.jbilling.server.pricing.db.RateCardDAS;
import com.sapienter.jbilling.server.pricing.db.RateCardDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.util.sql.JDBCUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang.StringUtils;
import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.UUID;

/**
 * Created by vivek on 28/11/14.
 */
public class RateCardCopyTask extends AbstractCopyTask {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    RateCardDAS rateCardDAS;
    CompanyDAS companyDAS;

    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<RateCardDTO> rateCardDTOs = new RateCardDAS().getRateCardsByEntity(entityId);
        return rateCardDTOs != null && !rateCardDTOs.isEmpty();
    }

    public RateCardCopyTask() {
        init();
    }

    private void init() {
        rateCardDAS = new RateCardDAS();
        companyDAS = new CompanyDAS();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        logger.debug("create RateCardCopyTask");
        List<RateCardDTO> rateCardDTOs = rateCardDAS.getRateCardsByEntity(entityId);
        List<RateCardDTO> copyRateCardDTOs = rateCardDAS.getRateCardsByEntity(targetEntityId);
        if (copyRateCardDTOs.isEmpty()) {
            for (RateCardDTO rateCardDTO : rateCardDTOs) {
                String randomUUID = rateCardDTO.getTableName() + "_" + UUID.randomUUID().toString().replace("-", "");
                randomUUID = JDBCUtils.getDatabaseObjectName(randomUUID);
                RateCardWS rateCardWS = RateCardBL.getWS(rateCardDTO);
                rateCardWS.setId(null);
                rateCardWS.setTableName(rateCardDTO.getTableName());
                rateCardWS.getChildCompanies().remove(targetEntityId);
                rateCardWS.getChildCompanies().add(entityId);
                RateCardDTO copyRateCardDTO = RateCardBL.getDTO(rateCardWS);
                File tempFile = new File(randomUUID + ".csv");
                try {
                    tempFile = createCSVFileFromTable(copyRateCardDTO, randomUUID);
                } catch (IOException ioex) {
                    logger.error("Caught IO Exception Here ", ioex);
                }
                rateCardWS.setTableName(randomUUID);
                createRateCard(rateCardWS, tempFile, targetEntityId);
            }
        }
        logger.debug("RateCardCopyTask has been completed.");
    }

    /*
    * Rate Card
    */
    public Integer createRateCard(RateCardWS rateCardWs, File rateCardFile, Integer targetEntityId) {
        RateCardDTO rateCardDTO = RateCardBL.getDTO(rateCardWs);
        rateCardDTO.setCompany(new CompanyDAS().find(targetEntityId));
        rateCardDTO.setGlobal(rateCardWs.isGlobal());
        return new RateCardBL().create(rateCardDTO, rateCardFile, true);
    }

    private File createCSVFileFromTable(RateCardDTO rateCard, String randomTableName) throws IOException {
        RateCardBL rateCardService = new RateCardBL(rateCard);

        // outfile
        File file = File.createTempFile(randomTableName, ".csv");

        // write csv header
        List<String> columns = rateCardService.getRateTableColumnNames();

        // read rows and write file
        ScrollableResults resultSet = null;

        try (CSVWriter writer = new CSVWriter(new FileWriter(file), ',')) {
            writer.writeNext(columns.toArray(new String[columns.size()]));
            resultSet = rateCardService.getRateTableRows();
            while (resultSet.next()) {
                writer.writeNext(convertToString(resultSet.get()));
            }
        } finally {
            try { resultSet.close(); } catch (Exception t) { logger.error(t.toString()); }
        }

        return file;
    }

    public String[] convertToString(Object[] objects) {
        String[] strings = new String[objects.length];

        int i = 0;
        for (Object object : objects) {
            if (object != null) {
                Converter converter = ConvertUtils.lookup(object.getClass());
                if (converter != null) {
                    strings[i++] = converter.convert(object.getClass(), object).toString();
                } else {
                    strings[i++] = object.toString();
                }
            } else {
                strings[i++] = StringUtils.EMPTY;
            }
        }
        return strings;
    }
}