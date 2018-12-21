package org.jboss.pnc.bacon.pig;

import javax.enterprise.context.ApplicationScoped;

// TODO remove
// this is a just a dummy implementation to show that CDI works
@ApplicationScoped
class DummyBuildDependency implements BuildDependency {

    @Override
    public String doSomething(String configLocation) {
        return "did something with config at:" + configLocation;
    }
}
