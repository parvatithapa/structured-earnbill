package com.sapienter.jbilling.server.config;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.sapienter.jbilling.twofactorauth.TwoFactorAuthExceptionHandler;
import com.sapienter.jbilling.twofactorauth.TwoFactorAuthenticationHelperService;
import com.sapienter.jbilling.twofactorauth.User2FALogService;

@Configuration
public class TwoFactoryConfiguration {

	private static final int TIME_OUT = 10_000;

	@Bean(name = "twoFactorAuthenticationHelperService")
	TwoFactorAuthenticationHelperService twoFactorAuthenticationHelperService() {
		return new TwoFactorAuthenticationHelperService();
	}

	@Bean(name = "user2FALogService")
	User2FALogService user2FALogService() {
		return new User2FALogService();
	}

	@Bean
	RestTemplate twoFactorRestClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
		SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
				.loadTrustMaterial(null, acceptingTrustStrategy)
				.build();
		SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
		CloseableHttpClient httpClient = HttpClients.custom()
				.setSSLSocketFactory(csf)
				.build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setConnectionRequestTimeout(TIME_OUT);
		requestFactory.setConnectTimeout(TIME_OUT);
		requestFactory.setReadTimeout(TIME_OUT);
		requestFactory.setHttpClient(httpClient);
		RestTemplate restTemplate = new RestTemplate(requestFactory);
		restTemplate.setErrorHandler(new TwoFactorAuthExceptionHandler());
		return restTemplate;
	}
}
