package com.jbilling.framework.globals;

public class GlobalEnumsPage {
	public enum CollectionAgeingStep {
		FIRST, SECOND, THIRD, FOURTH
	}

	public enum PaymentMethodField {
		ALL_ACCOUNTS, REECURRING, ALL;
	}

	public enum DefaultPayment {
		ALL, PAYMENT_DEFAULT;
	}

	public enum UpdateMetaField {
		NAME_TYPE, NAME_TYPE_DISABLE;
	}

	public enum AccountTypeField {
		ALL, ACCOUNT_NAME, BILLING_CYCLE, INVOICE;
	}

	public enum AccountTypeInfo {
		SIMPLE, DISABLE_CHECKBOX;
	}

	public enum AddProductField {
		FLAT, LINEPERCENTAGE, ASSETMANAGEMENT, GRADUATED, DESCRIPTION, FLATPRICE, GRADUATEDPRICE, GRADUATECAPPRICE,
		TIMEOFDAY, TIERED, VOLUME, POOLED, ITEMPAGESELECTOR, ITEMSELECTOR, QUANTITYADON, LINEPERCENTAGEWITHCOMPANY,
        ADDPRODUCTWITHASSETMANAGEMENT, TEASERPRICING;
	}

	public enum AddMetaDataFields {
		DATA_FIELD, DATA_TYPE, DATA_DEFAULT_VALUE;
	}

	public enum AddMetaDataGroupFields {
		GROUP_DATA_FIELD, GROUP_DDATA_TYPE, GROUP_DDATA_DEFAULT_VALUE;
	}

	public enum AddPlanField {
		ALL, DETAILS, PRODUCT, BUNDLEDPERIOD, PLANPERIOD, MULTIPLEPLAN, WITHNOTE;
	}

	public enum NewEnumerationMsg {
		ALL, NAME, VALUE;
	}

	public enum SetEnumValues {
		ZERO, ONE;
	}

	public enum AddNewEnumeration {
		ALL, VERIFY_MANDATORY_FIELDS;
	}

	public static enum PageConfigurationItems {
		All("All"),

		AccountType("Account Type"),

		AgentCommissionProcess("Agent Commission Process"),

		BillingProcess("Billing Process"),

		Blacklist("Blacklist"),

		Collections("Collections"),

		Company("Company"),

		Companies("Companies"),

		Currencies("Currencies"),

		DataTables("Data Tables"),

		Email("Email"),

		Enumerations("Enumerations"),

		EntityLogos("Entity Logos"),

		FreeUsagePools("Free Usage Pools"),

		InvoiceDisplay("Invoice Display"),

		InvoiceTemplates("Invoice Templates"),

		Languages("Languages"),

		Mediation("Mediation"),

		MetaFields("Meta Fields"),

		MetaFieldGroups("Meta Field Groups"),

		Notification("Notification"),

		OrderChangeStatuses("Order Change Statuses"),

		OrderChangeTypes("Order Change Types"),

		OrderPeriods("Order Periods"),

		OrderStatuses("Order Statuses"),

		PaymentMethod("Payment Method"),

		Plugins("Plug-ins"),

		Roles("Roles"),

		RateCards("Rate Cards"),

		RatingUnit("Rating Unit"),

		RouteRateCard("Route Rate Card"),

		RouteTest("Route Test"),

		Tools("Tools"),

        Users("Users"),
        CustomInvoice("Ad-hoc Invoice");

		public static PageConfigurationItems GetKey(final String Value) {
			for (PageConfigurationItems tc : PageConfigurationItems.values()) {
				if (tc.GetValue().equals(Value)) {
					return tc;
				}
			}
			return null;
		}

		private final String indexLabel;

		private PageConfigurationItems(final String labelText) {
			this.indexLabel = labelText;
		}

		public String GetValue() {
			return this.indexLabel;
		}
	}

	public static enum PageSuccessMessages {
		OrderPeriodCreationSuccess("Order Period created successfully"),

		PaymentMethodCreationSuccess("Payment Method Type XXX created successfully");

		public static PageSuccessMessages GetKey(final String Value) {
			for (PageSuccessMessages tc : PageSuccessMessages.values()) {
				if (tc.GetValue().equals(Value)) {
					return tc;
				}
			}
			return null;
		}

		private final String indexLabel;

		private PageSuccessMessages(final String labelText) {
			this.indexLabel = labelText;
		}

		public String GetValue() {
			return this.indexLabel;
		}
	}

	public enum setProductCategoryWithAssetMgmt {
		PCWAMG1, PCWAMG2, PCWAMG3, PCWAMG4
	}

    public enum MakePayment {
        UNCHANGEDPAYEMENT, UN_CHANGED_PAYEMENT_CARD, PAYEMENT_CARD_REAL_TIME;
    }
}
