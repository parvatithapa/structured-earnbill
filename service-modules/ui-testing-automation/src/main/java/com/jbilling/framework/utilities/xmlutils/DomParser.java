package com.jbilling.framework.utilities.xmlutils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.jbilling.framework.utilities.textutilities.TextUtilities;

class DomParser {

    protected final Log logger = LogFactory.getLog(getClass());
    
	private Document doc = null;

	protected DomParser(final String xmlDir, final String xmlFileName) {
		try {
			String xmlFileNameWithPath = xmlDir + xmlFileName;
			if (TextUtilities.isBlank(xmlFileNameWithPath)) {
				throw new IOException("No XML file name provided to read");
			}

			if (! new File(xmlFileNameWithPath).exists()) {
				throw new IOException("No such XML file exists to read: " + xmlFileNameWithPath);
			}

			File inputFile = new File(xmlFileNameWithPath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			this.doc = dBuilder.parse(inputFile);
			this.doc.getDocumentElement().normalize();
			logger.debug("Root element :" + this.doc.getDocumentElement().getNodeName());
		} catch (final Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Exception in DomParser constructor", e);
        }
	}

	private String getNodeAttr(String attrName, Node node) {
		NamedNodeMap attrs = node.getAttributes();
		for (int y = 0; y < attrs.getLength(); y++) {
			Node attr = attrs.item(y);
			if (attr.getNodeName().equalsIgnoreCase(attrName)) {
				return attr.getNodeValue();
			}
		}
		return "";
	}

	/**
	 * 
	 * @param nodeNameWithPath
	 *            E.g. /class/student
	 * @return
	 */
	private NodeList getNodes(String tagName) {
		NodeList nodesList = this.doc.getElementsByTagName(tagName);
		logger.debug("Nodes found with " + tagName + " = " + nodesList.getLength());
		return nodesList;
	}

	private String getNodeValue(Node node) {
		NodeList childNodes = node.getChildNodes();
		for (int x = 0; x < childNodes.getLength(); x++) {
			Node data = childNodes.item(x);
			if (data.getNodeType() == Node.TEXT_NODE) {
				return data.getNodeValue();
			}
		}
		return "";
	}

	private NodeList getTestDataSetNodesList(String testEnv, String dataSetName, String category) {
		NodeList nodes = this.getNodes("testdataset");
		NodeList eligibleNodesList = null;

		for (int temp = 0; temp < nodes.getLength(); temp++) {
			Node nd = nodes.item(temp);
			logger.debug("Checking node [" + nd.getNodeName() + "] at index " + temp);

			if (nd.getNodeType() == Node.ELEMENT_NODE) {
				String env = this.getNodeAttr("testenv", nd);
				String dsName = this.getNodeAttr("name", nd);
				String cat = this.getNodeAttr("category", nd);
				logger.debug(env + " -- " + dsName + " -- " + cat);
				if (TextUtilities.contains(env, testEnv) && TextUtilities.equalsIgnoreCase(dsName, dataSetName)
						&& TextUtilities.equalsIgnoreCase(cat, category)) {
					eligibleNodesList = nd.getChildNodes();
					break;
				}
			}
		}
		return eligibleNodesList;
	}

	protected Map<String, String> getTestDataSetNodesWithValues(String testEnv, String dataSetName, String category) {
		NodeList eligibleNodesList = this.getTestDataSetNodesList(testEnv, dataSetName, category);
		if (eligibleNodesList == null) {
			return null;
		}

		Map<String, String> nl = new HashMap<String, String>();
		logger.debug(eligibleNodesList.getLength());
		for (int temp = 0; temp < eligibleNodesList.getLength(); temp++) {
			Node nd = eligibleNodesList.item(temp);
			if (nd.getNodeType() == Node.TEXT_NODE) {
				logger.debug("text node found; skipping this node");
				continue;
			}

			logger.debug(nd.getNodeName() + " <<---->>  " + this.getNodeValue(nd) + " <<---->> " + nd.getNodeType());
			nl.put(nd.getNodeName(), this.getNodeValue(nd));
		}
		return nl;
	}
}
