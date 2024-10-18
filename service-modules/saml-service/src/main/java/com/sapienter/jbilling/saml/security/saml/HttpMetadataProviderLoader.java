package com.sapienter.jbilling.saml.security.saml;

import org.apache.commons.httpclient.HttpClient;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.parse.ParserPool;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate;

import java.util.Timer;

/**
 * Implementation of {@link MetadataProviderLoader} that loads metadata from a URL.
 *
 * @author Aamir Ali
 */
public class HttpMetadataProviderLoader implements MetadataProviderLoader {
    private final Timer timer;
    private final HttpClient client;
    private final ParserPool parserPool;

    /**
     * Creates a new {@link HttpMetadataProviderLoader}.
     *
     * @param timer      The timer used for for background metadata refresh.
     * @param client     The HTTP client to be used to fetch the metadata.
     * @param parserPool The metadata parser pool.
     */
    public HttpMetadataProviderLoader(
            Timer timer,
            HttpClient client,
            ParserPool parserPool) {
        this.timer = timer;
        this.client = client;
        this.parserPool = parserPool;
    }

    @Override
    public MetadataProvider load(String entityId, String metadataLocation) throws MetadataProviderException {
        HTTPMetadataProvider provider = new HTTPMetadataProvider(timer, client, metadataLocation);
        provider.setParserPool(parserPool);
        MetadataProvider processed = postProcess(provider);
        provider.initialize();
        return processed;
    }

    protected MetadataProvider postProcess(MetadataProvider provider) {
        ExtendedMetadata extendedMetadata = new ExtendedMetadata();
        extendedMetadata.setIdpDiscoveryEnabled(false);
        extendedMetadata.setSignMetadata(true);

        ExtendedMetadataDelegate extendedMetadataDelegate = new ExtendedMetadataDelegate(provider, extendedMetadata);
        extendedMetadataDelegate.setMetadataTrustCheck(false);
        extendedMetadataDelegate.setMetadataRequireSignature(false);
        return extendedMetadataDelegate;
    }
}
