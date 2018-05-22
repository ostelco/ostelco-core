# Prime Modules

### Problem definition
 - Originally `prime` started as a structured _monolith_.
 - But over time, the components started to have inter-dependencies and `prime` started to become slightly unstructured.    
 - To force the loose coupling between components, prime was split by putting each component into a separate library.

### Requirement
 - Each of these components implements a certain `function`.
 - Components thus provide service for:
    - external client
    - other internal components
    - both 
 - Component libraries should not have any direct dependencies with each other, thus keeping them loosely coupled. 

### Solution
 - `prime` acts has a single deployable unit.
 - But, `prime` has minimal boilerplate code needed for it to act as an aggregator.
 - All the `functions` in `prime` are moved to separate libraries.
 - `prime-api` is an library which acts as a **bridge** between `prime` and all the components.
 - Components are of different types:
   - Components which are need access to Dropwizard's environment, which is provided by `prime-api`. 
   - Components which implement an interface, which is defined in `prime-api`.

### Dependency
    
    [prime] --(compile-time dependency)--> [prime-api] <--(compile-time dependency)-- [Component] <--(runtime dependency)
        \                                                                                ^   \           /
         \__________________________(runtime dependency)________________________________/     \_________/

### Implementation

##### Components needing Dropwizard environment
 - Implement `org.ostelco.prime.provider.Service` interface.
 - This interface has `fun init(env: Environment)` via which Dropwizard environment will be passed.
 - Add following files in `src/main/resources/META-INF/services`:
   - File named `io.dropwizard.jackson.Discoverable` which contains a line `org.ostelco.prime.provider.Service`.
   - File named `org.ostelco.prime.provider.Service` which contains name of class (including package name) which implements `org.ostelco.prime.provider.Service`. 

##### Components implementing an interface
 - These components act as a **provider** for a **service** defined by an `interface` in `prime-api`.
 - Other components **consume service provided** by these components.
 - Implement the `interface` defined in `prime-api`.
 - The implementing class should have a `public no-arg constructor`.
 - Add a file in `src/main/resources/META-INF/services`:
    - Name of the file should be name of interface including package name.
    - File should contain 1 line - name of the class (including package name) which implements the interface.
 - Care should be taken that there is only one such implementing class.
 - The object of implementing class can then be injected using `getResource()` defined in `ResourceRegistry.kt` in `prime-api` as:
    
    
    var instance: InterfaceName = getResource()
 