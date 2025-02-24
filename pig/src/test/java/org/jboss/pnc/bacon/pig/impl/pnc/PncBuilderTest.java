package org.jboss.pnc.bacon.pig.impl.pnc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jboss.pnc.client.GroupBuildClient;
import org.jboss.pnc.client.GroupConfigurationClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.BuildConfigurationRef;
import org.jboss.pnc.dto.GroupBuild;
import org.jboss.pnc.dto.GroupConfiguration;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PncBuilderTest {

    private GroupBuildClient groupBuildClient;
    private GroupConfigurationClient groupConfigurationClient;

    private EasyRandom easyRandom = new EasyRandom();

    @BeforeEach
    void setup() {
        groupBuildClient = mock(GroupBuildClient.class);
        groupConfigurationClient = mock(GroupConfigurationClient.class);
    }

    @Test
    void getCountOfBuildConfigsForGroupBuild() throws Exception {

        Map<String, BuildConfigurationRef> buildConfigurations = new HashMap<>();
        BuildConfigurationRef bc1 = easyRandom.nextObject(BuildConfigurationRef.class);
        BuildConfigurationRef bc2 = easyRandom.nextObject(BuildConfigurationRef.class);
        BuildConfigurationRef bc3 = easyRandom.nextObject(BuildConfigurationRef.class);
        buildConfigurations.put(bc1.getName(), bc1);
        buildConfigurations.put(bc2.getName(), bc2);
        buildConfigurations.put(bc3.getName(), bc3);

        String groupConfigurationId = "5";
        String groupBuildId = "1";

        GroupConfiguration gc = GroupConfiguration.builder()
                .id(groupConfigurationId)
                .buildConfigs(buildConfigurations)
                .build();
        GroupBuild gb = GroupBuild.builder().id(groupBuildId).groupConfig(gc).build();

        when(groupBuildClient.getSpecific(groupBuildId)).thenReturn(gb);
        when(groupConfigurationClient.getBuildConfigs(groupConfigurationId))
                .thenReturn(new RemoteCollection<BuildConfiguration>() {
                    @Override
                    public int size() {
                        return buildConfigurations.size();
                    }

                    @Override
                    public Collection<BuildConfiguration> getAll() {
                        return null;
                    }

                    @Override
                    public Iterator<BuildConfiguration> iterator() {
                        return null;
                    }
                });

        try (PncBuilder builder = new PncBuilder(
                groupBuildClient,
                groupBuildClient,
                groupConfigurationClient,
                groupConfigurationClient)) {
            assertEquals(buildConfigurations.size(), builder.getCountOfBuildConfigsForGroupBuild(groupBuildId));
        }
    }
}
