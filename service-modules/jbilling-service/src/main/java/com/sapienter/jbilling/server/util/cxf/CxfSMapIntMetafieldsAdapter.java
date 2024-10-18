package com.sapienter.jbilling.server.util.cxf;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;


/**
 * @author Gerhard
 * @since 19/12/13
 */
public class CxfSMapIntMetafieldsAdapter extends BaseCxfSortedMapAdapter<Integer, MetaFieldValueWS[], CxfSMapIntMetaFields.KeyValueEntry, CxfSMapIntMetaFields> {

    @Override
    protected CxfSMapIntMetaFields createCxfMap() {
        return new CxfSMapIntMetaFields();
    }

    @Override
    protected CxfSMapIntMetaFields.KeyValueEntry createEntry() {
        return new CxfSMapIntMetaFields.KeyValueEntry();
    }

}
