package com.sapienter.jbilling.saml.security.saml;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang.StringUtils;
import org.opensaml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.XMLObject;
import org.springframework.security.saml.metadata.CachingMetadataManager;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This on demand metadata manager extends {@link CachingMetadataManager} to allow for dynamically loading
 * providers. Loaded providers are cached in memory.
 *
 * @author Aamir Ali
 */
public class OnDemandMetadataManager extends CachingMetadataManager {
    private final MetadataLocationResolver locationResolver;
    private final MetadataProviderLoader providerLoader;
    private final LoadingCache<String, MetadataProvider> providers;

    /**
     * Creates a new {@link OnDemandMetadataManager}.
     *
     * @param resolver            The resolver to determine the metadata location based on entity ID.
     * @param providerLoader      The loader that dinamically creates metadata providers using the metadata location.
     * @param maxCacheSize        Maximum number of loaded providers allowed in memory.
     * @param cacheExpirationMins Expiration time for loaded providers.
     * @param additionalProviders Additional providers known in advance that do not need to be loaded dynamically.
     * @throws MetadataProviderException If there is an issue constructing this manager.
     */
    public OnDemandMetadataManager(
            MetadataLocationResolver resolver,
            MetadataProviderLoader providerLoader,
            long maxCacheSize,
            long cacheExpirationMins,
            List<MetadataProvider> additionalProviders) throws MetadataProviderException {
        super(additionalProviders);
        this.locationResolver = resolver;
        this.providerLoader = providerLoader;
        this.providers = CacheBuilder.newBuilder()
                .maximumSize((int) maxCacheSize)
                .expireAfterWrite(cacheExpirationMins, TimeUnit.MINUTES)
                .build(new CacheLoader<String, MetadataProvider>() {
                    @Override
                    public MetadataProvider load(String entityId) throws Exception {
                        return doLoad(entityId);
                    }
                });
    }

    private MetadataProvider doLoad(String entityId) throws MetadataProviderException, IOException, NoSuchProviderException {
        String metadataLocation = locationResolver.resolve(entityId);
        if (StringUtils.isBlank(metadataLocation)) {
            throw new NoSuchProviderException("No metadata location known for entity ID '" + entityId + "'.");
        }
        return providerLoader.load(entityId, metadataLocation);
    }

    private MetadataProvider getProvider(String entityId) throws MetadataProviderException {
        try {
            return providers.get(entityId);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof NoSuchProviderException) {
                return null;
            }
            if (e.getCause() instanceof MetadataProviderException) {
                throw (MetadataProviderException) e.getCause();
            }
            throw new MetadataProviderException("Error loading metadata provider for entity ID '" + entityId + "'.", e);
        }
    }

    @Override
    public XMLObject getMetadata() throws MetadataProviderException {
        return super.getMetadata();
    }

    @Override
    public EntitiesDescriptor getEntitiesDescriptor(String name) throws MetadataProviderException {
        return super.getEntitiesDescriptor(name);
    }

    @Override
    public EntityDescriptor getEntityDescriptor(String entityId) throws MetadataProviderException {
        // try a get dynamically loaded descriptor, if not available, look for static ones.
        MetadataProvider provider = getProvider(entityId);
        if (provider == null) {
            return super.getEntityDescriptor(entityId);
        }
        return provider.getEntityDescriptor(entityId);
    }

    @Override
    public List<RoleDescriptor> getRole(String entityId, QName roleName) throws MetadataProviderException {
        // try a get dynamically loaded roles, if not available, look for static ones.
        MetadataProvider provider = getProvider(entityId);
        if (provider == null) {
            return super.getRole(entityId, roleName);
        }
        return provider.getRole(entityId, roleName);
    }

    @Override
    public RoleDescriptor getRole(String entityId, QName roleName, String supportedProtocol) throws MetadataProviderException {
        // try a get dynamically loaded roles, if not available, look for static ones.
        MetadataProvider provider = getProvider(entityId);
        if (provider == null) {
            return super.getRole(entityId, roleName, supportedProtocol);
        }
        return provider.getRole(entityId, roleName, supportedProtocol);
    }

    protected static class NoSuchProviderException extends Exception {
        private static final long serialVersionUID = -7786207393938744332L;

        public NoSuchProviderException(String message) {
            super(message);
        }
    }
}
