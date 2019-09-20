package com.skytizens.alfresco.actions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.alfresco.repo.action.constraint.BaseParameterConstraint;

public class GenerateQRListSaveasSelector extends BaseParameterConstraint
{
	/** Name constant */
    public static final String NAME = "sky-generate-qr-saveas";
          
    /**
     * @see org.alfresco.service.cmr.action.ParameterConstraint#getAllowableValues()
     */
    protected Map<String, String> getAllowableValuesImpl()
    {   
    	Map<String, String> result = new LinkedHashMap<String, String>();
        result.put("newversion", getI18NLabel("newversion"));
        result.put("newfile", getI18NLabel("newfile"));
        return result;
    }
    
    public String getSorted(){
    	return "false";
    }
}
