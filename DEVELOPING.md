
# Developers Guide

## Adding JSON Output

The JSON option (`-o`) is defined as an inherited option in the top level App. In order for implementations to retrieve its value they should extend `JSONCommandHandler` which provides a function to return the value:
```
    protected boolean getJsonOutput()
```

## Adding Example Usage Messages

Sometimes it is useful to be able to add an example of how to use a command on top of the basic usage message. This may be
accomplished by using the `footer` attribute combined with a predefined constant e.g.
```java
    @Command(
            name = "get",
            description = "Get an artifact by its id",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc artifact get 10")
    public static class Get extends AbstractGetSpecificCommand<Artifact> {
```
which produces
```
java -jar cli/target/bacon.jar pnc artifact get -h
Usage: bacon pnc artifact get [-hov] [-p=<configurationFileLocation>] [--profile=<profile>] <id>
Get an artifact by its id
...
Example:
$ bacon pnc artifact get 10
```
