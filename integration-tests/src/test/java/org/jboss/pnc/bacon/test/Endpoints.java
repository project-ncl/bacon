package org.jboss.pnc.bacon.test;

import lombok.experimental.UtilityClass;

import java.util.function.Function;

/**
 *
 * @author jbrazdil
 */
@UtilityClass
public class Endpoints {
    public final String PRODUCT = "/products";
    public final Function<String, String> PRODUCT_VERSIONS = id -> PRODUCT + "/" + id + "/versions";
    public final String PRODUCT_MILESTONE = "/product-milestones";
    public final String SCM_REPOSITORY = "/scm-repositories";
    public final String SCM_REPOSITORY_CREATE = "/scm-repositories/create-and-sync";
    public final String PROJECT = "/projects";
    public final String BUILD_CONFIG = "/build-configs";
    public final Function<String, String> BUILD_CONFIG_DEPENDENCIES = id -> BUILD_CONFIG + "/" + id + "/dependencies";
    public final String GROUP_CONFIG = "/group-configs";
    public final Function<String, String> GROUP_CONFIG_BUILD_CONFIGS = id -> GROUP_CONFIG + "/" + id + "/build-configs";
    public final String PRODUCT_VERSION = "/product-versions";
    public final Function<String, String> PRODUCT_VERSION_MILESTONES = id -> PRODUCT_VERSION + "/" + id + "/milestones";
}
