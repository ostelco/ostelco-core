# TODO


* Start using something other than this file to track tasks.
* Get rid of the interface/impl pattern that is present in
  the entities package.  Since we simplified the data classes
  with lombok, that separation gives little or no benefit.
* The interactions between the various types of messages are
  confusing.  Consider autogenerating sequence diagrams when
  running tests to help document what is going on.
* Increase unit testability, restructure to make almost everything
  unit testable.
* Refactor firebase database into something that is  integration testable.
* Make a template project for dropwizard.
* Automatically generate javadoc in Travis build.
* Automatically publish javadoc to the github website.
   https://github.com/blog/2233-publish-your-project-documentation-with-github-pages
* Look into making a healthcheck for firebase/firestore
       - https://www.firebase.com/docs/web/guide/offline-capabilities.html#section-connection-state
         this.firebaseDatabase.getReference("/.info/connected").addValueEventListener()

* This looks like a good writeup of best (&worst) practices for testing
  http://blog.codepipes.com/testing/software-testing-antipatterns.html   We should
  absorb this and adapt and institutionalise the practices we want to use.