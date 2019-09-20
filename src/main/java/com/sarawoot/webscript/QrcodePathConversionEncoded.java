package com.skytizens.alfresco.webscript;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.filefolder.HiddenAspect;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ClassManager;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.interfaces.CustomAbstractWebScript;
import org.antlr.grammar.v3.CodeGenTreeWalker.element_action_return;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.StringUtils;
import org.json.JSONObject;
import org.springframework.extensions.surf.util.Content;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

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
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import com.skytizens.alfresco.actions.GenerateQRCode;

public class QrcodePathConversionEncoded extends CustomAbstractWebScript {
	
	private static String MODULE_NAME = "QrcodeGen";
	private static String MODULE_CODE = "0";
	private static String MODULE_ENCODED_CODE = "0";
	
	private final static String FILENAME_REPLACE = "[\\\"\\*\\\\\\>\\<\\?\\/\\:\\|]";
	
	private boolean useModule = false;

	private static Log logger = LogFactory.getLog(QrcodePathConversionEncoded.class);
	
	private ServiceRegistry serviceRegistry;
	private NodeService nodeService;
	private RetryingTransactionHelper retryingTransactionHelper;
	private ContentService contentService;
	
	private String logoPath;
	private boolean insideImage;
	private boolean pdftiff;
	private String fontPath;
	private String convertPath;
	private String confPath;
	private String command;
	/**
	 * @param serviceRegistry
	 *            the serviceRegistry to set
	 */
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.nodeService = serviceRegistry.getNodeService();
		this.retryingTransactionHelper = serviceRegistry.getRetryingTransactionHelper();
		this.contentService = serviceRegistry.getContentService();
		registerFonts();
	}

	/**
	 * @param classManager
	 *            the classManager to set
	 */
	public void setClassManager(ClassManager classManager) {
		useModule = MODULE_CODE.equals(classManager.getVerifyCode(MODULE_ENCODED_CODE, this));
	}

	@Override
	public void setParams(Map<String, String> params) {
		this.logoPath = params.containsKey("logoPath") ? params.get("logoPath") : null;
		this.fontPath = params.containsKey("fontPath") ? params.get("fontPath") : null;
		this.convertPath = params.containsKey("convertPath") ? params.get("convertPath") : null;
		this.insideImage = params.containsKey("insideImage") ? Boolean.valueOf(params.get("insideImage")) : false;
		this.pdftiff = params.containsKey("pdftiff") ? Boolean.valueOf(params.get("pdftiff")) : false;
		this.confPath = params.containsKey("confPath") ? params.get("confPath") : null;
		this.command = params.containsKey("command") ? params.get("command") : null;
	}
	/**
	 * @param command
	 *            the command to set
	 */

	/**
	 * @param command
	 *            the command to set
	 */
	// public void setObjects(Map<String,Object> objects) {
	// this.xmlUtility = (XMLUtility) (objects.containsKey("xmlUtility") ?
	// objects.get("xmlUtility") : null);
	// }

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		if (useModule) {
			try {

				Content c = req.getContent();
				if (c == null) {
					throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Missing POST body.");
				}
				
				String currentUser = AuthenticationUtil.getRunAsUser();
				JSONObject json = new JSONObject(c.getContent());
				String qrcodeText = json.has("qrcodetitle") ? json.getString("qrcodetitle") : null;
				int qrcodeSize = json.has("qrcodesize") ? json.getInt("qrcodesize") : null;
				String saveNode = json.has("sourcenodeRef") ? json.getString("sourcenodeRef") : null;
				NodeRef destinationFolder = new NodeRef(saveNode);
				File tempDir = TempFileProvider.getTempDir();
				
				String sourcePath = tempDir.getAbsolutePath() + "/" + getClass().getSimpleName() + "_sign_source_"
						+destinationFolder.getId()+ ".pdf";
				String destPath = tempDir.getAbsolutePath() + "/" + getClass().getSimpleName() + "_sign_destPath_"
						+destinationFolder.getId()+ ".pdf";
				String tiffPath = tempDir.getAbsolutePath() + "/" + getClass().getSimpleName() + "_sign_tiff_"
						+destinationFolder.getId()+ ".tiff";;
				File tiff = null;
				
				BufferedImage qrCodeImage = null;
				if(insideImage){
					File src = new File(logoPath);
					double percent = 10;
					qrCodeImage = createQRImageWithImageInside(qrcodeText, qrcodeSize, percent, qrcodeSize, src);
				}
				else{
					qrCodeImage = createQRImage(qrcodeText, qrcodeSize);
				}
				
				NodeRef confParentNodeRef = getNodeRef(confPath);
	        	NodeRef template = getFile(confParentNodeRef, "template.html");
				if(template != null){
					Map<String, Object> model = new HashMap<String, Object>();
					model.put("text", qrcodeText);
					model.put("size", qrcodeSize);
					
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
					createPdfWithQrcode(qrcodeText, sourcePath, qrcodeSize, destPath, qrCodeImage);
				}
				
				File srcFile = new File(sourcePath);
				if(srcFile.exists()){
					if(pdftiff){
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
								QrcodePathConversion.generateImageToAlfresco(this, retryingTransactionHelper, currentUser,
										destinationFolder, tiff, qrcodeText, "tiff");
							}
							srcFile.delete();
						}
					}else{
						QrcodePathConversion.generateImageToAlfresco(this, retryingTransactionHelper, currentUser,
								destinationFolder, srcFile, qrcodeText, "pdf");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new WebScriptException("Cannot Generate Qrcode");

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
	
	private void createPdfWithQrcode(String title, String sourcePath, int qrbordersize, String destPath, BufferedImage imageInPDF){

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

			image.setAbsolutePosition((document.getPageSize().getWidth()-qrbordersize)/2, (document.getPageSize().getHeight()-qrbordersize)/2);
			
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
	
	 private BufferedImage createQRImageWithImageInside(String qrCodeText, int size, double percent, int qrbordersize, File inputPng) throws WriterException, IOException
	 {		
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
		int scaledWidth = qrImage.getWidth() - overly.getWidth();
		int scaledHeight = qrImage.getHeight() - overly.getHeight();
		double newwidth = 0;
		double logoOverWidth = 0;
		// LogoHeight is LogoHeight inside.
		double logoHeight = ((double) qrImage.getHeight() / 100.0d) * (double) percent;
		// Load logo image isn't Square.
		if (overly.getWidth() > qrImage.getWidth()) {
			logoOverWidth = ((double) overly.getWidth() / (double) overly.getHeight());
			newwidth = (double) (logoOverWidth * logoHeight);
		} else {
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
		//ByteArrayOutputStream os = new ByteArrayOutputStream();
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
	
    private MatrixToImageConfig getMatrixConfig() {
		// ARGB Colors
        // Check Colors ENUM
		return new MatrixToImageConfig(QrcodePathConversionEncoded.Colors.BLACK.getArgb(), QrcodePathConversionEncoded.Colors.WHITE.getArgb());
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
    
    private String registerFonts() {
		String windir = System.getenv("windir");
		String fileseparator = System.getProperty("file.separator");
		String fontPath = null; 
		if (windir != null && fileseparator != null) {
			fontPath = windir + fileseparator + "fonts" + fileseparator + "ARIALUNI.TTF";	
		}else{
			fontPath = "/usr/share/fonts/truetype/openoffice/ARIALUNI.TTF";
		}
		File fontFile = new File(fontPath);
		if(fontFile.exists()){
			FontFactory.register(fontPath);
		}else{
			logger.error("Cannot find font path \"" + fontPath + "\"");
		}
		return fontPath;
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
}
