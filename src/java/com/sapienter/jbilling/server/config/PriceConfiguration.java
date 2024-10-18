package com.sapienter.jbilling.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sapienter.jbilling.server.item.PriceService;

@Configuration
public class PriceConfiguration {

	@Bean
	public PriceService priceService() {
		return new PriceService();
	}
}