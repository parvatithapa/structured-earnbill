package com.sapienter.jbilling.server.mediation.converter.customMediations.performanceMediation;

/**
 * Created by marcolin on 15/04/16.
 */
public class SharedCounter {
    private static Integer counter = 0;

    public static void reset() {
        counter = 0;
    }

    public static synchronized Integer getNextCount() {
        counter = counter + 1;
        return counter;
    }
}
