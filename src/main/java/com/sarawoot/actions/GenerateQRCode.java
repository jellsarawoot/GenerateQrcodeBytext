package com.skytizens.alfresco.actions;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.RuntimeActionService;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionDefinition;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.ClassManager;
import org.alfresco.util.CodeTransformer;
import org.alfresco.util.interfaces.CustomActionExecuter;

public class GenerateQRCode extends ActionExecuterAbstractBase implements CustomActionExecuter {
	
	public static final String NAME = "generate-qr";
	
	public static final String PARAM_PROPERTIES_GENERATE_QR = "properties-generate-qr-param"; //text
	public static final String PARAM_SAVE_GENERATE_AS = "save-generate-as-param"; //drop-down
	public static final String PARAM_DESTINATION_FOLDER = "destination-folder-param"; //select control for destination folder
	public static final String PARAM_DESTINATION_MIMETYPE = "destination-mimetype-param"; //drop-down
	
	public static final String PARAM_QR_X_LOCATION = "qr-x-location-param"; //text
	public static final String PARAM_QR_Y_LOCATION = "qr-y-location-param"; //text
	public static final String PARAM_QR_SIZE = "qr-size-param"; //text
	
    private CustomActionExecuter worker;
	
	public GenerateQRCode(ClassManager classManager)
    {
		super();
    	CodeTransformer codeTrans = new CodeTransformer(classManager);
    	worker = (CustomActionExecuter) codeTrans.getObject("com.skytizens.alfresco.actions.GenerateQRCodeEncoded", getClass());
    	if(worker == null){
    		worker = new CustomActionExecuter() {

				@Override
				public String getQueueName() {return null;}

				@Override
				public boolean getIgnoreLock() {return false;}

				@Override
				public boolean getTrackStatus() {return false;}

				@Override
				public ActionDefinition getActionDefinition() {return null;}

				@Override
				public void execute(Action action, NodeRef actionedUponNodeRef) {}

				@Override
				public void init() {}

				@Override
				public void setServiceRegistry(ServiceRegistry serviceRegistry) {}

				@Override
				public void setClassManager(ClassManager classManager) {}

				@Override
				public void setParameters(Map<String, String> parameters) {}

				@Override
				public void setRuntimeActionService(RuntimeActionService runtimeActionService) {}

				@Override
				public void setLockService(LockService lockService) {}

				@Override
				public void setBaseNodeService(NodeService nodeService) {}

				@Override
				public void setDictionaryService(DictionaryService dictionaryService) {}

				@Override
				public void setTrackStatus(boolean trackStatus) {}

				@Override
				public void setObjects(Map<String, Object> objects) {
					// TODO Auto-generated method stub
					
				}
    			
    		};
    	}else{
    		worker.setClassManager(classManager);
    	}
    }
	
	/**
	 * @param serviceRegistry the serviceRegistry to set
	 */
	@Override
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		worker.setServiceRegistry(serviceRegistry);
	}
	
	@Override
	public void setClassManager(ClassManager classManager) {
		// worker.setClassManager(classManager);
	}
	
	@Override
	public void setParameters(Map<String, String> parameters) {
		worker.setParameters(parameters);
	}

	@Override
	public void execute(Action action, NodeRef actionedUponNodeRef) {
		worker.execute(action, actionedUponNodeRef);
	}
	
	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {		
		paramList.add(new ParameterDefinitionImpl(PARAM_PROPERTIES_GENERATE_QR, DataTypeDefinition.TEXT, false, getParamDisplayLabel(PARAM_PROPERTIES_GENERATE_QR)));
		paramList.add(new ParameterDefinitionImpl(PARAM_SAVE_GENERATE_AS, DataTypeDefinition.TEXT, true, getParamDisplayLabel(PARAM_SAVE_GENERATE_AS), false, "sky-generate-qr-saveas"));
		paramList.add(new ParameterDefinitionImpl(PARAM_DESTINATION_FOLDER, DataTypeDefinition.NODE_REF, false, getParamDisplayLabel(PARAM_DESTINATION_FOLDER)));
		paramList.add(new ParameterDefinitionImpl(PARAM_DESTINATION_MIMETYPE, DataTypeDefinition.TEXT, true, getParamDisplayLabel(PARAM_DESTINATION_MIMETYPE), false, "sky-generate-dest-mimetype"));
		paramList.add(new ParameterDefinitionImpl(PARAM_QR_X_LOCATION, DataTypeDefinition.TEXT, false, getParamDisplayLabel(PARAM_QR_X_LOCATION)));
		paramList.add(new ParameterDefinitionImpl(PARAM_QR_Y_LOCATION, DataTypeDefinition.TEXT, false, getParamDisplayLabel(PARAM_QR_Y_LOCATION)));
		paramList.add(new ParameterDefinitionImpl(PARAM_QR_SIZE, DataTypeDefinition.TEXT, false, getParamDisplayLabel(PARAM_QR_SIZE)));
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
	
	public static void updateVersion(final Object object, final RetryingTransactionHelper retryingTransactionHelper, final String currentUser,
			final NodeRef nodeRef, final File sourceFile, final String ext){
		final RetryingTransactionCallback<Object> processCallBack = new RetryingTransactionCallback<Object>()
		{
			public Object execute() throws Exception
			{   
				object.getClass().getMethod("updateVersion", NodeRef.class, File.class, String.class)
					.invoke(object, nodeRef, sourceFile, ext);
				
				return null;
			}
		};
		AuthenticationUtil.runAs(
				new AuthenticationUtil.RunAsWork<Object>() {
					public Object doWork() throws Exception {
						retryingTransactionHelper.doInTransaction(processCallBack, true, false);
						return null;
					}
				}, currentUser);
	}
}
