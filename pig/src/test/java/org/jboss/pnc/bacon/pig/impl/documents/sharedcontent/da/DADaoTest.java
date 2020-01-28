/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da;

import org.apache.commons.lang.StringUtils;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.testcommon.RemoteTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 24/04/2019
 */
@RemoteTest
class DADaoTest {

    private static DADao daDao;

    @BeforeAll
    private static void init() throws IOException {
        String configLocation = System.getProperty("configLocation");
        if (StringUtils.isBlank(configLocation)) {
            fail("configLocation system property pointing to a config yaml has to be defined to run " + DADaoTest.class);
        }
        Config.configure(configLocation);
        Config.initialize();

        daDao = DADao.getInstance();
    }

    @Test
    public void shouldLookupJunit412() {
        CommunityDependency dependency = new CommunityDependency("junit", "junit", "4.12", "jar");
        daDao.fillDaData(dependency);
        assertThat(dependency.getRecommendation()).isEqualTo("4.12.0.redhat-003");
        assertThat(dependency.getState()).isEqualTo(DependencyState.MATCH_FOUND);
    }

    @Test
    public void shouldLookupJunit456() {
        CommunityDependency dependency = new CommunityDependency("junit", "junit", "4.56", "jar");
        daDao.fillDaData(dependency);
        assertThat(dependency.getRecommendation()).isNull();
        assertThat(dependency.getState()).isEqualTo(DependencyState.REVERSION_POSSIBLE);
    }

    @Test
    public void shouldLookupJunity456() {
        CommunityDependency dependency = new CommunityDependency("junit", "junity", "4.56", "jar");
        daDao.fillDaData(dependency);
        assertThat(dependency.getRecommendation()).isNull();
        assertThat(dependency.getState()).isEqualTo(DependencyState.NO_MATCH);
    }

}