package com.skytizens.alfresco.webscript;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.util.ClassManager;
import org.alfresco.util.interfaces.CustomDeclarativeWebScript;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class QrcodePathFormEncoded extends CustomDeclarativeWebScript {
	
	private static String MODULE_NAME = "QrcodeGen";
	private static String MODULE_CODE = "0";
	private static String MODULE_ENCODED_CODE = "0";
	
	private boolean useModule = false;
	
	private ServiceRegistry serviceRegistry;

	/**
	 * @param serviceRegistry the serviceRegistry to set
	 */
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	/**
	 * @param classManager the classManager to set
	 */
	public void setClassManager(ClassManager classManager) {
		useModule = MODULE_CODE.equals(classManager.getVerifyCode(MODULE_ENCODED_CODE, this));
	}
	
	public void setParams(Map<String,String> params){
		
	
	}
	
	/**
	 * @param command the command to set
	 */
	public void setObjects(Map<String,Object> objects) {
	}

	/**
	 * Webscript execute method
	 * Txt2PDF form value
	 * 
	 * 
	 */
	@Override
	public Map<String, Object> executeImplExt(WebScriptRequest req, Status status, Cache cache)
	{
		if(useModule)
		{			
			try {
				Map<String, Object> model = new HashMap<String, Object>();
				return model;
				
			} catch (Exception e) {
				e.printStackTrace();
				throw new WebScriptException("Cannot add definition.");
			}

		}
		else
		{
			throw new AlfrescoRuntimeException("You cannot use module: " + MODULE_NAME);
		}
	}
}
