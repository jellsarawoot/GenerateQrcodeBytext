package com.skytizens.alfresco.actions;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.model.filefolder.HiddenAspect;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.version.Version2Model;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.Period;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ClassManager;
import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.interfaces.CustomActionExecuter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.extensions.webscripts.WebScriptException;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.itextpdf.awt.geom.AffineTransform;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

public class GenerateQRCodeEncoded extends ActionExecuterAbstractBase implements CustomActionExecuter {
	
	private static Log logger = LogFactory.getLog(GenerateQRCodeEncoded.class);

	private static String MODULE_NAME = "QrcodeGen";
	private static String MODULE_CODE = "0";
	private static String MODULE_ENCODED_CODE = "0";
	
	private final static String FILENAME_REPLACE = "[\\\"\\*\\\\\\>\\<\\?\\/\\:\\|]";

	private boolean useModule = false;

	private ServiceRegistry serviceRegistry;
	private NodeService nodeService;
	private ContentService contentService;
	private MimetypeService mimetypeService;
	private RetryingTransactionHelper retryingTransactionHelper;
	private NamespaceService nspr;
	private DictionaryService dictionaryService;
	private CheckOutCheckInService checkOutCheckInService;
	
	private String logoPath;
	private String convertPath;
	private boolean insideImage;
	private String confPath;
	private String command;

	/**
	 * @param serviceRegistry
	 *            the serviceRegistry to set
	 */
	@Override
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.nodeService = serviceRegistry.getNodeService();
		this.contentService = serviceRegistry.getContentService();
		this.mimetypeService = serviceRegistry.getMimetypeService();
		this.retryingTransactionHelper = serviceRegistry.getRetryingTransactionHelper();
		this.dictionaryService = serviceRegistry.getDictionaryService();
		this.checkOutCheckInService = serviceRegistry.getCheckOutCheckInService();
		this.nspr = serviceRegistry.getNamespaceService();
	}

	@Override
	public void setClassManager(ClassManager classManager) {
		useModule = MODULE_CODE.equals(classManager.getVerifyCode(MODULE_ENCODED_CODE, this));
	}

	@Override
	public void setParameters(Map<String, String> parameters) {
		this.logoPath = parameters.containsKey("logoPath") ? parameters.get("logoPath") : null;
		this.convertPath = parameters.containsKey("convertPath") ? parameters.get("convertPath") : null;
		this.insideImage = parameters.containsKey("insideImage") ? Boolean.valueOf(parameters.get("insideImage")) : false;
		this.confPath = parameters.containsKey("confPath") ? parameters.get("confPath") : null;
		this.command = parameters.containsKey("command") ? parameters.get("command") : null;
	}
	
	@Override
	public void setObjects(Map<String, Object> objects) {
		
	}

	@Override
	protected void executeImpl(Action action, NodeRef nodeRef) {
		if (useModule) {
			if (nodeService.exists(nodeRef)) {				
				String propertiesGenerateQR = (String) action.getParameterValue(GenerateQRCode.PARAM_PROPERTIES_GENERATE_QR);
				String saveGenerateas = (String) action.getParameterValue(GenerateQRCode.PARAM_SAVE_GENERATE_AS);
				NodeRef destinationFolder = (NodeRef) action.getParameterValue(GenerateQRCode.PARAM_DESTINATION_FOLDER);
				String destinationMimetype = (String) action.getParameterValue(GenerateQRCode.PARAM_DESTINATION_MIMETYPE);
				String xLocation = (String) action.getParameterValue(GenerateQRCode.PARAM_QR_X_LOCATION);
				String yLocation = (String) action.getParameterValue(GenerateQRCode.PARAM_QR_Y_LOCATION);
				String size = (String) action.getParameterValue(GenerateQRCode.PARAM_QR_SIZE);
				
				String qrcodeText = null;
				if((propertiesGenerateQR != null) && (!propertiesGenerateQR.isEmpty())){
					qrcodeText = translateQRCode(propertiesGenerateQR, nodeRef);
				}
				
				boolean isNewversion = false;
				boolean isNewfile = false;
				if((saveGenerateas != null) && (!saveGenerateas.isEmpty())){
					switch(saveGenerateas){
						case "newversion":
							isNewversion = true;
						break;
						case "newfile":
							isNewfile = true;
						break;
						default:						
						break;
					}
				}

				String destinationExt = null;
				if((destinationMimetype != null) && (!destinationMimetype.isEmpty())){
					switch(destinationMimetype){
						case "source":
							ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
							destinationExt = mimetypeService.getExtension(reader.getMimetype());					
						break;
						default:
							destinationExt = destinationMimetype;
						break;
					}
				}else{
					destinationExt = "bin";
				}
				
				float xLocationPosition = 0;
				if((xLocation != null) && (!xLocation.isEmpty())){
					xLocationPosition = Float.parseFloat(xLocation);
				}
				
				float yLocationPosition = 0;
				if((yLocation != null) && (!yLocation.isEmpty())){
					yLocationPosition = Float.parseFloat(yLocation);
				}
				
				int qrSize = 0;
				if((size != null) && (!size.isEmpty())){
					qrSize = Integer.parseInt(size);
				}				
											
				if((qrcodeText != null) && (isNewversion || isNewfile)){				
					try {
						String currentUser = AuthenticationUtil.getRunAsUser();
						String nodeID = nodeRef.getId();
						File tempDir = TempFileProvider.getTempDir();
						
						String sourcePath = tempDir.getAbsolutePath() + "/" + getClass().getSimpleName() + "_sign_source_"
								+ nodeID + ".pdf";
						String destPath = tempDir.getAbsolutePath() + "/" + getClass().getSimpleName() + "_sign_destPath_"
								+ nodeID + ".pdf";
						String tiffPath = tempDir.getAbsolutePath() + "/" + getClass().getSimpleName() + "_sign_tiff_"
								+ nodeID + ".tiff";;
						File tiff = null;
						
						BufferedImage qrCodeImage = null;
						if(insideImage){
							File src = new File(logoPath);
							double percent = 10;
							qrCodeImage = createQRImageWithImageInside(qrcodeText, qrSize, percent, qrSize, src);
						}
						else{
							qrCodeImage = createQRImage(qrcodeText, qrSize);
						}
						
						NodeRef confParentNodeRef = getNodeRef(confPath);
			        	NodeRef template = getFile(confParentNodeRef, "template.html");
						if(template != null){
							Map<String, Object> model = new HashMap<String, Object>();
							model.put("text", qrcodeText);
							model.put("size", qrSize);
							
							ByteArrayOutputStream out = new ByteArrayOutputStream();
							ImageIO.write(qrCodeImage, "png", out);
							out.flush();
							out.close();
							byte[] bytes = out.toByteArray();
							model.put("qrcode", getSignChunk(Base64.encodeBase64String(bytes)));
							
							String content = serviceRegistry.getTemplateService().processTemplate("freemarker", template.toString(), model);
							
							File htmlFile = TempFileProvider.createTempFile(
			                        "GenerateQRCode_source_tmp_",
			                        ".html");
			        		
			        		FileOutputStream fos = new FileOutputStream(htmlFile);
			        		byte[] contentInBytes = content.getBytes("UTF8");
			        		fos.write(contentInBytes);
			        		fos.flush();
			        		fos.close();
			        		
			        		Process proc = Runtime.getRuntime().exec(command + " " + htmlFile.getAbsolutePath() + " " + sourcePath);
			        		proc.waitFor();
						}else{
							createPdfWithQrcode(qrcodeText, sourcePath, qrSize, destPath, qrCodeImage, xLocationPosition, yLocationPosition);
						}
						
						File srcFile = new File(sourcePath);
						if(srcFile.exists()){
							if(mimetypeService.getMimetypes(destinationExt).equals(MimetypeMap.MIMETYPE_IMAGE_TIFF)){
								StringBuilder ccmd = new StringBuilder();
								ccmd.append(this.convertPath);
								ccmd.append(" -s " + sourcePath);
								ccmd.append(" -d " + tiffPath);
								ccmd.append(" -tifm ");
								ccmd.append(" -r 200");
								Process pr = Runtime.getRuntime().exec(ccmd.toString());
								if (pr.waitFor() == 0) {
									tiff = new File(tiffPath);
									if (tiff.exists()) {							
										if(isNewfile){
											GenerateQRCode.generateImageToAlfresco(this, retryingTransactionHelper, currentUser,
													destinationFolder, tiff, qrcodeText, destinationExt);
										}
										if(isNewversion){
											GenerateQRCode.updateVersion(this, retryingTransactionHelper, currentUser, 
													nodeRef, tiff, destinationExt);											
										}																			
									}
									srcFile.delete();
								}
							}else {
								if(isNewfile){
									GenerateQRCode.generateImageToAlfresco(this, retryingTransactionHelper, currentUser,
											destinationFolder, srcFile, qrcodeText, destinationExt);
								}
								if(isNewversion){									
									GenerateQRCode.updateVersion(this, retryingTransactionHelper, currentUser, 
											nodeRef, srcFile, destinationExt);
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						throw new WebScriptException("Cannot Generate Qrcode");
					}				
				}				
			}
		} else {
			throw new AlfrescoRuntimeException("You cannot use module: " + MODULE_NAME);
		}
	}
	
	private List<String> getSignChunk(String value)
	{
		String[] chunks = value.split("(?<=\\G.{4098})");
		List<String> list = new ArrayList<String>();
		for(String chunk : chunks){
			list.add(chunk);
		}
		return list;
	}

	private void createPdfWithQrcode(String title, String sourcePath, int qrbordersize, String destPath, BufferedImage imageInPDF, float xLocationPosition, float yLocationPosition){
		
		try {
			Document document = new Document();
			// step 2
			PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(sourcePath));
			// step 3
			document.open();
			// step 4
			PdfContentByte pdfCB = new PdfContentByte(writer);
	        Image image = Image.getInstance(pdfCB, imageInPDF, 1);
	        
			PdfContentByte canvas = writer.getDirectContentUnder();

			image.setAbsolutePosition(xLocationPosition, yLocationPosition);

			document.add(image);
			writer.setCompressionLevel(0);
			canvas.saveState(); // q
			canvas.beginText(); // BT
			canvas.moveText(40, ((document.getPageSize().getHeight()-qrbordersize)/2)-20);// 36 788 Td
			canvas.setFontAndSize(BaseFont.createFont(), 14); // /F1 12 Tf

			Font font = FontFactory.getFont("Arial Unicode MS", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 14);
			Paragraph p = new Paragraph(title, font);
			p.setAlignment(Element.ALIGN_JUSTIFIED);
			document.add(p);
			
			canvas.endText(); // ET
			canvas.restoreState(); // Q
			// step 5
			document.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private BufferedImage createQRImageWithImageInside(String qrCodeText, int size, double percent, int qrbordersize, File inputPng) throws WriterException, IOException{

		//	ByteArrayOutputStream os = new ByteArrayOutputStream();
		// Create the ByteMatrix for the QR-Code that encodes the given String
		Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		hints.put(EncodeHintType.MARGIN,qrbordersize); /* default = 4 */
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, size , size, hints);

		// Load QR image
		BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix, getMatrixConfig());

		// Load logo image
		BufferedImage overly = ImageIO.read(inputPng);

		// Calculate the scale height and width between QR code and logo
		int scaledWidth = qrImage.getWidth()- overly.getWidth();
		int scaledHeight = qrImage.getHeight()-overly.getHeight();
		double newwidth=0;
		double logoOverWidth= 0;
		//LogoHeight is LogoHeight inside.
		double logoHeight = ((double)qrImage.getHeight()/100.0d)*(double)percent;
		//Load logo image isn't Square.
		if (overly.getWidth()>qrImage.getWidth()){ 
			logoOverWidth = ((double)overly.getWidth()/(double)overly.getHeight());
			newwidth = (double)(logoOverWidth*logoHeight);
		}else {
			newwidth = logoHeight;
		}  
		// Initialize combined image
		BufferedImage combined = new BufferedImage(qrImage.getWidth(),qrImage.getHeight(), BufferedImage.TYPE_INT_RGB);
		combined.createGraphics();
		Graphics2D graphics = (Graphics2D) combined.getGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, scaledWidth, scaledHeight);

		// Paint and save the image using the ByteMatrix
		graphics.setColor(Color.BLACK);

		// Write QR code to new image at position 0/0
		graphics.drawImage(qrImage, 0, 0, null);
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

		// Write logo into combine image at position (deltaWidth / 2) and
		// (deltaHeight / 2). Background: Left/Right and Top/Bottom must be
		// the same space for the logo to be centered 
		int bgsizeHeight = (qrImage.getHeight()- (int)logoHeight)/2;
		int bgsizeWidth = (qrImage.getWidth()- (int)newwidth)/2;
		graphics.drawImage(overly, bgsizeWidth, bgsizeHeight, (int)newwidth, (int)logoHeight, null);

		return combined;
	}

	private BufferedImage createQRImage(String qrCodeText, int size) throws WriterException, IOException {
		//	ByteArrayOutputStream os = new ByteArrayOutputStream();
		// Create the ByteMatrix for the QR-Code that encodes the given String
		Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
		hints.put(EncodeHintType.MARGIN,2);
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix byteMatrix = qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, size, size, hints);
		// Make the BufferedImage that are to hold the QRCode
		int matrixWidth = byteMatrix.getWidth();
		BufferedImage image = new BufferedImage(matrixWidth, matrixWidth, BufferedImage.TYPE_INT_RGB);
		image.createGraphics();

		Graphics2D graphics = (Graphics2D) image.getGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, matrixWidth, matrixWidth);
		// Paint and save the image using the ByteMatrix
		graphics.setColor(Color.BLACK);

		for (int i = 0; i < matrixWidth; i++) {
			for (int j = 0; j < matrixWidth; j++) {
				if (byteMatrix.get(i, j)) {
					graphics.fillRect(i, j, 1, 1);
				}
			}
		}
		
		return image;
	}

	private static MatrixToImageConfig getMatrixConfig() {
		// ARGB Colors
		// Check Colors ENUM
		return new MatrixToImageConfig(GenerateQRCodeEncoded.Colors.BLACK.getArgb(), GenerateQRCodeEncoded.Colors.WHITE.getArgb());
	}
	
	public enum Colors {
		WHITE(0xFFFFFFFF),
		BLACK(0xFF000000);

		private final int argb;

		Colors(final int argb){
			this.argb = argb;
		}

		public int getArgb(){
			return argb;
		}
	}
	
	private String translateQRCode(String qrCode, NodeRef nodeRef){		
		Calendar cal = Calendar.getInstance(Locale.ENGLISH);
		cal.setTime(new Date());

		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);

		String pattern = "\\{(.*?)\\}";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(qrCode);

		while (m.find()) {
		   String group = m.group(0);
		   String innerText = group.substring(1, group.length() - 1);		   
		   if(innerText.contains("_")){
			   String[] splitted = innerText.split("_");
			   String prefix = splitted[0];
			   String localName = splitted[1];
			   QName propertyQName = getPropertyQname(prefix, localName);
			   if(propertyQName != null){
				   Serializable value = nodeService.getProperty(nodeRef, propertyQName);
				   
				   String value2 = getValue(value, getType(propertyQName));
				   if(value2 != null){
					   qrCode = qrCode.replace(group, value2);
				   }
			   }
		   }else if(innerText.equalsIgnoreCase("yyyy")){
			   qrCode = qrCode.replace(group, String.valueOf(year));
		   }else if(innerText.equalsIgnoreCase("mm")){
			   qrCode = qrCode.replace(group, ((month < 10) ? "0" : "") + String.valueOf(month));
		   }else if(innerText.equalsIgnoreCase("dd")){
			   qrCode = qrCode.replace(group, ((day < 10) ? "0" : "") + String.valueOf(day));
		   }
		}		
		return qrCode;
	}

	private QName getPropertyQname(String prefix, String localName){
		QName qname = null;
		String uri = nspr.getNamespaceURI(prefix);
        if (uri != null)
        {
            qname = QName.createQName(uri, localName);
        }
        return qname;
	}
	
	private String getValue(Serializable value, String propType) {
		String result = null;
		if((propType != null) && (value != null)){		
			switch (propType) {
			case "text": 
				result = (String) value;
				break;
			case "mltext": 				
				MLText ml = (MLText) value;
				result = ml.getDefaultValue();
				break;
			case "int":
				result = String.valueOf(((int) value));
				break;
			case "long":							
				result = String.valueOf(((Long) value));
				break;
			case "float":								
				result = String.valueOf(((Float) value));
				break;
			case "double":				
				result = String.valueOf(((Double) value));
				break;
			case "date": 				
				result = ISO8601DateFormat.format(((Date) value));	
				break;
			case "datetime": 				
				result = ISO8601DateFormat.format(((Date) value));
				break;
			case "boolean":				
				result = String.valueOf(((Boolean) value));						
				break;
			case "noderef":
				result = ((NodeRef) value).toString();
				break;
			case "qname":				
				result = ((QName) value).toString();
				break;
			case "category":
				result = (String) value;
				break;
			case "path":				
				result = ((Path) value).toString();
				break;
			case "period":				
				result = ((Period) value).toString();
				break;
			default: 
				break;
			}
		}else{
			result = null;
		}		
		return result;
	}

	private String getType(QName propertyQName){
		PropertyDefinition def = dictionaryService.getProperty(propertyQName);
		String dtype = null;
		if(def != null){
			dtype = def.getDataType().toString();
			dtype = dtype.substring(dtype.indexOf("}") + 1, dtype.length()).trim();
		}
		return dtype;
	}
	
	public Object generateImageToAlfresco(NodeRef destination, File sourceFile, String fileName, String ext){
		try {
			String tempName = fileName;
			tempName = tempName.replaceAll(FILENAME_REPLACE,"_");
			tempName = tempName.replace("\n", "_");
			tempName = StringUtils.abbreviate(tempName, 200);
			int ind = tempName.lastIndexOf('.');
			if(ind > 0){
				tempName = tempName.substring(0, ind);
			}
			int cnt = 1;
			String fName = tempName + "." + ext;
			while(nodeService.getChildByName(destination, ContentModel.ASSOC_CONTAINS, fName) != null){
				fName = tempName + "-" + cnt + "." + ext;
				cnt++;
			}

			Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
			props.put(ContentModel.PROP_NAME, fName);  

			// use the node service to create a new node
			NodeRef node = nodeService.createNode(
					destination, 
					ContentModel.ASSOC_CONTAINS, 
					QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, fName),
					ContentModel.TYPE_CONTENT, props).getChildRef();
			
			if(sourceFile != null){
				ContentWriter writer = contentService.getWriter(node, ContentModel.PROP_CONTENT, true);
				writer.guessMimetype(fName);
				writer.setEncoding("UTF-8");
				writer.putContent(sourceFile);
			}
			
			// Return a node reference to the newly created node
			//return null;
		}catch(Exception e){
			e.printStackTrace();
		} finally {
			try{
				if(sourceFile != null && sourceFile.exists()){
					sourceFile.delete();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public void updateVersion(NodeRef nodeRef, File sourceFile, File deleteFile, String ext){

		try {		
			//Add new version
			NodeRef workingCopyNode = checkOutCheckInService.checkout(nodeRef);
			ContentWriter writer = contentService.getWriter(workingCopyNode, ContentModel.PROP_CONTENT, true);
			writer.setMimetype(mimetypeService.getMimetype(ext));
			writer.putContent(sourceFile);					
			
			Map<String, Serializable> versionProperties = new HashMap<String, Serializable>();
			versionProperties.put(Version2Model.PROP_VERSION_TYPE, VersionType.MINOR);
			checkOutCheckInService.checkin(workingCopyNode, versionProperties);

		}catch(Exception e){
			e.printStackTrace();
		} finally {
			try{
				if(sourceFile != null && sourceFile.exists()){
					sourceFile.delete();
					if(deleteFile != null && deleteFile.exists()){
						deleteFile.delete();
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	private NodeRef getNodeRef(String path)
	{
		NodeRef pathNodeRef = null;
		
		if(path != null && !path.isEmpty())
		{
			StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	    	pathNodeRef = serviceRegistry.getNodeService().getRootNode(storeRef);
	    	QName qname = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "company_home");
	    	List<ChildAssociationRef> assocRefs = serviceRegistry.getNodeService().getChildAssocs(pathNodeRef, ContentModel.ASSOC_CHILDREN, qname);
	    	pathNodeRef = assocRefs.get(0).getChildRef();
	    	String[] paths = path.split("/");
	    	for(String name : paths){
	    		if(!name.isEmpty())
	    		{
	    			pathNodeRef = getFolder(pathNodeRef, name);
	    			if(pathNodeRef == null){
	    				return null;
	    			}
	    		}
	    	}
		}
		
		return pathNodeRef;
	}
	
	private NodeRef getFolder(NodeRef nodeRef, String name)
    {
    	NodeRef folderNodeRef = serviceRegistry.getNodeService().getChildByName(nodeRef, ContentModel.ASSOC_CONTAINS, name);
		if(folderNodeRef == null)
		{
			try{
				folderNodeRef = serviceRegistry.getFileFolderService().create(nodeRef, name, ContentModel.TYPE_FOLDER).getNodeRef();
			}catch(Exception e){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				folderNodeRef = serviceRegistry.getNodeService().getChildByName(nodeRef, ContentModel.ASSOC_CONTAINS, name);
				if(folderNodeRef == null){
					throw new FileExistsException(nodeRef, name);
				}
			}
        }
		
		return folderNodeRef;
    }
	
	private NodeRef getFile(NodeRef nodeRef, String name)
    {
    	NodeRef fileNodeRef = serviceRegistry.getNodeService().getChildByName(nodeRef, ContentModel.ASSOC_CONTAINS, name);
		
		return fileNodeRef;
    }
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		
	}
	
}
