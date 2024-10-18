package com.sapienter.jbilling.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import com.sapienter.jbilling.einvoice.db.EInvoiceLogDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessInfoDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.twofactorauth.db.User2FALogDAS;

@Configuration
public class DasConfiguration {

	@Bean
	public BillingProcessDAS billingProcessDAS () {
		return new BillingProcessDAS();
	}

	@Bean
	public BillingProcessInfoDAS billingProcessInfoDAS () {
		return new BillingProcessInfoDAS();
	}

	@Bean
	public UserDAS userDAS () {
		return new UserDAS();
	}

	@Bean
	public InvoiceDAS invoiceDAS () {
		return new InvoiceDAS();
	}

	@Bean
	public OrderDAS orderDAS () {
		return new OrderDAS();
	}

	@Bean
	@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
	public MetaFieldDAS metaFieldDAS () {
		return new MetaFieldDAS();
	}

	@Bean
	public AssetDAS assetDAS() {
		return new AssetDAS();
	}

	@Bean
	public OrderLineDAS orderLineDAS() {
		return new OrderLineDAS();
	}

	@Bean
	public OrderStatusDAS orderStatusDAS() {
		return new OrderStatusDAS();
	}

	@Bean
	public ItemDAS itemDAS() {
		return new ItemDAS();
	}

	@Bean
	public PluggableTaskTypeCategoryDAS pluggableTaskTypeCategoryDAS() {
		return new PluggableTaskTypeCategoryDAS();
	}

	@Bean
	public EInvoiceLogDAS eInvoiceLogDAS() {
		return new EInvoiceLogDAS();
	}

	@Bean
	public User2FALogDAS user2faLogDAS() {
		return new User2FALogDAS();
	}
}
