#*------------------------------------------------------------------------------
#******************************************************************************
# COPYRIGHT Ericsson 2022
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.
#******************************************************************************
#------------------------------------------------------------------------------

@RunAllTests
Feature: Full_Edc_Deployment

    Scenario: IDUN-12702 - [EDC] IT tests - create java tests
    This scenario tests the end to end integration for the full EDC deployment

        Given EDC is fully deployed
        When InputMessages are sent to EDC Deployment
        Then Expect 10223 Events on Output Topic