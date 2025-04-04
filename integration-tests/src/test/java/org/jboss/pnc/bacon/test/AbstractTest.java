/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.pnc.bacon.test;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;

/**
 *
 * @author jbrazdil
 */
public class AbstractTest {
    private static final Logger log = LoggerFactory.getLogger(AbstractTest.class);

    private static WireMockServer wireMockServer;

    private static Random generator = new Random();

    protected PNCWiremockHelper wmock = new PNCWiremockHelper();

    @BeforeAll
    static void startWiremockServer() {
        wireMockServer = new WireMockServer(options().port(8080).notifier(new ConsoleNotifier(false)));
        wireMockServer.start();
    }

    @AfterAll
    static void stopWiremockServer() {
        wireMockServer.stop();
    }

    @BeforeEach
    void stubBanner() {
        wmock.init();
    }

    @AfterAll
    static void clearStubs() {
        wireMockServer.resetAll();
    }

    protected static String getRandomString() {
        byte[] bytes = new byte[6];
        generator.nextBytes(bytes);
        final String randomString = Base64.getEncoder().encodeToString(bytes);
        return randomString.replaceAll("[+/]", "-"); // some fields allow only [a-zA-Z0-9-]
    }

    protected void execute(String... args) {
        ExecutionResult result = CLIExecutor.runCommand(args);
        assertThat(result.getOutput()).isEmpty();
        assertThat(result.getError().replaceAll("Bacon version.*\\)[\\n]*", "")).isEmpty();
        assertThat(result.getRetval()).isZero();
    }

    protected <T> T executeAndDeserialize(Class<T> clazz, String... args) throws JsonProcessingException {
        ExecutionResult result = CLIExecutor.runCommand(args);
        log.debug("stderr:{}{}", System.lineSeparator(), result.getError());
        assertThat(result.getRetval()).isZero();
        return result.fromYAML(clazz);
    }

    protected <T> T executeAndDeserializeJSON(Class<T> clazz, String... args) throws JsonProcessingException {
        ExecutionResult result = CLIExecutor.runCommand(args);
        log.debug("stderr:{}{}", System.lineSeparator(), result.getError());
        assertThat(result.getRetval()).isZero();
        return result.fromYAML(clazz);
    }

    protected ExecutionResult executeAndGetResult(List<String> env, String... args) {
        return CLIExecutor.runCommand(env, args);
    }

}
