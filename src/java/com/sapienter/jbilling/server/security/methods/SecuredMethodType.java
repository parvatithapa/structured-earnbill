package com.sapienter.jbilling.server.security.methods;

import com.sapienter.jbilling.server.discount.db.DiscountDAS;
import com.sapienter.jbilling.server.discount.db.DiscountDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.item.db.*;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.mediation.MediationProcessService;
import com.sapienter.jbilling.server.mediation.db.MediationConfiguration;
import com.sapienter.jbilling.server.mediation.db.MediationConfigurationDAS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroupDAS;
import com.sapienter.jbilling.server.order.db.*;
import com.sapienter.jbilling.server.payment.db.*;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pricing.db.*;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningCommandDAS;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningCommandDTO;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningRequestDAS;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningRequestDTO;
import com.sapienter.jbilling.server.security.MappedSecuredWS;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.MatchingFieldWS;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessConfigurationDAS;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessConfigurationDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerDAS;

import java.io.Serializable;
import java.util.UUID;

import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.EnumerationDAS;
import com.sapienter.jbilling.server.util.db.EnumerationDTO;
import org.apache.commons.collections.CollectionUtils;

/**
 * Created by IntelliJ IDEA.
 * User: bcowdery
 * Date: 14/05/12
 * Time: 9:28 PM
 * To change this template use File | Settings | File Templates.
 */
public enum SecuredMethodType {

        USER {
            public WSSecured getMappedSecuredWS(Serializable id) {
                return id != null ? new MappedSecuredWS(null, (Integer) id) : null;
            }
        },

        CUSTOMER {
            public WSSecured getMappedSecuredWS(Serializable id) {
                CustomerDTO customer = new CustomerDAS().findNow(id);
                return customer != null ? new MappedSecuredWS(null, customer.getBaseUser().getId()) : null;
            }
        },

        PARTNER {
            public WSSecured getMappedSecuredWS(Serializable id) {
                PartnerDTO partner = new PartnerDAS().findNow(id);
                return partner != null ? new MappedSecuredWS(null, partner.getUser().getId()) : null;
            }
        },

        ITEM {
            public WSSecured getMappedSecuredWS(Serializable id) {
                ItemDTO item = new ItemDAS().findNow(id);
                if ((item.getEntities() != null && item.getEntities().size() == 1)) {
                	return new MappedSecuredWS(item.getEntities().iterator().next().getId(), null);
                }
                return null; //An item may not be owned by the caller company anymore. It is now a shared/shareable entity
            }
        },

        ITEM_CATEGORY {
            public WSSecured getMappedSecuredWS(Serializable id) {
                ItemTypeDTO itemType = new ItemTypeDAS().findNow(id);
                if ( itemType !=null && itemType.getEntities().size() == 1 ) {
                	return new MappedSecuredWS(itemType.getEntities().iterator().next().getId(), null);
                }
                return null;
            }
        },

        ORDER {
            public WSSecured getMappedSecuredWS(Serializable id) {
                OrderDTO order = new OrderDAS().findNow(id);
                return order != null ? new MappedSecuredWS(null, order.getUserId()) : null;
            }
        },

        ORDER_LINE {
            public WSSecured getMappedSecuredWS(Serializable id) {
                OrderLineDTO line = new OrderLineDAS().findNow(id);
                return line != null ? new MappedSecuredWS(null, line.getPurchaseOrder().getUserId()) : null;
            }
        },

        ORDER_PERIOD {
            public WSSecured getMappedSecuredWS(Serializable id) {
                OrderPeriodDTO period = new OrderPeriodDAS().findNow(id);
                return period != null ? new MappedSecuredWS(period.getCompany().getId(), null) : null;
            }
        },

        ORDER_STATUS {
            public WSSecured getMappedSecuredWS(Serializable id) {
                OrderStatusDTO status = new OrderStatusDAS().findNow(id);
                return status != null ? new MappedSecuredWS(status.getEntity().getId(), null) : null;
            }
        },

        INVOICE {
            public WSSecured getMappedSecuredWS(Serializable id) {
                InvoiceDTO invoice = new InvoiceDAS().findNow(id);
                return invoice != null ? new MappedSecuredWS(null, invoice.getUserId()) : null;
            }
        },

        PAYMENT {
            public WSSecured getMappedSecuredWS(Serializable id) {
                PaymentDTO payment = new PaymentDAS().findNow(id);
                return payment != null ? new MappedSecuredWS(null, payment.getBaseUser().getId()) : null;
            }
        },

        PAYMENT_METHOD_TYPE {
            public WSSecured getMappedSecuredWS(Serializable id) {
                PaymentMethodTypeDTO paymentMethodType = new PaymentMethodTypeDAS().findNow(id);
                return paymentMethodType != null ? new MappedSecuredWS(paymentMethodType.getEntity().getId(), null) : null;
            }
        },

        PAYMENT_INFORMATION {
            public WSSecured getMappedSecuredWS(Serializable id) {
                PaymentInformationDTO paymentInformation = new PaymentInformationDAS().findNow(id);
                return paymentInformation != null ? new MappedSecuredWS(null, paymentInformation.getUser().getId()) : null;
            }
        },

        BILLING_PROCESS_CONFIGURATION {
            public WSSecured getMappedSecuredWS(Serializable id) {
                Serializable entityId = (id instanceof BillingProcessConfigurationWS) ? ((BillingProcessConfigurationWS) id).getEntityId() : id;
                BillingProcessConfigurationDTO process = new BillingProcessConfigurationDAS().findNow(entityId);
                return process != null ? new MappedSecuredWS(process.getEntity().getId(), null) : null;
            }
        },

		MEDIATION_PROCESS {
		    public WSSecured getMappedSecuredWS(Serializable id) {
                MediationProcessService service = Context.getBean("mediationProcessService");
		        MediationProcess process = service.getMediationProcess((UUID) id);
		        return process != null ? new MappedSecuredWS(process.getEntityId(), null) : null;
		    }
		},
		
		MEDIATION_CONFIGURATION {
		    public WSSecured getMappedSecuredWS(Serializable id) {
		        MediationConfiguration config = new MediationConfigurationDAS().findNow(id);
		        return config != null ? new MappedSecuredWS(config.getEntityId(), null) : null;
		    }
		},

        PLUG_IN {
            public WSSecured getMappedSecuredWS(Serializable id) {
                PluggableTaskDTO task = new PluggableTaskBL((Integer)id).getDTO();
                return task != null ? new MappedSecuredWS(task.getEntityId(), null) : null;
            }
        },

        PLAN {
            public WSSecured getMappedSecuredWS(Serializable id) {
                PlanDTO plan = new PlanDAS().findNow(id);
                if(plan!=null && plan.getItem().getEntities().size() == 1 ) {
                    return plan != null ? new MappedSecuredWS(plan.getItem().getEntities().iterator().next().getId(), null) : null;
                }
                return null;
            }
        },

        ASSET {
            public WSSecured getMappedSecuredWS(Serializable id) {
                AssetDTO asset = new AssetDAS().findNow(id);
                if(asset!=null && asset.getItem().getEntities().size() == 1 ) {
					return (asset.getId()!=0)  ? new MappedSecuredWS(asset.getEntity().getId(), null) : null;
                }
                return null;
            }
        },
        
        DISCOUNT {
            public WSSecured getMappedSecuredWS(Serializable id) {
                DiscountDTO discount = new DiscountDAS().findNow(id);
                return discount != null ? new MappedSecuredWS(discount.getEntity().getId(), null) : null;
            }                
        },

        ORDER_PROCESS {
            public WSSecured getMappedSecuredWS(Serializable id) {
                OrderProcessDTO dto = new OrderProcessDAS().findNow(id);
                return dto != null ? new MappedSecuredWS(dto.getBillingProcess().getEntity().getId(), dto.getPurchaseOrder().getUserId()) : null;
            }
        },

        ACCOUNT_TYPE {
            public WSSecured getMappedSecuredWS(Serializable id) {
                AccountTypeDTO dto = new AccountTypeDAS().findNow(id);
                return (null!=dto && 0!=dto.getId()) ? new MappedSecuredWS(dto.getCompany().getId(), null) : null;
            }
        },

        META_FIELD {
            public WSSecured getMappedSecuredWS(Serializable id) {
                MetaField dto = new MetaFieldDAS().find(id);
                return dto != null ? new MappedSecuredWS(dto.getEntityId(), null) : null;
            }
        },

        META_FIELD_GROUP {
            public WSSecured getMappedSecuredWS(Serializable id) {
                MetaFieldGroup dto = new MetaFieldGroupDAS().findNow(id);
                return dto != null ? new MappedSecuredWS(dto.getEntityId(), null) : null;
            }
        },

        ROUTE {
            public WSSecured getMappedSecuredWS(Serializable id) {
                RouteDTO dto = new RouteDAS().findNow(id);
                return dto != null ? new MappedSecuredWS(dto.getCompany().getId(), null) : null;
            }
        },

        ROUTE_RATE_CARD {
            public WSSecured getMappedSecuredWS(Serializable id) {
                RouteRateCardDTO dto = new RouteRateCardDAS().findNow(id);
                return dto != null ? new MappedSecuredWS(dto.getCompany().getId(), null) : null;
            }
        },

        ORDER_CHANGE_STATUS {
            public WSSecured getMappedSecuredWS(Serializable id) {
                OrderChangeStatusDTO dto = new OrderChangeStatusDAS().findNow(id);
                return dto != null ? new MappedSecuredWS(dto.getCompany().getId(), null) : null;
            }
        },

        ORDER_CHANGE {
            public WSSecured getMappedSecuredWS(Serializable id) {
                OrderChangeDTO dto = new OrderChangeDAS().findNow(id);
                return dto != null ? new MappedSecuredWS(null, dto.getUser().getId()) : null;
            }
        },

        ORDER_CHANGE_TYPE {
            public WSSecured getMappedSecuredWS(Serializable id) {
                OrderChangeTypeDTO dto = new OrderChangeTypeDAS().findNow(id);
                return dto != null ? new MappedSecuredWS(dto.getEntity().getId(), null) : null;
            }
        },

        ENUMERATION {
            public WSSecured getMappedSecuredWS(Serializable id) {
                EnumerationDTO dto = new EnumerationDAS().findNow(id);
                return (null!=dto && 0!=dto.getId()) ? new MappedSecuredWS(dto.getEntityId(), null) : null;
            }
        },

        USAGE_POOL {
            public WSSecured getMappedSecuredWS(Serializable id) {
                UsagePoolDTO dto = new UsagePoolDAS().findNow(id);
                return dto != null ? new MappedSecuredWS(dto.getEntity().getId(), null) : null;
            }
        },

        CUSTOMER_USAGE_POOL {
            public WSSecured getMappedSecuredWS(Serializable id) {
                CustomerUsagePoolDTO dto = new CustomerUsagePoolDAS().findNow(id);
                return dto != null ? new MappedSecuredWS(null, dto.getCustomer().getBaseUser().getId()) : null;
            }
        },

        RATE_CARD {
            public WSSecured getMappedSecuredWS(Serializable id) {
                RateCardDTO dto = new RateCardDAS().findNow(id);
                return (null!=dto && 0!=dto.getId()) ? new MappedSecuredWS(dto.getCompany().getId(), null) : null;
            }
        },

        RATING_UNIT {
            public WSSecured getMappedSecuredWS(Serializable id) {
                RatingUnitDTO dto = new RatingUnitDAS().findNow(id);
                return (null!=dto && 0!=dto.getId()) ? new MappedSecuredWS(dto.getCompany().getId(), null) : null;
            }
        },

        COMPANY {
            public WSSecured getMappedSecuredWS(Serializable id) {
                CompanyDTO dto = new CompanyDAS().findNow(id);
                return (null!=dto && 0!=dto.getId()) ? new MappedSecuredWS(dto.getId(), null) : null;
            }
        },

        AGEING {
            public WSSecured getMappedSecuredWS(Serializable id) {
                CompanyDTO dto = new CompanyDAS().findNow(((CompanyWS)id).getId());
                return (null!=dto && 0!=dto.getId()) ? new MappedSecuredWS(dto.getId(), null) : null;
            }
        },

        BILLING_PROCESS {
            public WSSecured getMappedSecuredWS(Serializable id) {
                BillingProcessDTO process = new BillingProcessDAS().findNow(id);
                return process != null ? new MappedSecuredWS(process.getEntity().getId(), null) : null;
            }
        },

        COMISSION_PROCESS_CONFIGURATION {
            public WSSecured getMappedSecuredWS(Serializable id) {
                CommissionProcessConfigurationDTO process = new CommissionProcessConfigurationDAS().findNow(id);
                return process != null ? new MappedSecuredWS(process.getEntity().getId(), null) : null;
            }
        },

        MATCHING_FIELD {
            public WSSecured getMappedSecuredWS(Serializable id) {
                Serializable matchingFieldId = (id instanceof MatchingFieldWS) ? ((MatchingFieldWS) id).getId() : id;
                MatchingFieldDTO dto = new MatchingFieldDAS().findNow(matchingFieldId);
                return (null!=dto && 0!=dto.getId()) ? new MappedSecuredWS(dto.getRoute() != null ? dto.getRoute().getCompany().getId() : dto.getRouteRateCard().getCompany().getId(), null) : null;
            }
        },

        DATA_TABLE_QUERY {
            public WSSecured getMappedSecuredWS(Serializable id) {
                DataTableQueryDTO dto = new DataTableQueryDAS().findNow(id);
                return (null!=dto && 0!=dto.getId()) ? new MappedSecuredWS(null, dto.getUser().getId()) : null;
            }
        },

        PROVISIONING_COMMAND {
            public WSSecured getMappedSecuredWS(Serializable id) {
                ProvisioningCommandDTO dto = new ProvisioningCommandDAS().findNow(id);
                return (null!=dto && 0!=dto.getId()) ? new MappedSecuredWS(dto.getEntity().getId(), null) : null;
            }
        },

        PROVISIONING_REQUEST {
            public WSSecured getMappedSecuredWS(Serializable id) {
                ProvisioningRequestDTO dto = new ProvisioningRequestDAS().findNow(id);
                return (null!=dto && 0!=dto.getId()) ? new MappedSecuredWS(dto.getProvisioningCommand().getEntity().getId(), null) : null;
            }
        };

        /**
         * implemented by each Type to return a secure object for validation based on the given ID.
         *
         * @param id id of the object type
         * @return secure object for validation
         */
        public abstract WSSecured getMappedSecuredWS(Serializable id);
}
