dt("\${${my.MACRO_NAME}}")
dd {
  span(my.shortHelpDescription)
  dl() {
    dt("reverse")
    dd(_("If true, show most recent builds at the top instead of the bottom. Defaults to false."))
    
    dt("format")
    dd() {
      span(_("For each build listed, a string containing %X, where %X is one of"))
      dl() {
        dt("%c")
        dd(_("changes"))
        
        dt("%n")
        dd(_("build number"))
      }
      p(_("Defaults to ") + my.defaultFormatValue)
    }
    dt("changesFormat")
    dd() {
      span(_("For each change in a build. See \${CHANGES_SINCE_LAST_BUILD} for placeholders."))
    }
  }
  span("Following Parameters are also supported: " +
          "showPaths, pathFormat, showDependencies, dateFormat, regex, replace, default, escapeHtml. " +
          "See \${CHANGES_SINCE_LAST_BUILD} details.")
}