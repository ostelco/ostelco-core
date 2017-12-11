# TODO

* Start using something other than this file to track tasks.
* Remove telenor digital references.
* Increase unit testability, restructure to make almost everything
  unit testable.
* Refactor firebase database into something that is  integration testable.
* Introduce project lombok to compactify code a _lot_ [done]
* Make a template project for dropwizard.
* Look into making a healthcheck for firebase/firestore
       - https://www.firebase.com/docs/web/guide/offline-capabilities.html#section-connection-state
         this.firebaseDatabase.getReference("/.info/connected").addValueEventListener()
