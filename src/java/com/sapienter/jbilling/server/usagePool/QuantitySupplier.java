package com.sapienter.jbilling.server.usagePool;

import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.function.Supplier;
import java.util.Date;


public class QuantitySupplier implements Supplier<BigDecimal> {

    private BigDecimal quantity;

    public QuantitySupplier(UsagePoolDTO usagePoolDTO, Date nextInvoiceDate, Date orderActiveSinceDate) {
        if (usagePoolDTO.getUsagePoolResetValue().equals(UsagePoolResetValueEnum.HOURS_PER_CALENDER_MONTH)) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(nextInvoiceDate);

            if (orderActiveSinceDate != null) {
                Calendar cals = Calendar.getInstance();
                cals.setTime(orderActiveSinceDate);

                if (cals.before(cal)) {
                    cal.add(Calendar.MONTH,-1);
                }else{
                    cal = cals;
                }
            } else {
                cal.add(Calendar.MONTH,-1);
            }

            int monthMaxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            quantity = new BigDecimal(monthMaxDays * 24);
        } else {
            quantity = usagePoolDTO.getQuantity();
        }
    }

    @Override
    public BigDecimal get() {
        return quantity;
    }
}
