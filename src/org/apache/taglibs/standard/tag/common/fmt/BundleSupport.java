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

package org.apache.taglibs.standard.tag.common.fmt;

import java.io.IOException;
import java.util.*;
import javax.servlet.ServletContext;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import org.apache.taglibs.standard.tag.common.core.Util;
import org.apache.taglibs.standard.resources.Resources;

/**
 * Support for tag handlers for &lt;bundle&gt;, the resource bundle
 * loading tag in JSTL 1.0.
 *
 * @author Jan Luehe
 */

public abstract class BundleSupport extends BodyTagSupport {

    //*********************************************************************
    // Private constants

    private static final String DEFAULT_BASE =
	"javax.servlet.jsp.jstl.fmt.basename";


    //*********************************************************************
    // Protected state

    protected String basename;                          // 'basename' attribute
    protected String prefix;                            // 'prefix' attribute


    //*********************************************************************
    // Private state

    private static ResourceBundle emptyResourceBundle;

    private String scope;                               // 'scope' attribute
    private String var;                                 // 'var' attribute
    private Locale fallbackLocale;
    private ResourceBundle bundle;


    //*********************************************************************
    // Constructor and initialization

    static {
	emptyResourceBundle = new ListResourceBundle() {
		public Object[][] getContents() {
		    return new Object[][] { { "", "" } };
		}
	    };
    }

    public BundleSupport() {
	super();
	init();
    }

    private void init() {
	basename = prefix = var = null;
	bundle = null;
	scope = "page";
    }

    
    //*********************************************************************
    // Tag attributes known at translation time

    public void setVar(String var) {
        this.var = var;
    }

    public void setScope(String scope) {
	this.scope = scope;
    }
    

    //*********************************************************************
    // Collaboration with subtags

    public ResourceBundle getBundle() {
	return bundle;
    }

    public String getPrefix() {
	return prefix;
    }


    //*********************************************************************
    // Tag logic

    public int doStartTag() throws JspException {
	if ((basename != null) && !basename.equals("")) {
	    bundle = getBundle(pageContext, basename);
	}
	return EVAL_BODY_BUFFERED;
    }

    public int doEndTag() throws JspException {
	if (var != null) {
	    if (bundle != null) {
		pageContext.setAttribute(var, bundle, Util.getScope(scope));
	    } else {
		pageContext.setAttribute(var, emptyResourceBundle,
					 Util.getScope(scope));
	    }
	} else if (getBodyContent() == null) {
	    /*
	     * If no 'var' attribute and empty body, we store our base name
	     * in the javax.servlet.jsp.jstl.fmt.basename scoped attribute
	     */
	    pageContext.setAttribute(DEFAULT_BASE + "." + scope, basename,
				     Util.getScope(scope));
	} else {
	    try {
		pageContext.getOut().print(getBodyContent().getString());
	    } catch (IOException ioe) {
		throw new JspTagException(ioe.getMessage());
	    }
	}

	return EVAL_PAGE;
    }

    // Releases any resources we may have (or inherit)
    public void release() {
	init();
    }


    //*********************************************************************
    // Public utility methods
    
    /**
     * Gets the resource bundle with the given base name, whose locale is
     * determined as follows:
     *
     * Check if a match exists between the ordered set of preferred
     * locales and the available locales, for the given base name.
     * The set of preferred locales consists of a single locale
     * (if the <tt>javax.servlet.jsp.jstl.fmt.locale</tt> scoped attribute or
     * context init parameter is present) or is equal to the client's preferred
     * locales determined from the client's browser settings.
     *
     * <p> If no match was found in the previous step, check if a match
     * exists between the fallback locale (given by the
     * <tt>javax.servlet.jsp.jstl.fmt.fallbackLocale</tt> scoped attribute
     * or context init parameter) and the available locales, for the given
     * base name.
     *
     * @param pageContext Page in which the resource bundle with the
     * given base name is requested
     * @param basename Resource bundle base name
     *
     * @return Resource bundle with the given base name for which a match
     * between the preferred (or fallback) and available locales exists, or
     * <tt>null</tt> if no match was found
     */
    public static ResourceBundle getBundle(PageContext pageContext,
					   String basename) {
	ResourceBundle ret = null;
	    
	Locale pref = LocaleSupport.getLocale(pageContext,
					      LocaleSupport.LOCALE, true);
	if (pref != null) {
	    // Preferred locale is application-based
	    ret = findMatch(basename, pref);
	} else {
	    // Preferred locales are browser-based
	    ret = findMatch(pageContext, basename);
	}

	if (ret == null) {
	    // no match found, use fallback locale (if present)
	    pref = LocaleSupport.getLocale(pageContext,
					   LocaleSupport.FALLBACK_LOCALE,
					   false);
	    if (pref != null) {
		ret = findMatch(basename, pref);
	    }
	}

	if (ret != null) {
	    // set response locale
	    LocaleSupport.setResponseLocale(pageContext, ret.getLocale());
	} else {
	    ServletContext sc = pageContext.getServletContext();
	    sc.log(Resources.getMessage("MISSING_RESOURCE_BUNDLE", basename));
	}
	
	return ret;
    }

    /**
     * Gets the resource bundle with the default base name, which is given by
     * the scoped attribute or initialization parameter named
     * javax.servlet.jsp.jstl.fmt.exception.basename.
     *
     * @param pageContext the page in which the resource bundle is requested
     * @name the name of the scoped attribute or initialization parameter
     *
     * @return the requested resource bundle, or <tt>null</tt>
     * if the scoped attribute or initialization parameter with the given name
     * does not exist, or the requested resource bundle does not exist
     */
    public static ResourceBundle getDefaultBundle(PageContext pc) {
	ResourceBundle ret = null;

	String def = (String) Util.getAttribute(pc, DEFAULT_BASE);
	if (def == null)
	    def = pc.getServletContext().getInitParameter(DEFAULT_BASE);
	if (def != null)
	    ret = getBundle(pc, def);

	return ret;
    }


    //*********************************************************************
    // Private utility methods
    
    /*
     * Determines the client's preferred locales from the request, and compares
     * each of the locales (in order of preference) against the available
     * locales in order to determine the best matching locale.
     *
     * @param pageContext the page in which the resource bundle with the
     * given base name is requested
     * @param basename the resource bundle's base name
     *
     * @return the resource bundle with the given base name and best matching
     * locale, or <tt>null</tt> if no match was found
     */
    private static ResourceBundle findMatch(PageContext pageContext,
					    String basename) {
	ResourceBundle match = null;
	
	// Determine locale from client's browser settings.
	for (Enumeration enum = pageContext.getRequest().getLocales();
	     enum.hasMoreElements(); ) {
	    /*
	     * If client request doesn't provide an Accept-Language header,
	     * the returned locale Enumeration contains the runtime's default
	     * locale, so it always contains at least one element.
	     */
	    Locale pref = (Locale) enum.nextElement();
	    match = findMatch(basename, pref);
	    if (match != null) {
		break;
	    }
	}
	
	return match;
    }

    /*
     * Gets the resource bundle with the given base name and preferred locale.
     * 
     * This method calls java.util.ResourceBundle.getBundle(), but ignores
     * its return value unless its locale represents an exact or language match
     * with the given preferred locale.
     *
     * @param basename the resource bundle base name
     * @param pref the preferred locale
     *
     * @return the requested resource bundle, or <tt>null</tt> if no resource
     * bundle with the given base name exists or if there is no exact- or
     * language-match between the preferred locale and the locale of
     * the bundle returned by java.util.ResourceBundle.getBundle().
     */
    private static ResourceBundle findMatch(String basename, Locale pref) {
	ResourceBundle match = null;

	try {
	    ResourceBundle bundle = ResourceBundle.getBundle(basename, pref);
	    Locale avail = bundle.getLocale();
	    if (pref.equals(avail)) {
		// Exact match
		match = bundle;
	    } else {
		if (pref.getLanguage().equals(avail.getLanguage())
		    && ("".equals(avail.getCountry()))) {
		    /*
		     * Language match.
		     * By making sure the available locale does not have a 
		     * country and matches the preferred locale's language, we
		     * rule out "matches" based on the container's default
		     * locale. For example, if the preferred locale is 
		     * "en-US", the container's default locale is "en-UK", and
		     * there is a resource bundle (with the requested base
		     * name) available for "en-UK", ResourceBundle.getBundle()
		     * will return it, but even though its language matches
		     * that of the preferred locale, we must ignore it,
		     * because matches based on the container's default locale
		     * are not portable across different containers with
		     * different default locales.
		     */
		    match = bundle;
		}
	    }
	} catch (MissingResourceException mre) {
	}

	return match;
    }
}
