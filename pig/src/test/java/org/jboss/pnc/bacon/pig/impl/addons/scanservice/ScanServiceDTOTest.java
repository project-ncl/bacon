package org.jboss.pnc.bacon.pig.impl.addons.scanservice;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ScanServiceDTOTest {

    // private PncBuild pncBuild;
    private Map<String, PncBuild> builds;
    static EasyRandom easyRandom = new EasyRandom();

    @BeforeEach
    public void init() {
        builds = new HashMap<>();

        for (int i = 0; i < 5; i++) {
            PncBuild b = easyRandom.nextObject(PncBuild.class);
            b.setInternalScmUrl("https://github.com/michalszynkiewicz/demo-bar");
            b.setScmRevision("1.0.1");
            builds.put(easyRandom.nextObject(String.class), b);
        }

    }

    public String getSerializedDTO(ScanServiceDTO ss) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(ss);
    }

    public static ScanServiceDTO dummyScanService() {

        ScanServiceDTO ss = new ScanServiceDTO();
        ss.setProductId("23");
        ss.setEventId(ScanServiceDTO.EVENT_ID_DFT_VALUE);
        ss.setIsManagedService(ScanServiceDTO.IS_MANAGED_SERVICE_DFT_VALUE);
        ss.setCpaasVersion(ScanServiceDTO.CPAAS_VERSION_DFT_VALUE);
        ss.setJobUrl(ScanServiceDTO.JOB_URL_DFT_VALUE);
        return ss;
    }

    @Test
    public void fromPncBuild() {
        ScanServiceDTO ss = new ScanServiceDTO(
                "productId",
                "eventId",
                true,
                "cpaasVersion",
                "jobUrl",
                null,
                null,
                builds);
        try {
            String jsonDTO = getSerializedDTO(ss);
            System.out.println("Test fromPncBuild: " + jsonDTO);
        } catch (JsonProcessingException e) {
            fail("ScanSerivce can't be instantiated as json");
        }
    }

    @Test
    public void fromPncBrewGit() {
        List<Integer> brewBuilds = Arrays.asList(easyRandom.nextInt(10000), easyRandom.nextInt(10000));
        Map<String, String> scmUrls = Stream
                .of(
                        new String[][] {
                                { "scmUrl", "https://github.com/michalszynkiewicz/demo-bar" },
                                { "scmRevision", "1.0.1" }, })
                .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));

        ArrayList<Map<String, String>> scmUrlsL = new ArrayList<>();
        scmUrlsL.add(scmUrls);

        ScanServiceDTO ss = new ScanServiceDTO(
                "productId",
                "eventId",
                true,
                "cpaasVersion",
                "jobUrl",
                brewBuilds,
                scmUrlsL,
                builds);
        try {
            String jsonDTO = getSerializedDTO(ss);
            System.out.println("Test fromPncBuild: " + jsonDTO);
        } catch (JsonProcessingException e) {
            fail("ScanSerivce can't be instantiated as json");
        }
    }

    @Test
    public void brewID() {
        ScanServiceDTO ss = ScanServiceDTOTest.dummyScanService();
        ss.addBrewBuild(easyRandom.nextInt(10000));
    }

    /*
     * @Test public void brewIDs() { ScanServiceDTO ss = this.dummyScanService(); ss.addBrewBuild(1234); }
     */

    @Test
    public void scmURL() {
        ScanServiceDTO ss = ScanServiceDTOTest.dummyScanService();
        ss.addSCMUrl("https://github.com/michalszynkiewicz/demo-bar#1.0.1");
        ss.addSCMUrl("https://github.com/michalszynkiewicz/demo-bar.git", "1.0.1");
        try {
            String jsonDTO = getSerializedDTO(ss);
            System.out.println("Test scmURL: " + jsonDTO);
        } catch (JsonProcessingException e) {
            fail("Cant produce json from SCMURL or repo/ref strings");
        }
    }

    @Test
    public void pncBrewAndGit() {
        ScanServiceDTO ss = ScanServiceDTOTest.dummyScanService();
        ss.addBrewBuild(easyRandom.nextInt(10000));
        ss.addPncBuilds(builds);
        ss.addSCMUrl("https://github.com/michalszynkiewicz/demo-bar.git", "1.0.1");
        try {
            String jsonDTO = getSerializedDTO(ss);
            System.out.println("Test pncBrewAndGit: " + jsonDTO);
        } catch (JsonProcessingException e) {
            fail("Cant produce json from combined component list");
        }
    }

    @Test
    public void fromSCMUrl() {
        ScanServiceDTO ss = ScanServiceDTOTest.dummyScanService();
        ss.addSCMUrl("https://github.com/michalszynkiewicz/demo-bar.git#1.0.1");
        try {
            String jsonDTO = getSerializedDTO(ss);
            System.out.println("Test SCMURL: " + jsonDTO);
        } catch (JsonProcessingException e) {
            fail("Cant produce json from combined component list");
        }
    }

    @Test
    public void fromWeirdSCMUrl() {
        ScanServiceDTO ss = ScanServiceDTOTest.dummyScanService();
        try {
            ss.addSCMUrl("https://github.com/michalszynkiewicz/demo-bar.git#1.0.1#22");
            fail("Expected an exception to be thrown for multiple url fragments");
        } catch (RuntimeException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void fromWeirdSCMRev() {
        ScanServiceDTO ss = ScanServiceDTOTest.dummyScanService();
        try {
            ss.addSCMUrl("https://github.com/michalszynkiewicz/demo-bar.git", "1.0.1#22");
        } catch (RuntimeException e) {
            fail("We should handle # (URL fragment) in SCM + Rev constructor");
        }
    }

    @Test
    void dumpDTO() throws JsonProcessingException {
        System.out.println(PostBuildScanService.showDTOfields(ScanServiceDTOTest.dummyScanService()));
    }
}
