package com.sapienter.jbilling.server.integration.common.appdirect.client;

import java.io.IOException;
import java.net.HttpURLConnection;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthException;

@Slf4j
public class TwoLeggedOAuthClientHttpRequestFactory extends SimpleClientHttpRequestFactory {
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 30_000;

    private OAuthConsumer consumer;

    public TwoLeggedOAuthClientHttpRequestFactory(String consumerKey, String consumerSecret) {
        init(consumerKey, consumerSecret, DEFAULT_CONNECT_TIMEOUT_MILLIS,DEFAULT_READ_TIMEOUT_MILLIS);
    }

    public TwoLeggedOAuthClientHttpRequestFactory(String consumerKey, String consumerSecret, int connectTimeout, int readTimeout) {
       init(consumerKey, consumerSecret, connectTimeout, readTimeout);
    }
    private void init(String consumerKey, String consumerSecret, int connectTimeout, int readTimeout) {
        consumer = new DefaultOAuthConsumer(consumerKey, consumerSecret);
        setConnectTimeout(connectTimeout);
        setReadTimeout(readTimeout);

        // Disable output streaming to avoid "org.springframework.web.client.ResourceAccessException: I/O error on POST request for [URL]: cannot retry due to server authentication, in streaming mode; nested exception is java.net.HttpRetryException: cannot retry due to server authentication, in streaming mode"
        setOutputStreaming(false);
    }

    @Override
    protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
        super.prepareConnection(connection, httpMethod);
        try {
            consumer.sign(connection);
        } catch (OAuthException e) {
            log.error("Error while signing request", e);
        }
    }
}
