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

package org.apache.taglibs.standard.tag.common.sql;

import java.util.*;
import javax.servlet.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.jstl.sql.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.jstl.core.Config;
import org.apache.taglibs.standard.tag.common.core.Util;
import org.apache.taglibs.standard.resources.Resources;


/**
 * <p>Tag handler for &lt;SetDataSource&gt; in JSTL, used to create
 * a simple DataSource for prototyping.</p>
 * 
 * @author Hans Bergsten
 * @author Justyna Horwat
 */
public class SetDataSourceTagSupport extends TagSupport {

    protected Object dataSource;
    protected String jdbcURL;
    protected String driverClassName;
    protected String userName;
    protected String password;

    private int scope = PageContext.PAGE_SCOPE;
    private String var;

    //*********************************************************************
    // Accessor methods

    /**
     * Setter method for the scope of the variable to hold the
     * result.
     *
     */
    public void setScope(String scopeName) {
        Util.getScope(scopeName);
    }

    public void setVar(String var) {
	this.var = var;
    }

    //*********************************************************************
    // Tag logic

    public int doStartTag() throws JspException {
        DataSourceWrapper ds;

        if (dataSource != null) {
            DataSourceUtil dsUtil = new DataSourceUtil(dataSource, pageContext);
            ds = (DataSourceWrapper) dsUtil.getDataSource();
        }
        else {
            ds = new DataSourceWrapper();
            try {
            ds.setDriverClassName(getDriverClassName());
            }
            catch (Exception e) {
                throw new JspTagException(
                    Resources.getMessage("DRIVER_INVALID_CLASS", e.getMessage()));
            }
            ds.setJdbcURL(getJdbcURL());
            ds.setUserName(getUserName());
            ds.setPassword(getPassword());
        }

        if (var != null) {
	    pageContext.setAttribute(var, ds, scope);
        }
        else {
            pageContext.setAttribute(Config.SQL_DATASOURCE, ds, scope);
        }
	return SKIP_BODY;
    }


    //*********************************************************************
    // Private utility methods

    private String getDriverClassName() {
	if (driverClassName != null) {
	    return driverClassName;
	}
	return (String) Config.find(pageContext, Config.SQL_DRIVER);
    }

    private String getJdbcURL() {
	if (jdbcURL != null) {
	    return jdbcURL;
	}
	return (String) Config.find(pageContext, Config.SQL_URL);
    }

    private String getUserName() {
	if (userName != null) {
	    return userName;
	}
	return (String) Config.find(pageContext, Config.SQL_USER);
    }

    private String getPassword() {
	if (password != null) {
	    return password;
	}
	return (String) Config.find(pageContext, Config.SQL_PASSWORD);
    }

}