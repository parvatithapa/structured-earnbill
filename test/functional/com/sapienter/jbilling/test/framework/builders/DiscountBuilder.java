package com.sapienter.jbilling.test.framework.builders;

import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;

import java.util.Date;

/**
 * Created by hristijan on 6/21/16.
 */
public class DiscountBuilder extends AbstractBuilder{

    private String code;
    private String type;
    private String description;
    private String rate;
    private Date startDate;
    private Date endDate;

    public DiscountBuilder(JbillingAPI api, TestEnvironment testEnvironment){
        super(api,testEnvironment);
    }

    public DiscountBuilder withCodeForTests(String codeForTest) {
        this.code = codeForTest;
        return this;
    }

    public static DiscountBuilder getBuilder(JbillingAPI api, TestEnvironment testEnvironment){
        return new DiscountBuilder(api,testEnvironment);
    }

    public DiscountBuilder withDescription(String description){
        this.description = description;
        return this;
    }

    public DiscountBuilder withType(String type){
        this.type = type;
        return this;
    }

    public DiscountBuilder withRate(String rate){
        this.rate = rate;
        return this;
    }

    public DiscountBuilder withStartDate(Date startDate){
        this.startDate = startDate;
        return this;
    }

    public DiscountBuilder withEndDate(Date endDate){
        this.endDate = endDate;
        return this;
    }

    public Integer build(){
        DiscountWS discountWS = new DiscountWS();

        discountWS.setCode(code);
        if (description != null) {
            discountWS.setDescription(description);
        }

        discountWS.setRate(rate);
        discountWS.setType(type);
        discountWS.setEntityId(api.getCallerCompanyId());
        discountWS.setStartDate(startDate);
        discountWS.setEndDate(endDate);
        Integer discountId = api.createOrUpdateDiscount(discountWS);
        if (testEnvironment != null && description != null) {
            testEnvironment.add(code, discountId, description, api, TestEntityType.DISCOUNT);
        }

        return discountId;
    }

    public DiscountLineBuilder dicountLine(){
        return new DiscountLineBuilder();
    }

    public class DiscountLineBuilder{

        private Integer discountId;
        private String description;
        private Integer itemId;

        public DiscountLineBuilder withDiscountId(Integer discountId){
            this.discountId = discountId;
            return this;
        }

        public DiscountLineBuilder withDescription(String description){
            this.description = description;
            return this;
        }

        public DiscountLineBuilder withItemId(Integer itemId){
            this.itemId = itemId;
            return this;
        }

        public DiscountLineWS build(){

            DiscountLineWS discountLineWS = new DiscountLineWS();
            discountLineWS.setDiscountId(discountId);
            discountLineWS.setDescription(description);
            discountLineWS.setItemId(itemId);

            return discountLineWS;
        }
    }
}