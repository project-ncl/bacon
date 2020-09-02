/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.pnc.bacon.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Random;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author jbrazdil
 */
public class AbstractTest {
    private static final Logger log = LoggerFactory.getLogger(AbstractTest.class);

    private static WireMockServer wireMockServer;

    private static Random generator = new Random();

    protected CLIExecutor executor = new CLIExecutor();

    protected PNCWiremockHelper wmock = new PNCWiremockHelper();

    @BeforeAll
    public static void startWiremockServer() {
        wireMockServer = new WireMockServer(options().port(8080).notifier(new ConsoleNotifier(false)));
        wireMockServer.start();
    }

    @AfterAll
    public static void stopWiremockServer() {
        wireMockServer.stop();
    }

    @BeforeEach
    public void stubBabnner() {
        wmock.init();
    }

    @AfterAll
    public void clearStubs() {
        wireMockServer.resetAll();
    }

    public static String getRandomString() {
        byte[] bytes = new byte[6];
        generator.nextBytes(bytes);
        final String randomString = Base64.getEncoder().encodeToString(bytes);
        return randomString.replaceAll("[+/]", "-"); // some fields allow only [a-zA-Z0-9-]
    }

    protected void execute(String... args) {
        ExecutionResult result = executor.runCommand(args);
        assertThat(result.getOutput()).isEmpty();
        assertThat(result.getError()).isEmpty();
        assertThat(result.getRetval()).isZero();
    }

    protected <T> T executeAndDeserialize(Class<T> clazz, String... args) throws JsonProcessingException {
        ExecutionResult result = executor.runCommand(args);
        log.debug("stderr:{}{}", System.lineSeparator(), result.getError());
        assertThat(result.getRetval()).isZero();
        return result.fromYAML(clazz);
    }

    protected <T> T executeAndDeserializeJSON(Class<T> clazz, String... args) throws JsonProcessingException {
        ExecutionResult result = executor.runCommand(args);
        log.debug("stderr:{}{}", System.lineSeparator(), result.getError());
        assertThat(result.getRetval()).isZero();
        return result.fromYAML(clazz);
    }

    protected ExecutionResult executeAndGetResult(String... args) {
        return executor.runCommand(args);
    }

}
