package com.sapienter.jbilling.server.creditnote.db;


public enum CreditType {
	
	AUTO_GENERATED ("Auto-generated from Invoice"),
	USER_GENERATED ("User Generated");

	private final String typeLabel;
	
	CreditType(String typeLabel) {
		this.typeLabel = typeLabel;
	}
	
	public String getTypeLabel() {
		return this.typeLabel;
	}
	
	public CreditType[] getAllCreditTypes() {
		return CreditType.values();
	}
}
