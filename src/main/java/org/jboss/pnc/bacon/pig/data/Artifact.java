/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

package org.jboss.pnc.bacon.pig.data;

import lombok.Getter;
import org.jboss.pnc.bacon.pig.FileDownloadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 6/13/17
 */
@Getter
public class Artifact {
    private final Logger log = LoggerFactory.getLogger(Artifact.class);

    private final String gapv;
    private final String fileName;
    private final String downloadUrl;

    public Artifact(Map<String, ?> map) {
        fileName = (String) map.get("filename");
        downloadUrl = (String) map.get("public_url");
        gapv = (String) map.get("identifier");
    }

    public GAV toGAV() {
        return GAV.fromColonSeparatedGAPV(gapv);
    }

    @Override
    public String toString() {
        return gapv;
    }

    public void downloadTo(File downloadedZip) {
        FileDownloadUtils.downloadTo(URI.create(downloadUrl), downloadedZip);
    }

    public void downloadToDirectory(Path parentDirPath) {
        File targetPath = parentDirPath.resolve(fileName).toFile();

        downloadTo(targetPath);
    }
}
