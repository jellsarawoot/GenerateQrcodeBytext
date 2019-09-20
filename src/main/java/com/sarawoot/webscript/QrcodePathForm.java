package com.skytizens.alfresco.webscript;

import java.util.Map;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.util.ClassManager;
import org.alfresco.util.CodeTransformer;
import org.alfresco.util.interfaces.CustomDeclarativeWebScript;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class QrcodePathForm extends CustomDeclarativeWebScript {
	
	private CustomDeclarativeWebScript worker;
	
	public QrcodePathForm(ClassManager classManager)
    {
		super();
    	CodeTransformer codeTrans = new CodeTransformer(classManager);
    	worker = (CustomDeclarativeWebScript) codeTrans.getObject("com.skytizens.alfresco.webscript.QrcodePathFormEncoded", getClass());
    	if(worker == null){
    		worker = new CustomDeclarativeWebScript() {};
    	}else{
    		worker.setClassManager(classManager);
    	}
    }

	@Override
	public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
		worker.setServiceRegistry(serviceRegistry);
    }
    
	@Override
    public void setClassManager(ClassManager classManager)
    {
    	//worker.setClassManager(classManager);
    }
	
	//@Override
	//public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException 
	//{
	//	worker.execute(req,res);
	//}
	
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status stat, Cache cache)
	{
		return worker.executeImplExt(req,stat,cache);
	}
	
	@Override
	public void setParams(Map<String,String> params) 
	{
		worker.setParams(params);
	}
	
	@Override
	public void setObjects(Map<String,Object> objects) 
	{
		worker.setObjects(objects);
	}
}
