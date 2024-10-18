package com.sapienter.jbilling.server.item;

public enum TariffPlan {
	TARIFF("Tariff"), NON_TARIFF("Non Tariff");
	
	private final String value;
	
	private TariffPlan(String value)
    {
        this.value = value;
    }
	
	public String toString() { return value; } 
    public String getKey() { return name(); }
}
