/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */
package com.sapienter.jbilling.server.pricing;

import au.com.bytecode.opencsv.CSVReader;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.user.MatchingFieldWS;
import com.sapienter.jbilling.server.user.RouteWS;
import com.sapienter.jbilling.server.user.db.MatchingFieldDAS;
import com.sapienter.jbilling.server.user.db.MatchingFieldDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.sql.JDBCUtils;
import com.sapienter.jbilling.server.util.sql.TableGenerator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

/**
 *  Business Logic for RouteDTO crud, and for creating and updating the route tables
 * associated with the card.
 * @author  Rahul Asthana
 * @since  26/6/13
 */
public class RouteBL {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String SQL_PATTERN = "^[a-zA-Z0-9_]*$";
    private static final String EMPTY_DOUBLE_QUOTES = "\"\"";

    public static final int BATCH_SIZE = 1000;
    public static final String DEFAULT_DATA_TYPE = "varchar(255)";
    public static final String NOTE_COLUMN_DATA_TYPE = "varchar(1000)";
    public static final String NOTE_COLUMN_HEADER = "note";

    private RouteDAS routeDAS;
    private JdbcTemplate jdbcTemplate;
    private TableGenerator tableGenerator;
    private RouteDTO routeDTO;
    private Pattern sqlPattern;

    public RouteBL() {
        _init();
    }

    public RouteBL(Integer routeId) {
        _init();
        set(routeId);
    }

    public RouteBL(RouteDTO routeDTO) {
        _init();
        this.routeDTO = routeDTO;
        this.tableGenerator = new TableGenerator(this.routeDTO.getTableName(),
            this.routeDTO.getRouteTable() ? RouteDTO.ROUTE_TABLE_COLUMNS : RouteDTO.NON_ROUTE_TABLE_COLUMNS);
    }

    public void set(Integer routeId) {
        this.routeDTO = routeDAS.find(routeId);
        this.tableGenerator = new TableGenerator(this.routeDTO.getTableName(),
            this.routeDTO.getRouteTable() ? RouteDTO.ROUTE_TABLE_COLUMNS : RouteDTO.NON_ROUTE_TABLE_COLUMNS);
    }

    public void set(String name, Integer entityId) {
        this.routeDTO = routeDAS.getRoute(entityId, name);
        this.tableGenerator = new TableGenerator(routeDTO.getTableName(),
            this.routeDTO.getRouteTable() ? RouteDTO.ROUTE_TABLE_COLUMNS : RouteDTO.NON_ROUTE_TABLE_COLUMNS);
    }

    /**
     * Create a new route with csv
     * @param routeDTO route for the entity
     * @param routeFile file handle of the CSV on disk containing the routes.
     * @return
     */
    public Integer create(RouteDTO routeDTO, File routeFile) {

        checkFileIsEmpty(routeFile);

        if (routeDTO != null) {
            logger.debug("Saving new rate card {}", routeDTO);

            checkRouteNameExistence(routeDTO.getCompany().getId(), routeDTO.getName());
            this.routeDTO = routeDAS.save(routeDTO);
            this.routeDTO.getMatchingFields().clear();

            // Requirements #7045 - If the Data Table is a Route Table we add the mandatory columns to the tableGenerator. Otherwise
            // we just add an id.
            this.tableGenerator = new TableGenerator(this.routeDTO.getTableName(),
                    routeDTO.getRouteTable() ? RouteDTO.ROUTE_TABLE_COLUMNS : RouteDTO.NON_ROUTE_TABLE_COLUMNS);
            logger.debug("Creating a new rate table & saving rating data");
            if (routeFile != null) {
                try {
                    checkRouteTableExistence(this.routeDTO.getTableName());
                    saveRoutes(routeFile);

                } catch (SessionInternalError e) {
                    try { dropRoutes(); } catch (Exception ex) {}
                    throw e;
                } catch (SQLException e) {
                    try { dropRoutes(); } catch (Exception ex) {}
                    throw new SessionInternalError("Exception saving rates to database", e, new String[] { "RouteWS,routes,cannot.save.rates.db.error" });
                } catch (Throwable e) {
                    logger.info(e.toString());
                    try { dropRoutes(); } catch (Exception ex) {}

                    throw new SessionInternalError("Could not load rating table", e, new String[] { "RouteWS,routes,cannot.read.file" });
                }

                registerSpringBeans();
            }

            return this.routeDTO.getId();
        }

        logger.error("Cannot save a null RateCardDTO!");
        return null;
    }

    private void _init() {
        this.routeDAS = new RouteDAS();
        this.jdbcTemplate = Context.getBean(Context.Name.JDBC_TEMPLATE);
        this.sqlPattern = Pattern.compile(SQL_PATTERN);
    }

    private void checkRouteTableExistence(String tableName) throws SQLException {
        DataSource dataSource = jdbcTemplate.getDataSource();
        Connection connection = DataSourceUtils.getConnection(dataSource);
        List<String> tableNames = JDBCUtils.getAllTableNames(connection);
        if (tableNames.contains(tableName.toLowerCase())) {
            String dropTable = tableGenerator.buildDropTableSQL();
            jdbcTemplate.execute(dropTable);
        }
    }

    /**
     * Updates the route table with the route information in
     * the given CSV
     *
     * @param routesFile file handle of the CSV on disk containing the route.
     * @throws IOException if file does not exist or is not readable
     */
    public void saveRoutes(File routesFile) throws IOException, SQLException {

        CSVReader reader = new CSVReader(new FileReader(routesFile));
        String[] line = reader.readNext();
        //replace all white spaces with underscore
        line = Arrays.stream(line).map(l -> l.trim().replaceAll(" ", "_")).toArray(String[]::new);

        // Requirements #7045 - If the Data Table is not set to be a Route Table we don't have to validate the mandatory column headers.
        if (routeDTO.getRouteTable()) {
            validateCsvHeader(Arrays.asList(line), true);
        } else {
            validateDataFileCsvHeader(line);
        }

        // parse the header and read out the extra columns.
        // Requirements #7045 - If the Data Table is set to be a Route table we have to start after the mandatory columns. Otherwise we start
        // from the first one.
        int start = routeDTO.getRouteTable() ? routeDTO.ROUTE_TABLE_COLUMNS.size() : 0;
        for (int i = start; i <line.length; i++) {
            if (!StringUtils.isBlank(line[i]) && line[i].contains(NOTE_COLUMN_HEADER)) {
                tableGenerator.addColumn(new TableGenerator.Column(line[i], NOTE_COLUMN_DATA_TYPE, true));
            } else {
                tableGenerator.addColumn(new TableGenerator.Column(line[i], DEFAULT_DATA_TYPE, true));
            }
        }

        // create route table
        String createSql = tableGenerator.buildCreateTableSQL();

        jdbcTemplate.execute(createSql);

        logger.debug("Created table '{}'", routeDTO.getTableName());

        // load rating data in batches
        String insertSql = tableGenerator.buildInsertPreparedStatementSQL();
        List<List<String>> rows = new ArrayList<List<String>>();
        int i = 1;
        while (true) {
            // add row to insert batch
            line = reader.readNext();
            if (line != null) {
                List<String> currentLine = new ArrayList<String>(line.length+1);
                //generate a primary key if it is no a route table
                if (!routeDTO.getRouteTable()) {
                    currentLine.add(Integer.toString(i));
                }
                // Handle free and non applicable pricing terms as extra column
                currentLine.addAll(Arrays.asList(line));
                rows.add(currentLine);
            }

            // end of file
            if (line == null) {
                executeBatchInsert(insertSql, rows);
                break; // done
            }

            // reached batch limit
            if (i++ % BATCH_SIZE == 0) {
                executeBatchInsert(insertSql, rows);
                rows.clear(); // next batch
            }
        }
    }

    /**
     * Updates an existing route
     *
     * @param routeDTO route to create
     * @param routeRatesFile file handle of the CSV on disk containing the routes
     */
    public void update(RouteDTO routeDTO, File routeRatesFile) {
        if (this.routeDTO != null) {
            boolean changedToRouteTable =  routeDTO.getRouteTable() && !this.routeDTO.getRouteTable();
            //set the route table here, the saveRoutes method requires the correct value
            this.routeDTO.setRouteTable(routeDTO.getRouteTable());

            routeDTO.getMatchingFields().clear();

            // re-create the route table
            logger.debug("Re-creating the route table & saving updated routes");
            if (routeRatesFile != null) {
                checkFileIsEmpty(routeRatesFile);
                dropRoutes();

                try {
                    this.tableGenerator = new TableGenerator(this.routeDTO.getTableName(),
                            this.routeDTO.getRouteTable() ? RouteDTO.ROUTE_TABLE_COLUMNS : RouteDTO.NON_ROUTE_TABLE_COLUMNS);
                    saveRoutes(routeRatesFile);
                } catch (SessionInternalError e) {
                    logger.error(e.toString());
                    try { dropRoutes(); } catch (Exception ex) {}
                    throw e;
                } catch (SQLException e) {
                    logger.error(e.toString());
                    try { dropRoutes(); } catch (Exception ex) {}
                    throw new SessionInternalError("Exception saving rates to database", e, new String[] { "RouteWS,rates,cannot.save.rates.db.error" });
                } catch (Throwable e) {
                    logger.error(e.toString());
                    try { dropRoutes(); } catch (Exception ex) {}
                    throw new SessionInternalError("Could not load route based rating table", e, new String[] { "RouteWS,csv,cannot.read.file" });
                }

            //if it changed from a non-route to route table, validate the columns
            } else if(changedToRouteTable) {
                List<String> columnNames = getBeanFactory().getTableDescriptorInstance().getColumnsNames();
                for(int i=columnNames.size()-1; i>=0; i--) {
                    columnNames.set(i, columnNames.get(i).toLowerCase());
                }
                validateCsvHeader(columnNames, false);
            }

            // prepare SQL to rename the table if the table name has changed
            String originalTableName = this.routeDTO.getTableName();
            String alterTableSql = null;

            if (!originalTableName.equals(routeDTO.getTableName())) {
                try {

                    checkRouteTableExistence(routeDTO.getTableName());
                } catch (SQLException e) {
                    dropRoutes();
                    throw new SessionInternalError("Exception saving rates to database", e,
                            new String[] { "RouteWS,csv,cannot.save.rates.db.error" });
                }
                alterTableSql = this.tableGenerator.buildRenameTableSQL(routeDTO.getTableName());
                //remove and re-register spring beans
                removeSpringBeans();
            }

            // do update
            if (!this.routeDTO.getName().equals(routeDTO.getName())) {
                checkRouteNameExistence(this.routeDTO.getCompany().getId(), routeDTO.getName());
                this.routeDTO.setName(routeDTO.getName());
            }

            if (!this.routeDTO.getTableName().equals(routeDTO.getTableName())) {
                this.routeDTO.setTableName(routeDTO.getTableName());
            }

            this.routeDTO.setDefaultRoute(routeDTO.getDefaultRoute());
            this.routeDTO.setOutputFieldName(routeDTO.getOutputFieldName());
            this.routeDTO.setRootTable(routeDTO.getRootTable());

            logger.debug("Saving updates to route {}", routeDTO.getId());
            this.routeDTO = routeDAS.save(this.routeDTO);
            this.tableGenerator = new TableGenerator(this.routeDTO.getTableName(),
            this.routeDTO.getRouteTable() ? RouteDTO.ROUTE_TABLE_COLUMNS : RouteDTO.NON_ROUTE_TABLE_COLUMNS);

            // do rename after saving the new table name
            if (alterTableSql != null) {
                logger.debug("Renaming the route table");
                jdbcTemplate.execute(alterTableSql);
            }

            //if the name changed we have to create new spring beans
            if (!originalTableName.equals(routeDTO.getTableName())) {
                registerSpringBeans();

            // re-register spring beans if rates were updated
            } else if (routeRatesFile != null) {
                removeSpringBeans();
                registerSpringBeans();
            }

        } else {
            logger.error("Cannot update, RouteDTO not found or not set!");
        }
    }

    /**
     * Validates that the uploaded CSV file starts with the expected columns from {@link RouteDTO#ROUTE_TABLE_COLUMNS}.
     * If the column names don't match or are in an incorrect order a SessionInternalError will be throw.
     *
     * @param header header line to validate
     * @throws SessionInternalError thrown if errors found in header data
     */
    private void validateCsvHeader(List<String> header, boolean validateOrder) throws SessionInternalError {
        List<String> errors = new ArrayList<String>();

        List<TableGenerator.Column> columns = RouteDTO.ROUTE_TABLE_COLUMNS;
        for (int i = 0; i < columns.size(); i++) {
            String expected = columns.get(i).getName();
            if(validateOrder) {
                if(header.size() > i) {
                    String columnName = header.get(i).trim();

                    if (!expected.equalsIgnoreCase(columnName)) {
                        errors.add("RouteWS,routes,route.unexpected.header.value," + expected + "," + columnName);
                    }
                } else {
                    errors.add("RouteWS,routes,route.not.found.position.header.value," + expected + "," + (i+1));
                }
            } else {
                if(!header.contains(expected.toLowerCase())) {
                    errors.add("RouteWS,routes,route.not.found.header.value," + expected);
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new SessionInternalError("Route CSV has errors in  columns, or is missing required columns",
                    errors.toArray(new String[errors.size()]));
        }
    }

    /**
     * Validate that a data file does not have a column called 'id'
     *
     * @param header
     * @throws SessionInternalError
     */
    private void validateDataFileCsvHeader(String[] header) throws SessionInternalError {
        List<String> errors = new ArrayList<String>();

        for (int i = 0; i < header.length; i++) {
            if ("id".equals(header[i].toLowerCase())) {
                errors.add("RouteWS,routes,route.unexpected.header.value.id");
            }
        }

        for (String h : header) {
            String headString = h.trim().isEmpty() ? EMPTY_DOUBLE_QUOTES : h.trim();
            if (Character.isDigit(headString.charAt(0))) {
                errors.add("RouteWS,routes,route.rate.card.contains.any.chars.columns.name," + headString);
            }

            if (!sqlPattern.matcher(headString).matches()) {
                errors.add("RouteWS,routes,route.rate.card.special.chars.disallowed.columns.name," + headString);
            }
        }

        if (!errors.isEmpty()) {
            throw new SessionInternalError("Route CSV has errors in  columns, or is missing required columns",
                    errors.toArray(new String[errors.size()]));
        }
    }

    /**
     * Inserts a batch of records into the database.
     *
     * @param insertSql prepared statement SQL
     * @param rows list of rows to insert
     */
    private void executeBatchInsert(String insertSql, final List<List<String>> rows) {
        logger.debug("Inserting {} records:", rows.size());
        logger.debug("{}", rows);

        jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement preparedStatement, int batch) throws SQLException {
                List<String> values = rows.get(batch);
                for (int i = 0; i < values.size(); i++) {
                    String value = values.get(i).trim();

                    switch (i) {
                        case 0:  // row id
                            preparedStatement.setInt(i + 1, StringUtils.isNotBlank(value) ? Integer.valueOf(value) : 0);
                            break;


                        default: // everything else
                            preparedStatement.setObject(i + 1, value);
                    }
                }
            }

            public int getBatchSize() {
                return rows.size();
            }
        });
    }

    /**
     * Drop the route table
     */
    public void dropRoutes() {
        String dropSql = tableGenerator.buildDropTableSQL();
        jdbcTemplate.execute(dropSql);
        logger.debug("Dropped table '{}'", routeDTO.getTableName());
    }

    /*
            Spring Beans stuff
     */

    public void registerSpringBeans() {
        registerSpringBeans(false);
    }
    /**
     * Registers spring beans with the application context so support caching and look-up
     * of pricing from the rating tables.
     *
     * @param finderOnly - only register the finder bean. Used in hadoop map/reduce jobs
     */
    public void registerSpringBeans(boolean finderOnly) {
        RouteBeanFactory factory = getBeanFactory();

        if (finderOnly) {
            String finderBeanName = factory.getFinderBeanName();
            BeanDefinition finderBeanDef = factory.getFinderBeanDefinition(null);

            logger.info("Registering beans: {}", finderBeanName);

            GenericApplicationContext ctx = (GenericApplicationContext) Context.getApplicationContext();
            ctx.registerBeanDefinition(finderBeanName, finderBeanDef);
        } else {

            String readerBeanName = factory.getReaderBeanName();
            BeanDefinition readerBeanDef = factory.getReaderBeanDefinition(routeDTO.getCompany().getId());

            String loaderBeanName = factory.getLoaderBeanName();
            BeanDefinition loaderBeanDef = factory.getLoaderBeanDefinition(readerBeanName);

            String finderBeanName = factory.getFinderBeanName();
            BeanDefinition finderBeanDef = factory.getFinderBeanDefinition(loaderBeanName);

            Map<String, BeanDefinition> updateBeans = factory.getRouteUpdaterDependentBeanDefinitions();
            String updaterBeanName = factory.getRouteUpdaterBeanName();
            BeanDefinition updaterBeanDef = factory.getRouteUpdaterAggregateBeanDefinitions(updateBeans.keySet());

            String tableDefBeanName = factory.getTableDescriptorBeanName();
            BeanDefinition tableDefBeanDef = factory.getTableDescriptorBeanDefinition();

            logger.info("Registering beans: {}, {}, {}, {}, {}", readerBeanName, loaderBeanName, finderBeanName, updaterBeanName, tableDefBeanName);

            // register spring beans!
            GenericApplicationContext ctx = (GenericApplicationContext) Context.getApplicationContext();
            ctx.registerBeanDefinition(readerBeanName, readerBeanDef);
            ctx.registerBeanDefinition(loaderBeanName, loaderBeanDef);
            ctx.registerBeanDefinition(finderBeanName, finderBeanDef);
            ctx.registerBeanDefinition(tableDefBeanName, tableDefBeanDef);

            for (String beanName : updateBeans.keySet()) {
                ctx.registerBeanDefinition(beanName, updateBeans.get(beanName));
            }
            ctx.registerBeanDefinition(updaterBeanName, updaterBeanDef);

            //the init method doesn't get called when registering a bean. Force loading of the cache
            factory.getLoaderInstance();
        }
    }

    public void removeSpringBeans() {
        removeSpringBeans(false);
    }

    /**
     * Removes registered spring beans from the application context.
     */
    public void removeSpringBeans(boolean finderOnly) {
        RouteBeanFactory factory = getBeanFactory();

        try {
            String finderBeanName = factory.getFinderBeanName();

            logger.info("Removing beans: {}", finderBeanName);

            GenericApplicationContext ctx = (GenericApplicationContext) Context.getApplicationContext();
            ctx.removeBeanDefinition(finderBeanName);
            if (!finderOnly) {
                String readerBeanName = factory.getReaderBeanName();
                String loaderBeanName = factory.getLoaderBeanName();

                String tableDefBeanName = factory.getTableDescriptorBeanName();

                Map<String, BeanDefinition> updateBeans = factory.getRouteUpdaterDependentBeanDefinitions();
                String updaterBeanName = factory.getRouteUpdaterBeanName();

                logger.info("Removing beans: {}, {}, {}, {}, {}", readerBeanName, loaderBeanName, finderBeanName, tableDefBeanName, updaterBeanName);

                ctx.removeBeanDefinition(readerBeanName);
                ctx.removeBeanDefinition(loaderBeanName);

                for (String beanName : updateBeans.keySet()) {
                    ctx.removeBeanDefinition(beanName);
                }
                ctx.removeBeanDefinition(tableDefBeanName);
                ctx.removeBeanDefinition(updaterBeanName);
            }

        } catch (NoSuchBeanDefinitionException e) {
            logger.warn("Beans not found");
        }
    }

    /**
     * Returns an instance of the {@link RouteBeanFactory}
     * @return route bean factory
     */
    public RouteBeanFactory getBeanFactory() {
        return new RouteBeanFactory(routeDTO);
    }

    /**
     * Returns a list of column names read from the route table in the database.
     * @return column names
     */
    public List<String> getRouteTableColumnNames() {
        DataSource dataSource = jdbcTemplate.getDataSource();
        Connection connection = DataSourceUtils.getConnection(dataSource);

        List<String> columns = Collections.emptyList();

        try {
            columns = JDBCUtils.getAllColumnNames(connection, routeDTO.getTableName());
        } catch (SQLException e) {

            throw new SessionInternalError("Could not read columns from route table.", e,
                    new String[] { "RouteWS,routes,route.cannot.read.rating.table" });

        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }

        return columns;
    }

    /**
     * Returns a list of column names read from the route table in the database.
     * @return column names
     */
    public List<String> getRouteTableColumnNames(String tableName) {
        DataSource dataSource = jdbcTemplate.getDataSource();
        Connection connection = DataSourceUtils.getConnection(dataSource);

        List<String> columns = Collections.emptyList();

        try {
            columns = JDBCUtils.getAllColumnNames(connection, tableName);
        } catch (SQLException e) {

            throw new SessionInternalError("Could not read columns from route table.", e,
                    new String[] { "RouteWS,routes,route.cannot.read.rating.table" });

        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }

        return columns;
    }
    
    public ScrollableResults getRouteTableRows() {
        return routeDAS.getRouteTableRows(routeDTO.getTableName());
    }
    
    /**
     * Deletes the current routes managed by this class.
     */
    public void delete() {

        if (routeDTO != null) {

            new DataTableQueryBL().deleteQueriesLinkedToTable(routeDTO.getId());

            routeDAS.delete(routeDTO);
            dropRoutes();
            routeDAS.flush();
        } else {
            logger.error("Cannot delete, RouteDTO not found or not set!");
        }
    }



    /**
     * MatchingFieldDTO To MatchingFieldWS Converter
     * @params matchingFieldDTO
     */
    public MatchingFieldWS convertMatchingFieldDTOToMatchingFieldWS(MatchingFieldDTO matchingFieldDTO){
        MatchingFieldWS matchingFieldWS = new MatchingFieldWS();
        matchingFieldWS.setId(matchingFieldDTO.getId());
        matchingFieldWS.setDescription(matchingFieldDTO.getDescription());
        matchingFieldWS.setRequired(matchingFieldDTO.getRequired());
        matchingFieldWS.setMatchingField(matchingFieldDTO.getMatchingField());
        matchingFieldWS.setMediationField(matchingFieldDTO.getMediationField());
        matchingFieldWS.setOrderSequence(matchingFieldDTO.getOrderSequence().toString());
        matchingFieldWS.setType(matchingFieldDTO.getType().name());
        if (matchingFieldDTO.getRouteRateCard() == null) {
            matchingFieldWS.setEntityId(matchingFieldDTO.getRoute().getCompany().getId());
        } else {
            matchingFieldWS.setEntityId(matchingFieldDTO.getRouteRateCard().getCompany().getId());
        }

        matchingFieldWS.setMandatoryFieldsQuery(matchingFieldDTO.getMandatoryFieldsQuery());
        
        if(matchingFieldDTO.getRoute()!=null) {
            matchingFieldWS.setRouteId(matchingFieldDTO.getRoute().getId());
        } else if(matchingFieldDTO.getRouteRateCard()!=null) {
            matchingFieldWS.setRouteRateCardId(matchingFieldDTO.getRouteRateCard().getId());
        }

        return matchingFieldWS;
    }

    /**
     * Get List of additional columns added from route csv
     * @param tableName Table name.
     * @return Additional columns added from route CSV.
     */
    public List<String> getAdditionalRouteColumns(String tableName){
        List<String> tableColumns=getRouteTableColumnNames(tableName);
        tableColumns.removeAll(RouteDTO.TABLE_COLUMNS_NAMES);
        return tableColumns;
    }

    /**
     * Get List of unused additional columns added from route csv
     * @param tableName Table name.
     * @return Unused Additional columns added from route CSV.
     */
    public List<String> getUnusedAdditionalRouteColumns(String tableName,Integer routeId){
        List<String> tableColumns=getRouteTableColumnNames(tableName);
        tableColumns.removeAll(RouteDTO.TABLE_COLUMNS_NAMES);
        tableColumns.removeAll(new MatchingFieldDAS().getRouteUsedMatchingFields(routeId));
        return tableColumns;
    }

    /**
     * Get List of matching field DTO  for a route based on by routeId
     * @param routeId route id for the entity i.e route_<entity>
     * @return List of matching field based on required field
     */
    public List<MatchingFieldDTO> getMatchingFieldsByRouteId(Integer routeId){
        List<MatchingFieldDTO> matchingFieldDTOs = new MatchingFieldDAS().getMatchingFieldsByRouteId(routeId);
        return  matchingFieldDTOs;

    }

    /**
     * Get List of matching field DTO  for a route based on matching field required value
     * @param routeId route id for the entity i.e route_<entity>
     * @param required matching field is required or not
     * @return List of matching field based on required field
     */
    public List<MatchingFieldDTO> getRouteRequiredFields(Integer routeId,String required){
        List<MatchingFieldDTO> matchingFieldDTOs = new MatchingFieldDAS().findMatchingFieldByRequiredField(routeId, isRequired(required));
        return  matchingFieldDTOs;

    }

    /**
     * Return boolean value of required field
     * @param required matching field is required or not
     * @return List of matching field based on required field
     */
    public Boolean isRequired(String required){
    	if ( "yes".equalsIgnoreCase(required) || "true".equalsIgnoreCase(required) )
            return true;
        else
            return false;
    }

    /**
     * Get List of matching field column names for a route based on matching field required value
     * @param routeId route id for the entity i.e route_<entity>
     * @param required matching field is required or not
     * @return List of matching field for a route  based on required field
     */
    public List<String> getRequiredMatchingColumns(Integer routeId,String required){
        List<MatchingFieldDTO> matchingFieldDTOs = getRouteRequiredFields(routeId, required);
        List<String> columnNames = new ArrayList<String>();
        for (MatchingFieldDTO matchingFieldDTO:matchingFieldDTOs){
            columnNames.add(matchingFieldDTO.getMatchingField());
        }
        return columnNames;
    }
    /**
     * Get List of matching field column names for a route
     * @param routeId route id for the entity i.e route_<entity>
     * @return List of matching field for a route  based on required field
     */
    public List<String> getMatchingFieldColumns(Integer routeId){
        List<MatchingFieldDTO> matchingFieldDTOs = getMatchingFieldsByRouteId(routeId);
        List<String> columnNames = new ArrayList<String>();
        for (MatchingFieldDTO matchingFieldDTO:matchingFieldDTOs){
            columnNames.add(matchingFieldDTO.getMatchingField());
        }
        return columnNames;
    }

    /**
     *Query builder helper that converts column and value to string column = 'value'
     * @param column Column of table
     * @param value  Value to be searched
     * @return   column = 'value'
     */

    public String getColumnSearchValue(String column,Object value){

        return value != null ? column+" = '"+value.toString()+ "'" : " ";

    }

    public Integer getLongestValueFor(String matchingField) {
    	return routeDAS.getLongestValue(getEntity().getTableName(), matchingField);
    }
    
    public Integer getSmallestValueFor(String matchingField) {
    	return routeDAS.getSmallestValue(getEntity().getTableName(), matchingField);
    }
    
    public String queryBuilderHelper(String tableName, List<String> requiredFields, List<String> notRequiredFields){

        StringBuilder finalSql = new StringBuilder();

        finalSql.append("select * from " + tableName)
                .append( " where ")
                .append(StringUtils.join(requiredFields," and "));
        if (notRequiredFields != null )
            finalSql.append(" and "+StringUtils.join(notRequiredFields," and "));
        return finalSql.toString();
    }

    public RouteDTO getEntity() {
    	return routeDTO;
    }

    public String createRouteTableName(Integer companyId, String routeTableName) {
        if (null == companyId || companyId <= 0 ||
                null == routeTableName || routeTableName.isEmpty()) {
            throw new IllegalArgumentException("Can not create route table name if route" +
                    " companyId or table name is not set");
        }
        return RouteDTO.TABLE_PREFIX + companyId.toString() + "_" + JDBCUtils.getDatabaseObjectName(routeTableName);
    }

    private void checkFileIsEmpty(File file) {

        if(file == null || file.length() == 0){
            throw new SessionInternalError("File selected is empty",
                    new String[]{"RouteWS,csv,route.validation.file.empty"});
        }
    }

    private void checkRouteNameExistence(Integer entityId, String routeName) {

        if(isRouteNameUsed(entityId, routeName)){
            throw new SessionInternalError("Route name is already used for this company",
                    new String[]{"RouteWS,name,route.validation.name.used,"+routeName});
        }
    }


    public RouteDTO getRootRouteTable(Integer companyId){
        if (null == companyId || companyId <= 0 ){
            throw new IllegalArgumentException("Company id must be known");
        }
        RouteDAS routeDAS = new RouteDAS();
        return routeDAS.getRootRoute(companyId);
    }

    private boolean isRouteNameUsed(Integer companyId, String name) {
        if (null == companyId || companyId <= 0 || null == name || name.isEmpty()) {
            throw new IllegalArgumentException("Company id and route name must be known");
        }
        RouteDAS routeDAS = new RouteDAS();
        return routeDAS.isRouteNameUsed(companyId, name);
    }

    public RouteDTO toDTO(RouteWS routeWS) {
        RouteDTO route = new RouteDTO();
        route.setId(routeWS.getId());
        route.setName(routeWS.getName());
        route.setTableName(routeWS.getTableName());
        route.setRootTable(routeWS.getRootTable());
        route.setRouteTable(routeWS.getRouteTable());
        route.setOutputFieldName(routeWS.getOutputFieldName());
        route.setDefaultRoute(routeWS.getDefaultRoute());
        return route;
    }

    public RouteWS toWS(){
        RouteWS routeWS = new RouteWS();
        routeWS.setId(this.routeDTO.getId());
        routeWS.setName(this.routeDTO.getName());
        routeWS.setTableName(this.routeDTO.getTableName());
        routeWS.setEntityId(this.routeDTO.getCompany().getId());
        routeWS.setRootTable(this.routeDTO.getRootTable());
        routeWS.setRouteTable(this.routeDTO.getRouteTable());
        routeWS.setOutputFieldName(this.routeDTO.getOutputFieldName());
        routeWS.setDefaultRoute(this.routeDTO.getDefaultRoute());
        return routeWS;
    }
    
}
