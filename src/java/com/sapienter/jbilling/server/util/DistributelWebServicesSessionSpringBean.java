/*

 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

/*
 * Created on Jan 27, 2005
 * One session bean to expose as a single web service, thus, one wsdl
 */
package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.strategy.TeaserPricingStrategy;
import com.sapienter.jbilling.server.spa.AddressType;
import com.sapienter.jbilling.server.spa.CustomerEmergency911AddressWS;
import com.sapienter.jbilling.server.spa.DistributelException;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.spa.SpaErrorCodes;
import com.sapienter.jbilling.server.spa.SpaHappyFox;
import com.sapienter.jbilling.server.spa.SpaHappyFoxBL;
import com.sapienter.jbilling.server.spa.SpaValidator;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.AccountTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.search.Filter;

import grails.plugin.springsecurity.SpringSecurityService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.hibernate.NonUniqueResultException;
import org.joda.time.DateTimeComparator;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.client.authentication.CompanyUserDetails;
import com.sapienter.jbilling.server.item.PlanBL;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.SpaPlanWS;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.CanadianQuoteTaxCalculationTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.spa.SpaImportBL;
import com.sapienter.jbilling.server.spa.SpaImportWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingDistributelAPI;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.SearchResultString;

import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * This bean holds actual implementation of Distributel APIs
 * just like core WebServicesSessionBean.
 */

@Transactional(propagation = Propagation.REQUIRED)
public class DistributelWebServicesSessionSpringBean implements JbillingDistributelAPI {

	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(DistributelWebServicesSessionSpringBean.class));

	private SpringSecurityService springSecurityService;
	private IWebServicesSessionBean webServicesSessionBean;

	public SpringSecurityService getSpringSecurityService() {
        if (springSecurityService == null)
            this.springSecurityService = Context.getBean(Context.Name.SPRING_SECURITY_SERVICE);
        return springSecurityService;
    }

	public void setSpringSecurityService(SpringSecurityService springSecurityService) {
		this.springSecurityService = springSecurityService;
	}

	public void setWebServicesSessionBean(IWebServicesSessionBean webServicesSessionBean) {
        this.webServicesSessionBean = webServicesSessionBean;
    }

	/**
	 * Returns the company ID of the authenticated user account making the web
	 * service call.
	 *
	 * @return caller company ID
	 */
	@Transactional(readOnly = true)
	public Integer getCallerCompanyId() {
		CompanyUserDetails details = (CompanyUserDetails) getSpringSecurityService()
				.getPrincipal();
		return details.getCompanyId();
	}

	@Override
    public SpaPlanWS[] getSpaPlansWS(String province, String userType) {
		PlanWS[] plansWS = this.getPlans(province, userType);
		List<SpaPlanWS> spaPlansWSList = new ArrayList<>();
        SearchResultString supportedModems = getRoutePlanTable(null, PlanBL.PLAN_SUPPORTED_MODEMS_TABLE);
        SearchResultString optionalPlans = getRoutePlanTable(null, PlanBL.PLAN_INFORMATION_OPTIONAL_TABLE);

		for (PlanWS planWS : plansWS) {
		    Integer planId = planWS.getId();
            if (planId != null && planId != 0) {
                spaPlansWSList.add(new SpaPlanWS(planWS, filterSupportedModemsByPlan(supportedModems, planId), filterOptionalPlansByPlan(optionalPlans, planId)));
            }
		}
		return spaPlansWSList.toArray(new SpaPlanWS[spaPlansWSList.size()]);
	}

    @Override
    public PlanWS[] getPlans(String province, String userType) {
        return new SpaImportBL().getPlans(province, userType, getCallerCompanyId());
    }

    @Override
    public String[] getSupportedModemsByPlan(Integer planId) {
        if(planId == null || planId == 0) {
			return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        SearchResultString resultString = getSupportedModemsSearchResultByPlan(planId);
        ArrayList<String> modems = new ArrayList();
        if(!resultString.getStringRows().isEmpty()){
            for(List<String> row : resultString.getRows()){
                if(row.get(2) != null && !row.get(2).isEmpty()){
                    modems.add(row.get(2));
                }
            }
        }
        return modems.toArray(new String[modems.size()]);
    }

    private String[] filterSupportedModemsByPlan(SearchResultString supportedModems, Integer planId) {
        return supportedModems.getStringRows().stream()
                                              .filter(row -> StringUtils.isNotEmpty(row.get(1)) && planId.equals(Integer.valueOf(row.get(1))))
                                              .map(row -> row.get(2))
                                              .filter(StringUtils::isNotEmpty)
                                              .toArray(String[]::new);
    }

    private  PlanWS[] filterOptionalPlansByPlan(SearchResultString resultString, Integer planId) throws SessionInternalError {
        PlanDAS planDAS = new PlanDAS();
	    return resultString.getStringRows().stream()
                                           .filter(row -> StringUtils.isNotEmpty(row.get(1)) && planId.equals(Integer.valueOf(row.get(1))))
                                           .map(row -> planDAS.findNow(Integer.valueOf(row.get(2))))
                                           .filter(Objects::nonNull)
                                           .map(PlanBL::getWS)
                                           .toArray(PlanWS[]::new);
    }

    @Override
    public  PlanWS[] getOptionalPlansByPlan(Integer planId) throws SessionInternalError {
        if (planId == null || planId == 0) {
            return new PlanWS[0];
        }
        SearchResultString resultString = getOptionalPlansSearchResultByPlan(planId);
        ArrayList<PlanWS> plans = new ArrayList<>();
        if (!resultString.getStringRows().isEmpty()) {
            PlanDAS planDAS = new PlanDAS();
            for (List<String> row : resultString.getRows()) {
                PlanDTO plan = planDAS.findNow(Integer.parseInt(row.get(2)));
                if (plan != null) {
                    plans.add(PlanBL.getWS(plan));
                }
            }
        }

        // When the current price model is teaser pricing, we have to set the rate with the first period value.
        plans.stream()
            .flatMap(plan -> plan.getPlanItems().stream())
            .map(PlanItemWS::getModel)
            .forEach(model -> {
                if (PriceModelStrategy.TEASER_PRICING.toString().equals(model.getType())) {
                    model.setRate(model.getAttributes().entrySet().stream()
                        .filter(e -> TeaserPricingStrategy.FIRST_PERIOD.equalsIgnoreCase(e.getKey()))
                        .map(Map.Entry::getValue)
                        .map(BigDecimal::new)
                        .findFirst()
                        .orElse(BigDecimal.ZERO));
                }
            });

        return plans.toArray(new PlanWS[plans.size()]);
    }

    private SearchResultString getRoutePlanTable(Integer planId, String routeTableName){
        RouteDAS routeDAS = new RouteDAS();
        RouteDTO route = routeDAS.getRoute(getCallerCompanyId(),routeTableName);
        SearchCriteria criteria = new SearchCriteria();
        criteria.setMax(routeDAS.getCountRows(route.getTableName()));
        if(planId != null && planId != 0){
            criteria.setFilters(new BasicFilter[]{
                    new BasicFilter("plan_id",com.sapienter.jbilling.server.util.search.Filter.FilterConstraint.EQ,  planId.toString() )
            });
        }else{
            criteria.setFilters(new BasicFilter[]{});
        }
        return webServicesSessionBean.searchDataTable(route.getId(), criteria);
    }

    @Override
    public SearchResultString getSupportedModemsSearchResultByPlan(Integer planId){
        return getRoutePlanTable(planId, PlanBL.PLAN_SUPPORTED_MODEMS_TABLE);
    }

    @Override
    public SearchResultString getOptionalPlansSearchResultByPlan(Integer planId){
        return getRoutePlanTable(planId, PlanBL.PLAN_INFORMATION_OPTIONAL_TABLE);
    }

	@Override
	public Integer processSpaImport(SpaImportWS spaImportWS) {
		JbillingDistributelAPI localProxy = Context.getBean("distributelApiService");
		Integer result = localProxy.processSpaImportInternalProcess(spaImportWS);
		if (result > 0 && !spaImportWS.isUpdateCustomer()) {
			Integer[] invoiceIds = webServicesSessionBean.getAllInvoices(result);
            if (SpaValidator.hasToSendNotification(spaImportWS) && ArrayUtils.getLength(invoiceIds) > 0) {
				try {
					webServicesSessionBean.notifyInvoiceByEmail(invoiceIds[0]);
				} catch (Exception ex) {
					LOG.error("processSpaImport fails: " + SpaErrorCodes.EMAIL_INVOICE_NOTIFICATION_ERROR.getValue(), ex);
					return SpaErrorCodes.EMAIL_INVOICE_NOTIFICATION_ERROR.getValue();
				}
			}
		}
		return result;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Integer processSpaImportInternalProcess(SpaImportWS spaImportWS) {
		SpaImportBL spaImportBL = new SpaImportBL(spaImportWS, this.getCallerCompanyId());
		Integer customerId;
		try {
			spaImportBL.processInitial();
			if (!spaImportBL.validateMandatory()) {
				LOG.error("processSpaImport fails, return: " + SpaErrorCodes.GENERAL_ERROR.getValue());
				return SpaErrorCodes.GENERAL_ERROR.getValue();
			}

			if (!spaImportBL.validateVOIPPhoneNumber()) {
                LOG.error("processSpaImport fails, return: " + SpaErrorCodes.REPEATED_VOIP_PHONE_NUMBER_ERROR.getValue());
                return SpaErrorCodes.REPEATED_VOIP_PHONE_NUMBER_ERROR.getValue();
            }

            if (!spaImportBL.validateServiceAssetIdentifier()) {
                LOG.error("processSpaImport fails, return: " + SpaErrorCodes.REPEATED_SERVICE_IDENTIFIER_NUMBER_ERROR.getValue());
                return SpaErrorCodes.REPEATED_SERVICE_IDENTIFIER_NUMBER_ERROR.getValue();
            }

            if (!spaImportBL.validateModemAssetIdentifier()) {
                LOG.error("processSpaImport fails, return: " + SpaErrorCodes.REPEATED_MODEM_IDENTIFIER_NUMBER_ERROR.getValue());
                return SpaErrorCodes.REPEATED_MODEM_IDENTIFIER_NUMBER_ERROR.getValue();
            }

			if (spaImportWS.isUpdateCustomer()) {
                customerId = spaImportWS.getCustomerId();
                UserDTO user = new UserDAS().findNow(customerId);
                updateCustomer(user, spaImportBL, spaImportWS);

                generateOrders(customerId, spaImportBL);

                if (StringUtils.isEmpty(spaImportWS.getRequiredAdjustmentDetails()) && spaImportBL.getRecordPayment()) {
                    createPayment(spaImportBL, customerId);
                }
                updateService(spaImportBL, user);
			} else {
				try {
					customerId = webServicesSessionBean.createUser(spaImportBL.getCustomer());
                    UserWS customer = webServicesSessionBean.getUserWS(customerId);
                    webServicesSessionBean.updateUser(customer);// to update customer name by plugin UpdateDistributelCustomerTask
				} catch (Exception ex) {
                    throw new DistributelException("Error creating customer", ex, SpaErrorCodes.CREATING_CUSTOMER_ERROR);
				}
                generateOrders(customerId, spaImportBL);

                if (StringUtils.isEmpty(spaImportWS.getRequiredAdjustmentDetails()) && spaImportBL.getRecordPayment()) {
                    if (spaImportBL.isValidToGenerateInvoice()) {
                        try {
                            webServicesSessionBean.createInvoice(customerId, false);
                        } catch (Exception ex) {
                            throw new DistributelException("Error creating invoice for customer " + customerId, ex, SpaErrorCodes.GENERATING_INVOICE_ERROR);
                        }
                    }
                    createPayment(spaImportBL, customerId);
                }
			}

            try {
                spaImportBL.dispatchNewProcessSpaImportEvent(customerId);
            } catch (Exception ex) {
                throw new DistributelException("Error provisioning activities", ex, SpaErrorCodes.PROVISIONING_ACTIVITIES_ERROR);
            }
		} catch (DistributelException ex) {
            LOG.error("processSpaImport fails: " + ex.getErrorCode().getValue(), ex);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ex.getErrorCode().getValue();
        } catch (Exception ex) {
            LOG.error("processSpaImport fails: " + SpaErrorCodes.GENERAL_ERROR.getValue(), ex);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			return SpaErrorCodes.GENERAL_ERROR.getValue();
		}
		return customerId;
	}

	private void updateCustomer(UserDTO user, SpaImportBL spaImportBL, SpaImportWS spaImportWS) throws DistributelException {
        if (user == null) {
            throw new DistributelException("Error trying to get the user with id " + spaImportWS.getCustomerId(), SpaErrorCodes.LOOKING_FOR_USER_ERROR);
        }

        try {
            AccountTypeDTO accountType = new AccountTypeDAS().findAccountTypeByName(getCallerCompanyId(), SpaConstants.ACCOUNT_TYPE_RESIDENTIAL);
            spaImportBL.setAccountTypeId(accountType.getId());
            spaImportBL.setAccountTypeCurrency(accountType.getCurrencyId());
            spaImportBL.setRecordPayment(SpaValidator.hasToRecordPayment(spaImportWS));    
        } catch (Exception e) {
            throw new DistributelException("Error updating the customer with id " + user.getId(), e, SpaErrorCodes.LOOKING_FOR_USER_ERROR);
        }
    }

    private void generateOrders(Integer customerId, SpaImportBL spaImportBL) throws DistributelException {
        try {
            spaImportBL.generateOrders(customerId);
            spaImportBL.getOrders().forEach((order, orderChange) -> {
                webServicesSessionBean.rateOrder(order, orderChange);
                Integer orderId = webServicesSessionBean.createUpdateOrder(order, orderChange);
                if (spaImportBL.getMainOfferOrderForProcessCenter() == order) {
                    spaImportBL.getMainOfferOrderForProcessCenter().setId(orderId);
                }
            });
        } catch (Exception ex) {
            throw new DistributelException("Error trying to generate orders for customer " + customerId, ex, SpaErrorCodes.GENERATING_ORDERS_ERROR);
        }
    }

    private void createPayment(SpaImportBL spaImportBL, Integer customerId) throws DistributelException {
        try {
            PaymentWS payment = spaImportBL.getPayment(customerId);
            Integer paymentId = webServicesSessionBean.createPayment(payment);
            spaImportBL.createPaymentAuthorization(paymentId);
            spaImportBL.updatePaymentStatus(paymentId, CommonConstants.RESULT_OK);
        } catch (Exception ex) {
            throw new DistributelException("Error creating payment for customer " + customerId, ex, SpaErrorCodes.SAVING_PAYMENT_ERROR);
        }
    }

    private void updateService(SpaImportBL spaImportBL, UserDTO user) throws DistributelException {
        try {
            spaImportBL.addCustomerAddress(user, CommonConstants.EPOCH_DATE, SpaConstants.EMERGENCY_ADDRESS_AIT,  AddressType.EMERGENCY);
            spaImportBL.updateAddresses(user);
        } catch (Exception ex) {
            throw new DistributelException("Error updating service for customer " + user.getId(), ex, SpaErrorCodes.ADDING_NEW_SERVICE_ERROR);
        }
    }

	@Override
	public OrderWS[] getOrdersWithActiveUntil(Integer userId, Date activeUntil){
		OrderBL orderBL = new OrderBL();
		UserWS user = webServicesSessionBean.getUserWS(userId);
		List<OrderDTO> orderDTOList = orderBL.getOrdersWithActiveUntil(userId, activeUntil);
		OrderWS[] activeUntilOrderList = new OrderWS[null != orderDTOList ? orderDTOList.size() : 0];
		int i = 0;
		if(null != orderDTOList){
			for(OrderDTO orderDTO : orderDTOList) {
				OrderBL bl = new OrderBL(orderDTO);
				activeUntilOrderList[i++] = bl.getWS((null != user && null != user.getLanguageId()) ? user.getLanguageId() : 1);
			}
		}
		return activeUntilOrderList;
	}

	@Override
	public OrderWS[] getOrdersWithOrderChangeEffectiveDate(Integer userId, Date effectiveDate) {
		OrderChangeBL orderChangeBL = new OrderChangeBL();
		UserWS user = webServicesSessionBean.getUserWS(userId);
		List<OrderChangeDTO> orderChangeDTOList = orderChangeBL.getOrderChangesWithEffectiveDate(userId, effectiveDate);
		List<OrderWS> orderWSList = new LinkedList<>();
		if (orderChangeDTOList != null) {
			orderChangeDTOList.stream().forEach( orderChangeDTO -> {
				if (orderChangeDTOList.isEmpty() ||
						orderWSList.stream().noneMatch( orderWS -> orderWS.getId().equals(orderChangeDTO.getOrder().getId()))) {
					OrderBL bl = new OrderBL(orderChangeDTO.getOrder());
					orderWSList.add(bl.getWS((null != user && null != user.getLanguageId()) ? user.getLanguageId() : 1));
				}
			});
		}
		return orderWSList.toArray(new OrderWS[orderWSList.size()]);
	}

	@Override
	public PlanWS getQuote(Integer planId, String province, String date, String languageId, Integer... optionalPlanIds) {
		PlanDAS planDAS = new PlanDAS();
		PlanDTO plan=null;
		if (planId != null ){
			plan = planDAS.findPlanById(planId);
			if(plan!=null){
				if (optionalPlanIds != null) {
					for (int i = 0; i < optionalPlanIds.length; i++) {
						PlanDTO optionalPlan = planDAS.findPlanById(optionalPlanIds[i]);
						plan.getPlanItems().addAll(optionalPlan.getPlanItems());
					}
				}
				if(province!=null) {
					plan.getPlanItems();
					PluggableTaskManager taskManager = null;
					try {
						taskManager = new PluggableTaskManager(getCallerCompanyId(), Constants.PLUGGABLE_TASK_QUOTE_TAX_CALCULATION);
					} catch (PluggableTaskException e) {
						LOG.error(e);
					}

					CanadianQuoteTaxCalculationTask task = null;
					try {
						if (taskManager != null) {
							task = (CanadianQuoteTaxCalculationTask) taskManager.getNextClass();
							task.calculateTax(plan, province, date, languageId);
						}
					} catch (PluggableTaskException e) {
						LOG.error(e);
					}
					// if one was not configured just use the basic task by default
					if (task == null) {
						LOG.info("No task is configured");
						task = new CanadianQuoteTaxCalculationTask();
						task.calculateTax(plan, province, date, languageId);
					}
				}
			}
		}
        PlanWS planWS = PlanBL.getWS(plan);
        planDAS.clear();
		return  planWS;
	}

	@Override
	public boolean setFurtherOrderAndAssetInformation(String banffAccountId, String trackingNumber, String courier, String serialNumber, String macAddress, String model, String serviceAssetIdentifier) {
		SpaImportBL spaImportBL = new SpaImportBL(new SpaImportWS(), this.getCallerCompanyId());
		spaImportBL.processInitial();
		return spaImportBL.setFurtherOrderAndAssetInformation(banffAccountId, trackingNumber, courier, serialNumber, macAddress, model, serviceAssetIdentifier);
	}

	@Override
	public Map<String, BigDecimal> getCanadianTaxDataTableValuesForUser(Integer userId, Date effectiveDate) {
		String province = null;

		UserDTO user = new UserBL(userId).getEntity();
		AccountInformationTypeDTO serviceAddressGroupAIT = new AccountInformationTypeDAS().findByName(SpaConstants.SERVICE_ADDRESS_AIT, user.getEntity().getId(), user.getCustomer().getAccountType().getId());
		Integer groupAITId = serviceAddressGroupAIT.getId();
		if ((Boolean)user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.SAME_AS_CUSTOMER_INFORMATION, serviceAddressGroupAIT.getId()).getMetaFieldValue().getValue()) {
			groupAITId = new AccountInformationTypeDAS().findByName(SpaConstants.CONTACT_INFORMATION_AIT, user.getEntity().getId(), user.getCustomer().getAccountType().getId()).getId();
		}

		province = (String)user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.PROVINCE, groupAITId).getMetaFieldValue().getValue();

		Map<String, BigDecimal> taxPercentages = new HashMap<String, BigDecimal>();
		RouteDAS routeDAS = new RouteDAS();
		RouteDTO route = routeDAS.getRoute(getCallerCompanyId(), SpaConstants.CANADIAN_TAXES);
		SearchCriteria criteria = new SearchCriteria();
		criteria.setMax(1);
		if (province != null) {
			criteria.setSort("date");
			criteria.setDirection(SearchCriteria.SortDirection.DESC);
			criteria.setFilters(new BasicFilter[]{
					new BasicFilter("province", com.sapienter.jbilling.server.util.search.Filter.FilterConstraint.EQ, province),
					new BasicFilter("date", Filter.FilterConstraint.LE, effectiveDate),

			});

			SearchResultString queryResult = routeDAS.searchDataTable(criteria, route);
			List<List<String>> rows = queryResult.getRows();

			if (rows.size() > 0) {
				List<String> row = rows.get(0);
				BigDecimal GST = new BigDecimal(row.get(SpaConstants.GST));
				BigDecimal PST = new BigDecimal(row.get(SpaConstants.PST));
				BigDecimal HST = new BigDecimal(row.get(SpaConstants.HST));
				taxPercentages.put("GST", GST);
				taxPercentages.put("PST", PST);
				taxPercentages.put("HST", HST);
			}
		}

		return taxPercentages;
	}

	@Override
	public List<AssetWS> getAssetsByMetaFieldValue(String mfName, String mfValue) {
        return new AssetDAS().findAssetByMetaFieldValue(getCallerCompanyId(), mfName, mfValue).stream()
                                                                                            .map(AssetBL::getWS)
                                                                                            .collect(Collectors.toList());
	}

	@Override
	public List<Integer> findOrderIdsByAssetIds(List<Integer> assetIds) {
        OrderChangeDAS orderChangeDAS = new OrderChangeDAS();
		List<Integer> orderIdList = new LinkedList<>();
		assetIds.forEach(assetId -> {
			OrderChangeDTO orderChange = orderChangeDAS.findByOrderChangeByAssetIdInPlanItems(assetId);
			if (orderChange != null) {
				orderIdList.add(orderChange.getOrder().getId());
			} else {
				orderChangeDAS.findOrderChangeIdsByAssetId(assetId).forEach(orderChangeId -> {
					orderIdList.add(orderChangeDAS.find(orderChangeId).getOrder().getId());
				});
			}
		});
		return orderIdList;
	}

	@Override
	public CustomerEmergency911AddressWS getCustomerEmergency911AddressByPhoneNumber(String phoneNumber) {

	    if (StringUtils.isEmpty(phoneNumber)) {
	        throw new SessionInternalError("Phone Number should not be null or empty : " + phoneNumber,
	                new String[] { "AssetWS,phoneNumber,validation.error.not.null" });
	    }
	    if (!phoneNumber.matches("\\d{10}")) {
	        throw new SessionInternalError("Phone Number should be a 10 digit number without dash or brackets : " + phoneNumber,
	                new String[] { "AssetWS,phoneNumber,validation.error.not.a.number.10.integer" });
	    }
	    List<AssetWS> assets = getAssetsByMetaFieldValue(SpaConstants.MF_PHONE_NUMBER, phoneNumber);

	    if (null == assets || assets.isEmpty()) {
	        return CustomerEmergency911AddressWS.PHONE_NUBER_NOT_FOUND_RESPONSE;
	    }
	    UserDTO user = new UserBL().getUserByAssetId(assets.get(0).getId());

	    if(null == user) {
	        return CustomerEmergency911AddressWS.USER_NOT_FOUND_RESPONSE;
	    }
	    Integer accountTypeId = user.getCustomer().getAccountType().getId();
	    Integer entityId = user.getEntity().getId();
	    AccountInformationTypeDAS accountInformationTypeDAS = new AccountInformationTypeDAS();
	    AccountInformationTypeDTO emergencyAddressAIT = accountInformationTypeDAS.findByName(SpaConstants.EMERGENCY_ADDRESS_AIT, entityId, accountTypeId);
	    AccountInformationTypeDTO contactInformationAIT = accountInformationTypeDAS.findByName(SpaConstants.CONTACT_INFORMATION_AIT, entityId, accountTypeId);
	    Integer addressGroupId = emergencyAddressAIT.getId();
	    Integer contactGroupId = contactInformationAIT.getId();

	    CustomerDTO customerDTO = user.getCustomer();
	    Date effectiveDate = customerDTO.getCurrentEffectiveDateByGroupId(addressGroupId);
	    CustomerAccountInfoTypeMetaField accountInfoTypeMetaField = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.MF_PROVIDED, addressGroupId,effectiveDate);
	    CustomerEmergency911AddressWS addressWS = SpaImportBL.getCustomerEmergency911AddressWS(user, contactGroupId, SpaConstants.CONTACT_INFORMATION_AIT);
	    if(null != accountInfoTypeMetaField && (boolean) accountInfoTypeMetaField.getMetaFieldValue().getValue()) {
	        addressWS = SpaImportBL.getCustomerEmergency911AddressWS(user, addressGroupId, SpaConstants.EMERGENCY_ADDRESS_AIT);
	    }

	    effectiveDate = customerDTO.getCurrentEffectiveDateByGroupId(contactGroupId);
	    accountInfoTypeMetaField = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.CUSTOMER_NAME, contactGroupId,effectiveDate);
	    if (null != accountInfoTypeMetaField) {
	        addressWS.setCustomerName((String)accountInfoTypeMetaField.getMetaFieldValue().getValue());
	    }

	    accountInfoTypeMetaField = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.PHONE_NUMBER_1, contactGroupId,effectiveDate);
	    if (null != accountInfoTypeMetaField) {
	        addressWS.setPhoneNumber((String)accountInfoTypeMetaField.getMetaFieldValue().getValue());
	    }
	    return addressWS;
	}

    @Override
    public SpaHappyFox getCustomerTicketInfoResult(SpaHappyFox spaHappyFox){
        SpaHappyFoxBL bl = new SpaHappyFoxBL(spaHappyFox);
        if (bl.validate()){
            return spaHappyFox;
        }
        try {
            Integer userId = bl.findUser();
            if(null == userId){
                return spaHappyFox;
            }
            UserWS user = webServicesSessionBean.getUserWS(userId);
            if(1 == user.getDeleted()){
                return spaHappyFox;
            }
            bl.getCustomerFields(user);
            bl.getOtherFields(user);
            bl.setAllFields();
        } catch(NonUniqueResultException e) {
            return spaHappyFox;
        }
        return spaHappyFox;
    }
}
