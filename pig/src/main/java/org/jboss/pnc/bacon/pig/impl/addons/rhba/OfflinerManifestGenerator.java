package org.jboss.pnc.bacon.pig.impl.addons.rhba;

import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.pnc.BuildInfoCollector;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.prod.generator.pnc.Artifact;
import org.jboss.prod.generator.pnc.PncDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OfflinerManifestGenerator extends AddOn {

    private static final Logger log = LoggerFactory.getLogger(OfflinerManifestGenerator.class);

    public OfflinerManifestGenerator(
            PigConfiguration config,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath) {
        super(config, builds, releasePath, extrasPath);
    }

    @Override
    protected String getName() {
        return "offlinerManifestGenerator";
    }

    @Override
    public void trigger() {
        log.info("Will generate the offliner manifest");
        log.info("Release path: " + releasePath);
        String milestone = getAddOnConfiguration().get("productMilestone").toString();
        log.info("productMilestone: " + milestone);
        String commandListBrs = String.format(
                "list-build-records -q \"status==SUCCESS and productMilestone.id==%s\" --sort '=desc=id'",
                milestone);
        Set<Integer> buildRecords = PncDao.invokeAndGetResultIds(commandListBrs);
        String filename = releasePath + getAddOnConfiguration().get("manifestFile");
        List<String> exclusions = (ArrayList<String>) getAddOnConfiguration().get("exclusions");
        PrintWriter file = null;
        try {
            file = new PrintWriter(filename);
            List<String> offlinerGavs = new ArrayList<>();
            for (Integer buildRecord : buildRecords) {
                PncBuild build = BuildInfoCollector.getBuildData(buildRecord);
                BuildInfoCollector.addBuiltArtifacts(build);

                List<Map<String, ?>> artifactsAsMaps = new ArrayList<>();
                int pageIndex = 0;
                while (true) {
                    String command = String.format(
                            "list-built-artifacts-minimized -p %d --page-index %d %d",
                            3000,
                            pageIndex++,
                            buildRecord);
                    List<String> output = PncDao.invoke(command, 4);

                    if (output.isEmpty()) {
                        break;
                    } else {
                        artifactsAsMaps.addAll(parseList(output));
                    }
                }

                offlinerGavs.addAll(
                        artifactsAsMaps.stream()
                                .map(Artifact::new)
                                .map(Artifact::getGapv)
                                .collect(Collectors.toList()));

                List<Map<String, ?>> depsAsMaps = new ArrayList<>();
                pageIndex = 0;
                while (true) {
                    String command = String.format(
                            "list-dependency-artifacts-minimized -p %d --page-index %d %d",
                            10000,
                            pageIndex++,
                            buildRecord);
                    List<String> output = PncDao.invoke(command, 4);

                    if (output.isEmpty()) {
                        break;
                    } else {
                        depsAsMaps.addAll(parseList(output));
                    }
                }
                offlinerGavs.addAll(
                        depsAsMaps.stream().map(Artifact::new).map(Artifact::getGapv).collect(Collectors.toList()));
            }
            // offlinerGavs.addAll(build.getBuiltArtifacts().stream().map(Artifact::getGapv).collect(Collectors.toList()));
            // BuildInfoCollector.addDependencies(build, "",
            // Integer.parseInt(getConfig().get("offlinerPageSize").toString()));
            // offlinerGavs.addAll(build.getDependencyArtifacts().stream().map(Artifact::getGapv).collect(Collectors.toList()));
            List<String> offlinerGavsNoDups = offlinerGavs.stream().distinct().collect(Collectors.toList());

            log.info("No duplicates: " + offlinerGavsNoDups.size());
            offlinerGavsNoDups.removeIf(artifact -> {
                for (String exclusion : exclusions) {
                    if (Pattern.matches(exclusion, artifact)) {
                        log.info("Excluded " + artifact);
                        return true;
                    }
                }
                return false;
            });
            for (String artifact : offlinerGavsNoDups) {
                file.println(artifact);
            }
        } catch (Exception e) {
            log.error("Error generating the offliner manifest", e);
            return;
        } finally {
            if (file != null) {
                file.flush();
                file.close();
            }
        }

    }

}
