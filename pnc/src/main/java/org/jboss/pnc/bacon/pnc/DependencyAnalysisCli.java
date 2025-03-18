/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pnc;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jboss.pnc.api.enums.DeliverableAnalyzerReportLabel;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.DeliverableAnalyzerReportClient;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.DeliverableAnalyzerLabelEntry;
import org.jboss.pnc.dto.DeliverableAnalyzerReport;
import org.jboss.pnc.dto.requests.labels.DeliverableAnalyzerReportLabelRequest;
import org.jboss.pnc.dto.response.AnalyzedArtifact;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "dependency-analysis",
        description = "Dependency Analysis",
        subcommands = {
                DependencyAnalysisCli.AddLabel.class,
                DependencyAnalysisCli.Get.class,
                DependencyAnalysisCli.GetAnalyzed.class,
                DependencyAnalysisCli.GetLabelHistory.class,
                DependencyAnalysisCli.List.class,
                DependencyAnalysisCli.RemoveLabel.class
        })
public class DependencyAnalysisCli {

    private static final ClientCreator<DeliverableAnalyzerReportClient> CREATOR = new ClientCreator<>(
            DeliverableAnalyzerReportClient::new);

    @Command(name = "add-label", description = "Add a label to a report")
    public static class AddLabel implements Callable<Integer> {

        @CommandLine.Parameters(description = "Deliverable Analysis ID")
        private String deliverableId;

        @CommandLine.Parameters(description = "Label")
        private DeliverableAnalyzerReportLabel label;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (DeliverableAnalyzerReportClient client = CREATOR.newClient()) {
                DeliverableAnalyzerReportLabelRequest request = DeliverableAnalyzerReportLabelRequest.builder()
                        .label(label)
                        .build();
                client.addLabel(deliverableId, request);
                return 0;
            }
        }
    }

    @Command(name = "list", description = "List all deliverable analyzer reports")
    public static class List extends AbstractListCommand<DeliverableAnalyzerReport> {

        @Override
        public Collection<DeliverableAnalyzerReport> getAll(String sort, String query) throws RemoteResourceException {

            try (DeliverableAnalyzerReportClient client = CREATOR.newClient()) {
                return client.getAll(Optional.ofNullable(sort), Optional.ofNullable(query)).getAll();
            }
        }
    }

    @Command(name = "get-analyzed", description = "Gets analyzed artifacts of this deliverable analysis report")
    public static class GetAnalyzed extends AbstractListCommand<AnalyzedArtifact> {

        @CommandLine.Parameters(description = "Deliverable Analysis ID")
        private String deliverableId;

        @Override
        public Collection<AnalyzedArtifact> getAll(String sort, String query) throws RemoteResourceException {
            try (DeliverableAnalyzerReportClient client = CREATOR.newClient()) {
                return client.getAnalyzedArtifacts(deliverableId).getAll();
            }
        }
    }

    @Command(name = "get-label-history", description = "Gets the label history of this deliverable analyzer report")
    public static class GetLabelHistory extends AbstractListCommand<DeliverableAnalyzerLabelEntry> {

        @CommandLine.Parameters(description = "Deliverable Analysis ID")
        private String deliverableId;

        @Override
        public Collection<DeliverableAnalyzerLabelEntry> getAll(String sort, String query)
                throws RemoteResourceException {
            try (DeliverableAnalyzerReportClient client = CREATOR.newClient()) {
                return client.getLabelHistory(deliverableId).getAll();
            }
        }
    }

    @Command(name = "get", description = "Gets specific deliverable analyzer report")
    public static class Get extends AbstractGetSpecificCommand<DeliverableAnalyzerReport> {

        @Override
        public DeliverableAnalyzerReport getSpecific(String id) throws RemoteResourceException {
            try (DeliverableAnalyzerReportClient client = CREATOR.newClient()) {
                return client.getSpecific(id);
            }
        }
    }

    @Command(name = "remove-label", description = "Removes label from this deliverable analyzer report")
    public static class RemoveLabel implements Callable<Integer> {

        @CommandLine.Parameters(description = "Deliverable Analysis ID")
        private String deliverableId;

        @CommandLine.Parameters(description = "Label")
        private DeliverableAnalyzerReportLabel label;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (DeliverableAnalyzerReportClient client = CREATOR.newClient()) {
                DeliverableAnalyzerReportLabelRequest request = DeliverableAnalyzerReportLabelRequest.builder()
                        .label(label)
                        .build();
                client.removeLabel(deliverableId, request);
                return 0;
            }
        }
    }
}
