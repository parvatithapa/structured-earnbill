package com.sapienter.jbilling.server.spa;

import java.util.Arrays;

/**
 * Created by pablo_galera on 16/01/17.
 */
public enum AddressType {
    BILLING("Billing"),  SHIPPING("Shipping"), SERVICE("Service"), EMERGENCY("Emergency"), PORTING("Porting");

    String name;

    private AddressType(String name) {
        this.name = name;
    }

    public static AddressType getByName(String name) {
      return Arrays.stream(AddressType.values()).filter(x -> x.name.equalsIgnoreCase(name)).findFirst().orElse(null);
    }

}
