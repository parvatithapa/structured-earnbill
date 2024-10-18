package com.sapienter.jbilling.server.security;

import java.util.List;

/**
 * Interface that marks a Web Service object as being "hierarchical" i.e. shared in Company Hierarchies. <code>HierarchicalEntity</code> objects
 * may be accessed for view by web service users (callers) within the same entity or its descendants as the object being accessed/modified.
 *
 * Implementing classes must be able to provide <strong>both</strong> an owning entity id as well as
 * list of entities that have access to the object.
 *
 * @author Vikas Bodani
 * @since 12-09-2015
 */

public interface HierarchicalEntity {

    /**
     * Returns the list of jBilling Entity IDs within a hierarchy that have access to this object.
     *
     * @return list of entities that have access.
     */
    public List<Integer> getAccessEntities();

    /**
     * Named differently to avoid name conflict with implementing entities.
     * @return
     */
    public Boolean ifGlobal();
}
