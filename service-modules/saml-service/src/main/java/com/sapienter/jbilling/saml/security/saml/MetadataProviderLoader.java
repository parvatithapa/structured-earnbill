package com.sapienter.jbilling.saml.security.saml;

import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;

/**
 * This interface loads metadata providers using the provided metadata location.
 *
 * @author Aamir Ali
 */
public interface MetadataProviderLoader {
    /**
     * Loads a metadata providers using the given metadta location.
     *
     * @param entityId         The entity ID associated to the metadata location. The metadata should contain this entity but implementors of this method don't need to enforce it.
     * @param metadataLocation The location of the metadata to be loaded.
     * @return The loaded and initialized metadata provider.
     * @throws MetadataProviderException If there is an issue while loading the provider.
     */
    MetadataProvider load(String entityId, String metadataLocation) throws MetadataProviderException;
}
