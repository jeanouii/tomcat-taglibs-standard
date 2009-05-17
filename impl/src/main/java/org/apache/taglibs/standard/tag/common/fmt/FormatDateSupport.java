/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package org.apache.taglibs.standard.tag.common.fmt;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Collections;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.taglibs.standard.resources.Resources;
import org.apache.taglibs.standard.tag.common.core.Util;

import org.apache.taglibs.standard.extra.commons.collections.map.LRUMap;

/**
 * Support for tag handlers for &lt;formatDate&gt;, the date and time
 * formatting tag in JSTL 1.0.
 *
 * @author Jan Luehe
 */

public abstract class FormatDateSupport extends TagSupport {

    //*********************************************************************
    // Private constants

	/**
	 * Name of configuration setting for maximum number of entries in the
	 * cached dateformat map
	 */
	private static final String DATE_CACHE_PARAM = 
		"org.apache.taglibs.standard.tag.common.fmt.dateFormatCacheSize";

    private static final String DATE = "date";
    private static final String TIME = "time";
    private static final String DATETIME = "both";

    private static Map dateFormatCache = null;

	/**
	 * Default maximum  cache size
	 */
	private static final int MAX_SIZE = 100;


    //*********************************************************************
    // Protected state

    protected Date value;                        // 'value' attribute
    protected String type;                       // 'type' attribute
    protected String pattern;                    // 'pattern' attribute
    protected Object timeZone;                   // 'timeZone' attribute
    protected String dateStyle;                  // 'dateStyle' attribute
    protected String timeStyle;                  // 'timeStyle' attribute


    //*********************************************************************
    // Private state

    private String var;                          // 'var' attribute
    private int scope;                           // 'scope' attribute


    //*********************************************************************
    // Constructor and initialization

    public FormatDateSupport() {
	super();
	init();
    }

    private void init() {
	type = dateStyle = timeStyle = null;
	pattern = var = null;
	value = null;
	timeZone = null;
	scope = PageContext.PAGE_SCOPE;
    }


   //*********************************************************************
    // Tag attributes known at translation time

    public void setVar(String var) {
        this.var = var;
    }

    public void setScope(String scope) {
	this.scope = Util.getScope(scope);
    }


    //*********************************************************************
    // Tag logic

    /*
     * Formats the given date and time.
     */
    public int doEndTag() throws JspException {

	String formatted = null;

	if (value == null) {
	    if (var != null) {
		pageContext.removeAttribute(var, scope);
	    }
	    return EVAL_PAGE;
	}

	// Create formatter
	Locale locale = SetLocaleSupport.getFormattingLocale(
            pageContext,
	    this,
	    true,
	    DateFormat.getAvailableLocales());

	if (locale != null) {
	    DateFormat formatter = createFormatter(locale, pattern);

	    // Set time zone
	    TimeZone tz = null;
	    if ((timeZone instanceof String)
		&& ((String) timeZone).equals("")) {
		timeZone = null;
	    }
	    if (timeZone != null) {
		if (timeZone instanceof String) {
		    tz = TimeZone.getTimeZone((String) timeZone);
		} else if (timeZone instanceof TimeZone) {
		    tz = (TimeZone) timeZone;
		} else {
		    throw new JspTagException(
                            Resources.getMessage("FORMAT_DATE_BAD_TIMEZONE"));
		}
	    } else {
		tz = TimeZoneSupport.getTimeZone(pageContext, this);
	    }
	    if (tz != null) {
		formatter.setTimeZone(tz);
	    }
	    formatted = formatter.format(value);
	} else {
	    // no formatting locale available, use Date.toString()
	    formatted = value.toString();
	}

	if (var != null) {
	    pageContext.setAttribute(var, formatted, scope);	
	} else {
	    try {
		pageContext.getOut().print(formatted);
	    } catch (IOException ioe) {
		throw new JspTagException(ioe.toString(), ioe);
	    }
	}

	return EVAL_PAGE;
    }

    // Releases any resources we may have (or inherit)
    public void release() {
	init();
    }


    //*********************************************************************
    // Private utility methods

    private DateFormat createFormatter(Locale loc, String pattern) throws JspException {
	DateFormat formatter = null;

	// lazy initialization of cache
	if (dateFormatCache == null) {
		String value = pageContext.getServletContext().getInitParameter(DATE_CACHE_PARAM);
		if (value != null) {
			dateFormatCache = Collections.synchronizedMap(new LRUMap(Integer.parseInt(value)));
		} else {
			dateFormatCache = Collections.synchronizedMap(new LRUMap(MAX_SIZE));
		}
	}

	// Apply pattern, if present
	if (pattern != null) {
		if ((type == null) || DATE.equalsIgnoreCase(type)) {
		    String key = DATE + pattern + loc;
            formatter = (DateFormat) dateFormatCache.get(key);
            if(formatter == null) {
                formatter = new SimpleDateFormat(pattern, loc);
                dateFormatCache.put(key, formatter);
            }
		} else if (TIME.equalsIgnoreCase(type)) {
		    String key = TIME + pattern + loc;
            formatter = (DateFormat) dateFormatCache.get(key);
            if(formatter == null) {
                formatter = new SimpleDateFormat(pattern, loc);
                dateFormatCache.put(key, formatter);
            }
		} else if (DATETIME.equalsIgnoreCase(type)) {
		    String key = DATETIME + pattern + loc;
            formatter = (DateFormat) dateFormatCache.get(key);
            if(formatter == null) {
                formatter = new SimpleDateFormat(pattern, loc);
                dateFormatCache.put(key, formatter);
            }
		} else {
		    throw new JspException(
  	                  Resources.getMessage("FORMAT_DATE_INVALID_TYPE", 
						 type));
		}
        return formatter;
	}

	if ((type == null) || DATE.equalsIgnoreCase(type)) {
		int style = Util.getStyle(dateStyle, "FORMAT_DATE_INVALID_DATE_STYLE");
		String key = DATE + style + loc;
		formatter = (DateFormat) dateFormatCache.get(key);
		if(formatter == null) {
			formatter = DateFormat.getDateInstance(style, loc);
			dateFormatCache.put(key, formatter);
		}
	} else if (TIME.equalsIgnoreCase(type)) {
		int style = Util.getStyle(timeStyle, "FORMAT_DATE_INVALID_TIME_STYLE");
		String key = TIME + style + loc;
		formatter = (DateFormat) dateFormatCache.get(key);
		if(formatter == null) {
			formatter = DateFormat.getTimeInstance(style, loc);
			dateFormatCache.put(key, formatter);
		}
	} else if (DATETIME.equalsIgnoreCase(type)) {
		int style1 = Util.getStyle(dateStyle, "FORMAT_DATE_INVALID_DATE_STYLE");
		int style2 = Util.getStyle(timeStyle, "FORMAT_DATE_INVALID_TIME_STYLE");
		String key = DATETIME + style1 + loc + style2;
		formatter = (DateFormat) dateFormatCache.get(key);
		if(formatter == null) {
			formatter = DateFormat.getDateTimeInstance(style1, style2, loc);
			dateFormatCache.put(key, formatter);
		}
	} else {
	    throw new JspException(
                    Resources.getMessage("FORMAT_DATE_INVALID_TYPE", 
					 type));
	}

	return formatter;
    }
}