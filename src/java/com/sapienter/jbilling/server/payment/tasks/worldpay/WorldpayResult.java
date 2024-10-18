package com.sapienter.jbilling.server.payment.tasks.worldpay;

public class WorldpayResult {

    private String orderCode;//
    private String customerOrderCode;
    private String errorCode;
    private String errorMessage;
    private String errorDescription;
    private boolean succeeded;
    private String status;//
    private Integer httpStatusCode;
    private String avs;
    private String token;

    public String getOrderCode() {
        return orderCode;
    }

    public String getCustomerOrderCode() {
        return customerOrderCode;
    }

    public String getErrorCode() {
        return errorCode;
    }


    public String getErrorMessage() {
        return errorMessage;
    }


    public String getErrorDescription() {
        return errorDescription;
    }

    public boolean isSucceeded() {
        return succeeded;
    }


    public String getStatus() {
        return status;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getAvs() {
        return avs;
    }

    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WorldpayResult [orderCode=");
        builder.append(orderCode);
        builder.append(", customerOrderCode=");
        builder.append(customerOrderCode);
        builder.append(", errorCode=");
        builder.append(errorCode);
        builder.append(", errorMessage=");
        builder.append(errorMessage);
        builder.append(", errorDescription=");
        builder.append(errorDescription);
        builder.append(", succeeded=");
        builder.append(succeeded);
        builder.append(", status=");
        builder.append(status);
        builder.append(", httpStatusCode=");
        builder.append(httpStatusCode);
        builder.append(", avs=");
        builder.append(avs);
        builder.append(", token=");
        builder.append(token);
        builder.append("]");
        return builder.toString();
    }

    private WorldpayResult(WorldpayResultBuilder builder) {
        this.orderCode = builder.orderCode;
        this.status = builder.status;
        this.customerOrderCode = builder.customerOrderCode;
        this.errorCode = builder.errorCode;
        this.errorDescription = builder.errorDescription;
        this.errorMessage = builder.errorMessage;
        this.succeeded = builder.succeeded;
        this.avs = builder.avs;
        this.httpStatusCode = builder.httpStatusCode;
        this.token = builder.token;
    }

    public static class WorldpayResultBuilder{
        //required parameters
        private String orderCode;
        private String status;
        private String customerOrderCode;
        private String errorCode;
        private String errorMessage;
        private String errorDescription;
        private boolean succeeded;
        private Integer httpStatusCode;
        private String avs;
        private String token;

        public WorldpayResultBuilder setStatus(String status) {
            this.status = status;
            return this;
        }

        public WorldpayResultBuilder setCustomerOrderCode(String customerOrderCode) {
            this.customerOrderCode = customerOrderCode;
            return this;
        }

        public WorldpayResultBuilder setOrderCode(String orderCode) {
            this.orderCode = orderCode;
            return this;
        }

        public WorldpayResultBuilder setErrorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public WorldpayResultBuilder setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public WorldpayResultBuilder setErrorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
            return this;
        }

        public WorldpayResultBuilder setSucceeded(boolean succeeded) {
            this.succeeded = succeeded;
            return this;
        }

        public WorldpayResultBuilder setHttpStatusCode(Integer httpStatusCode) {
            this.httpStatusCode = httpStatusCode;
            return this;
        }

        public WorldpayResultBuilder setAvs(String avs) {
            this.avs = avs;
            return this;
        }

        public WorldpayResultBuilder token(String token) {
            this.token = token;
            return this;
        }

        public WorldpayResult build() {
            return new WorldpayResult(this);
        }
    }

}
