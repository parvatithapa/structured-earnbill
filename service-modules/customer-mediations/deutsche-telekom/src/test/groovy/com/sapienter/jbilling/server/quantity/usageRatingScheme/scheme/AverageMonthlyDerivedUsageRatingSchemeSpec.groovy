package com.sapienter.jbilling.server.quantity.usageRatingScheme.scheme

import com.sapienter.jbilling.server.item.PricingField
import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.DtConstants
import com.sapienter.jbilling.server.usageRatingScheme.scheme.IUsageRatingScheme
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.math.RoundingMode


class AverageMonthlyDerivedUsageRatingSchemeSpec extends Specification {

    @Shared
    IUsageRatingScheme ratingScheme

    def setupSpec() {
        ratingScheme = new AverageMonthlyDerivedUsageRatingScheme()
    }

    @Unroll("#unrollDescription")
    def "average consumption needs to be calculated given the raw data usage, begin time and end time"() {

        List<PricingField> fields = createPricingFields(fieldsData)

        BigDecimal expected = quantity
                .multiply(actualTimeInSecs)
                .divide(getSecondsPerMonth(), DtConstants.PRECISION, RoundingMode.UP)

        when:
        BigDecimal result = ratingScheme.compute(null, quantity, null, fields)

        then:
        result == expected

        where:
        fieldsData << [ [ [ name: "BeginTime", value: "20180228210000" ], [ name: "EndTime", value: "20180228215959" ] ],   //  1 hr
                        [ [ name: "BeginTime", value: "20180228210000" ], [ name: "EndTime", value: "20180228215959" ] ],   //  1 hr
                        [ [ name: "BeginTime", value: "20180228210000" ], [ name: "EndTime", value: "20180228214459" ] ] ]  // 45 minutes

        actualTimeInSecs << [ new BigDecimal(3600),
                              new BigDecimal(3600),
                              new BigDecimal(2700) ]

        quantity << [ new BigDecimal(250).divide(new BigDecimal(1024), DtConstants.PRECISION, RoundingMode.UP),  //  250 mb
                      new BigDecimal(1.5),                                                                       //  1.5 gb
                      new BigDecimal(750).divide(new BigDecimal(1024), DtConstants.PRECISION, RoundingMode.UP) ] //  800 mb

        unrollDescription = "Data in GB: $quantity, Duration in secs: $actualTimeInSecs"
    }

    private BigDecimal getSecondsPerMonth() {
        int daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
        return new BigDecimal(daysInMonth * 24 * 3600)
    }

    List<PricingField> createPricingFields(List<Map> fieldsData) {
        return fieldsData.collect {
            field ->
                new PricingField(field['name'], field['value'])
        }
    }
}
