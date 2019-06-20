# Developer guidelines

## Checking for dependency updates

```bash
./gradlew dependencyUpdates -Drevision=release
```

## Checking for dependency resolution

```bash
./gradlew :prime:dependencyInsight --configuration runtimeClasspath --dependency dependency-name
```

## Package / Namespace naming convention

### Format

    [Optional]
    <Variable>
    literal

### Expression
    
    org.ostelco[.group]<.module/component>
    org.ostelco[.group]<.module/component>[.sub-module/component]

### Examples

    org.ostelco.auth
    org.ostelco.analytics
    org.ostelco.diameter
    org.ostelco.ext_pgw
    org.ostelco.ocs.api
    org.ostelco.ocs
    org.ostelco.ocsgw
    org.ostelco.prime
    
    