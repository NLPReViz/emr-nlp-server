/**
 * 
 */
package edu.pitt.cs.nih.backend.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Phuong Pham
 *
 * Save and load datasets, models in XML format defined by Alexander Conrad
 */
public class XMLUtil {
public static String DATASET_TYPE_COLON = "datasetcolonoscopy";
	
    /**
     * Save list of record IDs as an XML file.
     * 
     * @param dbName
     * @param itemList
     * @param nodeName
     * @param fn_xml
     * @throws Exception
     */
    public static void createXMLDatasetFileFromList(String dbName, String[] itemList,
            String nodeName, String fn_xml) throws Exception {                
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // root elements
        Document doc = docBuilder.newDocument();        
        
        Element rootElement = doc.createElement("Dataset");
        doc.appendChild(rootElement);        
        // attributes for root node
        rootElement.setAttribute("type", DATASET_TYPE_COLON);
        rootElement.setAttribute("name", dbName);
        rootElement.setAttribute("databaseroot", "./");
        
        Element ele;
        for(String item : itemList) {
            ele = doc.createElement(nodeName);
            ele.appendChild(doc.createTextNode(item));
            
            rootElement.appendChild(ele);
        }
        
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        File outputFile = new File(fn_xml);
        StreamResult result = new StreamResult(outputFile);
        // replace %20 by whitespace
        result.setSystemId(result.getSystemId().replaceAll("%20", " "));
        
        transformer.transform(source, result);
    }
    
    /**
     * Save a list of models in XML file
     * 
     * @param sessionIDList
     * @param userIDList
     * @param varIDList
     * @param predictorXMLFolder
     * @param maxSessionID
     * @param maxUserID
     * @param fn_xmlPredictor
     * @return
     * @throws Exception
     */
    public static String createXMLPredictor(String[] sessionIDList, String[] userIDList, 
            String[] varIDList, String predictorXMLFolder, String maxSessionID,
            String maxUserID, String fn_xmlPredictor) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // root elements
        Document doc = docBuilder.newDocument();        
        
        Element rootElement = doc.createElement("MLPredictor");
        doc.appendChild(rootElement);        
        // attributes for root node
        rootElement.setAttribute("type", "mlpredictor_colonoscopy_vars");
        rootElement.setAttribute("name", "emr-vis-nlp_colonoscopy_" +
                maxSessionID  + "_" + maxUserID);
        rootElement.setAttribute("modelroot", "./");
        // removed the foldname
        
        Element ele;
        Element vals;
        String varID, userID, sessionID;
        for(int i = 0; i < varIDList.length; i++) {
            varID = varIDList[i];
            userID = userIDList[i];
            sessionID = sessionIDList[i];
            ele = doc.createElement("Attr");
            ele.setAttribute("name", varID);
            ele.setAttribute("type", "variable_categorical");
            ele.setAttribute("fileName", sessionID + "." + userID + "." + varID);
            vals = doc.createElement("Vals");
            vals.appendChild(doc.createTextNode("False True"));
            ele.appendChild(vals);
            
            rootElement.appendChild(ele);
        }
        
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        File outputFile = new File(fn_xmlPredictor);
        StreamResult result = new StreamResult(outputFile);
        // replace %20 by whitespace
        result.setSystemId(result.getSystemId().replaceAll("%20", " "));
        
        transformer.transform(source, result);
        
        return rootElement.getAttribute("name");
    }
    
    /**
     * Load a list of report IDs from XML file
     * 
     * @param fn_xmlList
     * @return
     * @throws Exception
     */
    public static List<String> getReportIDFromXMLList(String fn_xmlList) throws Exception {
    	List<String> reportIDList = new ArrayList<>();
    	
    	org.w3c.dom.Document dom = null;
    	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        dom = db.parse(fn_xmlList);
        
        Element doclistRoot = dom.getDocumentElement();
        String doclistTypeName = doclistRoot.getAttribute("type").trim().toLowerCase();
        
        if (doclistTypeName.equals(DATASET_TYPE_COLON)) {
        	// read in all docs from xml
            NodeList documentNodes = doclistRoot.getElementsByTagName("Document");
            
            if (documentNodes != null && documentNodes.getLength() > 0) {
                for (int n = 0; n < documentNodes.getLength(); n++) {
                    Element documentNode = (Element) documentNodes.item(n);
                    reportIDList.add(documentNode.getFirstChild().getNodeValue().trim());
                }
            }
        }
        
        return reportIDList;
    }
    
    public static List<String> getModelFnFromXMLList(String fn_xmlList)
    		throws Exception {
    	List<String> modelNameList = new ArrayList<>();
    	
		if (fn_xmlList != null && Util.fileExists(fn_xmlList)) {
			org.w3c.dom.Document dom = null;
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			dom = db.parse(fn_xmlList);
			Element modellistRoot = dom.getDocumentElement();
			NodeList attributeNodes = modellistRoot
					.getElementsByTagName("Attr");

			for (int n = 0; n < attributeNodes.getLength(); n++) {
				Element attributeNode = (Element) attributeNodes.item(n);
				modelNameList.add(attributeNode.getAttribute("fileName").trim());
			}
		}
        
    	return modelNameList;
    }
}
