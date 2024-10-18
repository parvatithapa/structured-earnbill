package com.sapienter.jbilling.server.util.time;

import org.junit.Test;

import java.time.LocalDate;
import static org.junit.Assert.*;
/**
 * Created by marcolin on 31/10/16.
 */
public class SemiMonthlyPeriodUnitTest {

    @Test public void testAddSemiMonthlyToStartOfMonth() {
        nidIs(2016, 1, 16).ifCustomerSubscribeOn(2016,1,1);
    }

    @Test public void testAddSemiMonthlyToHalfMonth() {
        nidIs(2016, 1, 29).ifCustomerSubscribeOn(2016,1, 14);
    }

    @Test public void testAddSemiMonthlyTo16OfMonth() {
        nidIs(2016, 2, 1).ifCustomerSubscribeOn(2016, 1, 16);
    }

    @Test public void testAddSemiMonthlyWithNonLeapYear() {
        nidIs(2015, 2, 28).ifCustomerSubscribeOn(2015, 2, 13);
        nidIs(2015, 2, 28).ifCustomerSubscribeOn(2015, 2, 14);
        nidIs(2015, 2, 28).ifCustomerSubscribeOn(2015, 2, 15);
    }

    @Test public void testAddSemiMonthlyWithLeapYear() {
        nidIs(2016, 2, 28).ifCustomerSubscribeOn(2016, 2, 13);
        nidIs(2016, 2, 29).ifCustomerSubscribeOn(2016, 2, 14);
        nidIs(2016, 2, 29).ifCustomerSubscribeOn(2016, 2, 15);
    }

    @Test public void testAddSemiMonthlyTo29OfOneMonth() {
        nidIs(2016, 3, 14).ifCustomerSubscribeOn(2016, 2, 29);
    }

    @Test public void testAddSemiMonthlyTo29OfFebruaryLeapYear() {
        nidIs(2016, 3, 13).ifCustomerSubscribeOn(2016, 2, 28);
    }

    @Test public void testAddSemiMonthlyTo29OfFebruaryNonLeapYear() {
        nidIs(2015, 3, 14).ifCustomerSubscribeOn(2015, 2, 28);
    }

    @Test public void testAddSemiMonthlyTo30ShouldGiveLessThan14NextMonth() {
        nidIs(2016, 4, 14).ifCustomerSubscribeOn(2016, 3, 30);
    }

    @Test public void testAddSemiMonthlyTo31ShouldGiveLessThan14NextMonth() {
        nidIs(2016, 4, 14).ifCustomerSubscribeOn(2016, 3, 31);
    }

    private TestHelper nidIs(int year, int month, int day) {
        return new TestHelper(year, month, day);
    }

    public class TestHelper {

        private int nidYear;
        private int nidMonth;
        private int nidDay;

        public TestHelper(int year, int month, int day) {
            nidYear = year;
            nidMonth = month;
            nidDay = day;
        }

        private void ifCustomerSubscribeOn(int year, int month, int day) {
            assertEquals(LocalDate.of(nidYear, nidMonth, nidDay), PeriodUnit.SEMI_MONTHLY.addTo(LocalDate.of(year, month, day), 1));
        }
    }

}
