package com.sapienter.jbilling.server.payment.tasks.westpac;

public class PayWayCreditCard {

    private String maskedCardNumber;
    private String expiryDateMonth;
    private String expiryDateYear;
    private String cardScheme;
    private String cardholderName;

    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    public void setMaskedCardNumber(String maskedCardNumber) {
        this.maskedCardNumber = maskedCardNumber;
    }

    public String getExpiryDateMonth() {
        return expiryDateMonth;
    }

    public void setExpiryDateMonth(String expiryDateMonth) {
        this.expiryDateMonth = expiryDateMonth;
    }

    public String getExpiryDateYear() {
        return expiryDateYear;
    }

    public void setExpiryDateYear(String expiryDateYear) {
        this.expiryDateYear = expiryDateYear;
    }

    public String getCardScheme() {
        return cardScheme;
    }

    public void setCardScheme(String cardScheme) {
        this.cardScheme = cardScheme;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }

    @Override
    public String toString() {
        return "CreditCard [maskedCardNumber=" + maskedCardNumber + ", expiryDateMonth=" + expiryDateMonth
                + ", expiryDateYear=" + expiryDateYear + ", cardScheme=" + cardScheme + ", cardholderName="
                + cardholderName + "]";
    }

}
