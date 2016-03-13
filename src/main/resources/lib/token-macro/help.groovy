st = namespace("jelly:stapler")


org.jenkinsci.plugins.tokenmacro.TokenMacro.all().each { tm ->
    st.include(it:tm, page: "help"/*, optional: true*/)
    br()
}

p(_("In addition to the tokens, you can modify the result of the token expansion using parameter " +
  "expansions. They follow similar rules as bash Parameter Expansions. The supported expansions are: \${#TOKEN} which " +
  "resolves to the length of the expanded token value, \${TOKEN:offset:length} which takes a substring of the token " +
  "result (length is optional and offset and length can both be negative), \${TOKEN#pattern} which matches the pattern " +
  "against the start of the expanded token and removes it if it, \${TOKEN%pattern} which matches the pattern " +
  "against the end of the expanded token and removes it it if matches."))