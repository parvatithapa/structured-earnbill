package com.jbilling.framework.interfaces;

import com.jbilling.framework.utilities.textutilities.TextUtilities;

/**
 * ElementField objective to parse each element xml/database info to an object
 * form.
 * 
 * @author Aishwarya Dwivedi
 * @since 1.0
 * @version 1.0
 */
public class ElementField {

	public String elementLocatorId = "";

	public String elementLocatorName = "";

	public String elementLocatorXpath = "";

	public String elementLocatorCss = "";

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString () {
        return "ElementField ["
                + (TextUtilities.isEmpty(elementLocatorId)    ? "" : "elementLocatorId="    + elementLocatorId + ", ")
                + (TextUtilities.isEmpty(elementLocatorName)  ? "" : "elementLocatorName="  + elementLocatorName + ", ")
                + (TextUtilities.isEmpty(elementLocatorXpath) ? "" : "elementLocatorXpath=" + elementLocatorXpath + ", ")
                + (TextUtilities.isEmpty(elementLocatorCss)   ? "" : "elementLocatorCss="   + elementLocatorCss)
                + "]";
    }
}
