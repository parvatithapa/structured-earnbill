package com.sapienter.jbilling.server.mediation;

import java.math.BigDecimal;
import java.util.List;

public class JMRQuantity {

    public static final JMRQuantity NONE = JMRQuantity.builder().build();

    private BigDecimal quantity;
    private String errors;

    private JMRQuantity() {}

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return errors != null && errors.trim().length() > 0;
    }

    public static JMRQuantityBuilder builder() {
        return new JMRQuantityBuilder();
    }

    public static class JMRQuantityBuilder {

        private JMRQuantity managedInstance = new JMRQuantity();

        public JMRQuantity quantity(BigDecimal quantity) {
            this.managedInstance.quantity = quantity;
            return managedInstance;
        }

        public JMRQuantity errors(String errors) {
            this.managedInstance.errors = errors;
            return managedInstance;
        }

        public JMRQuantity fromErrorCodes(List<String> errorCodes) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            sb.append(String.join(",", errorCodes));
            sb.append(']');

            this.managedInstance.errors = sb.toString();
            return managedInstance;
        }

        public JMRQuantity build() {
            return managedInstance;
        }
    }
}
