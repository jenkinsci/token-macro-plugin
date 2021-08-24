dt("\${LOG_REGEX}")
dd() {
  span(_("Uses a regular expression to find a single log entry and generates " +
     "a new output using the capture groups from it. This is partially " +
     "based on the description-setter plugin (https://github.com/jenkinsci/description-setter-plugin)."))
  dl() {
    dt("regex")
    dd(_("Text that matches this regular expression is selected for replacement. " +
         "See also java.util.regex.Pattern."))
    dt("replacement")
    dd(_("Replace the matched text with this text. Capture groups from the regex can be referred " +
         "to using \\\\1 for the first group, \\\\2 for the second and so on."))
  }
}
