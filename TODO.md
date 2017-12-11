# TODO

* Start using something other than this file to track tasks.
* Get rid of the interface/impl pattern that is present in
  the entities package.  Since we simplified the data classes
  with lombok, that separation gives little or no benefit.
* Remove telenor digital references.
* Increase unit testability, restructure to make almost everything
  unit testable.
* Refactor firebase database into something that is  integration testable.
* Introduce project lombok to compactify code a _lot_ [done]
* Make a template project for dropwizard.
* Look into making a healthcheck for firebase/firestore
       - https://www.firebase.com/docs/web/guide/offline-capabilities.html#section-connection-state
         this.firebaseDatabase.getReference("/.info/connected").addValueEventListener()
