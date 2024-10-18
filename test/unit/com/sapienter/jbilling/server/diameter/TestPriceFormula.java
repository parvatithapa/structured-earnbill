package com.sapienter.jbilling.server.diameter;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Test;

public class TestPriceFormula {

	@Test
	public void testPricingFormula() {
		BigDecimal quantity = new BigDecimal("2");
		BigDecimal perMinute = new BigDecimal("0.75");
		BigDecimal dropCharge = new BigDecimal("2");
		
		BigDecimal result = quantity.multiply(perMinute).add(dropCharge).divide(quantity);
		assertEquals(0, result.compareTo(new BigDecimal("1.75")));
		quantity = new BigDecimal("5");
		result = quantity.multiply(perMinute).add(dropCharge).divide(quantity);
		assertEquals(0, result.compareTo(new BigDecimal("1.15")));
	}

}
