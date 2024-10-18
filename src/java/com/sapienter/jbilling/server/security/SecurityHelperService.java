package com.sapienter.jbilling.server.security;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Collection;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.RatingUnitWS;
import com.sapienter.jbilling.server.process.AgeingWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.BillingProcessWS;
import com.sapienter.jbilling.server.security.methods.SecuredMethodType;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.UserTransitionResponseWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.partner.CommissionProcessConfigurationWS;
import com.sapienter.jbilling.server.user.partner.PartnerWS;
import com.sapienter.jbilling.server.util.SecurityValidator;

@Transactional
public class SecurityHelperService {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Resource
	private SecurityValidator securityValidator;


	public void validateAccess(Method method, Object[] args, Validator.Type type) {
		// try validating the method call itself
		WSSecured securedMethod = getMappedSecuredWS(method, args);
		if (securedMethod != null)
			validate(securedMethod, type);

		// validate each method call argument
		for (Object o : args) {
			if (o != null) {
				if (o instanceof Collection) {
					for (Object element : (Collection<?>) o)
						validate(element, type);

				} else if (o.getClass().isArray()) {
					for (Object element : (Object[]) o)
						validate(element, type);

				} else {
					validate(o, type);
				}
			}
		}
	}

	/**
	 * Attempt to map the method call as an instance of WSSecured so that it can be validated.
	 *
	 * @see com.sapienter.jbilling.server.security.WSSecurityMethodMapper
	 *
	 * @param method method to map
	 * @param args method arguments
	 * @return mapped method call, or null if method call is unknown
	 */
	private WSSecured getMappedSecuredWS(final Method method, final Object[] args) {
		return WSSecurityMethodMapper.getMappedSecuredWS(method, args);
	}

	/**
	 * Attempt to map the given object as an instance of WSSecured so that it can be validated.
	 *
	 * @see com.sapienter.jbilling.server.security.WSSecurityEntityMapper
	 *
	 * @param o object to map
	 * @return mapped object, or null if object is of an unknown type
	 */
	private WSSecured getMappedSecuredWS(final Object o) {
		logger.debug("Non WSSecured object={}, attempting to map a secure class for validation.", o.getClass().getSimpleName());
		return WSSecurityEntityMapper.getMappedSecuredWS(o);
	}

	/**
	 * Attempt to validate the given object.
	 *
	 * @param o object to validate
	 * @throws SecurityException thrown if user is accessing data that does not belonging to them
	 */
	private void validate(Object o, final Validator.Type validatorType) {
		if (o != null) {
			if (o instanceof WSSecured) {
				validateEntityChange((WSSecured) o, validatorType);
			}

			final WSSecured secured = (o instanceof WSSecured) ? (WSSecured) o : getMappedSecuredWS(o);

			if (secured != null) {
				logger.debug("Validating secure object{}", secured.getClass().getSimpleName());
				if (secured.getOwningUserId() != null)
					securityValidator.validateUserAndCompany(secured, validatorType);
				else if (secured.getOwningEntityId() != null) {
					securityValidator.validateCompany(secured, null, validatorType);
				}
			}
		}
	}

	/**
	 * This method validate is entity (user) was changed in input object (for update usually)
	 * Changing the entity (company) is not allowed for invoices, items, orders, etc.
	 * So, we should compare persisted object, is it owned by entity for caller user, or not
	 * Not persisted objects (without id) is not checked
	 * @param inputObject WSSecured input object for check entity in persisted one
	 */
	private void validateEntityChange(final WSSecured inputObject, final Validator.Type validatorType) {
		Integer persistedId = null;
		SecuredMethodType type = null;
		if (inputObject instanceof AgeingWS) {
			// do nothing, entity can't be changed
		} else if (inputObject instanceof AssetWS && ((AssetWS) inputObject).getId() != null) {
			persistedId = ((AssetWS) inputObject).getId();
			type = SecuredMethodType.ASSET;
		} else if (inputObject instanceof BillingProcessWS) {
			// do nothing, entity can't be changed
		} else if (inputObject instanceof InvoiceWS && ((InvoiceWS) inputObject).getId() != null) {
			type = SecuredMethodType.INVOICE;
			persistedId = ((InvoiceWS) inputObject).getId();
		} else if (inputObject instanceof ItemDTOEx && ((ItemDTOEx) inputObject).getId() != null) {
			type = SecuredMethodType.ITEM;
			persistedId = ((ItemDTOEx) inputObject).getId();
		} else if (inputObject instanceof OrderWS && ((OrderWS) inputObject).getId() != null) {
			type = SecuredMethodType.ORDER;
			persistedId = ((OrderWS) inputObject).getId();
		} else if (inputObject instanceof OrderPeriodWS && ((OrderPeriodWS) inputObject).getId() != null) {
			type = SecuredMethodType.ORDER_PERIOD;
			persistedId = ((OrderPeriodWS) inputObject).getId();
		} else if (inputObject instanceof OrderStatusWS && ((OrderStatusWS) inputObject).getId() != null) {
			type = SecuredMethodType.ORDER_STATUS;
			persistedId = ((OrderStatusWS) inputObject).getId();
		} else if (inputObject instanceof PartnerWS && ((PartnerWS) inputObject).getId() != null) {
			type = SecuredMethodType.PARTNER;
			persistedId = ((PartnerWS) inputObject).getId();
		} else if (inputObject instanceof PaymentWS && ((PaymentWS) inputObject).getId() > 0) {
			type = SecuredMethodType.PAYMENT;
			persistedId = ((PaymentWS) inputObject).getId();
		} else if (inputObject instanceof RatingUnitWS && ((RatingUnitWS) inputObject).getId() != null && ((RatingUnitWS) inputObject).getId() > 0) {
			type = SecuredMethodType.RATING_UNIT;
			persistedId = ((RatingUnitWS) inputObject).getId();
		} else if (inputObject instanceof UsagePoolWS && ((UsagePoolWS) inputObject).getId() > 0) {
			type = SecuredMethodType.USAGE_POOL;
			persistedId = ((UsagePoolWS) inputObject).getId();
		} else if (inputObject instanceof PluggableTaskWS) {
			// do nothing, entity can't be changed
		} else if (inputObject instanceof UserTransitionResponseWS) {
			// do nothing, entity can't be changed
		} else if (inputObject instanceof UserWS && ((UserWS) inputObject).getId() > 0) {
			persistedId = ((UserWS) inputObject).getId();
			type = SecuredMethodType.USER;
		} else if (inputObject instanceof CompanyWS && ((CompanyWS) inputObject).getId() > 0) {
			persistedId = ((CompanyWS) inputObject).getId();
			type = SecuredMethodType.COMPANY;
		} else if (inputObject instanceof CommissionProcessConfigurationWS && ((CommissionProcessConfigurationWS) inputObject).getId() > 0) {
			persistedId = ((CommissionProcessConfigurationWS) inputObject).getId();
			type = SecuredMethodType.COMISSION_PROCESS_CONFIGURATION;
		} else if (inputObject instanceof BillingProcessConfigurationWS && ((BillingProcessConfigurationWS) inputObject).getId() > 0) {
			persistedId = ((BillingProcessConfigurationWS) inputObject).getId();
			type = SecuredMethodType.BILLING_PROCESS_CONFIGURATION;
		}

		if (type != null && persistedId != null) {
			// validate user and entity in persisted object - they should be the same as for caller
			final SecuredMethodType finalType = type;
			final Integer finalId = persistedId;
			WSSecured persistedSecuredObject = finalType.getMappedSecuredWS(finalId);
			if (persistedSecuredObject != null) {
				if (persistedSecuredObject.getOwningUserId() != null) {
					securityValidator.validateUserAndCompany(persistedSecuredObject, validatorType);
				} else if (persistedSecuredObject.getOwningEntityId() != null) {
					securityValidator.validateCompany(persistedSecuredObject, null, validatorType);
				}
			}
		}
	}

}