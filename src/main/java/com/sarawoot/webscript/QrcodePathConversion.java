package com.skytizens.alfresco.webscript;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.ClassManager;
import org.alfresco.util.CodeTransformer;
import org.alfresco.util.interfaces.CustomAbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class QrcodePathConversion extends CustomAbstractWebScript {

	private CustomAbstractWebScript worker;

	public QrcodePathConversion(ClassManager classManager) {
		super();
		CodeTransformer codeTrans = new CodeTransformer(classManager);
		worker = (CustomAbstractWebScript) codeTrans
				.getObject("com.skytizens.alfresco.webscript.QrcodePathConversionEncoded", getClass());
		if (worker == null) {
			worker = new CustomAbstractWebScript() {
			};
		} else {
			worker.setClassManager(classManager);
		}
	}

	@Override
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		worker.setServiceRegistry(serviceRegistry);
	}

	@Override
	public void setClassManager(ClassManager classManager) {
		// worker.setClassManager(classManager);
	}

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		worker.execute(req, res);
	}

	@Override
	public void setParams(Map<String, String> params) {
		worker.setParams(params);
	}

	@Override
	public void setObjects(Map<String, Object> objects) {
		worker.setObjects(objects);
	}
	
	public static Object generateImageToAlfresco(final Object object, final RetryingTransactionHelper retryingTransactionHelper, final String currentUser, 
			final NodeRef destination, final File sourceFile, final String fileName, final String ext){
		final RetryingTransactionCallback<Object> processCallBack = new RetryingTransactionCallback<Object>()
		{
			public Object execute() throws Exception
			{   
				object.getClass().getMethod("generateImageToAlfresco", NodeRef.class, File.class, String.class, String.class)
					.invoke(object, destination, sourceFile, fileName, ext);
				
				return null;
			}
		};
		 return AuthenticationUtil.runAs(
				new AuthenticationUtil.RunAsWork<Object>() {
					public Object doWork() throws Exception {
						retryingTransactionHelper.doInTransaction(processCallBack, true, false);
						return null;
					}
				}, currentUser);
	}
}
