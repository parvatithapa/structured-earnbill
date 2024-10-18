package com.sapienter.jbilling.server.fileProcessing.xmlParser;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

import static com.sapienter.jbilling.server.fileProcessing.FileConstants.*;

/**
 * Created by aman on 24/8/15.
 */
public class XMLParser {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(XMLParser.class));


    public static FileStructure parseXML(File formatFile) throws Exception {

        //Get the DOM Builder Factory
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        //Get the DOM Builder
        DocumentBuilder builder = factory.newDocumentBuilder();
        //Load and Parse the XML document
        //document contains the complete XML as a Tree.
        InputStream inputStream = new FileInputStream(formatFile);
        Document document = builder.parse(inputStream);

        //Iterating through the nodes and extracting the data.
        Element xmlParentTag = document.getDocumentElement();

        List<Record> records = generateRecords(xmlParentTag.getElementsByTagName(TAG_NAME_RECORDS).item(0));

        return generateFileStructure(xmlParentTag.getElementsByTagName(TAG_NAME_STRUCTURE).item(0), records);
    }

    private static List<Record> generateRecords(Node recordsNode) {
        List records = new ArrayList<Record>();
        List<Node> recordNodes = getChildNodes(recordsNode, TAG_NAME_RECORD);
        for (Node recordNode : recordNodes) {
            records.add(generateRecord(recordNode));
        }
        return records;
    }

    private static Record generateRecord(Node recordNode) {
        Record record = new Record();

        //First validate and generate rec_id
        Node recIdNode = getOnlyChildNode(recordNode, TAG_NAME_REC_ID);
        if (recIdNode == null) {
            throw new SessionInternalError("Record must contain the : " + TAG_NAME_REC_ID + " information");
        }
        Field recId = generateField(recIdNode);

        // Validate that rec_id has default value
        if (recId.getDefaultValue().trim().isEmpty()) {
            throw  new SessionInternalError("Record(" + recId.getFieldName() + ") must contain the : " + TAG_NAME_DEFAULT_VALUE);
        }

        record.setRecId(recId);
        Node parentFieldNodes = getOnlyChildNode(recordNode, TAG_NAME_FIELDS);
        for (Node fieldNode : getChildNodes(parentFieldNodes, TAG_NAME_FIELD)) {
            record.getFields().add(generateField(fieldNode));
        }
        return record;
    }

    private static Field generateField(Node fieldNode) {
        Field field = new Field();

        boolean notUsed = getOnlyChildNode(fieldNode, TAG_NAME_NOT_USED)!=null;
        field.setNotUsed(notUsed);
        boolean mandatory = notUsed?false:true;

        field.setFieldName(getTagValue(fieldNode, TAG_NAME_FIELD_NAME, mandatory));

        String maxSize = getTagValue(fieldNode, TAG_NAME_MAX_SIZE, false);
        if(maxSize!=null)field.setMaxSize(Integer.parseInt(maxSize));

        field.setDateFormat(getTagValue(fieldNode, TAG_NAME_DATE_FORMAT, false));
        field.setDefaultValue(getTagValue(fieldNode, TAG_NAME_DEFAULT_VALUE, false));

        String inbound = getTagValue(fieldNode, TAG_NAME_INBOUND, mandatory);
        String outbound = getTagValue(fieldNode, TAG_NAME_OUTBOUND, mandatory);

        //Validate that every field must contains the inbound and outbound attr.
        if (field.isNotUsed() && (inbound.trim().isEmpty() || outbound.trim().isEmpty())) {
            throw new SessionInternalError(TAG_NAME_INBOUND + " and " + TAG_NAME_OUTBOUND + " are mandatory for a field(" + field.getFieldName() + ")");
        } else {
            field.setInbound(Visibility.valueOf(inbound));
            field.setOutbound(Visibility.valueOf(outbound));
        }
        field.setComment(getTagValue(fieldNode, TAG_NAME_COMMENT, false));
        field.setPossibleValues(generateValues(fieldNode));
        return field;
    }

    private static Values generateValues(Node fieldNode) {
        Values values = new Values();
        Node parentValueNode = getOnlyChildNode(fieldNode, TAG_NAME_POSSIBLE_VALUES);
        if(parentValueNode==null) return values;
        List<Node> options = getChildNodes(parentValueNode, TAG_NAME_OPTION);
        for (Node option : options) {
            String value = getTagValue(option, TAG_NAME_VALUE, true);
            String comment = getTagValue(option, TAG_NAME_COMMENT, true);
            values.addOption(value, comment);
        }
        return values;
    }

    private static FileStructure generateFileStructure(Node structureNode, List<Record> records) {
        FileStructure fileStructure = new FileStructure();
        fileStructure.setRecords(records);
        List<Node> structureNodes = getChildNodes(structureNode, fileStructure.recordNames());
        if (structureNodes == null || structureNodes.size() < 1) {
            throw new SessionInternalError("Structure must contains at least one record");
        }

        fileStructure.setRecordStructures(iterateRecordStructures(structureNodes, fileStructure));

        return fileStructure;
    }

    private static List<RecordStructure> iterateRecordStructures(List<Node> structureNodes, FileStructure fileStructure) {
        List<RecordStructure> recordStructures = new LinkedList<RecordStructure>();
        List<Node> childNodes = new LinkedList<Node>();
        List<String> recordNames = fileStructure.recordNames();
        for (Node parentNode : structureNodes) {
            RecordStructure recordStructure = new RecordStructure();
            String nodeName = parentNode.getNodeName();
            Record record = fileStructure.findRecordByName(nodeName);
            recordStructure.setRecord(record);

            recordStructure.setLoop(getAttrWithDefaultValue(parentNode, TAG_ATTR_LOOP, "1"));

            // If visibility defined at structure than override visibility at record level
            // It is possible to have different scope for a record in same file. Like QTY as summary record is mandatory
            // but QTY as interval is optional
            String inbound = getAttrValue(parentNode, TAG_NAME_INBOUND);
            String outbound = getAttrValue(parentNode, TAG_NAME_OUTBOUND);
            if (inbound != null) {
                record.getRecId().setInbound(Visibility.valueOf(inbound));
            }
            if (outbound != null) {
                record.getRecId().setOutbound(Visibility.valueOf(outbound));
            }

            recordStructures.add(recordStructure);

            //Find all child records
            childNodes = getChildNodes(parentNode, recordNames);
            if (childNodes.size() > 0) {
                recordStructure.setChildRecord(iterateRecordStructures(childNodes, fileStructure));
            }
        }
        return recordStructures;
    }

    private static List<String> getRecordNames(List<Record> records) {
        List<String> recordNames = new LinkedList<String>();
        for (Record record : records) {
            recordNames.add(record.getRecId().defaultValue);
        }
        return recordNames;
    }

    private static List<Node> getChildNodes(Node node, String nodeName) {
        NodeList list = node.getChildNodes();
        List<Node> nodes = new LinkedList<Node>();
        for (int i = 0; i < list.getLength(); i++) {
            Node item = list.item(i);
            if (item.getNodeName().equals(nodeName)) {
                nodes.add(item);
            }
        }
        return nodes;
    }

    private static List<Node> getChildNodes(Node node, List<String> nodeNames) {
        NodeList list = node.getChildNodes();
        List<Node> nodes = new LinkedList<Node>();
        for (int i = 0; i < list.getLength(); i++) {
            Node item = list.item(i);
            if (nodeNames.contains(item.getNodeName())) {
                nodes.add(item);
            }
        }
        return nodes;
    }

    private static Node getOnlyChildNode(Node node, String nodeName) {
        List<Node> nodes = getChildNodes(node, nodeName);
        if (nodes.size() != 1) return null;
        return nodes.get(0);
    }

    private static String getAttrValue(Node node, String attrName) {
        Node attributeNode = node.getAttributes().getNamedItem(attrName);
        if (attributeNode != null) return attributeNode.getNodeValue().trim();
        return null;
    }

    private static String getAttrWithDefaultValue(Node node, String idName, String defaultValue) {
        String value = getAttrValue(node, idName);
        return value != null ? value : defaultValue;
    }

    private static String getTagValue(Node parentNode, String tagName, boolean mandatory) {
        Node node = getOnlyChildNode(parentNode, tagName);
        if (node == null && mandatory == true) {
            throw new SessionInternalError("Tag \"" + tagName + "\" should exist.");
        } else if (node == null) {
            return null;
        }
        String content = node.getTextContent();
        if (mandatory == true && (content == null || content.trim().isEmpty())) {
            throw new SessionInternalError("Value in tag \"" + tagName + "(" + node.getNodeName() + ")\" should exist.");
        }
        return content;
    }
}
