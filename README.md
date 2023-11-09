# Jenkins Token Macro Plugin

This plugin adds reusable macro expansion capability for other plugins to use 
in job configurations.

## Motivation
In various Jenkins plugins, such as email-ext and description-setter, a 
user is asked to provide an "expression" that evaluates to text. For 
example, in the case of the email-ext plugin, the whole email content is 
one big expression, so is the subject. This often includes some kind of 
tokens, which evaluates to things like build number, cause, the 
current branch being built, etc. In the description-setter plugin, the 
user configures a similar expression that evaluates to the string that 
becomes a build string. In instant messenger plugin, one might configure 
the template that controls what messages the user will receive.

All these plugins share the common concept of token expansions, where 
something like ${SUBVERSION_REVISION} expands to some textual values, 
and since this notion is useful beyond any single use case, it makes 
sense to define it elsewhere. And this is exactly what this plugin does 
— to define the mechanism for plugins to define tokens (and their 
parameters) and their evaluation, and allow this mechanism to be reused 
by different plugins.

## Token Expansion Model

Much of the definition of this is modeled after the email-ext plugin. In 
the general form, a token can have an arbitrary number of parameters, 
with multiple values allowed for a single parameter name:


| Example | Description |
|---------|-------------|
| ${FOO}  | no parameter |
| $FOO    | alternative syntax for token with no parameter |
| ${FOO,param1=value1,param2=value2} | 2 parameters |
| ${FOO,param=v1,param=v2,param=v3}  | 1 parameter with 3 values |


Those plugins that wish to define custom tokens can do so by providing 
implementations of the TokenMacro class that evaluates this into an 
arbitrary string.

## Using Token Expansion
Those plugins that evaluate an expression can call `TokenMacro.expand` 
to have all the occurrences of the macros expanded by the available definitions.

```java
String template = "My revision is ${SUBVERSION_REVISION} at #${BUILD_NUMBER}"
String text = TokenMacro.expand( build, listener, template );
System.out.println(text); // something like "My revision is 125 at #12"
```

You can also ask to expand all macros managed by the Token Macro plugin 
but also all environment and [build
variables](http://ci.jenkins-ci.org/env-vars.html) using `TokenMacro.expandAll`.

```java
String template = "My revision is ${SUBVERSION_REVISION} at #${BUILD_NUMBER} and was executed on node ${NODE_NAME}"
String text = TokenMacro.expandAll( build, listener, template );
System.out.println(text); // something like "My revision is 125 at #12 and was executed on node Controller"
```

## Databinding Token Macro

The base `TokenMacro` class only defines a minimalistic contract between 
the user of the tokens. To simplify the development of custom tokens, a 
more convenient subtype called `DataBoundTokenMacro` is provided to 
simplify the parameter parsing. In this subtype, you get your parameters 
injected to your instance, so that your `evaluate` method can access 
parameter values in a type-safe manner. The following sample is taken 
from the Git plugin that defines `GIT_REVISION` macro that expands to 
the SHA1 commit ID used for a build, with a parameter that controls the 
length.

For example, this macro can be used like `${GIT_REVISION,length=8}` to 
only show first 8 letters of the commit.

```java
@Extension
public class GitRevisionTokenMacro extends DataBoundTokenMacro {
    /** Number of chars to use */
    @Parameter
    public int length = 40;

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals("GIT_REVISION");
    }

    @Override
    public String evaluate(Run<?,?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        BuildData data = run.getAction(BuildData.class);
        if (data == null) {
            return "";  // shall we report an error more explicitly?
        }

        Revision lb = data.getLastBuiltRevision();
        if (lb == null) {
            return "";
        }

        String s = lb.getSha1String();
        return s.substring(0, Math.min(length, s.length()));
    }
}
```

## Token Transforms

There are a few different transforms that can be applied on top of the 
result of the token itself. These are similar to BASH shell transforms.

```
${#MACRO_NAME} // returns the number of characters in the result of evaluating MACRO_NAME.
${MACRO_NAME:<START>[:LENGTH]} // returns the substring starting at START, and optionally specifying the length. You may use negative numbers similar to BASH.
${MACRO_NAME#<NEEDLE>} // returns the value with <NEEDLE> removed, IF it is at the START of the result value.
${MACRO_NAME%<NEEDLE>} // returns the value with <NEEDLE> removed, IF it is at the END of the result value.
```
