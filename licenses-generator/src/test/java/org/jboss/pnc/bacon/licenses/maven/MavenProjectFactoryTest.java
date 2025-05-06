package org.jboss.pnc.bacon.licenses.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class MavenProjectFactoryTest {

    @Mock
    private File mockFile;

    @Mock
    private Artifact mockArtifact;

    @Mock
    private ProjectBuilder mockProjectBuilder;

    @Mock
    private ProjectBuildingRequestFactory mockProjectBuildingRequestFactory;

    @Mock
    private ProjectBuildingRequest mockProjectBuildingRequest;

    @Mock
    private ProjectBuildingResult mockProjectBuildingResult;

    @Mock
    private MavenProject mockMavenProject;

    private MavenProjectFactory mavenProjectFactory;

    @BeforeEach
    public void before() throws ProjectBuildingException {
        MockitoAnnotations.initMocks(this);

        when(mockProjectBuildingRequestFactory.getProjectBuildingRequest()).thenReturn(mockProjectBuildingRequest);
        when(mockProjectBuilder.build(any(Artifact.class), eq(mockProjectBuildingRequest)))
                .thenReturn(mockProjectBuildingResult);
        when(mockProjectBuilder.build(eq(mockFile), eq(mockProjectBuildingRequest)))
                .thenReturn(mockProjectBuildingResult);
        when(
                mockProjectBuilder
                        .build(eq(Collections.singletonList(mockFile)), eq(true), eq(mockProjectBuildingRequest)))
                .thenReturn(Collections.singletonList(mockProjectBuildingResult));
        when(mockProjectBuildingResult.getProject()).thenReturn(mockMavenProject);
        when(mockMavenProject.getFile()).thenReturn(mockFile);

        mavenProjectFactory = new MavenProjectFactory(mockProjectBuilder, mockProjectBuildingRequestFactory);
    }

    @Test
    public void shouldGetMavenProjectFromArtifactWithoutDependencies() throws ProjectBuildingException {
        Optional<MavenProject> mavenProjectOptional = mavenProjectFactory.getMavenProject(mockArtifact, false);

        assertThat(mavenProjectOptional.isPresent()).isTrue();
        assertThat(mavenProjectOptional.get()).isEqualTo(mockMavenProject);

        verify(mockProjectBuildingRequestFactory).getProjectBuildingRequest();
        verify(mockProjectBuildingRequest).setResolveDependencies(false);
        verify(mockProjectBuilder).build(mockArtifact, mockProjectBuildingRequest);
        verify(mockProjectBuildingResult).getProject();
    }

    @Test
    public void shouldGetMavenProjectFromArtifactWithDependencies() throws ProjectBuildingException {
        Optional<MavenProject> mavenProjectOptional = mavenProjectFactory.getMavenProject(mockArtifact, true);

        assertThat(mavenProjectOptional.isPresent()).isTrue();
        assertThat(mavenProjectOptional.get()).isEqualTo(mockMavenProject);

        verify(mockProjectBuildingRequestFactory).getProjectBuildingRequest();
        verify(mockProjectBuildingRequest).setResolveDependencies(true);
        verify(mockProjectBuilder).build(mockArtifact, mockProjectBuildingRequest);
        verify(mockProjectBuildingResult).getProject();
    }

    @Test
    public void shouldNotGetProjectFromArtifactInCaseOfException() throws ProjectBuildingException {
        when(mockProjectBuilder.build(any(Artifact.class), eq(mockProjectBuildingRequest)))
                .thenThrow(ProjectBuildingException.class);
        Optional<MavenProject> mavenProjectOptional = mavenProjectFactory.getMavenProject(mockArtifact, true);

        assertThat(mavenProjectOptional.isPresent()).isFalse();
    }

    @Test
    public void shouldGetMavenProjectFromPomWithoutDependencies() throws ProjectBuildingException {
        List<MavenProject> mavenProjects = mavenProjectFactory.getMavenProjects(mockFile, false);

        assertThat(mavenProjects).containsOnly(mockMavenProject);

        verify(mockProjectBuildingRequestFactory).getProjectBuildingRequest();
        verify(mockProjectBuildingRequest).setResolveDependencies(false);
        verify(mockProjectBuilder).build(Collections.singletonList(mockFile), true, mockProjectBuildingRequest);
        verify(mockProjectBuildingResult).getProject();
    }

    @Test
    public void shouldGetMavenProjectsFromPomWithDependencies() throws ProjectBuildingException {
        List<MavenProject> mavenProjects = mavenProjectFactory.getMavenProjects(mockFile, true);

        assertThat(mavenProjects).containsOnly(mockMavenProject);

        verify(mockProjectBuildingRequestFactory).getProjectBuildingRequest();
        verify(mockProjectBuildingRequest).setResolveDependencies(true);
        verify(mockProjectBuilder).build(Collections.singletonList(mockFile), true, mockProjectBuildingRequest);
        verify(mockProjectBuildingResult, times(2)).getProject();
    }

    @Test
    public void shouldGetMavenProjectsFromPomWithModules() throws ProjectBuildingException {
        when(
                mockProjectBuilder
                        .build(eq(Collections.singletonList(mockFile)), eq(true), eq(mockProjectBuildingRequest)))
                .thenReturn(Arrays.asList(mockProjectBuildingResult, mockProjectBuildingResult));

        List<MavenProject> mavenProjects = mavenProjectFactory.getMavenProjects(mockFile, true);

        assertThat(mavenProjects).hasSize(2);
        assertThat(mavenProjects).containsOnly(mockMavenProject);

        verify(mockProjectBuildingRequestFactory).getProjectBuildingRequest();
        verify(mockProjectBuildingRequest).setResolveDependencies(true);
        verify(mockProjectBuilder).build(Collections.singletonList(mockFile), true, mockProjectBuildingRequest);
        verify(mockProjectBuildingResult, times(4)).getProject();
    }

    @Test
    public void shouldNotGetProjectsFromPomInCaseOfException() throws ProjectBuildingException {
        when(
                mockProjectBuilder
                        .build(eq(Collections.singletonList(mockFile)), eq(true), eq(mockProjectBuildingRequest)))
                .thenThrow(ProjectBuildingException.class);
        List<MavenProject> mavenProjects = mavenProjectFactory.getMavenProjects(mockFile, false);

        assertThat(mavenProjects).isEmpty();
    }

}
