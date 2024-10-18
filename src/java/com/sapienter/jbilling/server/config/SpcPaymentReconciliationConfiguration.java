package com.sapienter.jbilling.server.config;

import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.sapienter.jbilling.server.spc.payment.reconciliation")
class SpcPaymentReconciliationConfiguration {

    @Bean
    FixedLengthTokenizer spcRecallPaymentRecordTokenizer() {
        FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();
        tokenizer.setStrict(false);
        tokenizer.setNames(new String [] {
                "Record Type", "PayWay Ref", "Transaction Type", "Payment Amount",
                "Originating System", "Receipt Number", "Voucher TraceNumber",
                "Extended ReceiptNumber", "Transaction Type",
                "Transaction Sequence No"
        });

        tokenizer.setColumns(new Range[] {
                new Range(1, 1), new Range(2, 30), new Range(31,31),
                new Range(32, 42), new Range(43, 44), new Range(45, 52),
                new Range(53, 68), new Range(69, 89), new Range(90, 93),
                new Range(94, 96)
        });
        return tokenizer;
    }

    @Bean
    DelimitedLineTokenizer spcCsvPaymentRecordTokenizer() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(",");
        tokenizer.setStrict(false);
        tokenizer.setNames(new String [] {
                "PayWayClientNumber", "MerchantId", "CardPAN", "CardCVN",
                "CardExpiry", "CustomerBankAccount", "YourBankAccount", "YourBankReference",
                "TransactionSource", "OrderType", "PrincipalAmount", "SurchargeAmount",
                "Amount", "Currency", "OrderNumber", "CustomerReferenceNumber",
                "CustomerName", "ECI", "User", "NoRetries",
                "OriginalOrderNumber", "OriginalCustomerReferenceNumber", "SummaryCode", "ResponseCode",
                "ResponseText", "ReceiptNumber", "SettlementDate", "CardSchemeName",
                "CreditGroup", "TransactionDateTime", "Status", "AuthorisationId",
                "FileName", "BPAY Ref", "BPAY Ref for Excel", "YourSurchargeAccount",
                "Custom Field 1", "Custom Field 2", "Custom Field 3", "Custom Field 4",
                "CustomerPayPalAccount", "YourPayPalAccount", "ParentTransactionReceiptNumber", "CustomerBankReference",
                "CustomerIpAddress", "FraudResult", "CustomerIpCountry", "CardCountry"
        });
        return tokenizer;
    }
}
