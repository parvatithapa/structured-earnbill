package com.sapienter.jbilling.server.util.cxf;

import java.util.Date;
import java.util.List;

/**
 * @author Gerhard
 * @since 19/12/13
 */
public class CxfMapListDateAdapter extends BaseCxfMapAdapter<Integer, List<Date>, CxfMapListDate.KeyValueEntry, CxfMapListDate> {

    @Override
    protected CxfMapListDate createCxfMap() {
        return new CxfMapListDate();
    }

    @Override
    protected CxfMapListDate.KeyValueEntry createEntry() {
        return new CxfMapListDate.KeyValueEntry();
    }

}
