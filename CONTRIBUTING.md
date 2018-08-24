# Contributing

# We accept contributions from anyone.  The prerequisites are:

* All contribution have to be compliant with [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0), with shared ownership.
* Contributions should, as a rule,  be produced as pull requests, either from personal cloned repositories (anyone can do that) or from feature branches within the repo (for those with required access).
* Pull requests should pass automated tests for code style, passing unit tests etc.
* Code in pull requests should conform to the project's coding standards (see below)
* Pull requests should also be manually approved by by a core team member before being merged to master.


# Code review

Code review is the gold standard of code quality.  If code passes code
review, then it's good enough.  If a reviewer  has comments the
committer _must_ take those comments into consideration before
merging to master.

TBD:   Guidelines for how to do code review.

# Coding standards

We have coding standards to make life as a development team
easier.  You should  do your best to follow the coding standards,
including suggesting changes to them if that makes life easier
for you and your fellow developers.

That said, we do not strictly enforce coding standards at this time.
This means that there will be code in this repository that do not
conform to the standards listed below.  The intent behind the
standards is to enforce an uniform "style" in which the code is
written, that makes it easily understandable both by people who
haven't written it and by people who wrote it a year ago.  The
standards also shouldn't feel like a burden to follow.  This isn't an
exercise in obedience, it's an exercise in respectful collaboration.

All developers should familarize themselves with the coding standards,
and to the best of of their ability followed by them. If the coding
standards are an impediment to getting your work done then consider
breaking them, or even better, to suggest an improvement to the coding
standard but only do so if the resulting code is still easily
understandable, as described above.

To the extent possible, we would want static code analysis serve as
quality gates that will not permit code that is in gross violation of
coding standards.  We also encourage code reviewers to help enforce
coding standards.  If the code is not following the coding standards,
or is not easily understandable even if it is following the standards
it is perfectly ok to reject pull requests.

Also, this coding standard document should be updated to include
coding standards applicable to all code in the repository.  We
currently don't use the Go language in this project, but if we do
start using Go, then a Go coding standard should be included.

## Everything, everywhere

This is an open source project.  Do not refer to or include any piece
of code or information that is not appropriately covered by an open
source license and made freely available.  Do not close off parts of
the project as it is present in github or other repositories linked to
by the github repository. It should all be open.

## All programming language and configuration files

* _Please avoid  commented out / dead code_:   If the code is part of
  documentation then the code must be preceded
  by a comment that explains how the commented out code is to be
  interpreted, as a template, as something to be uncommented very soon
  in the future or what.   As a general rule no commented-out code
  should be found permanently in our codebase.

* _Avoid repetition_: Don't say the same thing more than once.  Don't
  implement the same thing more than once if it can be avoided.
  It's annoying to be told the same thing more than once.   Don't
  be more redundant than necessary.    ... etc. :-)

* _Concentrate dependencies as much as practical_: Eventually all
  dependencies will have to be updated.  It makes sense to make
  those updates as simple as possible, by concentrating the
  dependencies in as few places as possible and upgrading them.

* _Whatever convension you use with respect to spacing, between
  lines_: Be consistent!   If you separate blocks with two lines,
  then always do that.  If you use three then always do that.
  These visual cues are picked up by experienced programmers,
  making them consistently useful is the polite thing to do.

* _When something weird needs to be done because of versioning
  problems, by all means do them, but document them_: Document what
  was done, and why.  Also document the date and who made the decision
  so that it is very obvious for a reader if the workaround is
  something recent, or if it is something that happened a while ago
  and might be reconsidered in light of new evidence.  (e.g. "we
  needed to use version foo.bar instad of foo.latest, since version
  foo.zot introduces a bug that causes the frobboz to bling.  This
  decision was made on march 21 2017 by Zaphood.").

* _State intent of scripts as comment near the beginning of the file_:
  Scripts of all kinds (sh, python, ...)  should (at least) have a
  paragraph immediately after the #! line that explains the purpose of
  the script, and typically also a typical usecase.

* Scripts of all kinds should be terminated by a blank line.

* All graphical elements should be available in the "most editable" version
  available, but also as a "markdown viewable" element if referred to in
  documentation. Don't submit bitmaps only except as a supplement to
  source code e.g. in Plant UML.

## Shell

* [Google's coding standards for shell scripts](https://google.github.io/styleguide/shell.xml).
* [Greg's wiki about shell scripting is a very good resource](http://mywiki.wooledge.org/)
* For complex output (multiple lines etc.), consider using "printf"
  instead of the simple "echo".
* Scripts, in particular scripts that are run during startup of containers
  either for production or testing, should die cleanly and visibly as soon as
  possible if they have to.  For instance, if the script depends on
  an executable that may be missing, the script should test for the
  existance of the executable before any of the script's "payload" is
  executed.  Example:

     DEPENDENCIES="foo bar baz gazonk"
     for dep in $DEPENDENCIES; do
	   if [[ -z "$(type $dep)" ]] ; then
		   echo "Cannot locate executable '${dep}', bailing out"
		   exit 1
	   fi
     done

## Kotlin

* [Coding confentions](https://kotlinlang.org/docs/reference/coding-conventions.html).
* |documenting kotlin code](https://kotlinlang.org/docs/reference/kotlin-doc.html).

## Java

* [Oracle java docing conventions](https://www.oracle.com/technetwork/java/javase/documentation/codeconvtoc-136057.html).
* [Google java style guide](https://google.github.io/styleguide/javaguide.html).

## Dockerfiles

* [Best practices for writing Dockerfiles](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)

## Docker images

* For google cloud-sdk images, always use "latest".
* For everything else, be explcit about which version of an image is being used.
