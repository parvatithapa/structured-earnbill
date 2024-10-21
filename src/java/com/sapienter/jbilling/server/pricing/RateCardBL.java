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

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.pricing.PricingBeanMessage.Action;
import com.sapienter.jbilling.server.pricing.db.PriceModelDAS;
import com.sapienter.jbilling.server.pricing.db.RateCardDAS;
import com.sapienter.jbilling.server.pricing.db.RateCardDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.sql.JDBCUtils;
import com.sapienter.jbilling.server.util.sql.TableGenerator;

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
import org.springframework.jms.core.JmsTemplate;

import javax.jms.Destination;
import javax.sql.DataSource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Business Logic for RateCardDTO crud, and for creating and updating the rating tables
 * associated with the card.
 *
 * @author Brian Cowdery
 * @since 16-Feb-2012
 */
public class RateCardBL extends ResultList {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    //SQL statement to find the company ids that a rate card is used in
    private static final String entitiesForRateCardSQL = "SELECT DISTINCT i.entity_id, e.description " +
            "FROM price_model_attribute pma " +
            "JOIN price_model pm ON pma.price_model_id=pm.id " +
            "JOIN item_price_timeline ipt ON pm.id=ipt.price_model_id " +
            "JOIN entity_item_price_map eipm ON ipt.model_map_id = eipm.id " +
            "JOIN item i ON eipm.item_id = i.id " +
            "JOIN entity e ON i.entity_id = e.id " +
            "WHERE pma.attribute_name='rate_card_id' AND pma.attribute_value=?";

    private static final String DEFAULT_DATA_TYPE = "varchar(255)";
    public static final int BATCH_SIZE = 10;

    private RateCardDAS rateCardDas;
    private JdbcTemplate jdbcTemplate;
    private TableGenerator tableGenerator;

    private RateCardDTO rateCard;

    public RateCardBL() {
        _init();
    }

    public RateCardBL(Integer rateCardId) {
        _init();
        set(rateCardId);
    }

    public RateCardBL(RateCardDTO rateCard) {
        _init();
        this.rateCard = rateCard;
        this.tableGenerator = new TableGenerator(rateCard.getTableName(), RateCardDTO.TABLE_COLUMNS);
    }

    public void set(Integer rateCardId) {
        this.rateCard = rateCardDas.find(rateCardId);
        this.tableGenerator = new TableGenerator(rateCard.getTableName(), RateCardDTO.TABLE_COLUMNS);
    }

    private void _init() {
        this.rateCardDas = new RateCardDAS();
        this.jdbcTemplate = Context.getBean(Context.Name.JDBC_TEMPLATE);
    }

    /**
     * Returns the RateCardDTO object being managed by this BL class.
     *
     * @return rate card object
     */
    public RateCardDTO getEntity() {
        return rateCard;
    }


    public Integer create(RateCardDTO rateCard, File ratesFile) {
        return create(rateCard, ratesFile, false);
    }

    /**
     * Create a new rate card with the specified rates.
     *
     * @param rateCard  rate card to create
     * @param ratesFile file handle of the CSV on disk containing the rates.
     * @return id of the saved rate card
     */
    public Integer create(RateCardDTO rateCard, File ratesFile, Boolean isCopyRateCard) {
        if (rateCard != null) {
            logger.debug("Saving new rate card {}", rateCard);
            this.rateCard = rateCardDas.save(rateCard);
            this.tableGenerator = new TableGenerator(this.rateCard.getTableName(), RateCardDTO.TABLE_COLUMNS);
            logger.debug("Creating a new rate table & saving rating data");
            if (ratesFile != null) {
                try {
                    checkRateTableExistance(this.rateCard.getTableName());
                    saveRates(ratesFile);
                } catch (SessionInternalError e) {
                    dropRates();
                    throw e;
                } catch (IOException e) {
                    dropRates();
                    throw new SessionInternalError("Could not load rating table", e, new String[]{"RateCardWS,rates,cannot.read.file"});
                } catch (SQLException e) {
                    dropRates();
                    throw new SessionInternalError("Exception saving rates to database", e, new String[]{"RateCardWS,rates,cannot.save.rates.db.error"});
                } catch (CsvValidationException e) {
                    throw new SessionInternalError("Could not load rating table", e, new String[]{"RateCardWS,rates,cannot.read.file"});
                }

                if (!isCopyRateCard) {
                    doRegisterBeanInCluster(Action.CREATE);
                }
            }

            return this.rateCard.getId();
        }

        logger.error("Cannot save a null RateCardDTO!");
        return null;
    }

    /**
     * Validate to check if the update of rate card can be allowed by verifying that access to any company that uses this
     * rate card is not taken away
     *
     * @param newRateCard
     * @return Null if validation succeeds, else the name of the company thats causing the issue
     * @throws Exception
     */
    public String validateRateCardUpdate(RateCardDTO newRateCard) throws Exception {
        if (newRateCard.isGlobal()) {
            //We are making the ratecard global so no need to validate if it will not be available to a company
            return null;
        }
        //Find the list of companies this rate card is visible to
        List<Integer> newCoIds = new ArrayList<>();
        Map<Integer, String> rateCardUsedCoIds = new HashMap<>();
        Set<CompanyDTO> newCos = newRateCard.getChildCompanies();
        if (newCos != null) {
            for (CompanyDTO companyDTO : newCos) {
                newCoIds.add(companyDTO.getId());
            }
        }
        //Now add the parent company because parent company has the visibility for the ratecard
        newCoIds.add(this.rateCard.getCompany().getId());

        //Find the list of companies this rate card is used in
        prepareStatement(entitiesForRateCardSQL);
        cachedResults.setString(1, this.rateCard.getId().toString());
        execute();
        while (cachedResults.next()) {
            rateCardUsedCoIds.put(cachedResults.getInt(1), cachedResults.getString(2));
        }
        //Now check if these companies are among the list of newly selected companies. If not throw an exception
        for (Integer coId : rateCardUsedCoIds.keySet()) {
            if (!newCoIds.contains(coId)) {
                //Return company name to use in validation
                return rateCardUsedCoIds.get(coId);
            }
        }
        conn.close();
        return null;
    }

    /**
     * Updates an existing rate card and rates.
     *
     * @param rateCard  rate card to create
     * @param ratesFile file handle of the CSV on disk containing the rates.
     */
    public void update(RateCardDTO rateCard, File ratesFile) throws Exception {
        if (this.rateCard != null) {
            // Validate to make sure this update does not make the ratecard inaccessible to companies which are using it
            //Validation done in groovy already but still add this here for API based calls
            String company = validateRateCardUpdate(rateCard);
            if (company != null) {
                //This exception will never be throws for UI calls because this validation is done already on groovy
                throw new Exception("Invalid update. Rate card used in company: " + this.rateCard);
            }

            // re-create the rating table
            logger.debug("Re-creating the rate table & saving updated rating data");
            if (ratesFile != null) {
                dropRates();

                try {
                    saveRates(ratesFile);

                } catch (IOException e) {
                    dropRates();
                    throw new SessionInternalError("Could not load rating table", e, new String[]{"RateCardWS,rates,cannot.read.file"});
                } catch (SQLException e) {
                    dropRates();
                    throw new SessionInternalError("Exception saving rates to database", e, new String[]{"RateCardWS,rates,cannot.save.rates.db.error"});
                }
            }

            // prepare SQL to rename the table if the table name has changed
            String originalTableName = this.rateCard.getTableName();
            String alterTableSql = null;

            if (!originalTableName.equals(rateCard.getTableName())) {
                try {
                    checkRateTableExistance(rateCard.getTableName());
                } catch (SQLException e) {
                    dropRates();
                    throw new SessionInternalError("Exception saving rates to database", e,
                            new String[]{"RateCardWS,rates,cannot.save.rates.db.error"});
                }
                alterTableSql = this.tableGenerator.buildRenameTableSQL(rateCard.getTableName());
                //remove and re-register spring beans
                doRegisterBeanInCluster(Action.REMOVE);
            }

            if (alterTableSql != null) {
                logger.debug("Renaming the rate table");
                jdbcTemplate.execute(alterTableSql);
            }

            // do update
            this.rateCard.setName(rateCard.getName());
            if (!this.rateCard.getTableName().equals(rateCard.getTableName())) {
                this.rateCard.setTableName(rateCard.getTableName());
                doRegisterBeanInCluster(Action.CREATE);
            }

            //preserver child entities, root entity and global variable value
//            rateCard.setChildCompanies(this.rateCard.getChildCompanies());
            rateCard.setCompany(this.rateCard.getCompany());
//            rateCard.setGlobal(this.rateCard.isGlobal());

            logger.debug("Saving updates to rate card {}", rateCard.getId());
            this.rateCard = rateCardDas.save(rateCard);
            this.tableGenerator = new TableGenerator(this.rateCard.getTableName(), RateCardDTO.TABLE_COLUMNS);

            // re-register spring beans if rates were updated
            if (ratesFile != null) {
                doRegisterBeanInCluster(Action.UPDATE);
            }

        } else {
            logger.error("Cannot update, RateCardDTO not found or not set!");
        }
    }

    /**
     * Deletes the current rate card managed by this class.
     */
    public void delete() {
        if (rateCard != null) {
            if (!new PriceModelDAS().findRateCardPriceModels(rateCard.getId()).isEmpty()) {
                throw new SessionInternalError("Exception deleting rates from database",
                        new String[]{"RateCardWS,rates,cannot.delete.rates.db.constraint"});
            }
            rateCardDas.delete(rateCard);
            dropRates();
        } else {
            logger.error("Cannot delete, RateCardDTO not found or not set!");
        }
    }


    /*
            Rate Table Database Stuff
     */

    /**
     * Drop the rate table of a rate card.
     */
    public void dropRates() {
        String dropSql = tableGenerator.buildDropTableSQL();
        jdbcTemplate.execute(dropSql);
        logger.debug("Dropped table '{}'", rateCard.getTableName());
    }

    /**
     * Updates the rate table of a rate card with the rating information in
     * the given CSF file of rates.
     *
     * @param ratesFile file handle of the CSV on disk containing the rates.
     * @throws IOException if file does not exist or is not readable
     */
    public void saveRates(File ratesFile) throws IOException, SQLException, CsvValidationException {

        CSVReader reader = new CSVReader(new FileReader(ratesFile));
        String[] line = reader.readNext();
        validateCsvHeader(line);

        // parse the header and read out the extra columns.
        // ignore the default rate card table columns as they should ALWAYS exist
        int start = RateCardDTO.TABLE_COLUMNS.size();
        for (int i = start; i < line.length; i++) {
            tableGenerator.addColumn(new TableGenerator.Column(line[i], DEFAULT_DATA_TYPE, true));
        }

        // create rate table
        String createSql = tableGenerator.buildCreateTableSQL();
        jdbcTemplate.execute(createSql);
        logger.debug("Created table '{}'", rateCard.getTableName());

        // load rating data in batches
        String insertSql = tableGenerator.buildInsertPreparedStatementSQL();
        List<List<String>> rows = new ArrayList<>();
        for (int i = 1; i <= BATCH_SIZE; i++) {
            // add row to insert batch
            line = reader.readNext();
            if (line != null) {
                rows.add(Arrays.asList(line));
            } else {
                // end of file
                executeBatchInsert(insertSql, rows);
                break; // done
            }

            // reached batch limit
            if (i == BATCH_SIZE) {
                executeBatchInsert(insertSql, rows);
                i = 1;
                rows.clear(); // next batch
            }
        }
    }

    private void checkRateTableExistance(String tableName) throws SQLException {

        DataSource dataSource = jdbcTemplate.getDataSource();
        Connection connection = DataSourceUtils.getConnection(dataSource);
        List<String> tableNames = JDBCUtils.getAllTableNames(connection);
        if (tableNames.contains(tableName.toLowerCase())) {
            throw new SessionInternalError("Exception saving rates to database.",
                    new String[]{"RateCardWS,rates,rate.card.db.exist," + tableName});
        }
    }

    /**
     * Validates that the uploaded CSV file starts with the expected columns from {@link RateCardDTO#TABLE_COLUMNS}.
     * If the column names don't match or are in an incorrect order a SessionInternalError will be throw.
     *
     * @param header header line to validate
     * @throws SessionInternalError thrown if errors found in header data
     */
    private void validateCsvHeader(String[] header) throws SessionInternalError {
        List<String> errors = new ArrayList<>();

        if (Arrays.stream(header).distinct().count() != header.length) { //Check if the file has headers duplicated with the same name
            errors.add("RateCardWS,rates,rate.card.header.duplicated");
        } else {
            List<TableGenerator.Column> columns = RateCardDTO.TABLE_COLUMNS;

            for (int i = 0; i < columns.size(); i++) {

                String columnName = StringUtils.EMPTY;
                String expected = columns.get(i).getName();

                if (header.length - 1 >= i) {
                    columnName = header[i].trim();
                }

                if (!expected.equalsIgnoreCase(columnName)) {
                    columnName = columnName.isEmpty() ? "\"\"" : columnName;
                    errors.add("RateCardWS,rates,rate.card.unexpected.header.value," + expected + "," + columnName);
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new SessionInternalError("Rate card CSV has errors in the order of columns, or is missing required columns",
                    errors.toArray(new String[errors.size()]));
        }
    }

    /**
     * Inserts a batch of records into the database.
     *
     * @param insertSql prepared statement SQL
     * @param rows      list of rows to insert
     */
    private void executeBatchInsert(String insertSql, final List<List<String>> rows) {
        logger.debug("Inserting {} records:{}", rows.size(), rows);

        jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int batch) throws SQLException {
                List<String> values = rows.get(batch);
                for (int i = 0; i < values.size(); i++) {
                    String value = values.get(i).trim();

                    // todo: we need a better solution here - maybe TableGenerator.Column should have a JDBC SQL Type?
                    switch (i) {
                        case 0:  // row id
                            preparedStatement.setInt(i + 1, StringUtils.isNotBlank(value) ? Integer.valueOf(value) : 0);
                            break;

                        case 3:  // rate card rate
                            preparedStatement.setBigDecimal(i + 1, StringUtils.isNotBlank(value) ? new BigDecimal(value) : BigDecimal.ZERO);
                            break;

                        default: // everything else
                            preparedStatement.setObject(i + 1, value);
                    }
                }
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });
    }

    /**
     * Returns a list of column names read from the rate table in the database.
     *
     * @return column names
     */
    public List<String> getRateTableColumnNames() {
        DataSource dataSource = jdbcTemplate.getDataSource();
        Connection connection = DataSourceUtils.getConnection(dataSource);

        List<String> columns = Collections.emptyList();

        try {
            columns = JDBCUtils.getAllColumnNames(connection, rateCard.getTableName());
        } catch (SQLException e) {
            throw new SessionInternalError("Could not read columns from rate card table.", e,
                    new String[]{"RateCardWS,rates,rate.card.cannot.read.rating.table"});

        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }

        return columns;
    }

    /**
     * Returns a scrollable result set for reading the rate table rows.
     *
     * <strong>You MUST remember to close the result set when your done reading!</strong>
     *
     * @return scrollable result set
     */
    public ScrollableResults getRateTableRows() {
        return rateCardDas.getRateTableRows(rateCard.getTableName());
    }



    /*
            Spring Beans stuff
     */

    public void registerSpringBeans() {
        registerSpringBeans(false);
    }

    private void doRegisterBeanInCluster(Action action) {
        JmsTemplate template = Context.getBean(Name.JMS_TEMPLATE);
        Destination destination = Context.getBean("pricingBeanRegisterDestination");
        template.send(destination, session -> session.createObjectMessage(PricingBeanMessage.of(action,
                PricingBeanRegisterType.RATE_CARD_BEAN, rateCard.getId())));
    }

    /**
     * Registers spring beans with the application context so support caching and look-up
     * of pricing from the rating tables.
     */
    public void registerSpringBeans(boolean finderOnly) {
        RateCardBeanFactory factory = getBeanFactory();
        GenericApplicationContext ctx = (GenericApplicationContext) Context.getApplicationContext();

        if (finderOnly) {
            String finderBeanName = factory.getFinderBeanName();
            BeanDefinition finderBeanDef = factory.getFinderBeanDefinition(null);

            logger.info("Registering beans: {}", finderBeanName);
            ctx.registerBeanDefinition(finderBeanName, finderBeanDef);
        } else {
            String readerBeanName = factory.getReaderBeanName();

            List<BeanDefinition> readerBeanDefList = new ArrayList<>();
            if (rateCard.getCompany() != null) {
                BeanDefinition readerBeanDef = factory.getReaderBeanDefinition(rateCard.getCompany().getId());
                readerBeanDefList.add(readerBeanDef);
            }
            for (CompanyDTO company : rateCard.getChildCompanies()) {
                BeanDefinition readerBeanDef = factory.getReaderBeanDefinition(company.getId());
                readerBeanDefList.add(readerBeanDef);
            }

            String loaderBeanName = factory.getLoaderBeanName();
            BeanDefinition loaderBeanDef = factory.getLoaderBeanDefinition(readerBeanName);

            String finderBeanName = factory.getFinderBeanName();
            BeanDefinition finderBeanDef = factory.getFinderBeanDefinition(loaderBeanName);

            logger.info("Registering beans: {}, {}, {}", readerBeanName, loaderBeanName, finderBeanName);

            // register spring beans!
            ctx.registerBeanDefinition(loaderBeanName, loaderBeanDef);
            ctx.registerBeanDefinition(finderBeanName, finderBeanDef);

            for (BeanDefinition readerBeanDef : readerBeanDefList) {
                ctx.registerBeanDefinition(readerBeanName, readerBeanDef);
            }

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
        try {
            RateCardBeanFactory factory = getBeanFactory();
            GenericApplicationContext ctx = (GenericApplicationContext) Context.getApplicationContext();

            String finderBeanName = factory.getFinderBeanName();

            if (finderOnly) {
                logger.debug("Removing beans: {}", finderBeanName);
                ctx.removeBeanDefinition(finderBeanName);
            } else {
                String readerBeanName = factory.getReaderBeanName();
                String loaderBeanName = factory.getLoaderBeanName();

                logger.debug("Removing beans: {}, {}, {}", readerBeanName, loaderBeanName, finderBeanName);

                ctx.removeBeanDefinition(readerBeanName);
                ctx.removeBeanDefinition(loaderBeanName);
                ctx.removeBeanDefinition(finderBeanName);
            }
        } catch (NoSuchBeanDefinitionException e) {
            logger.warn("Beans not found", e);
        }
    }

    /**
     * Returns an instance of the {@link RateCardBeanFactory} for producing rate card beans
     * used for pricing.
     *
     * @return rate card bean factory
     */
    public RateCardBeanFactory getBeanFactory() {
        return new RateCardBeanFactory(rateCard);
    }

    public static final RateCardWS getWS(RateCardDTO rateCard) {
        RateCardWS ws = new RateCardWS();
        ws.setId(rateCard.getId());
        ws.setName(rateCard.getName());
        ws.setTableName(rateCard.getTableName());
        return ws;
    }


    public static final RateCardDTO getDTO(RateCardWS ws) {

        RateCardDTO rateCard = new RateCardDTO();
        rateCard.setId(ws.getId());
        rateCard.setName(ws.getName());
        rateCard.setTableName(ws.getTableName());
        return rateCard;
    }
}
