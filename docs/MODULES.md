# Prime Modules

### Problem definition
 - Originally `prime` started as a structured _monolith_.
 - But over time, the modules started to have inter-dependencies and `prime` started to become slightly unstructured.    
 - To force the loose coupling between modules, prime was split by putting each modules into a separate library.

### Requirement
 - Each of these modules implements a certain `function`.
 - Modules thus provide service for:
    - external client
    - other internal modules
    - both 
 - Modules libraries should not have any direct dependencies with each other, thus keeping them loosely coupled. 

### Solution
 - `prime` acts has a single deployable unit.
 - But, `prime` has minimal boilerplate code needed for it to act as an aggregator.
 - All the `functions` in `prime` are moved to separate libraries.
 - `prime-modules` is an library which acts as a **bridge** between `prime` and all the modules.
 - Modules are of different types:
   - Modules which are need access to Dropwizard's environment or configuration, which is provided via `prime-modules`. 
   - Modules which implement an interface, which is defined in `prime-modules`.

### Dependency

```text  
+-------+                              +---------------+                              +-----------+
|       |                              |               |                              |           |
| PRIME +--(compile-time dependency)-->| prime-modules |<--(compile-time-dependency)--+ Component |<-----------+
|       |                              |               |                              |           |            |
+---+---+                              +---------------+                              +-----+---+-+            |
    |                                                                                       ^   |     (runtime dependency)
    |                                                                                       |   |              |
    +--------------------------------(runtime dependency)-----------------------------------+   +--------------+
```

### Implementation
 - New module library will have `prime-modules` as `implementation` dependency (which is `compile` dependency in gradle).   
 - Add the new module library as `runtimeOnly` dependency in `prime`.

##### Modules needing Dropwizard environment or configuration
 - Implement `org.ostelco.prime.module.PrimeModule` interface.
 - This interface has `fun init(env: Environment)` via which Dropwizard environment will be passed.
 - Implementing class may also receive module specific configuration like Dropwizard's configuration.
 - Add following files in `src/main/resources/META-INF/services`:
   - File named `io.dropwizard.jackson.Discoverable` which contains a line `org.ostelco.prime.module.PrimeModule`.
   - File named `org.ostelco.prime.module.PrimeModule` which contains name of class (including package name) which implements `org.ostelco.prime.module.PrimeModule`. 

##### Modules implementing an interface
 - These components act as a **provider** for a **service** defined by an `interface` in `prime-modules`.
 - Other components **consume service provided** by these components.
 - Implement the `interface` defined in `prime-modules`.
 - The implementing class should have a `public no-arg constructor`.
 - Add a file in `src/main/resources/META-INF/services`:
    - Name of the file should be name of interface including package name.
    - File should contain 1 line - name of the class (including package name) which implements the interface.
 - Care should be taken that there is only one such implementing class.
 - The object of implementing class can then be injected using `getResource()` defined in `ResourceRegistry.kt` in `prime-modules` as:

```kotlin
private val instance: InterfaceName = getResource()
```

You may also do lazy initialization using Property Delegate feature from Kotlin.

```kotlin
private val instance by lazy { getResource<InterfaceName>() }
```
