package com.sapienter.jbilling.server.audit;

import java.io.Serializable;

/**
 * Created by marcomanzicore on 23/11/15.
 */
public interface Auditable {

    String getAuditKey(Serializable id);

}
