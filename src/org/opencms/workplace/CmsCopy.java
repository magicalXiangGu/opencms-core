/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/Attic/CmsCopy.java,v $
 * Date   : $Date: 2003/07/11 12:38:54 $
 * Version: $Revision: 1.7 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2003 Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.opencms.workplace;

import com.opencms.core.CmsException;
import com.opencms.file.CmsResource;
import com.opencms.flex.jsp.CmsJspActionElement;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * Provides methods for the copy resources dialog.<p> 
 * 
 * The following files use this class:
 * <ul>
 * <li>/jsp/copy_html
 * </ul>
 *
 * @author  Andreas Zahner (a.zahner@alkacon.com)
 * @version $Revision: 1.7 $
 * 
 * @since 5.1
 */
public class CmsCopy extends CmsDialog {

    // always start individual action id's with 100 to leave enough room for more default actions
    public static final int ACTION_COPY = 100;
    
    public static final String DIALOG_TYPE = "copy";
    public static final String PARAM_KEEPRIGHTS = "keeprights";    

    private String m_paramTarget;
    private String m_paramKeeprights;    
    
    /**
     * Public constructor with JSP action element.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsCopy(CmsJspActionElement jsp) {
        super(jsp);
    }
    
    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsCopy(PageContext context, HttpServletRequest req, HttpServletResponse res) {
        this(new CmsJspActionElement(context, req, res));
    }        

    /**
     * Returns the value of the target parameter, 
     * or null if this parameter was not provided.<p>
     * 
     * The target parameter selects the target name 
     * of the operation.<p>
     * 
     * @return the value of the target parameter
     */    
    public String getParamTarget() {
        return m_paramTarget;
    }
    
    /**
     * Sets the value of the target parameter.<p>
     * 
     * @param value the value to set
     */
    public void setParamTarget(String value) {
        m_paramTarget = value;
    }    

    /**
     * Returns the value of the keeprights parameter.<p>
     * 
     * @return the value of the keeprights parameter
     */    
    public String getParamKeeprights() {
        return m_paramKeeprights;
    }
    
    /**
     * Sets the value of the "keeprights" parameter.<p>
     * 
     * @param value the value of the "keeprights" parameter
     */
    public void setParamKeeprights(String value) {
        m_paramKeeprights = value;
    }    

    /**
     * @see org.opencms.workplace.CmsWorkplace#initWorkplaceRequestValues(org.opencms.workplace.CmsWorkplaceSettings, javax.servlet.http.HttpServletRequest)
     */
    protected void initWorkplaceRequestValues(CmsWorkplaceSettings settings, HttpServletRequest request) {
        // fill the parameter values in the get/set methods
        fillParamValues(request);
        // set the dialog type
        setParamDialogtype(DIALOG_TYPE);
        // set the action for the JSP switch 
        if (DIALOG_TYPE.equals(getParamAction())) {
            setAction(ACTION_COPY);                            
        } else if (DIALOG_CONFIRMED.equals(getParamAction())) {
            setAction(ACTION_CONFIRMED);
        } else if (DIALOG_WAIT.equals(getParamAction())) {
            setAction(ACTION_WAIT);
        } else {                        
            setAction(ACTION_DEFAULT);
            // build title for copy dialog     
            setParamTitle(key("title.copy") + ": " + CmsResource.getName(getParamFile()));
        }      
    } 

    /**
     * Performs the copy action, will be called by the JSP page.<p>
     * 
     * @throws JspException if problems including sub-elements occur
     */
    public void actionCopy() throws JspException {
        // save initialized instance of this class in request attribute for included sub-elements
        getJsp().getRequest().setAttribute(C_SESSION_WORKPLACE_CLASS, this);
        CmsResource res = null;
        try {
            res = getCms().readFileHeader(getParamFile());
            if (performCopyOperation())  {
                // if no exception is caused and "true" is returned copy operation was successful
                getJsp().include(C_FILE_EXPLORER_FILELIST);
            } else  {
                // "false" returned, display "please wait" screen
                getJsp().include(C_FILE_DIALOG_SCREEN_WAIT);
            }    
        } catch (CmsException e) {
            // prepare common message part
            String message = "<p>\n" 
                + key("source") + ": " + getParamFile() + "<br>\n" 
                + key("target") + ": " + getParamTarget() + "\n</p>\n";
            // check if this exception requires a confirmation or error screen
            if ((e.getType() == CmsException.C_FILE_EXISTS) 
            && !(res.isFolder())) {
                // file copy but file already exists, show confirmation dialog
                setParamMessage(message + key("confirm.message." + getParamDialogtype()));
                getJsp().include(C_FILE_DIALOG_SCREEN_CONFIRM);        
            } else {                
                // error during copy, show error dialog
                setParamErrorstack(e.getStackTraceAsString());
                setParamMessage(message + key("error.message." + getParamDialogtype()));
                getJsp().include(C_FILE_DIALOG_SCREEN_ERROR);
            }
        }
    }
    
    /**
     * Performs the resource copying.<p>
     * 
     * @return true, if the resource was copied, otherwise false
     * @throws CmsException if copying is not successful
     */
    private boolean performCopyOperation() throws CmsException {

        // on folder copy display "please wait" screen, not for simple file copy
        CmsResource sourceRes = getCms().readFileHeader(getParamFile());
        if (sourceRes.isFolder() && ! DIALOG_WAIT.equals(getParamAction())) {
            // return false, this will trigger the "please wait" screen
            return false;
        }

        // calculate the target name
        String target = getParamTarget();
        if (target == null) target = "";

        if (! target.startsWith("/")) {
            // target is not an absolute path, add the current parent folder
            target = CmsResource.getParent(getParamFile()) + target; 
        }
        try {
            CmsResource res = getCms().readFileHeader(target);
            if (res.isFolder()) {
                // target folder already exists, so we add the current folder name
                if (! target.endsWith("/")) target += "/";
                target = target + CmsResource.getName(getParamFile());
            }
        } catch (CmsException e) {
            // target folder does not already exist, so target name is o.k.
        }
        
        // set the target parameter value
        setParamTarget(target);        
        
        // delete existing target resource if confirmed by the user
        if (DIALOG_CONFIRMED.equals(getParamAction())) {
            getCms().deleteResource(target);
        }            
        
        // copy the resource       
        getCms().copyResource(getParamFile(), target, "true".equals(getParamKeeprights()));
        return true;
    }
}
