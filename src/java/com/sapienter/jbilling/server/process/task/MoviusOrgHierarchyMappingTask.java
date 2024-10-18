package com.sapienter.jbilling.server.process.task;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

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
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.mediation.customMediations.movius.MoviusHelperService;
import com.sapienter.jbilling.server.mediation.movius.db.OrgCountPositionDAS;
import com.sapienter.jbilling.server.mediation.movius.db.OrgCountPositionDTO;
import com.sapienter.jbilling.server.movius.integration.MoviusConstants;
import com.sapienter.jbilling.server.movius.integration.MoviusTaskUtils;
import com.sapienter.jbilling.server.movius.integration.Organization;
import com.sapienter.jbilling.server.movius.integration.Organizations;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

/**
 * @author Manish Bansod
 * @since 10-11-2017
 * This schedule task is for creating the customers and orders with provided
 * XML by movius.
 */
public class MoviusOrgHierarchyMappingTask extends AbstractCronTask {

	public static final ParameterDescription PARAMETER_BASE_DIR =
			new ParameterDescription("XML Base Directory", true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_XSD_BASE_DIR =
                        new ParameterDescription("XSD Base Directory", true, ParameterDescription.Type.STR);
    public static final ParameterDescription BILLING_CYCLE_PERIOD =
            new ParameterDescription("billing cycle period Id", true, ParameterDescription.Type.STR);
    public static final ParameterDescription BILLING_CYCLE_DAY =
            new ParameterDescription("billing cycle day", true, ParameterDescription.Type.STR);

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static IWebServicesSessionBean sessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
	private IMethodTransactionalWrapper actionTxWrapper = Context.getBean("methodTransactionalWrapper");
	private static Unmarshaller unmarshaller;
	private String xmlPath;
	private MoviusTaskUtils.Errors errors;
	private List<MoviusTaskUtils.Errors> notificationList = new ArrayList<>();
	
	{
		descriptions.add(PARAMETER_BASE_DIR);
		descriptions.add(PARAMETER_XSD_BASE_DIR);
		descriptions.add(BILLING_CYCLE_PERIOD);
		descriptions.add(BILLING_CYCLE_DAY);
	}

	static {
		try {
			unmarshaller = JAXBContext.newInstance(Organizations.class).createUnmarshaller();
		} catch(JAXBException e) {
			throw new SessionInternalError(e);
		}
	}

	@Override
	public String getTaskName() {
		return this.getClass().getName() + "-" + getEntityId();
	}

	@Override
	public void doExecute(JobExecutionContext context) throws JobExecutionException {
		_init(context);
		try (RunAsUser ctx = new RunAsCompanyAdmin(getEntityId())) {
			logger.debug("Running MoviusOrgHierarchyMappingTask on batch-1 server using {} account", ctx.getUserName());
			Timestamp currentTimeStamp = new Timestamp(System.currentTimeMillis());
			xmlPath = getParameters().get(PARAMETER_BASE_DIR.getName()) + File.separator;
			// list all files in the 'path' directory and filter list of files present
			File currDir = new File(xmlPath);
			logger.debug("file path {}", xmlPath);

			File[] xmlFilesList = currDir.listFiles(file -> file.getName().endsWith(".xml"));
			if(ArrayUtils.isEmpty(xmlFilesList)) {
				logger.debug(MoviusConstants.ORGANIZATION_ERROR_NO_UNPARSED_FILE_FOUND , xmlPath);
                return ;
            }
			Arrays.sort(xmlFilesList, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
			
			String xsdPath = getParameters().get(PARAMETER_XSD_BASE_DIR.getName()) + File.separator;
			File xsdFile = new File(xsdPath + MoviusConstants.XSD_FILE_NAME);

			if(xsdFile.exists()) {
				for (File xmlFileName : xmlFilesList) {
					try {
						logger.debug("Going to parse file {}", xmlFileName);
						errors = new MoviusTaskUtils.Errors();
						notificationList = new ArrayList<>();
						if (validateXMLSchema(xsdFile, xmlFileName)) {
							logger.debug("XML is Validated and started processing");
							Organizations org = (Organizations) unmarshaller.unmarshal(xmlFileName);
							boolean isCompanyExist = actionTxWrapper.execute(() ->
								null != new CompanyDAS().findEntityByName(org.getSystemId())
							);
							if(!isCompanyExist) {
								createInitialErrorMessage(String.format("Entity %s is not available, processing next file", org.getSystemId()),
										xmlFileName.getName());
								continue;
							}

							Integer orgEntityId = actionTxWrapper.execute(() ->
								new CompanyDAS().findEntityByName(org.getSystemId()).getId()
							);
							if(!orgEntityId.equals(getEntityId())) {
								createInitialErrorMessage(String.format("Entity is different in processing file %s, recieved entity id is %s and task executor entity id is %s",
										xmlFileName, orgEntityId, getEntityId()), xmlFileName.getName());
								continue;
							}
							logger.debug("Starting creating users in entity {} ",org.getSystemId());
							createUser(org, org.getSystemId(), getEntityId(), null, xmlFileName.getName());
						} else {
							moveFile(xmlFileName,MoviusConstants.ERROR_FILE_RENAME_EXTENSION);
						}
					} catch (SessionInternalError e) {
						createExceptionErrorMessage(e.getMessage(), String.format("Exception occured while parsing file %s",
								xmlFileName.getName()), xmlFileName.getName());
					} catch (Exception e) {
						createExceptionErrorMessage(e.getMessage(), String.format("Exception occured while parsing file %s",
								xmlFileName.getName()), xmlFileName.getName());
					} finally {
						if(CollectionUtils.isEmpty(notificationList)) {
							moveFile(xmlFileName,MoviusConstants.FILE_RENAME_EXTENSION);
							sendReport(currentTimeStamp, MoviusConstants.ORGANIZATION_HIERARCHY_MAPPING_TASK_MESSAGE_KEY_FOR_SUCCESS,
									xmlFileName.getName());
						} else {
							moveFile(xmlFileName,MoviusConstants.ERROR_FILE_RENAME_EXTENSION);
							sendReport(currentTimeStamp, MoviusConstants.ORGANIZATION_HIERARCHY_MAPPING_TASK_MESSAGE_KEY,
									MoviusTaskUtils.buildErrorsParameter(notificationList));
						}
					}
				}
			} else {
				createInitialErrorMessage(String.format(MoviusConstants.ORIGINATION_ERROR_XSD_FILE_NOT_FOUND , xsdPath), xsdFile.getName());
				sendReport(currentTimeStamp, MoviusConstants.ORGANIZATION_HIERARCHY_MAPPING_TASK_MESSAGE_KEY,
						MoviusTaskUtils.buildErrorsParameter(notificationList));
			}
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

	private void moveFile(File xmlFileName, String renameFileExtn) {
		String doneDirPath = xmlPath + MoviusConstants.DONE_DIR_NAME;
		if(!Paths.get(doneDirPath).toFile().exists()) {
		    new File(doneDirPath).mkdir();
		}

		File renamedFile = new File(doneDirPath + File.separator +
				renameFileExtn.concat(new Timestamp(System.currentTimeMillis()) + "." + xmlFileName.getName()));
		if (xmlFileName.renameTo(renamedFile)) {
			logger.debug("File moved successfully from {} to {}", xmlFileName.getName(), renamedFile.getName());
		} else {
			createInitialErrorMessage(String.format("Failed file moving %s ", xmlFileName.getName()), xmlFileName.getName());
		}
	}

	private boolean validateXMLSchema(File xsdFile, File xmlFile) {
		try {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = factory.newSchema(xsdFile);
			Validator validator = schema.newValidator();
			validator.validate(new StreamSource(xmlFile));
		} catch (IOException | SAXException e) {
			String errorMsg = String.format("XML validation failed for file %s", xmlFile.getName());
			createExceptionErrorMessage(e.getMessage(), errorMsg, xmlFile.getName());
			return false;
		}
		return true;
	}

    private void createUser(Organizations orgs, String systemId, Integer entityId, Integer parentId,
            String xmlFileName) throws SessionInternalError {
        for (Organization organization : orgs.getOrganizations()) {
            Integer billingCyclePeriod = Integer.parseInt(getParameters().get(BILLING_CYCLE_PERIOD.getName()));
            Integer billingCycleDay = Integer.parseInt(getParameters().get(BILLING_CYCLE_DAY.getName()));
            UserWS userWS = MoviusTaskUtils.getUserWS(systemId, entityId, parentId, organization, billingCyclePeriod, billingCycleDay);
            String subscription = organization.getSubscription();
            String quantity = organization.getCount();
            boolean isValid = validateForOrderCreation(xmlFileName, organization, userWS);
            try {
                actionTxWrapper.execute(() -> {
                    Integer orderPeriodId = MoviusTaskUtils.getMonthlyOrderPeriod(getEntityId());
                    if (MoviusTaskUtils.isUserExist(organization.getId())) {
                        sessionBean.updateUserWithCompanyId(userWS, entityId);
                        if (isValid && StringUtils.isNotEmpty(subscription) && organization.isBillable()) {
                            OrderWS orderWS = MoviusTaskUtils.getLatestMonthlyActiveOrder(userWS.getId(), userWS.getLanguageId(), orderPeriodId);
                            if (null != orderWS) {
                                String originationOrderLineQuantity = MoviusTaskUtils.getOriginationOrderLineQuantity(
                                        orderWS.getOrderLines(), userWS, getQuantity(organization, new BigDecimal(quantity)));
                                updateOrder(entityId, subscription, originationOrderLineQuantity, userWS, orderWS);
                                // Set next invoice date in sync with parent NID
                        if (null != parentId && !organization.isBillable()) {
                            UserWS parentWS = sessionBean.getUserWS(parentId);
                            userWS.setNextInvoiceDate(parentWS.getNextInvoiceDate());
                            userWS.setMainSubscription(parentWS.getMainSubscription());
                            sessionBean.updateUserWithCompanyId(userWS, entityId);
                        }
                    } else {
                        // if there is no previous quantity and now it received in file for the user
                        createOrder(userWS, entityId, subscription, getQuantity(organization, new BigDecimal(quantity)), organization.getId());
                    }
                }
            } else {
                userWS.setId(sessionBean.createUser(userWS));
                logger.debug("User Created With Id {}", userWS.getId());
                // Set next invoice date in sync with parent NID
                if (null != parentId && !organization.isBillable()) {
                    UserWS parentWS = sessionBean.getUserWS(parentId);
                    userWS.setNextInvoiceDate(parentWS.getNextInvoiceDate());
                    userWS.setMainSubscription(parentWS.getMainSubscription());
                    sessionBean.updateUserWithCompanyId(userWS, entityId);
                }

                if (isValid && StringUtils.isNotEmpty(subscription) && organization.isBillable()) {
                    createOrder(userWS, entityId, subscription, getQuantity(organization, new BigDecimal(quantity)), organization.getId());

                }
            }
            if (organization.hasSubOrgs()) {
                createUser(organization.getOrganizations(), systemId, entityId, userWS.getId(), xmlFileName);
            }
        })      ;
            } catch (SessionInternalError error) {
                createExceptionErrorMessage(error.getMessage(), String.format("Exception Occured During Parsing org id %s in file %s",
                        organization.getId(), xmlFileName), xmlFileName);
            } catch (Exception error) {
                createExceptionErrorMessage(error.getMessage(), String.format("Exception Occured During Parsing org id %s in file %s",
                        organization.getId(), xmlFileName), xmlFileName);
            }
        }
    }

	public boolean validateForOrderCreation(String xmlFileName,
			Organization organization, UserWS userWS) {
		if(null == userWS.getParentId() && (StringUtils.isEmpty(organization.getSubscription())
				|| StringUtils.isEmpty(organization.getCount()))) {
			if (organization.isBillable())
				createInitialErrorMessage(String.format("SUBSCRIPTION or COUNT not provided in XML for parent billable org id :%s,"
						+ " it got processed but subscription order is expected for this, please process the org by"
						+ " correcting file :%s", organization.getId(), xmlFileName), xmlFileName);
			return false;
		}

		if (StringUtils.isNotEmpty(organization.getCount()) && (new BigDecimal(organization.getCount()).compareTo(BigDecimal.ZERO) < 0)) {
			createInitialErrorMessage(String.format("COUNT is Negative in XML for org id :%s,"
					+ " it got processed but subscription order is expected for this, please process the org by"
					+ " correcting file :%s", organization.getId(), xmlFileName), xmlFileName);
			return false;
		}

		return null != userWS.getParentId() && StringUtils.isEmpty(organization.getCount()) ? false : true;
	}

	/*
	 * if child org is non-billable and quantity is received, then this count
	 * must be roll up to parent subscription
	 */
	public void updateParentOrder(Integer entityId, Organization organization, String quantity, boolean isValid, Integer orderPeriodId) {
		if(isValid && StringUtils.isNotEmpty(quantity) && !organization.isBillable()) {
			MoviusHelperService helperService = Context.getBean("moviusHelperServiceBean");
            Map<String, Integer> result = helperService.resolveUserIdByOrgId(entityId, organization.getId());
            Integer targetUserId = result.get("userId");
			UserWS parentUserWS = sessionBean.getUserWS(targetUserId);
		    OrderWS orderWS = MoviusTaskUtils.getLatestMonthlyActiveOrder(parentUserWS.getId(), parentUserWS.getLanguageId(), orderPeriodId);
		    boolean isParentSubscriptionLinePresent = false;
		    if (null != orderWS) {
				String originationOrderLineQuantity = MoviusTaskUtils.getOriginationOrderLineQuantity(
						orderWS.getOrderLines(), parentUserWS, quantity);
		        BigDecimal lineQuantity = BigDecimal.ZERO;
		        BigDecimal linePrice = BigDecimal.ZERO;

		        for (OrderLineWS ol : orderWS.getOrderLines()) {
		            if(ol.getItemId().equals(MoviusTaskUtils.getItem(entityId).getId())) {
		                lineQuantity = lineQuantity.add(ol.getQuantityAsDecimal());
		                linePrice = linePrice.add(ol.getPriceAsDecimal());
		                isParentSubscriptionLinePresent = true;
		            }
		        }

		        if (isParentSubscriptionLinePresent) {
		            if (new BigDecimal(originationOrderLineQuantity).compareTo(new BigDecimal(quantity)) != 0) {
	                    updateOrder(entityId, linePrice.toString(), originationOrderLineQuantity, parentUserWS, orderWS);
	                } else {
	                    lineQuantity = lineQuantity.add(new BigDecimal(quantity));
	                    updateOrder(entityId, linePrice.toString(), lineQuantity.toString(), parentUserWS, orderWS);
	                }
		        } else {
		            if (new BigDecimal(quantity).compareTo(BigDecimal.ZERO) > 0) {
		                OrderChangeWS[] oldOrderChanges = sessionBean.getOrderChanges(orderWS.getId());
		                Integer itemId = MoviusTaskUtils.getItem(entityId).getId();
		                for (OrderChangeWS change : oldOrderChanges) {
		                    if(change.getItemId().equals(itemId)
		                            && null == change.getEndDate()) {
		                        linePrice = linePrice.add(change.getPriceAsDecimal());
		                    }
		                }
		                MoviusTaskUtils.updateOrderChangeEndDate(oldOrderChanges, itemId);
		                createNewSubscriptionLine(entityId, linePrice.toString(), quantity, orderWS,
	                            MoviusTaskUtils.getItem(entityId));
		            }
		        }
			}
		}
	}

	private void updateOrder(Integer entityId, String subscription,
			String originationOrderLineQuantity, UserWS userWS, OrderWS orderWS) throws SessionInternalError {
		OrderChangeWS[] oldOrderChanges = sessionBean.getOrderChanges(orderWS.getId());
		if (MoviusTaskUtils.isPriceOrQuantityUpdated(oldOrderChanges, userWS, subscription, originationOrderLineQuantity)) {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			ItemDTOEx itemDtoEx = MoviusTaskUtils.getItem(entityId);
			
			//if same file uploaded on same date, then it should update existing subscription order change for the customer
			OrderChangeWS existingOrderChange = null;
			for (OrderChangeWS change : oldOrderChanges) {
				if(change.getItemId().equals(itemDtoEx.getId())
						&& null == change.getEndDate()) {
					existingOrderChange = change;
				}
			}
			if(null != existingOrderChange &&
					formatter.format(new Date()).equals(formatter.format(existingOrderChange.getStartDate()))) {
				existingOrderChange.setQuantity(originationOrderLineQuantity);
				existingOrderChange.setPrice(subscription);
				orderWS.setProrateFlag(orderWS.getProrateFlag());
			    sessionBean.updateOrder(orderWS, new OrderChangeWS[]{existingOrderChange});
			    setCountPosition(entityId, originationOrderLineQuantity, orderWS,
					MoviusTaskUtils.getCurrentOrderLine(orderWS.getOrderLines(), userWS));
			} else {
				OrderLineWS tempOrderLine = MoviusTaskUtils.getCurrentOrderLine(orderWS.getOrderLines(), userWS);
				if(null != tempOrderLine) {
				    OrderChangeWS[] orderChangeWSArray = new OrderChangeWS[1];
					MoviusTaskUtils.updateOrderChangeEndDate(oldOrderChanges, itemDtoEx.getId());
				    orderChangeWSArray[0] = OrderChangeBL.buildFromLine(tempOrderLine, orderWS, MoviusTaskUtils.getOrderChangeApplyStatus(entityId));
				    orderChangeWSArray[0].setQuantity(originationOrderLineQuantity);
				    orderChangeWSArray[0].setPrice(subscription);
				    orderWS.setProrateFlag(orderWS.getProrateFlag());
				    sessionBean.updateOrder(orderWS, orderChangeWSArray);
				    setCountPosition(entityId, originationOrderLineQuantity, orderWS, tempOrderLine);
				} else {
					createNewSubscriptionLine(entityId, subscription, originationOrderLineQuantity, orderWS, itemDtoEx);
				}
			}
			
			//Update order line quantity after updating the order since its adding up the new quantity with previous one
			OrderLineWS existingOrderLine = MoviusTaskUtils.getCurrentOrderLine(orderWS.getOrderLines(), userWS);
			if (null != existingOrderLine) {
				existingOrderLine.setQuantity(originationOrderLineQuantity);
				existingOrderLine.setPrice(subscription);
				existingOrderLine.setAmountAsDecimal(new BigDecimal(originationOrderLineQuantity)
						.multiply(new BigDecimal(subscription)));
				sessionBean.updateOrderLine(existingOrderLine);
			}
		}
	}

    private void createNewSubscriptionLine(Integer entityId, String subscription, String originationOrderLineQuantity,
            OrderWS orderWS, ItemDTOEx itemDtoEx) {
        OrderChangeWS[] orderChangeWSArray = new OrderChangeWS[1];
        OrderLineWS newLine = MoviusTaskUtils.buildOrderLine(itemDtoEx.getId(), originationOrderLineQuantity,
                itemDtoEx.getDescription(), subscription);
        orderChangeWSArray[0] = OrderChangeBL.buildFromLine(newLine, orderWS,
                MoviusTaskUtils.getOrderChangeApplyStatus(entityId));
        orderChangeWSArray[0].setQuantity(originationOrderLineQuantity);
        orderChangeWSArray[0].setPrice(subscription);
        orderChangeWSArray[0].setOrderWS(orderWS);
        orderWS.setProrateFlag(orderWS.getProrateFlag());
        sessionBean.updateOrder(orderWS, orderChangeWSArray);
        setCountPosition(entityId, originationOrderLineQuantity, orderWS, newLine);
    }

	public void setCountPosition(Integer entityId,
			String originationOrderLineQuantity, OrderWS orderWS,
			OrderLineWS existingOrderLine) {
		OrgCountPositionDAS orgCountPositionDAS = new OrgCountPositionDAS();
		String orgId = MoviusTaskUtils.findOrgIdbyUserId(orderWS.getUserId());
		OrgCountPositionDTO positionDTO = orgCountPositionDAS.findByOrgIdOrderIdAndItemId(orgId, orderWS.getId(), existingOrderLine.getItemId(), entityId);
		if(Objects.nonNull(positionDTO)) {
		    updateOrgCountPositionRecord(positionDTO, new BigDecimal(originationOrderLineQuantity), positionDTO.getCount());
		}
	}

	private static Integer createOrder(UserWS user, Integer entityId, String price, String quantity, String orgId) throws SessionInternalError {
		ItemDTOEx itemDtoEx = MoviusTaskUtils.getItem(entityId);
		OrderWS orderWS = MoviusTaskUtils.getOrderWS(user, itemDtoEx.getId(), quantity, itemDtoEx.getDescription(),price);
		OrderChangeWS []orderChanges = OrderChangeBL.buildFromOrder(orderWS, MoviusTaskUtils.getOrderChangeApplyStatus(entityId));
		Integer orderId = sessionBean.createOrder(orderWS, orderChanges);
		 createOrgCountPositionRecord(dto -> {
             dto.setOrgId(orgId);
             dto.setBillableOrgId(orgId);
             dto.setCount(new BigDecimal(quantity));
             dto.setOldCount(BigDecimal.ZERO);
             dto.setItemId(MoviusTaskUtils.getSubscriptionItemIdByEntity(entityId));
             dto.setEntityId(entityId);
             dto.setOrderId(orderId);
         });
		return orderId;
	}

	private void createInitialErrorMessage(String message, String fileName){
		errors = null == errors ? new MoviusTaskUtils.Errors() : errors;
		errors.setFilename(fileName);
		errors.getErrorList().add(message);
		if (CollectionUtils.isEmpty(notificationList)) {
			notificationList.add(errors);
		}
		logger.debug(message);
    }

    private void createExceptionErrorMessage(String exceptionMessage, String customMessage, String fileName){
        String error = customMessage + " - " + exceptionMessage.substring(0, Math.min(exceptionMessage.length(), 300));
        errors = null == errors ? new MoviusTaskUtils.Errors() : errors;
        errors.setFilename(fileName);
        errors.getErrorList().add(error);
        if (CollectionUtils.isEmpty(notificationList)) {
        	notificationList.add(errors);
        }
        logger.error(error);
    }

    private static Integer createOrgCountPositionRecord(Consumer<OrgCountPositionDTO> dtoSetters) {
        OrgCountPositionDAS das = new OrgCountPositionDAS();
        OrgCountPositionDTO record = new OrgCountPositionDTO();
        dtoSetters.accept(record);
        Integer recordId =  das.save(record).getId();
        logger.debug("Created Org Count Record {} for order {} with Quantity {}", record, record.getOrderId(), record.getCount());
        return recordId;
    }

    private static Integer updateOrgCountPositionRecord(OrgCountPositionDTO record, BigDecimal count, BigDecimal oldCount) {
        OrgCountPositionDAS das = new OrgCountPositionDAS();
        record.setCount(count);
        record.setOldCount(oldCount);
        record.setLastUpdatedDate(TimezoneHelper.serverCurrentDate());
        Integer recordId =  das.save(record).getId();
        logger.debug("Updated Org Count Record {} for order {} with Quantity {}", record, record.getOrderId(), count);
        return recordId;
    }

    /**
     * Helper method which recursively get the total quantity of the orgs by adding the count from all the non-billable orgs recursively
     *
     * @param organization
     * @param oriQuantity
     * @return
     */
    private String getQuantity(Organization organization, BigDecimal oriQuantity) {
        oriQuantity = null != oriQuantity ? oriQuantity : new BigDecimal(organization.getCount());
        if (organization.hasSubOrgs()) {
            for (Organization org : organization.getOrganizations().getOrganizations()) {
                if (!org.isBillable()) {
                    String count = org.getCount();
                    if (StringUtils.isNotBlank(count) && new BigDecimal(count).compareTo(BigDecimal.ZERO) >= 0) {
                        oriQuantity = oriQuantity.add(new BigDecimal(count));
                    }
                    oriQuantity = new BigDecimal(getQuantity(org, oriQuantity));
                }
            }
        }
        return String.valueOf(oriQuantity);
    }
}
