package org.jboss.pnc.bacon.test;

/**
 * If a test is supposed to be executed either only with mocks or only with real service, add one of the tags to the
 * test
 *
 * If the test should run regardless of environment, no tag is required
 */
public class TestType {
    public static final String MOCK_ONLY = "MockOnly";
    public static final String REAL_SERVICE_ONLY = "RealServiceOnly";

    private TestType() {
    }
}
