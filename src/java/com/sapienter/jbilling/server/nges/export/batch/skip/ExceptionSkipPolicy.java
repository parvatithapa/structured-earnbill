/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2016] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.nges.export.batch.skip;

import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;

/**
 * Spring Batch uses a default skip policy implementation ( LimitCheckingItemSkipPolicy ), but
 * we want to declare our own skip policy as a Spring bean and plug it into your step. This
 * gives you more control if the skippable-exception-classes and skip-limit pair
 * isn’t enough.
 * <p>
 * Let’s say you know exactly on which exceptions you want to skip items, but you don’t
 * care about the number of skipped items. You can implement your own skip policy, as
 * shown in the following listing.
 *
 * @author Hitesh Yadav
 * @version 1.0
 * @since 2016-08-16
 */

public class ExceptionSkipPolicy implements SkipPolicy {

    private Class<? extends Exception> exceptionClassToSkip;

    public ExceptionSkipPolicy(
            Class<? extends Exception> exceptionClassToSkip) {
        super();
        this.exceptionClassToSkip = exceptionClassToSkip;
    }

    @Override
    public boolean shouldSkip(Throwable t, int skipCount)
            throws SkipLimitExceededException {
        return exceptionClassToSkip.isAssignableFrom(
                t.getClass()
        );
    }
}
