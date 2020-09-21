package org.jboss.pnc.bacon.pig.impl.validation;

import org.jboss.pnc.bacon.pig.impl.config.GenerationData;
import org.jboss.pnc.bacon.pig.impl.config.JavadocGenerationData;
import org.jboss.pnc.bacon.pig.impl.config.JavadocGenerationStrategy;
import org.jboss.pnc.bacon.pig.impl.config.LicenseGenerationData;
import org.jboss.pnc.bacon.pig.impl.config.LicenseGenerationStrategy;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationData;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationStrategy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class GenerationDataCheckTest {

    static Validator validator;

    @BeforeAll
    static void setup() {

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Test that for when the Generation strategy is DOWNLOAD, that the sourceArtifact and the sourceBuild are also
     * specified
     */
    @Test
    void testGenerationDataValidationOnDownload() {

        LicenseGenerationData licenseGenerationData = new LicenseGenerationData();
        licenseGenerationData.setStrategy(LicenseGenerationStrategy.DOWNLOAD);
        this.<LicenseGenerationData> testGenerationDataValidationOnDownloadHelper(licenseGenerationData);

        RepoGenerationData repoGenerationData = new RepoGenerationData();
        repoGenerationData.setStrategy(RepoGenerationStrategy.DOWNLOAD);
        this.<RepoGenerationData> testGenerationDataValidationOnDownloadHelper(repoGenerationData);

        JavadocGenerationData javadocGenerationData = new JavadocGenerationData();
        javadocGenerationData.setStrategy(JavadocGenerationStrategy.DOWNLOAD);
        this.<JavadocGenerationData> testGenerationDataValidationOnDownloadHelper(javadocGenerationData);
    }

    private <T extends GenerationData> void testGenerationDataValidationOnDownloadHelper(
            GenerationData generationData) {
        // when
        GenerationDataWrapper generationDataWrapper = new GenerationDataWrapper<T>();
        generationDataWrapper.generationData = generationData;
        Set<ConstraintViolation<GenerationDataWrapper>> violations = validator.validate(generationDataWrapper);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.stream().findFirst().get().getMessage()).contains("sourceBuild")
                .contains("sourceArtifact");

        // when
        generationDataWrapper.generationData.setSourceBuild("source build");
        generationDataWrapper.generationData.setSourceArtifact(null);
        Set<ConstraintViolation<GenerationDataWrapper>> violationsSourceBuild = validator
                .validate(generationDataWrapper);

        // then
        assertThat(violationsSourceBuild).hasSize(1);
        assertThat(violationsSourceBuild.stream().findFirst().get().getMessage()).contains("sourceBuild");

        // when
        generationDataWrapper.generationData.setSourceBuild(null);
        generationDataWrapper.generationData.setSourceArtifact("source artifact");
        Set<ConstraintViolation<GenerationDataWrapper>> violationsSourceArtifact = validator
                .validate(generationDataWrapper);

        // then
        assertThat(violationsSourceArtifact).hasSize(1);
        assertThat(violationsSourceArtifact.stream().findFirst().get().getMessage()).contains("sourceArtifact");

        // when
        generationDataWrapper.generationData.setSourceBuild("source build");
        generationDataWrapper.generationData.setSourceArtifact("source artifact");
        Set<ConstraintViolation<GenerationDataWrapper>> violationsNot = validator.validate(generationDataWrapper);

        // then
        assertThat(violationsNot).hasSize(0);
    }

    class GenerationDataWrapper<T extends GenerationData> {
        @GenerationDataCheck
        T generationData;
    }
}
