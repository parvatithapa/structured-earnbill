package com.sapienter.jbilling.server.company.task;

import com.opencsv.CSVWriter;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.pricing.RouteBasedRateCardBL;
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDAS;
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDTO;
import com.sapienter.jbilling.server.user.RouteRateCardWS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.MatchingFieldDAS;
import com.sapienter.jbilling.server.user.db.MatchingFieldDTO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.log4j.Logger;
import org.hibernate.ScrollableResults;

/**
 * Created by vivek on 4/12/14.
 */
public class RouteRateCardCopyTask extends AbstractCopyTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(RouteRateCardCopyTask.class));

    RouteRateCardDAS routeRateCardDAS = null;
    CompanyDAS companyDAS = null;
    MatchingFieldDAS matchingFieldDAS = null;

    private static final Class dependencies[] = new Class[]{
            RatingUnitCopyTask.class
    };

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<RouteRateCardDTO> routeRateCardDTOs = routeRateCardDAS.getRouteRateCardsByEntity(targetEntityId);
        return routeRateCardDTOs != null && !routeRateCardDTOs.isEmpty();
    }

    public RouteRateCardCopyTask() {
        init();
    }

    private void init() {
        routeRateCardDAS = new RouteRateCardDAS();
        companyDAS = new CompanyDAS();
        matchingFieldDAS = new MatchingFieldDAS();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create ReportCopyTask");
        List<RouteRateCardDTO> routeRateCardDTOs = routeRateCardDAS.getRouteRateCardsByEntity(entityId);
        List<RouteRateCardDTO> copyRouteRateCardDTOs = routeRateCardDAS.getRouteRateCardsByEntity(targetEntityId);
        if (copyRouteRateCardDTOs.isEmpty()) {
            for (RouteRateCardDTO routeRateCardDTO : routeRateCardDTOs) {
                RouteRateCardWS routeRateCardWS = new RouteBasedRateCardBL(routeRateCardDTO).getWS();
                String randomUUID = routeRateCardDTO.getTableName() + "_" + UUID.randomUUID().toString().replace("-", "");
                routeRateCardWS.setId(null);
                routeRateCardWS.setEntityId(targetEntityId);
                routeRateCardWS.setTableName(randomUUID);
                File tempFile = new File(routeRateCardWS.getTableName() + ".csv");
                try {
                    tempFile = createCSVFileFromTable(routeRateCardDTO);
                    Integer routeRateCardId = createRouteRateCard(routeRateCardWS, tempFile, targetEntityId);

                    CopyCompanyUtils.oldNewRouteRateCardMap.put(routeRateCardDTO.getId(), routeRateCardId);

                    RouteRateCardDTO copyRouteRateCardDTO = routeRateCardDAS.find(routeRateCardId);

                    List<MatchingFieldDTO> matchingFieldDTOs = matchingFieldDAS.getMatchingFieldsByRouteRateCardId(routeRateCardDTO.getId());
                    for (MatchingFieldDTO matchingFieldDTO : matchingFieldDTOs) {
                        MatchingFieldDTO copyMatchingFieldDTO = createMatchingField(matchingFieldDTO, routeRateCardDTO, copyRouteRateCardDTO);
                        matchingFieldDAS.save(copyMatchingFieldDTO);
                    }
                } catch (IOException ioex) {
                    LOG.debug("Caught IO Exception Here " + ioex);
                }
            }
            LOG.debug("Route Rate card has been completed");
        }
    }

    private File createCSVFileFromTable(RouteRateCardDTO routeRateCardDTO) throws IOException {
        RouteBasedRateCardBL routeRateCardService = new RouteBasedRateCardBL(routeRateCardDTO);

        // outfile
        File file = File.createTempFile(routeRateCardDTO.getTableName(), ".csv");
        CSVWriter writer = new CSVWriter(new FileWriter(file));

        // write csv header
        List<String> columns = routeRateCardService.getRouteTableColumnNames(routeRateCardDTO.getTableName());
        writer.writeNext(columns.toArray(new String[columns.size()]));

        ScrollableResults resultSet = null;

        // read rows and write file
        try{
            resultSet = routeRateCardService.getRouteTableRows();
            while (resultSet.next()) {
                writer.writeNext(convertToString(resultSet.get()));
            }
        }finally {
            try { resultSet.close(); } catch (Throwable t) { LOG.error(t); }
            writer.close();
        }

        // send file

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
                strings[i++] = "";
            }
        }
        return strings;
    }

    public Integer createRouteRateCard(RouteRateCardWS routeRateCardWS, File routeRateCardFile, Integer targetEntityId) throws SessionInternalError {
        RouteRateCardDTO routeRateCardDTO = RouteBasedRateCardBL.getRouteRateCardDTO(routeRateCardWS, targetEntityId);
        CompanyDTO targetEntity = companyDAS.find(targetEntityId);
        routeRateCardDTO.setCompany(targetEntity);
        routeRateCardDTO.setName(routeRateCardWS.getName());
        routeRateCardDTO.setTableName(new RouteBasedRateCardBL().createRouteRateCardTableName(targetEntityId, routeRateCardWS.getName()));
        return new RouteBasedRateCardBL().create(routeRateCardDTO, routeRateCardFile);
    }

    private MatchingFieldDTO createMatchingField(MatchingFieldDTO matchingFieldDTO, RouteRateCardDTO routeRateCardDTO, RouteRateCardDTO copyRouteRateCardDTO) {
        MatchingFieldDTO copyMatchingFieldDTO = new MatchingFieldDTO();
        copyMatchingFieldDTO.setDescription(matchingFieldDTO.getDescription());
        copyMatchingFieldDTO.setOrderSequence(matchingFieldDTO.getOrderSequence());
        copyMatchingFieldDTO.setRequired(matchingFieldDTO.getRequired());
        copyMatchingFieldDTO.setMediationField(matchingFieldDTO.getMatchingField());
        copyMatchingFieldDTO.setMatchingField(matchingFieldDTO.getMatchingField());
        copyMatchingFieldDTO.setType(matchingFieldDTO.getType());
        copyMatchingFieldDTO.setRouteRateCard(copyRouteRateCardDTO);
        copyMatchingFieldDTO.setMandatoryFieldsQuery("obsoleted");
        return copyMatchingFieldDTO;
    }
}
