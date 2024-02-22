# Overview

This project generates `licenses.xml` and `licenses.html` files based on a `pom.xml`. It loads all transitive dependnencies from `dependencies` section of the project's `pom.xml` and collects all their licenses. Then it aligns the license names and URLs to comply with the approved Red Hat license names and URLs. Finally, when generating `licenses.html`, it also downloads license contents for offline use.

# Standalone usage
This project can be pakaged as an uber-jar and used standalone. To create an uber-jar run the following command:
```
mvn clean package
```

Then generate `licenses.(xml|html)`:
```
java -jar target/licenses-generator-shaded.jar -Dpom={path to pom.xml} -Ddestination={destination directory} [-DgeneratorProperties={path to a properties file}] [-DaliasesFile={path to aliases file}] [-DexceptionsFile={path to exceptions file}]
```

# Usage in an application
You can add this project as a dependency and generate license files by using its API directly. Add the following dependency to your project's `pom.xml`:
```
<dependency>
  <groupId>me.snowdrop</groupId>
  <artifactId>licenses-generator</artifactId>
  <version>${project.version}</version>
</dependency>
```

Create an instance of `LicensesGenerator` and use `generateLicensesForPom` or `generateLicensesForGavs`. The latter method is only exposed to use directly and will not resolve transitive dependencies.

# Configuration
Project can be configured with a `src/main/resources/generator.properties` file. However, it can be oveerriden when running in standalone mode by providing `-DgeneratorProperties={path to a properties file}` as an application argument. When using generator via API, you can provide `GeneratorProperties` when creating an instance of `LicensesGenerator`.

These are the available properties (you can also see them in `PropertyKeys`):

Name|Description|Default value
---|---|---
repository.names | Comma separated list of repository names. Must be the same length as repository.urls | Maven Central
repository.urls | Comma separated list of repository URLs. Must be the same length as repository.names | http://repo1.maven.org/maven2
licenseServiceUrl | An optional URL of a license service. <br> If not provided, the license data will be collected from the the rh-license-exceptions.json file or artifacts' pom.xml | *null*
aliasesFile | An absolute path to the license aliases file (can be overwritten by -DaliasesFile) | rh-license-names.json from this project
exceptionsFile | An absolute path to the license exceptions file (can be overwritten by -DexceptionsFile)   | rh-license-exceptions.json from this project
