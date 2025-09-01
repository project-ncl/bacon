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
package org.jboss.pnc.bacon.config;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/14/18
 */
public class Config {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Config.class);
    private static String configLocation;
    private static String configFilePath;
    private static String activeProfileName;
    private ConfigProfile activeProfile;
    private List<ConfigProfile> profile;
    private static Config instance;

    public static Config instance() throws RuntimeException {
        if (instance == null) {
            try {
                initialize();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    public static void configure(String configLocation, String configFileName, String profileName) {
        Config.configLocation = configLocation;
        Config.configFilePath = configLocation + File.separator + configFileName;
        Config.activeProfileName = profileName;
    }

    public static void initialize() throws IOException {
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            throw new IOException(
                    "Config file " + configFilePath
                            + " does not exist! Please create it. See: https://github.com/project-ncl/bacon/blob/master/config.yaml for an example");
        } else if (configFile.length() == 0) {
            log.warn("Config file: {} has no content", configFilePath);
            instance = new Config();
        } else {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            instance = mapper.readValue(new File(configFilePath), Config.class);
        }
        instance.setActiveProfile(getProfileByName(activeProfileName));
        checkPNCURL(instance);
    }

    public static ConfigProfile getProfileByName(String name) {
        List<ConfigProfile> potentialConfig;
        potentialConfig = instance.getProfile()
                .stream()
                .filter(ConfigProfile -> ConfigProfile.getName().equals(name))
                .collect(Collectors.toList());
        if (potentialConfig.size() == 0 && name.equals("default"))
            throw new IllegalArgumentException("Default configuration profile doesn\'t exist");
        if (potentialConfig.size() == 0)
            throw new IllegalArgumentException("Configuration profile with this name doesn\'t exist");
        if (potentialConfig.size() > 1)
            throw new IllegalArgumentException("There are multiple configuration profiles with same name!");
        return potentialConfig.get(0);
    }

    public static String getConfigLocation() {
        return configLocation;
    }

    public static String getConfigFilePath() {
        return configFilePath;
    }

    // for tests only
    @Deprecated
    public static void setInstance(Config instance) {
        Config.instance = instance;
    }

    private static void checkPNCURL(Config profile) {
        try {
            URL pncUrl = new URL(profile.activeProfile.getPnc().getUrl());
            if (pncUrl.getPath() != "") {
                log.warn("PNC URL should not contain path.");
            }
        } catch (MalformedURLException e) {
            log.error("PNC URL is malformed.");
        }
    }

    @java.lang.SuppressWarnings("all")
    public Config() {
    }

    @java.lang.SuppressWarnings("all")
    public ConfigProfile getActiveProfile() {
        return this.activeProfile;
    }

    @java.lang.SuppressWarnings("all")
    public List<ConfigProfile> getProfile() {
        return this.profile;
    }

    @java.lang.SuppressWarnings("all")
    public void setActiveProfile(final ConfigProfile activeProfile) {
        this.activeProfile = activeProfile;
    }

    @java.lang.SuppressWarnings("all")
    public void setProfile(final List<ConfigProfile> profile) {
        this.profile = profile;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Config))
            return false;
        final Config other = (Config) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$activeProfile = this.getActiveProfile();
        final java.lang.Object other$activeProfile = other.getActiveProfile();
        if (this$activeProfile == null ? other$activeProfile != null : !this$activeProfile.equals(other$activeProfile))
            return false;
        final java.lang.Object this$profile = this.getProfile();
        final java.lang.Object other$profile = other.getProfile();
        if (this$profile == null ? other$profile != null : !this$profile.equals(other$profile))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Config;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $activeProfile = this.getActiveProfile();
        result = result * PRIME + ($activeProfile == null ? 43 : $activeProfile.hashCode());
        final java.lang.Object $profile = this.getProfile();
        result = result * PRIME + ($profile == null ? 43 : $profile.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "Config(activeProfile=" + this.getActiveProfile() + ", profile=" + this.getProfile() + ")";
    }
}
