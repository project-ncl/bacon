package org.jboss.pnc.bacon.pig;

class DummyBuildDependency implements BuildDependency {

    @Override
    public String doSomething(String configLocation) {
        return "did something with config at:" + configLocation;
    }
}
