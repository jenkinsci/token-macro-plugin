# Release History

### Version 2.8 (June 11, 2019)

-   [Fix security issue](http://Fix%20security%20issue){.external-link}

### Version 2.7 (March 7, 2019)

-   Updates to support Java 11

### Version 2.6 (January 28, 2019)

-   [Fix security
    issue](https://jenkins.io/security/advisory/2019-01-28/){.external-link}

### Version 2.3 (September 1, 2017)

-   Fixed incompatibility issue with Docker Plugin by reducing the scope
    of the pipeline dependencies.

### Version 2.2 (August 22, 2017)

-   **There is an incompatibility issue with Token Macro 2.2 and the
    Docker Plugin. Downgrade to 2.1 if you are using the Docker Plugin**
-   Add ability to use JsonPath for the JSON macro, use expr="jsonpath
    expression here" instead of path=""
-   Add tm pipeline step that will take a string with macros and expand
    and return the resulting string
-   Fix issue with BUILD\_STATUS in pipeline jobs
    ([JENKINS-44322](https://issues.jenkins-ci.org/browse/JENKINS-44322){.external-link})

### Version 2.1 (April 4, 2017)

-   Fixed issue where the plugin may report incorrect parameters when it
    shouldn't
    ([JENKINS-38871](https://issues.jenkins-ci.org/browse/JENKINS-38871){.external-link})
-   Fixed issue with CHANGES\_SINCE\_LAST\_SUCCESS not showing "No
    Changes"
    ([JENKINS-38668](https://issues.jenkins-ci.org/browse/JENKINS-38668){.external-link})
-   Additional tests were added
    ([JENKINS-40683](https://issues.jenkins-ci.org/browse/JENKINS-40683){.external-link})
-   Fixed issues so that the plugin can be used in the global plugin
    tests for core
    ([JENKINS-39672](https://issues.jenkins-ci.org/browse/JENKINS-39672){.external-link})
-   Fixed issue with whitespace following the variable definition
    ([JENKINS-38420](https://issues.jenkins-ci.org/browse/JENKINS-38420){.external-link})
-   Added JSON token for retrieving data from a JSON file.

### Version 2.0 (September 20, 2016)

-   Thanks to the following for contributions in this
    release [dcoraboeuf](https://github.com/dcoraboeuf){.external-link}, [duemir](https://github.com/duemir){.external-link}
-   Initial support for using Token Macro in pipeline script context
    ([JENKINS-35368](https://issues.jenkins-ci.org/browse/JENKINS-35368){.external-link})
-   Fixed issue with synchronization which was causing slowness
    ([JENKINS-32331](https://issues.jenkins-ci.org/browse/JENKINS-32331){.external-link})
-   Provide new method to get auto-completion possibilities for a given
    string
    ([JENKINS-9345](https://issues.jenkins-ci.org/browse/JENKINS-9345){.external-link})
-   Allow for additional transforms on the result of the token
    ([JENKINS-28951](https://issues.jenkins-ci.org/browse/JENKINS-28951){.external-link})

### Version 1.12.1 (December 14, 2015)

-   Fixed bad dependency in pom.xml causing circular dependency issue

### Version 1.12 (December 10, 2015)

-   Migrated tokens from email-ext to token-macro
-   Fixed issue with multiline JOB\_DESCRIPTION in subject
    ([JENKINS-32012](https://issues.jenkins-ci.org/browse/JENKINS-32012){.external-link})
-   Fixed issue where extra $'s for escaping $'s were being removed
    ([JENKINS-29816](https://issues.jenkins-ci.org/browse/JENKINS-29816){.external-link})
-   Added UPSTREAM\_RUN\_NAME macro
    ([JENKINS-27542](https://issues.jenkins-ci.org/browse/JENKINS-27542){.external-link})

### Version 1.11 (October 16, 2015)

-   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg) Added FILE macro to display Workspace file contents
    ([JENKINS-27540](https://issues.jenkins-ci.org/browse/JENKINS-27540))
-   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg) Added XML macro, which retrieves the data from XML
    file using XPath
    ([JENKINS-12742](https://issues.jenkins-ci.org/browse/JENKINS-12742))
-   ![(info)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/information.svg) Allow configuring charsets in LOG\_REGEX
    macro ([PR
    18](https://github.com/jenkinsci/token-macro-plugin/pull/18){.external-link})
-   ![(error)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/error.svg) Cleanup of issues discovered by FindBugs, the
    plugin is FindBugs-clean ([PR
    18](https://github.com/jenkinsci/token-macro-plugin/pull/18){.external-link})

### Version 1.10 (February 13, 2014)

-   Migrated tests to use JenkinsRule instead of HudsonTestCase
-   Added additional tests for the DataBoundTokenMacro
-   Added ADMIN\_EMAIL token to retrieve the administrator email address
-   Added the ability to mark a parameter with an alias so that Java
    keywords can be used as macro arguments.
-   Updated pom.xml to include the MIT license

### Version 1.9 (October 28, 2013)

-   Cleaned up the pom a bit
-   Fixed issue with private tokens accumulating over time ([issue
    \#18912](https://issues.jenkins-ci.org/browse/JENKINS-18912){.external-link})

### Version 1.8.1 (Jul 20, 2013)

-   Added error message output instead of just letting the exception be
    caught and ignored

### Version 1.8 (Jul 16, 2013)

-   Updated the way that macros are escaped so that it's more like
    Groovy ([issue
    \#18014](https://issues.jenkins-ci.org/browse/JENKINS-18014){.external-link})
-   Created macro (LOG\_REGEX) to match against the log output and allow
    replacing it.
-   Fix issue with PROPFILE token that would hang the build if the
    PROPFILE was not found.
-   Updated to depend on a newer LTS (1.509.1) in the pom.
-   Disallow tokens that begin with a number to follow identifier
    constraints in programming languages.

### Version 1.7 (May 23, 2013)

-   Fixed issue with long string parameters and backslash escaped items.

### Version 1.6 (Feb 25, 2013)

-   Added nested token support
-   Added ability to escape tokens
-   Added ability to have private macros

### Version 1.5.1 (Nov 29, 2011)

-   New method `TokenMacro.expandAll( build, listener, template )` was
    broken
    ([JENKINS-11914](https://issues.jenkins-ci.org/browse/JENKINS-11914))

### Version 1.5 (Nov 28, 2011)

-   New method `TokenMacro.expandAll( build, listener, template )` that
    supports to expand all macros, but also all environment and [build
    variables](http://ci.jenkins-ci.org/env-vars.html){.external-link}.

### Version 1.4

-   Magnifying plugin name.
-   Fixing inaccurate documentation

### Version 1.3

-   Added macro for retrieving build environment variables.
-   Added macro for retrieving properties from property files in the
    build workspace.

### Version 1.2

-   Added description for update center

### Version 1.1

-   Fixed exception

### Version 1.0

-   Initial release
