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

import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith( Cucumber.class )
@CucumberOptions(
        features = "classpath:features",
        glue = {
                "com.ericsson.oss.services.sonom.edc.test.steps",
                "com.ericsson.oss.services.sonom.edc.test.helpers"
        },
        plugin = {
                "pretty"
        },
        tags = "@RunAllTests"
)
public class EdcIT {

}