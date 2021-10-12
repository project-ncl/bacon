package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import org.assertj.core.util.Files;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.ConfigProfile;
import org.jboss.pnc.bacon.config.DaConfig;
import org.jboss.pnc.bacon.config.PigConfig;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.config.Flow;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationData;
import org.jboss.pnc.bacon.pig.impl.documents.Deliverables;
import org.jboss.pnc.bacon.pig.impl.repo.RepositoryData;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;

public class QuarkusCommunityDepAnalyzerTest {

    public static final String REPO_ZIP_PATH = null;
    public static final String INDY_URL = null;
    public static final String DA_URL = null;

    @Test
    @Disabled
    void test() {
        File tempFolder = Files.newTemporaryFolder();
        System.out.println(tempFolder.getAbsolutePath());

        File extras = new File(tempFolder, "extras");
        extras.mkdir();

        RepositoryData repositoryData = new RepositoryData();
        repositoryData.setRepositoryPath(Paths.get(REPO_ZIP_PATH));
        PigContext context = new PigContext();
        PigConfiguration pigConfig = new PigConfiguration();
        Flow flow = new Flow();
        RepoGenerationData repositoryGeneration = new RepoGenerationData();
        repositoryGeneration.setBomArtifactId("quarkus-bom");
        flow.setRepositoryGeneration(repositoryGeneration);
        pigConfig.setFlow(flow);
        Map<String, Map<String, ?>> addons = new HashMap<>();
        Map<String, Object> depAnalyzerConfig = new HashMap<>();
        depAnalyzerConfig
                .put("skippedExtensions", asList("quarkus-resteasy-reactive-kotlin", "quarkus-mongodb-client"));
        addons.put(QuarkusCommunityDepAnalyzer.NAME, depAnalyzerConfig);
        pigConfig.setAddons(addons);
        context.setPigConfiguration(pigConfig);
        context.setRepositoryData(repositoryData);
        PigContext.setInstance(context);

        Config instance = new Config();

        ConfigProfile configProfile = new ConfigProfile();
        PigConfig pig = new PigConfig();
        pig.setIndyUrl(INDY_URL);
        configProfile.setPig(pig);

        DaConfig da = new DaConfig();
        da.setUrl(DA_URL);
        configProfile.setDa(da);
        instance.setActiveProfile(configProfile);

        Config.setInstance(instance);

        QuarkusCommunityDepAnalyzer analyzer = new QuarkusCommunityDepAnalyzer(
                pigConfig,
                Collections.emptyMap(),
                tempFolder.getAbsolutePath(),
                extras.getAbsolutePath(),
                new Deliverables());
        analyzer.trigger();
    }
}
