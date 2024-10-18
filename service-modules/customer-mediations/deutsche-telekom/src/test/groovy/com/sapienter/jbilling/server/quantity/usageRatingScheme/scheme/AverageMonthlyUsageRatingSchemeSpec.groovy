package com.sapienter.jbilling.server.quantity.usageRatingScheme.scheme

import com.sapienter.jbilling.server.item.PricingField
import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.DtConstants
import com.sapienter.jbilling.server.usageRatingScheme.domain.IUsageRatingSchemeModel
import com.sapienter.jbilling.server.usageRatingScheme.scheme.IUsageRatingScheme
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.math.RoundingMode


class AverageMonthlyUsageRatingSchemeSpec extends Specification {

    @Shared
    IUsageRatingScheme ratingScheme

    @Shared
    String ATTR_INPUT_UNITS

    @Shared
    String ATTR_OUTPUT_UNITS

    def setupSpec() {
        ratingScheme = new AverageMonthlyUsageRatingScheme()
        ATTR_INPUT_UNITS = ratingScheme.fixedAttributes[0].name
        ATTR_OUTPUT_UNITS = ratingScheme.fixedAttributes[1].name
    }

    @Unroll("#unrollDescription")
    def "average consumption needs to be calculated given the raw data usage and duration"() {

        IUsageRatingSchemeModel model = new UsageRatingSchemeModel(attrs)
        List<PricingField> fields = createPricingFields(fieldsData)

        BigDecimal expected = data
                .multiply(quantity)
                .divide(getHoursPerMonth(), DtConstants.PRECISION, RoundingMode.UP)

        when:
            BigDecimal result = ratingScheme.compute(model, quantity, null, fields)

        then:
            result == expected

        where:
            fieldsData << [ [ [ name: "ExtendParams", value: "262144000" ] ],    // 250 mb
                            [ [ name: "ExtendParams", value: "1610612736"] ],    // 1.5 gb
                            [ [ name: "ExtendParams", value: "838860800" ] ] ]   // 800 mb

            attrs << [ [ ("$ATTR_INPUT_UNITS" as String): "Byte", ("$ATTR_OUTPUT_UNITS" as String): "GB" ],
                       [ ("$ATTR_INPUT_UNITS" as String): "Byte", ("$ATTR_OUTPUT_UNITS" as String): "GB" ],
                       [ ("$ATTR_INPUT_UNITS" as String): "Byte", ("$ATTR_OUTPUT_UNITS" as String): "GB" ] ]

            // actual data after conversion: based on ATTR_INPUT_UNITS (here Byte to GB)
            data << [ new BigDecimal(262144000).divide(1024 * 1024 * 1024, DtConstants.PRECISION, RoundingMode.UP),
                      new BigDecimal(1610612736).divide(1024 * 1024 * 1024, DtConstants.PRECISION, RoundingMode.UP),
                      new BigDecimal(838860800).divide(1024 * 1024 * 1024, DtConstants.PRECISION, RoundingMode.UP) ]

            quantity << [ new BigDecimal(1),        //  1 hour
                          new BigDecimal(1),        //  1 hour
                          new BigDecimal(0.75) ]    // 45 minutes

            unrollDescription = "Data in ${attrs[ATTR_INPUT_UNITS]}: ${fieldsData[0]['value']}, " +
                    "Duration in hour: $quantity"
    }

    private BigDecimal getHoursPerMonth() {
        int daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
        return new BigDecimal(daysInMonth * 24)
    }

    List<PricingField> createPricingFields(List<Map> fieldsData) {
        return fieldsData.collect {
            field ->
                new PricingField(field['name'], field['value'])
        }
    }

    class UsageRatingSchemeModel implements IUsageRatingSchemeModel {

        def fixedAttributes

        UsageRatingSchemeModel(fixedAttributes) {
            this.fixedAttributes = fixedAttributes
        }

        @Override
        Map<String, String> getFixedAttributes() {
            return fixedAttributes
        }

        @Override
        String getRatingSchemeCode() { return _ }

        @Override
        IUsageRatingScheme getRatingScheme() { return _ }

        @Override
        List<Map<String, String>> getDynamicAttributes() { return _ }
    }
}
