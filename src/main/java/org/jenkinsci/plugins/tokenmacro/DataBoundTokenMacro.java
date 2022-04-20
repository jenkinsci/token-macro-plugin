/*
 * The MIT License
 *
 * Copyright 2011 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.tokenmacro;

import com.google.common.collect.ListMultimap;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.beanutils.ConvertUtils;

import java.beans.Introspector;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Convenient base class for implementing {@link TokenMacro} that does parameter databinding to fields.
 *
 * <p>
 * When you define your token macro as a subtype of this class, a fresh instance is created for each
 * evaluation, and fields or setters with the {@link Parameter} annotation will receive the corresponding
 * parameter values, then the {@link #evaluate(AbstractBuild, TaskListener, String)} method gets invoked.
 *
 * <p>
 * In this way, you simplify the parameter parsing and type conversion overhead.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class DataBoundTokenMacro extends TokenMacro {

    @Retention(RUNTIME)
    @Target({FIELD,METHOD})
    public @interface Parameter {
        boolean required() default false;

	    String alias() default "";
    }

    private interface Setter {
        /**
         * Type of the value this field or the setter method expects.
         */
        Class<?> getType();

        /**
         * Adds a new value to this field or setter.
         */
        void set(Object target, Object value);

        boolean required();
    }

    private Map<String,Setter> setters;
    @Parameter
    public boolean escapeHtml = false;

    public DataBoundTokenMacro() {
        buildMap();
    }

    /**
     * Builds up the {@link #setters} map that encapsulates where/how to set the value.
     */
    private void buildMap() {
        if (setters!=null)  return;

        setters = new ConcurrentHashMap<>();
        for (final Field f : getClass().getFields()) {
            final Parameter p = f.getAnnotation(Parameter.class);
            if (p !=null) {
                String name = f.getName();
                if (StringUtils.isNotEmpty(p.alias())) {
                    name = p.alias();
                }

                setters.put(name,new Setter() {
                    public Class<?> getType() {
                        return f.getType();
                    }

                    public void set(Object target, Object value) {
                        try {
                            f.set(target,value);
                        } catch (IllegalAccessException e) {
                            throw (IllegalAccessError)new IllegalAccessError(e.getMessage()).initCause(e);
                        }
                    }

                    public boolean required() {
                        return p.required();
                    }
                });
            }
        }

        for (final Method m : getClass().getMethods()) {
            final Parameter p = m.getAnnotation(Parameter.class);
            if (p !=null) {
                final Class<?>[] pt = m.getParameterTypes();
                if (pt.length!=1)
                    throw new IllegalArgumentException("Expecting one-arg method for @Parameter but found "+m+" instead");

                String name = m.getName();
                if (name.startsWith("set")) {
                    name = Introspector.decapitalize(name.substring(3));
                }

                if (StringUtils.isNotEmpty(p.alias())) {
                    name = p.alias();
                }

                setters.put(name,new Setter() {
                    public Class<?> getType() {
                        return pt[0];
                    }

                    public void set(Object target, Object value) {
                        try {
                            m.invoke(target,value);
                        } catch (IllegalAccessException e) {
                            throw (IllegalAccessError)new IllegalAccessError(e.getMessage()).initCause(e);
                        } catch (InvocationTargetException e) {
                            throw new Error(e);
                        }
                    }

                    public boolean required() {
                        return p.required();
                    }
                });
            }
        }
    }

    private DataBoundTokenMacro prepare(String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException {
        DataBoundTokenMacro copy;
        try {
            copy = getClass().newInstance();

            for (Entry<String, String> e : argumentMultimap.entries()) {
                Setter s = setters.get(e.getKey());
                if (s==null)
                    throw new MacroEvaluationException(MessageFormat.format("Undefined parameter {0} in token {1}", e.getKey(),macroName));

                Object v;
                if (s.getType()==boolean.class && e.getValue()==null)
                    v = true;
                else
                    v = ConvertUtils.convert(e.getValue(), s.getType());

                s.set(copy, v);
            }

            for (Entry<String, Setter> e : setters.entrySet()) {
                if (!arguments.containsKey(e.getKey()) && e.getValue().required())
                    throw new MacroEvaluationException(MessageFormat.format("Parameter {0} in token {1} is required but was not specfified", e.getKey(), macroName));
            }
        } catch (InstantiationException e) {
            throw new Error(e);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }

        return copy;
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException, IOException, InterruptedException {
        DataBoundTokenMacro copy = prepare(macroName,arguments,argumentMultimap);
        String res = copy.evaluate(build, listener, macroName);
        if (copy.escapeHtml && !copy.handlesHtmlEscapeInternally()) {
            res = StringEscapeUtils.escapeHtml(res);
        }
        return res;
    }

    @Override
    public String evaluate(Run<?,?> run, FilePath workspace, TaskListener listener, String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException, IOException, InterruptedException {
        DataBoundTokenMacro copy = prepare(macroName,arguments,argumentMultimap);
        String res = copy.evaluate(run, workspace, listener, macroName);
        if (copy.escapeHtml && !copy.handlesHtmlEscapeInternally()) {
            res = StringEscapeUtils.escapeHtml(res);
        }
        return res;
    }

    /**
     * Indicates whether this macro handles {@link #escapeHtml} on its own inside the <code>evaluate</code> methods.
     *
     * If this method returns <code>false</code> and {@link #escapeHtml} is <code>true</code> then the returned value from
     * {@link #evaluate(AbstractBuild, TaskListener, String)} and {@link #evaluate(Run, FilePath, TaskListener, String)}
     * will be escaped. If this method returns <code>true</code> no escaping will be performed,
     * and it is assumed the escaping will be handled internally by the implementing class. It is then also assumed that
     * the <code>help.jelly</code> file for that class mentions the {@link #escapeHtml} parameter.
     *
     * @return true if the implementing class handles its own html escaping.
     */
    public boolean handlesHtmlEscapeInternally() {
        return false;
    }

    public abstract String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException;

    public String evaluate(Run<?,?> run, FilePath workspace, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
        return macroName + " is not supported in this context";
    }

    @Override
    public boolean hasNestedContent() {
        return false;
    }
}
