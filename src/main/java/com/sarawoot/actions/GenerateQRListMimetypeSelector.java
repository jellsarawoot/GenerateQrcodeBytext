package com.skytizens.alfresco.actions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.alfresco.repo.action.constraint.BaseParameterConstraint;

public class GenerateQRListMimetypeSelector extends BaseParameterConstraint
{
	/** Name constant */
    public static final String NAME = "sky-generate-dest-mimetype";
          
    /**
     * @see org.alfresco.service.cmr.action.ParameterConstraint#getAllowableValues()
     */
    protected Map<String, String> getAllowableValuesImpl()
    {   
    	Map<String, String> result = new LinkedHashMap<String, String>();
        result.put("source", getI18NLabel("source"));
        result.put("pdf", getI18NLabel("pdf"));
        result.put("tiff", getI18NLabel("tiff"));         
        return result;
    }
    
    public String getSorted(){
    	return "false";
    }
}
