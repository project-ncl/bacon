package org.jboss.pnc.bacon.pig.config.build;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SourcesGenerationData extends GenerationData<SourcesGenerationStrategy> {

  /**
   * Each entry can be a part of an artifact
   * If none are supplied, then it's assumed that sources
   * from all artifacts are to be added
   */
  private List<String> whitelistedArtifacts;

  /**
   * Add defaults to avoid having existing configurations
   * having to define a sourceGeneration object in the flow section
   */
  public SourcesGenerationData() {
    whitelistedArtifacts = new ArrayList<>();
    setStrategy(SourcesGenerationStrategy.GENERATE);
  }
}
