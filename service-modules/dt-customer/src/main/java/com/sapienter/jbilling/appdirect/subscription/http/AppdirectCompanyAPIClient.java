package com.sapienter.jbilling.appdirect.subscription.http;

import com.sapienter.jbilling.appdirect.subscription.companydetails.AppdirectCompanyWS;
import com.sapienter.jbilling.appdirect.subscription.http.exception.AppdirectCompanyClientException;
import com.sapienter.jbilling.appdirect.subscription.http.exception.UnauthorizedException;
import com.sapienter.jbilling.appdirect.subscription.oauth.OAuthRestTemplateService;
import com.sapienter.jbilling.common.FormatLogger;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.CheckedFunction1;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Collections;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;

@Service
public class AppdirectCompanyAPIClient {

    private static final  Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String API_URL = "/api/account/v2/companies/";

    private OAuthRestTemplateService oAuthRestTemplateServiceService;


    @PostConstruct
    public void init() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    }

    public AppdirectCompanyWS getCompanyDetails(
            String companyUuid,
            String baseUrl,
            String consumerKey,
            String consumerSecret) {

        ResponseEntity<AppdirectCompanyWS> response;
        String uri = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path(API_URL + companyUuid)
                .toUriString();

        RequestCreds creds = new RequestCreds(uri, consumerKey, consumerSecret);

        response = Try.of(() -> creds)
                .mapTry(request())
                .recoverWith(
                        UnauthorizedException.class,
                        (x) -> Try.of(() -> creds).mapTry(requestWithNewCreds())
                )
                .getOrElseThrow(e ->
                        new AppdirectCompanyClientException("Rest client exception", e)
                );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();

        } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            logger.warn("Company info not found");
            return null;
        } else {
            logger.warn("Un-categorized http request issue");
            throw new AppdirectCompanyClientException("Un-categorized exception in Request");
        }
    }

    private ResponseEntity<AppdirectCompanyWS> getWithNewCreds(RequestCreds creds) {
        RestTemplate restTemplate = oAuthRestTemplateServiceService
                .refresh(creds.consumerKey, creds.consumerSecret);

        return restTemplate.getForEntity(creds.uri, AppdirectCompanyWS.class);
    }

    private CheckedFunction1<RequestCreds, ResponseEntity<AppdirectCompanyWS>> requestWithNewCreds() {
        logger.warn("Refreshing tokens and retrying...");
        return Retry.decorateCheckedFunction(retryInstance(2), this::getWithNewCreds);
    }

    private ResponseEntity<AppdirectCompanyWS> get(RequestCreds creds) {
        RestTemplate restTemplate = oAuthRestTemplateServiceService
                .instance(creds.consumerKey, creds.consumerSecret);

        return restTemplate.getForEntity(creds.uri, AppdirectCompanyWS.class);
    }

    private CheckedFunction1<RequestCreds, ResponseEntity<AppdirectCompanyWS>> request() {
        logger.debug("Making request to marketplace...");
        return Retry.decorateCheckedFunction(retryInstance(3), this::get);
    }

    private Retry retryInstance(int maxAttempts) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(3_000))
                .retryOnException(throwable ->
                        Match(throwable).of(
                                Case($(instanceOf(UnauthorizedException.class)), false),
                                Case($(instanceOf(RestClientException.class)), true)))
                .build();

        return Retry.of("dt-customer-api-id", config);
    }

    private static class RequestCreds {
        private String uri;
        private String consumerKey;
        private String consumerSecret;

        public RequestCreds(String uri, String consumerKey, String consumerSecret) {
            this.uri = uri;
            this.consumerKey = consumerKey;
            this.consumerSecret = consumerSecret;
        }
    }

    public void setoAuthRestTemplateServiceService(OAuthRestTemplateService oAuthRestTemplateServiceService) {
        this.oAuthRestTemplateServiceService = oAuthRestTemplateServiceService;
    }
}
