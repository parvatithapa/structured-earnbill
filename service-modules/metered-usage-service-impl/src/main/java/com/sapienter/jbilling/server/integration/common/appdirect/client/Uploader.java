package com.sapienter.jbilling.server.integration.common.appdirect.client;

import java.time.Duration;

import lombok.Builder;
import lombok.Data;

import com.sapienter.jbilling.appdirect.vo.UsageBean;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.NetworkTimeoutException;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.UnAuthorizedTransientException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.CheckedFunction1;

@Data
@Builder
public class Uploader {

	private int retries;
	private long retryWait;
	private IntegrationClient integrationClient;

	public CheckedFunction1<UsageBean, Boolean> getUploadFunction() {
		RetryConfig config = RetryConfig.custom()
			.maxAttempts(retries)
			.waitDuration(Duration.ofMillis(retryWait))
			.retryOnException(throwable -> throwable instanceof UnAuthorizedTransientException ||
				throwable instanceof NetworkTimeoutException).build();

		Retry retry = Retry.of("metered-usage-writer-id", config);

		return Retry.decorateCheckedFunction(retry, integrationClient::send);

	}
}
