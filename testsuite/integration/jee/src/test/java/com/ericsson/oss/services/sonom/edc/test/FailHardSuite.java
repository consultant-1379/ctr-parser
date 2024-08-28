/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------
 */

package com.ericsson.oss.services.sonom.edc.test;

import org.junit.internal.TextListener;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

/**
 * {@link FailHardSuite} class responsible to fail hard when a failure happens during the execution of integration tests.
 * <p>
 * Whenever a test fails it will stop the execution of the coming tests and will throw {@link StoppedByUserException}.
 */
public class FailHardSuite extends Suite {

    public FailHardSuite(final Class<?> klass, final RunnerBuilder builder) throws InitializationError {
        super(klass, builder);
    }

    @Override
    public void run(final RunNotifier notifier) {
        notifier.addListener(new FailHardListener(notifier));
        notifier.addListener(new TextListener(System.out));
        super.run(notifier);
    }

    private static final class FailHardListener extends RunListener {
        private final RunNotifier notifier;

        private FailHardListener(final RunNotifier notifier) {
            super();
            this.notifier = notifier;
        }

        @Override
        public void testFailure(final Failure failure) {
            failure.getException().printStackTrace(System.out);

            notifier.pleaseStop();
        }
    }
}
