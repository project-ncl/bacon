/*
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
package org.jboss.pnc.bacon.pig.impl.addons;

import org.jboss.pnc.bacon.pig.impl.config.Config;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;

import java.util.Map;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 12/11/17
 */
public abstract class AddOn {
    protected final Config config;
    protected final Map<String, PncBuild> builds;
    protected final String releasePath;
    protected final String extrasPath;

    protected AddOn(Config config,
                    Map<String, PncBuild> builds,
                    String releasePath,
                    String extrasPath) {
        this.config = config;
        this.builds = builds;
        this.releasePath = releasePath;
        this.extrasPath = extrasPath;
    }

    public boolean shouldRun() {
        return config.getAddons().keySet().contains(getName());
    }

    public Map<String, ?> getConfig() {
        return config.getAddons().get(getName());
    }

    protected abstract String getName();

    public abstract void trigger();
}
