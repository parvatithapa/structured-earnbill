package com.sapienter.jbilling.server.util.cxf;

import com.sapienter.jbilling.server.pricing.PriceModelWS;

import java.util.Date;

/**
 * @author Gerhard
 * @since 19/12/13
 */
public class CxfSMapDatePriceModelAdapter extends BaseCxfSortedMapAdapter<Date, PriceModelWS, CxfSMapDatePriceModel.KeyValueEntry, CxfSMapDatePriceModel> {

    @Override
    protected CxfSMapDatePriceModel createCxfMap() {
        return new CxfSMapDatePriceModel();
    }

    @Override
    protected CxfSMapDatePriceModel.KeyValueEntry createEntry() {
        return new CxfSMapDatePriceModel.KeyValueEntry();
    }

}
