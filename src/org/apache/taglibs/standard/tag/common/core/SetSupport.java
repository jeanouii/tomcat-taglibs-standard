/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */ 

package org.apache.taglibs.standard.tag.common.core;

import java.beans.*;
import java.lang.reflect.*;
import java.util.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import org.apache.taglibs.standard.tag.common.core.*;
import org.apache.taglibs.standard.lang.support.ExpressionEvaluatorManager;
import org.apache.taglibs.standard.resources.Resources;

/**
 * <p>Support for handlers of the &lt;set&gt; tag.</p>
 *
 * @author Shawn Bayern
 */
public class SetSupport extends BodyTagSupport {

    //*********************************************************************
    // Internal state

    protected Object value;                             // tag attribute
    protected boolean valueSpecified;			// status
    protected Object target;                            // tag attribute
    protected String property;                          // tag attribute
    private String var;					// tag attribute
    private int scope;					// tag attribute
    private boolean scopeSpecified;			// status

    //*********************************************************************
    // Construction and initialization

    /**
     * Constructs a new handler.  As with TagSupport, subclasses should
     * not provide other constructors and are expected to call the
     * superclass constructor.
     */
    public SetSupport() {
        super();
        init();
    }

    // resets local state
    private void init() {
        value = var = null;
	scopeSpecified = valueSpecified = false;
	scope = PageContext.PAGE_SCOPE;
    }

    // Releases any resources we may have (or inherit)
    public void release() {
        super.release();
        init();
    }


    //*********************************************************************
    // Tag logic

    public int doEndTag() throws JspException {

        Object result;		// what we'll store in scope:var

        // determine the value by...
        if (value != null) {
	    // ... reading our attribute
	    result = value;
  	} else if (valueSpecified) {
	    // ... accepting an explicit null
	    result = null;
	} else {
	    // ... retrieving and trimming our body
	    if (bodyContent == null || bodyContent.getString() == null)
		result = "";
	    else
	        result = bodyContent.getString().trim();
	}

	// decide what to do with the result
	if (var != null) {

	    /*
             * Store the result, letting an IllegalArgumentException
             * propagate back if the scope is invalid (e.g., if an attempt
             * is made to store something in the session without any
	     * HttpSession existing).
             */
	    if (result != null) {
	        pageContext.setAttribute(var, result, scope);
	    } else {
		if (scopeSpecified)
		    pageContext.removeAttribute(var, scope);
		else
		    pageContext.removeAttribute(var);
	    }

	} else if (target != null) {

	    // save the result to target.property
	    if (target instanceof Map) {
		// ... treating it as a Map entry
		if (result == null)
		    ((Map) target).remove(property);
		else
		    ((Map) target).put(property, result);
	    } else {
		// ... treating it as a bean property
		try {
                    PropertyDescriptor pd[] =
                        Introspector.getBeanInfo(target.getClass())
			    .getPropertyDescriptors();
		    boolean succeeded = false;
                    for (int i = 0; i < pd.length; i++) {
                        if (pd[i].getName().equals(property)) {
			    pd[i].getWriteMethod().invoke(target,
			        new Object[] { result });
			    succeeded = true;
			}
		    }
		    if (!succeeded) {
			throw new JspTagException(
			    Resources.getMessage("SET_INVALID_PROPERTY",
				property));
		    }
		} catch (IllegalAccessException ex) {
		    throw new JspException(ex);
		} catch (IntrospectionException ex) {
		    throw new JspException(ex);
		} catch (InvocationTargetException ex) {
		    throw new JspException(ex);
		}
	    }
	} else {
	    // should't ever occur because of validation in TLV and setters
	    throw new JspTagException();
	}

	return EVAL_PAGE;
    }


    //*********************************************************************
    // Accessor methods

    // for tag attribute
    public void setVar(String var) {
	this.var = var;
    }

    // for tag attribute
    public void setScope(String scope) {
        this.scope = Util.getScope(scope);
	this.scopeSpecified = true;
    }
}