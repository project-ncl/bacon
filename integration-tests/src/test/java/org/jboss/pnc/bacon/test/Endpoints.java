package org.jboss.pnc.bacon.test;

import java.util.function.Function;

/**
 *
 * @author jbrazdil
 */
public class Endpoints {

    public static final String PRODUCT = "/products";
    public static final Function<String, String> PRODUCT_VERSIONS = id -> PRODUCT + "/" + id + "/versions";
    public static final String PRODUCT_MILESTONE = "/product-milestones";
    public static final String SCM_REPOSITORY = "/scm-repositories";
    public static final String SCM_REPOSITORY_CREATE = "/scm-repositories/create-and-sync";
    public static final String PROJECT = "/projects";
    public static final String BUILD_CONFIG = "/build-configs";
    public static final Function<String, String> BUILD_CONFIG_DEPENDENCIES = id -> BUILD_CONFIG + "/" + id
            + "/dependencies";
    public static final String GROUP_CONFIG = "/group-configs";
    public static final Function<String, String> GROUP_CONFIG_BUILD_CONFIGS = id -> GROUP_CONFIG + "/" + id
            + "/build-configs";
    public static final String PRODUCT_VERSION = "/product-versions";
    public static final Function<String, String> PRODUCT_VERSION_MILESTONES = id -> PRODUCT_VERSION + "/" + id
            + "/milestones";
}
