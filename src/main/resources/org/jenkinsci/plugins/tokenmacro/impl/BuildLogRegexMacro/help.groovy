dt("\${BUILD_LOG_REGEX}")
dd() {
  span(_("Displays lines from the build log that match the regular expression."))
  dl() {
    dt("regex")
    dd(_("Lines that match this regular expression are included. " +
         "See also java.util.regex.Pattern." + 
         "Defaults to \"(?i)\\\\b(error|exception|fatal|fail(ed|ure)|un(defined|resolved))\\\\b\""))
    
    dt("linesBefore")
    dd(_("The number of lines to include before the matching line. " +
         "Lines that overlap with another match or linesAfter are only inlcuded once. " +
         "Defaults to 0."))
    
    dt("linesAfter")
    dd(_("The number of lines to include after the matching line. " +
         "Lines that overlap with another match or linesBefore are only included once. " +
         "Defaults to 0."))
  
    dt("maxMatches")
    dd(_("The maximum number of matches to include from the head of the log. If 0, all matches will be included. " +
         "Defaults to 0."))
  
    dt("maxTailMatches")
    dd(_("The maximum number of matches to include from the tail of the log. When combined with maxMatches, it further limits the matches to the tail end of matched results. " +
         "If 0, all matches will be included. Defaults to 0."))
  
    dt("maxLineLength")
    dd(_("A maximum length for log lines. When lines are longer than the specified value, they are truncated and a \"...\" marker is appended at the end. " +
         "If 0, no truncation is done. Defaults to 0."))

    dt("showTruncatedLines")
    dd(_("If true, include [...truncated ### lines...] lines. " +
         "Defaults to true."))
    
    dt("substText")
    dd(_("If non-null, insert this text into the email rather than the " +
         "entire line. Defaults to null."))
   
    dt("escapeHtml")
    dd(_("If true, escape HTML. Defauts to false."))
  
    dt("matchedLineHtmlStyle")
    dd(_("If non-null, output HTML. Matched lines will become <b style=\"your-style-value\"> " +
         "html escaped matched line</b>. Defaults to null."))
    
    dt("addNewline")
    dd(_("If true, adds a newline after subsText. Defaults to true."))
  
    dt("defaultValue")
    dd(_("This value will be used if nothing is replaced."))
  
    dt("greedy")
    dd(_("When false and maxMatches is non-zero it causes more conservative addition of results when used with other parameters such as linesBefore and linesAfter"))
  }
}
