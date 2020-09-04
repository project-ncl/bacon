/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
