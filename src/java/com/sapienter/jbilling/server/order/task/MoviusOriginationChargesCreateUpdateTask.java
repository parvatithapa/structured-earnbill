package com.sapienter.jbilling.server.order.task;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.mediation.customMediations.movius.MoviusHelperService;
import com.sapienter.jbilling.server.movius.integration.MoviusConstants;
import com.sapienter.jbilling.server.movius.integration.MoviusOrderHelper;
import com.sapienter.jbilling.server.movius.integration.MoviusTaskUtils;
import com.sapienter.jbilling.server.movius.integration.OriginationCharges;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.util.Context;

/**
 * Created by faizan on 10/5/17.
 *
 * This is a scheduled plugin that creates and updates Subscription and Origination Charges for Movius.
 */
public class MoviusOriginationChargesCreateUpdateTask extends AbstractCronTask {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ParameterDescription PARAMETER_XML_BASE_DIR =
            new ParameterDescription(MoviusConstants.ORIGINATION_XML_PARAMETER_BASE_DIR, true, ParameterDescription.Type.STR);
    private static final ParameterDescription PARAMETER_XSD_BASE_DIR =
            new ParameterDescription(MoviusConstants.ORIGINATION_XSD_PARAMETER_BASE_DIR, true, ParameterDescription.Type.STR);
    private static final ParameterDescription PARAMETER_MAX_RETRY_COUNT =
            new ParameterDescription(MoviusConstants.ORIGINATION_MAX_RETRY_COUNT, false, ParameterDescription.Type.INT);

    private static final String XML_DEFAULT_DIR_PATH;
    private static final String XSD_DEFAULT_DIR_PATH;

    private IMethodTransactionalWrapper actionTxWrapper = Context.getBean("methodTransactionalWrapper");

    private static Unmarshaller unmarshaller;
    private String error;
    private Integer currentRetryCount;
    private Integer maxRetryCount;
    private MoviusTaskUtils.Errors errors;
    private List<String> doneOrgIdList = new ArrayList<>();
    private List<MoviusTaskUtils.Errors> notificationList = new ArrayList<>();
    private Timestamp currentTimeStamp;
    private OriginationCharges moviusOriginationCharges;

    //initializer for pluggable params
    {
        descriptions.add(PARAMETER_XML_BASE_DIR);
        descriptions.add(PARAMETER_XSD_BASE_DIR);
        descriptions.add(PARAMETER_MAX_RETRY_COUNT);
    }

    static {
        try {
			unmarshaller = JAXBContext.newInstance(OriginationCharges.class).createUnmarshaller();
            XML_DEFAULT_DIR_PATH = getXmlDefaultDir();
            XSD_DEFAULT_DIR_PATH = getXsdDefaultDir();
        } catch(JAXBException e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    public String getTaskName() {
        return this.getClass().getName() + "-" + getEntityId();
    }

    private String getXmlBaseDirectory() {
        return getParameter(PARAMETER_XML_BASE_DIR.getName(), XML_DEFAULT_DIR_PATH);
    }

    private String getXsdBaseDirectory() {
        return getParameter(PARAMETER_XSD_BASE_DIR.getName(), XSD_DEFAULT_DIR_PATH);
    }

    private Integer getMaxRetryCount() {
        Integer count = MoviusConstants.DEFAULT_MAX_RETRY_COUNT;
        try {
			count = getParameter(PARAMETER_MAX_RETRY_COUNT.getName(), MoviusConstants.DEFAULT_MAX_RETRY_COUNT);
        } catch (PluggableTaskException e) {
            logger.debug("Exception while getting the Max Retry Count Parameter Value {}", e.getMessage());
        }
        return count;
    }

    @Override
    public void doExecute (JobExecutionContext context) throws JobExecutionException {
        _init(context);
        try (RunAsUser ctx = new RunAsCompanyAdmin(getEntityId())) {
            String adminUserName = ctx.getUserName();
            logger.debug("Running MoviusOriginationChargesCreateUpdateTask on batch-1 server using {} account", adminUserName);
            currentTimeStamp = new Timestamp(System.currentTimeMillis());
            maxRetryCount = getMaxRetryCount();

            //Getting parameter values
            String xmlBaseDirPath = getXmlBaseDirectory();

            if(!isValidXmlDir(xmlBaseDirPath)) {
                error = String.format(MoviusConstants.ORIGINATION_ERROR_INVALID_XML_DIR, getEntityId().toString(), xmlBaseDirPath);
                createInitialErrorMessage(error);
                logger.debug(error);
                sendReport(currentTimeStamp, MoviusConstants.ORIGINATION_CREATE_UPDATE_TASK_MESSAGE_KEY,
                        MoviusTaskUtils.buildErrorsParameter(notificationList));
            } else {
                File xmlBaseDir = new File(xmlBaseDirPath);
                boolean isDoneDirCreated = true;
                String doneDirPath = appendStrings(xmlBaseDir.getAbsolutePath().concat(File.separator), MoviusConstants.DONE_DIR_NAME);
                if(!Paths.get(doneDirPath).toFile().exists()) {
                    isDoneDirCreated = new File(doneDirPath).mkdir();
                }
                if (isDoneDirCreated) {
                    processFile(xmlBaseDir, new File(getXsdBaseDirectory()), new File(doneDirPath));
                } else {
                    error = MoviusConstants.ORIGINATION_ERROR_DONE_DIR_NOT_FOUND;
                    createInitialErrorMessage(error);
                    logger.debug(error);
                    sendReport(currentTimeStamp, MoviusConstants.ORIGINATION_CREATE_UPDATE_TASK_MESSAGE_KEY,
                            MoviusTaskUtils.buildErrorsParameter(notificationList));
                }

            }

            logger.debug("Done. Running task using {} account", adminUserName);
        } catch (Exception e) {
            throw new JobExecutionException(e);
        }
    }

    public void sendReport(Timestamp currentTimeStamp, String key, String param) {
        // verify if there are notifications to be sent to the Billing Administrator
        String billingAdminEmail = MoviusTaskUtils.getMetaFieldValueByName(MoviusConstants.COMPANY_ADMIN_EMAIL_MF_NAME,
                getEntityId());
        if(StringUtils.isNotEmpty(billingAdminEmail)) {
            String[] params = new String[2];
            params[0] = currentTimeStamp.toString();
            params[1] = param;
            MoviusTaskUtils.sendMoviusErrorReportMessage(getEntityId(),billingAdminEmail,
                    key, params);
        } else {
            logger.debug(MoviusConstants.ORIGINATION_ERROR_COMPANY_ADMIN_EMAIL_NOT_FOUND, getEntityId());
        }
    }

    private boolean isValidXmlDir(String dir) {
        String[] tokens = dir.trim().split(File.separator);
        return tokens[tokens.length - 1].equals(getEntityId().toString()) && Paths.get(dir).toFile().exists();
    }

    private static String getXmlDefaultDir(){
        return new StringBuilder()
                .append(Util.getSysProp("base_dir"))
                .append(MoviusConstants.ORG_DIR)
                .append(File.separator)
                .toString();
    }

    private static String getXsdDefaultDir(){
        return new StringBuilder().append(Util.getSysProp("base_dir"))
                .append(MoviusConstants.ORG_DIR)
                .append(File.separator)
                .append("xsd")
                .append(File.separator)
                .toString();
    }

    public void processFile(File xmlBaseDir, File xsdBaseDir, File doneDir) throws JobExecutionException {
        File[] xmlFilesList = xmlBaseDir.listFiles(file -> {
            String fileName = file.getName();
            return (fileName.endsWith(".xml") || fileName.endsWith(".XML")) &&
                    !fileName.contains(MoviusConstants.FILE_RENAME_EXTENSION);
        });
        if(ArrayUtils.isEmpty(xmlFilesList)) {
            error = String.format(MoviusConstants.ORIGINATION_ERROR_XML_FILE_NOT_FOUND, xmlBaseDir);
            logger.debug(error);
            return ;
        }
        String xsdPath = appendStrings(xsdBaseDir.getAbsolutePath().concat(File.separator) , MoviusConstants.ORIGINATION_XSD_FILE_NAME);
        File xsdFile = new File(xsdPath);
        if( xsdFile.exists()) {
            for (File xmlFile : xmlFilesList) {
                errors = new MoviusTaskUtils.Errors();
                notificationList = new ArrayList<>();
                String fileNameWithTimeStamp = appendStrings(currentTimeStamp.toString().concat("."), xmlFile.getName());
                String doneFileName = appendStrings(MoviusConstants.FILE_RENAME_EXTENSION , fileNameWithTimeStamp);
                String errorFileName = appendStrings(MoviusConstants.ERROR_FILE_RENAME_EXTENSION , fileNameWithTimeStamp);
                String currentFileName = xmlFile.getAbsolutePath();
                String retryFileName = getRetryFileName(xmlFile.getName());

                try {
                	if (validateXMLSchema(xsdFile, xmlFile) && parseXML(xmlFile)) {
						String updatedFileName;
						if(areAllOrgsExists(getEntityId()) || null == retryFileName) {
							updatedFileName = appendStrings(doneDir.getAbsolutePath().concat(File.separator), doneFileName);
							errors.setFilename(doneFileName);
						} else {
						    updatedFileName = appendStrings(xmlBaseDir.getAbsolutePath().concat(File.separator), retryFileName);
						    errors.setFilename(retryFileName);
						}
						renameFile(currentFileName, updatedFileName);
						createUpdateOriginationOrders();
                    } 
                } catch(JAXBException | IOException | SAXException ex) {
                    String updatedFileName = appendStrings(doneDir.getAbsolutePath().concat(File.separator), errorFileName);
                    errors.setFilename(errorFileName);
                    renameFile(currentFileName, updatedFileName);
                    if(ex instanceof JAXBException) {
                        createExceptionErrorMessage(ex.getMessage(), MoviusConstants.ORIGINATION_ERROR_XML_FILE_PARSING_FAILED, xmlFile.getName());
                    } else {
                        createExceptionErrorMessage(ex.getMessage(), MoviusConstants.ORIGINATION_ERROR_XSD_VALIDATION_FAILED, xmlFile.getName());
                    }

                } finally {
                    if(CollectionUtils.isNotEmpty(errors.getErrorList())) {
                        notificationList.add(errors);
                    }

                    if(CollectionUtils.isNotEmpty(notificationList)) {
                        sendReport(currentTimeStamp, MoviusConstants.ORIGINATION_CREATE_UPDATE_TASK_MESSAGE_KEY,
                                MoviusTaskUtils.buildErrorsParameter(notificationList));
                    } else {
                        sendReport(currentTimeStamp, MoviusConstants.ORIGINATION_CREATE_UPDATE_TASK_MESSAGE_KEY_FOR_SUCCESS,
                                xmlFile.getName());
                    }
                }
            }
        }  else {
            error = String.format(MoviusConstants.ORIGINATION_ERROR_XSD_FILE_NOT_FOUND , xsdPath);
            createInitialErrorMessage(error);
            logger.debug(error);
            sendReport(currentTimeStamp, MoviusConstants.ORIGINATION_CREATE_UPDATE_TASK_MESSAGE_KEY,
                    MoviusTaskUtils.buildErrorsParameter(notificationList));
        }
    }

    private boolean areAllOrgsExists(Integer entityId){
        return actionTxWrapper.execute(() ->
            !moviusOriginationCharges.getProvider().stream().filter( provider ->
            provider.getCountry().stream().filter(country ->
            country.getOrgMapping().getOrg().stream().filter(org ->
            null == getUserIdByCustomerMetaField(org.getOrgId(), MoviusConstants.ORG_ID, entityId)
                    ).findFirst().isPresent()
                    ).findFirst().isPresent()
                    ).findFirst().isPresent()
        );
    }

    private void createUpdateOriginationOrders() {
        Integer entityId = getEntityId();
        Map<String, Integer> currencyLanguageMap = actionTxWrapper.execute(() -> {
            CompanyDTO companyDTO = new CompanyDAS().findEntityByName(moviusOriginationCharges.getSystemId());
            if (null == companyDTO) {
                error = String.format(MoviusConstants.ORIGINATION_ERROR_COMPANY_NOT_FOUND , moviusOriginationCharges.getSystemId());
                logger.debug(error);
                errors.getErrorList().add(error);
                return Collections.<String, Integer>emptyMap();
            } else if(0 != entityId.compareTo(companyDTO.getId())){
                error = String.format(MoviusConstants.ORIGINATION_ERROR_INVALID_XML_ENTITY , entityId, companyDTO.getId());
                logger.debug(error);
                errors.getErrorList().add(error);
                return Collections.<String, Integer>emptyMap();
            }

            Integer currencyId = companyDTO.getCurrencyId();
            Integer languageId = companyDTO.getLanguageId();

            Map<String, Integer> result = new HashMap<>();
            result.put(MoviusConstants.CURRENCY_ID, currencyId);
            result.put(MoviusConstants.LANGUAGE_ID, languageId);
            return result;
        });

        if(currencyLanguageMap.isEmpty()) {
            return ;
        }

        for(OriginationCharges.Provider moviusProviderAttribute : moviusOriginationCharges.getProvider()){
            String categoryName = moviusProviderAttribute.getName();
            for(OriginationCharges.Provider.Country moviusCountryAttribute : moviusProviderAttribute.getCountry()) {
                String itemCode = categoryName.concat("-").concat(moviusCountryAttribute.getName());
                try {
                    Integer itemId = getItemId(itemCode);
                    if(Objects.isNull(itemId)) {
                        error = String.format(MoviusConstants.ORIGINATION_ERROR_ITEM_NOT_FOUND, itemCode);
                        logger.debug(error);
                        errors.getErrorList().add(error);
                        continue;

                    } else {
                        createUpdateOrder(moviusCountryAttribute.getOrgMapping().getOrg(),
                                moviusCountryAttribute.getCharges(), currencyLanguageMap.get(MoviusConstants.LANGUAGE_ID), itemId,
                                currencyLanguageMap.get(MoviusConstants.CURRENCY_ID), entityId, itemCode);
                        logger.debug(MoviusConstants.ORIGINATION_ITEM_ID , itemId);
                    }

                } catch (HibernateException | SessionInternalError e) {
                    createExceptionErrorMessage(e.getMessage(), MoviusConstants.ORIGINATION_ERROR_ITEM_NOT_FOUND, itemCode);
                    logger.debug(e.getMessage(), e);
                    continue;
                }
            }
        }
    }

    private void createUpdateOrder(List<OriginationCharges.Provider.Country.OrgMapping.Org> orgList, Double xmlCharges,
                                   Integer languageId, Integer itemId, Integer currencyId, Integer entityId, String itemCode) {
        for (OriginationCharges.Provider.Country.OrgMapping.Org moviusOrgAttribute : orgList) {
            String orgId = moviusOrgAttribute.getOrgId();
            try {
                actionTxWrapper.execute(() -> {
                    Integer userId = getUserIdByCustomerMetaField(orgId, MoviusConstants.ORG_ID, entityId);
                    Integer quantity = moviusOrgAttribute.getCount();
                    if (null != userId && quantity.compareTo(0) >= 0) {
                        MoviusHelperService helperService = Context.getBean("moviusHelperServiceBean");
                        Map<String, Integer> result = helperService.resolveUserIdByOrgId(entityId, orgId);
                        Integer targetUserId = result.get("userId");
                        BigDecimal charges = null != xmlCharges ? new BigDecimal(xmlCharges).setScale(xmlCharges.toString().length(), BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;

                        Integer orderPeriodId = MoviusTaskUtils.getMonthlyOrderPeriod(entityId);
                        Integer orderStatusId = MoviusTaskUtils.getOrderChangeApplyStatus(entityId);

                        OrderWS latestOrder = MoviusTaskUtils.getLatestMonthlyActiveOrder(targetUserId, languageId, orderPeriodId);
                        String billableOrgId  = MoviusTaskUtils.findOrgIdbyUserId(targetUserId);
                        if (null == latestOrder && !doneOrgIdList.contains(orgId)) {
                            doneOrgIdList.add(orgId);
                            Integer orderId = MoviusOrderHelper.of(targetUserId, new BigDecimal(quantity.toString()), charges, itemId, entityId)
                                    .addOrgId(orgId)
                                    .addBillableOrgId(billableOrgId)
                                    .create(orderStatusId, orderPeriodId)
                                    .getOrderId();
                            logger.debug(MoviusConstants.ORIGINATION_SUCCESS_ORDER_CREATED , orderId);
                        } else if (null != latestOrder && !MoviusTaskUtils.isOrderLineExists(latestOrder, itemId)) {
                            if(quantity.intValue()!=0) {
                                doneOrgIdList.add(orgId);
                                MoviusOrderHelper.of(latestOrder, new BigDecimal(quantity.toString()), charges, itemId, entityId)
                                        .addOrgId(orgId)
                                        .addBillableOrgId(billableOrgId)
                                        .addNewLine(orderStatusId);
                            } else {
                                logger.error("Invalid origination tag {} For Org Id {} In File {} ",moviusOrgAttribute, orgId, errors.getFilename());
                            }
                        } else if (null != latestOrder) {
                            doneOrgIdList.add(orgId);
                            MoviusOrderHelper.of(latestOrder, new BigDecimal(quantity.toString()), charges, itemId, entityId)
                                    .addOrgId(orgId)
                                    .addBillableOrgId(billableOrgId)
                                    .updateOrderLine(orderStatusId);
                        }
                    } else {
                        if (doneOrgIdList.contains(orgId)) {
                            error = String.format(MoviusConstants.ORIGINATION_ERROR_ORDER_ALREADY_CREATED, orgId);
                        } else if(quantity < 0){
                            error = String.format(MoviusConstants.ORIGINATION_ERROR_QUANTITY_IS_NEGATIVE, quantity.toString(), orgId);
                        } else {
                            error = String.format(MoviusConstants.ORIGINATION_ERROR_CUSTOMER_NOT_FOUND, orgId);
                            logger.debug(error);
                            if((currentRetryCount < maxRetryCount))
                                error = MoviusConstants.EMPTY;
                        }
                        if(StringUtils.isNotEmpty(error.trim())) {
                            logger.debug(error);
                            errors.getErrorList().add(error);
                        }
                    }
                });
            } catch (Exception e) {
                createExceptionErrorMessage(e.getMessage(), MoviusConstants.ORIGINATION_ERROR_ORDER_CREATION_FAILED, orgId);
                logger.debug(e.getMessage(), e);
                continue;
            }
        }
    }

    private String getRetryFileName(String fileName){
        StringBuilder fileNameBuilder = new StringBuilder().append(MoviusConstants.RETRY_FILE_RENAME_EXTENSION);
        currentRetryCount = 0;
        if(fileName.contains("retry")){
            String[] tokens = fileName.split("\\.");
            currentRetryCount = Integer.parseInt(tokens[1]);
            if(currentRetryCount < maxRetryCount){
                fileNameBuilder = fileNameBuilder.append(currentRetryCount+1).append(".").append(currentTimeStamp);
                for(int i = 4; i<tokens.length; i++){
                    fileNameBuilder = fileNameBuilder.append(".").append(tokens[i]);
                }
                return fileNameBuilder.toString();
            } else
                return null;
        }
        return fileNameBuilder.append(currentRetryCount+1).append(".").append(currentTimeStamp).append(".").append(fileName).
                toString();
    }


    private String appendStrings(String message, String parameter) {
        return message.concat(parameter);
    }

    private Integer getUserIdByCustomerMetaField(String metaFieldValue, String metaFieldName, Integer entityId) {
        UserBL userBl = new UserBL();
        List<CustomerDTO> customerList = userBl.getUserByCustomerMetaField(metaFieldValue, metaFieldName, entityId);
        CustomerDTO customer = null != customerList ?
					customerList.stream().filter(customerDTO ->
					(!customerList.isEmpty())).findFirst().orElse(null) : null;

		return null != customer ? customer.getBaseUser().getId() : null;
    }

    private Integer getItemId(String itemCode) {
        return actionTxWrapper.execute(() -> {
            ItemDTO itemDTO = new ItemDAS().findItemByInternalNumber(itemCode, getEntityId());
            return Objects.nonNull(itemDTO) ? itemDTO.getId() : null;
        });
    }

    private boolean parseXML(File fileName) throws JAXBException {
        moviusOriginationCharges = (OriginationCharges) unmarshaller.unmarshal(fileName);
        return true;
    }

    private boolean validateXMLSchema(File xsdFile, File xmlFile) throws SAXException, IOException {
        SchemaFactory factory =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(xsdFile);
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(xmlFile));
        return true;
    }

    private void renameFile(String sourcePath, String updatedPath) {
        File oldfile = new File(sourcePath);
        File newfile = new File(updatedPath);

        if(oldfile.renameTo(newfile)) {
            logger.debug(MoviusConstants.ORIGINATION_SUCCESS_FILE_NAME_CHANGED, updatedPath);
        } else {
            logger.debug(MoviusConstants.ORIGINATION_ERROR_FILE_RENAMING_FAILED);
        }
    }

    private void createInitialErrorMessage(String message) {
        errors = new MoviusTaskUtils.Errors();
        errors.getErrorList().add(message);
        notificationList.add(errors);
    }

    private void createExceptionErrorMessage(String exceptionMessage, String customMessage, Object... args){
        error = null != exceptionMessage ? exceptionMessage.substring(0, Math.min(exceptionMessage.length(), 100)) :
                String.format(customMessage, args);
        errors.getErrorList().add(error);
        logger.debug(error);
    }
}
