package com.sapienter.jbilling.server.diameter;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;

public class TestBinarySearchForAmount {

    private static final DiameterPriceLocator PRICE_0dot1 = new DiameterPriceLocator() {
        @Override
        public BigDecimal rate(Integer itemId, BigDecimal units,
                               PricingFieldsHelper fieldHelper) {
            return new BigDecimal("0.1");
        }
    };

    private static final DiameterPriceLocator PRICE_3 = new DiameterPriceLocator() {
        @Override
        public BigDecimal rate(Integer itemId, BigDecimal units,
                               PricingFieldsHelper fieldHelper) {
            return new BigDecimal("3");
        }
    };

    @Test
    public void testBinarySearchForAmount() throws Exception {

        UserDTO user = new UserDTO();
        BinarySearchUnits bpl = new BinarySearchUnits(1, user, PRICE_0dot1);
        user.setCustomer(new CustomerDTO());
        user.getCustomer().setDynamicBalance(new BigDecimal("10"));
        PricingField[] params = new PricingField[0];
        PricingFieldsHelper fieldHelper = new PricingFieldsHelper(params);

        // 0 x 0.1 = 0
        // User with 10$ balance, request of 0 units
        assertEquals(BigDecimal.ZERO, bpl.calculateUnits(0, BigDecimal.ZERO, fieldHelper));
        assertEquals(BigDecimal.ZERO, bpl.getRatedUnits());
        assertEquals(0, bpl.getRatedUnitPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, bpl.getRatedTotalPrice().compareTo(BigDecimal.ZERO));

        // 30 x 0.1 = 3
        // User with 10$ balance, request of 30 units at 0.1$ each = 3$
        assertEquals(new BigDecimal("30"), bpl.calculateUnits(0, new BigDecimal("30"), fieldHelper));
        assertEquals(new BigDecimal("30"), bpl.getRatedUnits());
        assertEquals(0, bpl.getRatedUnitPrice().compareTo(new BigDecimal("0.1")));
        assertEquals(0, bpl.getRatedTotalPrice().compareTo(new BigDecimal("3")));

        // 30 x 0.1 = 3
        // User with 10$ balance, request of 60 units at 0.1$ each = 6$
        assertEquals(0, new BigDecimal("60").compareTo(bpl.calculateUnits(0, new BigDecimal("60"), fieldHelper)));
        assertEquals(new BigDecimal("60"), bpl.getRatedUnits());
        assertEquals(0, bpl.getRatedUnitPrice().compareTo(new BigDecimal("0.1")));
        assertEquals(0, bpl.getRatedTotalPrice().compareTo(new BigDecimal("6")));

        // 200 x 0.1 = 20 > 10
        // User with 10$ balance, request of 200 units at 0.1$ each = 20$
        // Available units should be 100 x 0.1$ = 10$
        assertEquals(new BigDecimal("100"), bpl.calculateUnits(0, new BigDecimal("200"), fieldHelper));
        assertEquals(new BigDecimal("100"), bpl.getRatedUnits());
        assertEquals(0, bpl.getRatedUnitPrice().compareTo(new BigDecimal("0.1")));
        assertEquals(0, bpl.getRatedTotalPrice().compareTo(new BigDecimal("10")));

        // 120 x 0.1 = 12 > 10
        // User with 10$ balance, request of 120 units at 0.1$ each = 12$
        // Available units should be 100 x 0.1$ = 10$
        assertEquals(0, new BigDecimal("100").compareTo(bpl.calculateUnits(0, new BigDecimal("120"), fieldHelper)));
        assertEquals(new BigDecimal("100"), bpl.getRatedUnits());
        assertEquals(0, bpl.getRatedUnitPrice().compareTo(new BigDecimal("0.1")));
        assertEquals(0, bpl.getRatedTotalPrice().compareTo(new BigDecimal("10")));

        // 10 x 0.1 = 1 > 0
        // User with 0$ balance, request of 10 units at 0.1$ each = 1$
        // Result should be 0 units
        user.getCustomer().setDynamicBalance(BigDecimal.ZERO);
        assertEquals(0, BigDecimal.ZERO.compareTo(bpl.calculateUnits(0, new BigDecimal("10"), fieldHelper)));
        assertEquals(BigDecimal.ZERO, bpl.getRatedUnits());
        assertEquals(0, bpl.getRatedUnitPrice().compareTo(new BigDecimal("0.1")));
        assertEquals(0, bpl.getRatedTotalPrice().compareTo(BigDecimal.ZERO));

        // 10 x 3 = 30 > 1
        // User with 1$ balance, request of 1 unit at 3$ each = 3$
        // Result should be 0 units
        bpl = new BinarySearchUnits(1, user, PRICE_3);
        user.getCustomer().setDynamicBalance(new BigDecimal("1"));
        assertEquals(BigDecimal.ZERO, bpl.calculateUnits(0, BigDecimal.ONE, fieldHelper));
        assertEquals(BigDecimal.ZERO, bpl.getRatedUnits());
        assertEquals(0, bpl.getRatedUnitPrice().compareTo(new BigDecimal("3")));
        assertEquals(0, bpl.getRatedTotalPrice().compareTo(BigDecimal.ZERO));

        // User with 10$ balance, request of 5 units at 3$ each = 15$
        // Available units should be 3 x 3$ = 9$
        user.getCustomer().setDynamicBalance(new BigDecimal("10"));
        assertEquals(new BigDecimal("3"), bpl.calculateUnits(0, new BigDecimal("5"), fieldHelper));
        assertEquals(new BigDecimal("3"), bpl.getRatedUnits());
        assertEquals(0, bpl.getRatedUnitPrice().compareTo(new BigDecimal("3")));
        assertEquals(0, bpl.getRatedTotalPrice().compareTo(new BigDecimal("9")));

        // User with 9$ balance, request of 3 units at 3$ each
        // Result should be 3 units
        user.getCustomer().setDynamicBalance(new BigDecimal("9"));
        assertEquals(new BigDecimal("3"), bpl.calculateUnits(0, new BigDecimal("5"), fieldHelper));
        assertEquals(new BigDecimal("3"), bpl.getRatedUnits());
        assertEquals(0, bpl.getRatedUnitPrice().compareTo(new BigDecimal("3")));
        assertEquals(0, bpl.getRatedTotalPrice().compareTo(new BigDecimal("9")));
    }
}
