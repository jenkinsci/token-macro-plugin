dt("\${BUILD_LOG}")
dd() {
  span(_("Displays the end of the build log."))
  dl() {
    dt("maxLines")
    dd(_("Display at most this many lines of the log. Defaults to 250."))
  
    dt("escapeHtml")
    dd(_("If true, HTML is escape. Defaults to false."))

    dt("maxLineLength")
    dd(_("A maximum length for log lines. When lines are longer than the specified value, they are truncated and a \"...\" marker is appended at the end. " +
         "If 0, no truncation is done. Defaults to 0."))
  }
}
