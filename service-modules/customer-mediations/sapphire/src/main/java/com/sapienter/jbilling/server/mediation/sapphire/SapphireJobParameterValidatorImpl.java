package com.sapienter.jbilling.server.mediation.sapphire;

import static com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation.PARAMETER_MEDIATION_ENTITY_ID_KEY;
import static com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation.PARAMETER_MEDIATION_FILE_PATH_KEY;
import static com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation.PARAMETER_RECYCLE_MEDIATION_PROCESS_ID_KEY;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.ACCOUNT_TYPE_ITEM_TABLE_FIELD_NAME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CARRIER_TABLE_FIELD_NAME;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.xml.sax.SAXException;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.validator.mediation.InvalidJobParameterException;
import com.sapienter.jbilling.server.validator.mediation.MediationJobParameterValidator;

public class SapphireJobParameterValidatorImpl implements MediationJobParameterValidator {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String EMPTY_NULL_MESSAGE     = "File parameter is null or empty!";
    private static final String FILE_NOT_FOUND_MESSAGE = "Cdr file not found!";
    private static final String INVALID_FILE_MESSAGE   = "Invalid cdr file!";
    private static final String COUNT_SQL              = "SELECT COUNT(*) FROM %s";


    @Value("classpath:cfs_billing.xsd")
    private Resource resource;

    @javax.annotation.Resource(name = "jBillingJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SapphireMediationHelperService sapphireMediationHelperService;

    private Validator validator;

    @PostConstruct
    void init() throws SAXException, IOException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(resource.getURL());
        validator = schema.newValidator();
    }

    @Override
    public void validate(JobParameters parameters) {
        try {
            logger.debug("Validating parameters {}", parameters);
            Integer entityId = Integer.valueOf(parameters.getString(PARAMETER_MEDIATION_ENTITY_ID_KEY));
            checkMandateConfig(entityId);
            Assert.notNull(parameters, "Parameter is null!");
            String recycleId = parameters.getString(PARAMETER_RECYCLE_MEDIATION_PROCESS_ID_KEY);
            if(StringUtils.isNotEmpty(recycleId)) {
                logger.debug("Skipping validation for recycle job!");
                return;
            }
            String cdrFileName = parameters.getString(PARAMETER_MEDIATION_FILE_PATH_KEY);
            doExecuteAndLogError(()->  Assert.isTrue(StringUtils.isNotBlank(cdrFileName), EMPTY_NULL_MESSAGE), EMPTY_NULL_MESSAGE);

            File cdrFile = Paths.get(cdrFileName).toFile();

            doExecuteAndLogError(()-> {
                logger.debug("Validating cdr file {}", cdrFileName);
                Assert.isTrue(cdrFile.exists(), FILE_NOT_FOUND_MESSAGE);
            }, FILE_NOT_FOUND_MESSAGE);

            validateXMLSchema(cdrFile);
        } catch(Exception ex) {
            logger.error("parameters validation failed!", ex);
            throw new InvalidJobParameterException(ex.getMessage(), ex);
        }
    }

    private void validateXMLSchema(File cdrFile) {
        try {
            validator.validate(new StreamSource(cdrFile));
        } catch(SAXException | IOException ex) {
            logger.error(INVALID_FILE_MESSAGE, ex);
            throw new IllegalArgumentException(INVALID_FILE_MESSAGE, ex);
        }  catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new SessionInternalError(e);
        }
    }

    private void doExecuteAndLogError(Runnable action, String message) {
        try {
            action.run();
        } catch(IllegalArgumentException ex) {
            logger.error(message, ex);
            throw ex;
        }
    }

    public Validator getValidator() {
        return validator;
    }

    private void checkMandateConfig(Integer entityId) {
        validateTableConfig(entityId, CARRIER_TABLE_FIELD_NAME);
        validateTableConfig(entityId, ACCOUNT_TYPE_ITEM_TABLE_FIELD_NAME);
    }

    private void validateTableConfig(Integer entityId, String tableFieldName) {
        String tableName = sapphireMediationHelperService.getMetaFieldsForEntity(entityId).get(tableFieldName);
        if(StringUtils.isEmpty(tableName)) {
            throw new IllegalArgumentException(tableFieldName + " is Required Compnay Level MetaFied!");
        }
        if(!isTablePresent(tableName)) {
            throw new IllegalArgumentException(tableName + " not found!");
        }
        if(!hasRows(tableName)) {
            throw new IllegalArgumentException( tableName + " has no reocord!");
        }
    }

    private boolean isTablePresent(String tableName) {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
                ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null);) {
                return rs.next();
        } catch (SQLException sqlException) {
            throw new SessionInternalError(sqlException);
        }
    }

    private boolean hasRows(String tableName) {
        return jdbcTemplate.queryForObject(String.format(COUNT_SQL, tableName), Integer.class)!=0;
    }
}
