package org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da;

import org.apache.commons.lang.StringUtils;
import org.jboss.pnc.bacon.config.Config;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 24/04/2019
 */
class DADaoTest {

    private static DADao daDao;

    @BeforeAll
    private static void init() throws IOException {
        String configLocation = System.getProperty("configLocation");
        if (StringUtils.isBlank(configLocation)) {
            fail("configLocation system property pointing to a config yaml has to be defined to run " + DADaoTest.class);
        }
        Config.initialize(configLocation);

        daDao = DADao.getInstance();
    }

    @Test
    public void shouldLookupJunit412() {
        CommunityDependency dependency =
                new CommunityDependency("junit", "junit", "4.12", "jar");
        daDao.fillDaData(dependency);
        assertThat(dependency.getRecommendation()).isEqualTo("4.12.0.redhat-003");
        assertThat(dependency.getState()).isEqualTo(DependencyState.MATCH_FOUND);
    }
    @Test
    public void shouldLookupJunit456() {
        CommunityDependency dependency =
                new CommunityDependency("junit", "junit", "4.56", "jar");
        daDao.fillDaData(dependency);
        assertThat(dependency.getRecommendation()).isNull();
        assertThat(dependency.getState()).isEqualTo(DependencyState.REVERSION_POSSIBLE);
    }
    @Test
    public void shouldLookupJunity456() {
        CommunityDependency dependency =
                new CommunityDependency("junit", "junity", "4.56", "jar");
        daDao.fillDaData(dependency);
        assertThat(dependency.getRecommendation()).isNull();
        assertThat(dependency.getState()).isEqualTo(DependencyState.NO_MATCH);
    }

}