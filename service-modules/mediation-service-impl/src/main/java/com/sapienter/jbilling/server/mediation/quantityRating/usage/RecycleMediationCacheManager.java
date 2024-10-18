package com.sapienter.jbilling.server.mediation.quantityRating.usage;

import java.util.Optional;

public interface RecycleMediationCacheManager {

    default Optional<RecycleMediationCacheProvider> recycleMediationCacheProvider() {
        return Optional.empty();
    }

    default void setRecycleMediationCacheProvider(RecycleMediationCacheProvider provider) {
        // no-ops
    }
}
