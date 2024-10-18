package com.sapienter.jbilling.server.company.task;

import au.com.bytecode.opencsv.CSVWriter;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.pricing.RouteBL;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.user.RouteWS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.log4j.Logger;
import org.hibernate.ScrollableResults;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Created by vivek on 4/12/14.
 */
public class DataTableCopyTask extends AbstractCopyTask {
    RouteDAS routeDAS = null;
    CompanyDAS companyDAS = null;
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(DataTableCopyTask.class));
    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<RouteDTO> routeDTOs = routeDAS.getRoutes(targetEntityId);
        return routeDTOs != null && !routeDTOs.isEmpty();
    }

    public DataTableCopyTask() {
        init();
    }

    private void init() {
        routeDAS = new RouteDAS();
        companyDAS = new CompanyDAS();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create DataTableCopyTask");
        List<RouteDTO> routeDTOs = routeDAS.getRoutes(entityId);
        List<RouteDTO> copyRouteDTOs = routeDAS.getRoutes(targetEntityId);
        if (copyRouteDTOs.isEmpty()) {
            for (RouteDTO routeDTO : routeDTOs) {
                String randomUUID = routeDTO.getTableName() + "_" + UUID.randomUUID().toString().replace("-", "");
                RouteWS routeWS = new RouteBL(routeDTO).toWS();
                routeWS.setId(null);
                routeWS.setEntityId(targetEntityId);
                routeWS.setTableName(randomUUID);
                File tempFile = new File(routeWS.getTableName() + ".csv");
                try {
                    tempFile = createCSVFileFromTable(routeDTO);
                } catch (IOException ioex) {
                    LOG.debug("Caught IO Exception Here " + ioex);
                }

                Integer copyRouteId = createRoute(routeWS, tempFile, targetEntityId);
                CopyCompanyUtils.oldNewDataTableMap.put(routeDTO.getId(), copyRouteId);
            }
            LOG.debug("DataTableCopyTask has been completed");
        }
    }

    private File createCSVFileFromTable(RouteDTO routeDTO) throws IOException {
//        RouteDTO route = params.id ? RouteDTO.get(params.int('id')) : null

        RouteBL rateCardService = new RouteBL(routeDTO);

        // outfile
        File file = File.createTempFile(routeDTO.getTableName(), ".csv");
        CSVWriter writer = new CSVWriter(new FileWriter(file), ',');

        // write csv header
        List<String> columns = rateCardService.getRouteTableColumnNames();
        if (columns.contains("id")) {
            columns.remove("id");
        }
        writer.writeNext(columns.toArray(new String[columns.size()]));

        // read rows and write file
//        def exporter = CsvExporter.createExporter(RouteDTO.class)
        ScrollableResults resultSet = rateCardService.getRouteTableRows();
        if(resultSet != null) {
            try {
                while (resultSet.next()) {
                    writer.writeNext(convertToString(resultSet.get()));
                }
            } finally {
                resultSet.close();
            }
        }

        writer.close();

        // send file

        return file;
    }

    public String[] convertToString(Object[] objects) {
        String[] strings = new String[objects.length - 1];

        int i = 0;
        int index = 0;
        for (Object object : objects) {
            if (index != 0) {
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
            } else {
                index++;
            }
        }
        return strings;
    }

    /*
     * Route Based Rating
     */
    public Integer createRoute(RouteWS routeWS, File routeFile, Integer targetEntityId) throws SessionInternalError {
        RouteBL routeBL = new RouteBL();

        CompanyDTO company = companyDAS.find(targetEntityId);

        RouteDTO routeDTO = routeBL.toDTO(routeWS);
        routeDTO.setCompany(company);
        routeDTO.setName(routeWS.getName());
        routeDTO.setTableName(routeBL.createRouteTableName(
                company.getId(), routeDTO.getName()));

        RouteDTO rootRoute = routeBL.getRootRouteTable(company.getId());
        if (routeWS.getRootTable() != null && routeWS.getRootTable()) {
            if ((null != rootRoute &&
                    (null == routeWS.getId() || routeWS.getId() <= 0)) ||

                    (null != rootRoute && null != routeWS.getId() &&
                            routeWS.getId() > 0 && rootRoute.getId() != routeWS.getId().intValue())
                    ) {

                throw new SessionInternalError("There can be only one root table per company",
                        new String[]{"RouteWS,rootTable,route.validation.only.one.root.table.allowed"});

            }
        }
        return new RouteBL().create(routeDTO, routeFile);
    }
}