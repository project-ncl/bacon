package org.jboss.pnc.bacon.pig.impl.validation;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.bacon.pig.impl.config.JavadocGenerationStrategy;
import org.jboss.pnc.bacon.pig.impl.config.LicenseGenerationStrategy;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationStrategy;
import org.jboss.pnc.enums.BuildType;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

class PigConfigurationValidationTest {

    static Validator validator;
    PigConfiguration generated;

    @BeforeAll
    static void setupAll() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @BeforeEach
    void setup() {
        EasyRandom easyRandom = new EasyRandom();

        BuildConfig buildConfig = easyRandom.nextObject(BuildConfig.class);
        buildConfig.setBuildType(BuildType.MVN.toString());
        generated = new EasyRandom().nextObject(PigConfiguration.class);
        generated.setBuilds(Lists.newArrayList(buildConfig));
    }

    @Test
    void defaultGeneratedPigConfigurationIsValid() {
        // no violations!
        assertThat(validator.validate(generated)).isEmpty();
    }

    @Test
    void testProductNotNull() {

        // when product is null
        generated.setProduct(null);
        Set<ConstraintViolation<PigConfiguration>> violations = validator.validate(generated);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.stream().findFirst().get().getPropertyPath().toString()).isEqualTo("product");

    }

    @Test
    void testProductContentIsNotBlank() {

        // when name is null
        generated.getProduct().setName(null);
        // then
        test(validator.validate(generated), "product.name");

        // when name is empty
        generated.getProduct().setName("");
        // then
        test(validator.validate(generated), "product.name");

        // when abbreviation is null
        generated.getProduct().setName("Mumbo");
        generated.getProduct().setAbbreviation(null);
        // then
        test(validator.validate(generated), "product.abbreviation");

        // when abbreviation is empty
        generated.getProduct().setName("Mumbo");
        generated.getProduct().setAbbreviation("");
        // then
        test(validator.validate(generated), "product.abbreviation");
    }

    @Test
    void versionMilestoneGroupNotBlank() {
        // when version is null
        generated.setVersion(null);
        // then
        test(validator.validate(generated), "version");

        // when version is empty
        generated.setVersion("");
        // then
        test(validator.validate(generated), "version");

        generated.setVersion("1.2.3");

        // when milestone is null
        generated.setMilestone(null);
        // then
        test(validator.validate(generated), "milestone");

        // when milestone is empty
        generated.setMilestone("");
        // then
        test(validator.validate(generated), "milestone");

        generated.setMilestone("DR1");

        // when group is null
        generated.setGroup(null);
        // then
        test(validator.validate(generated), "group");

        // when group is empty
        generated.setGroup("");
        // then
        test(validator.validate(generated), "group");
    }

    @Test
    void testBuildConfigCannotBeEmpty() {
        // when output is empty
        generated.setBuilds(Collections.EMPTY_LIST);
        // then
        test(validator.validate(generated), "builds");
    }

    @Test
    void testOutputPrefixesNotNull() {
        // when output is null
        generated.setOutputPrefixes(null);
        // then
        test(validator.validate(generated), "outputPrefixes");
    }

    @Test
    void testOutputPrefixesContentNotBlank() {
        // when releaseDir is null
        generated.getOutputPrefixes().setReleaseDir(null);
        // then
        test(validator.validate(generated), "outputPrefixes.releaseDir");

        // when releaseDir is empty
        generated.getOutputPrefixes().setReleaseDir("");
        // then
        test(validator.validate(generated), "outputPrefixes.releaseDir");

        generated.getOutputPrefixes().setReleaseDir("hi");

        // when releaseFile is null
        generated.getOutputPrefixes().setReleaseFile(null);
        // then
        test(validator.validate(generated), "outputPrefixes.releaseFile");

        // when releaseFile is empty
        generated.getOutputPrefixes().setReleaseFile(" ");
        // then
        test(validator.validate(generated), "outputPrefixes.releaseFile");
    }

    @Test
    void flowDataMustNotBeNull() {
        // when
        generated.setFlow(null);
        // then
        test(validator.validate(generated), "flow");
    }

    @Test
    void flowDataMustBeValidIfJavadocStrategyIsDownload() {
        // when
        generated.getFlow().getJavadocGeneration().setStrategy(JavadocGenerationStrategy.DOWNLOAD);
        generated.getFlow().getJavadocGeneration().setSourceBuild(null);

        // then
        test(validator.validate(generated), "flow.javadocGeneration");
    }

    @Test
    void flowDataMustBeValidIfRepoStrategyIsDownload() {
        // when
        generated.getFlow().getRepositoryGeneration().setStrategy(RepoGenerationStrategy.DOWNLOAD);
        generated.getFlow().getRepositoryGeneration().setSourceBuild(null);

        // then
        test(validator.validate(generated), "flow.repositoryGeneration");
    }

    @Test
    void flowDataMustBeValidIfLicenseStrategyIsDownload() {
        // when
        generated.getFlow().getLicensesGeneration().setStrategy(LicenseGenerationStrategy.DOWNLOAD);
        generated.getFlow().getLicensesGeneration().setSourceArtifact(null);

        // then
        test(validator.validate(generated), "flow.licensesGeneration");
    }

    private void test(Set<ConstraintViolation<PigConfiguration>> violations, String expectedPropertyPath) {

        assertThat(violations).hasSize(1);
        assertThat(violations.stream().findFirst().get().getPropertyPath()).hasToString(expectedPropertyPath);
    }
}
