package com.sapienter.jbilling.server.payment.tasks.ep2;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


/**
 * 
 * @author Krunal Bhavsar
 *
 */
public class EP2ContentManager {

	private EP2ContentManager() {
		
	}
	
	/**
	 * returns List of {@link PropertyDetails} 
	 * it creates {@link PropertyDetails} from Passed {@link Ep2Payer} object
	 * @param payer
	 * @return
	 */
	public static List<PropertyDetails> createPropertyDetailsFromEp2Payer(Ep2Payer payer)  {
		List<PropertyDetails> details = new ArrayList<PropertyDetails>();

		PropertyDetails paymentMethods = new PropertyDetails("payment-methods", "", "","",
				Arrays.asList(new PropertyDetails[]{
						new PropertyDetails(RequestParameters.PAYMENT_METHOD.toString(), "", "name", payer.getPaymentMethodName(), null)})
				);

		PropertyDetails merchantId = new PropertyDetails(RequestParameters.MERCHANT_ID.toString(), payer.getMerchantId(), "", "", null);
		PropertyDetails requestId = new PropertyDetails(RequestParameters.REQUEST_ID.toString(), payer.getRequestedId(), "", "", null);
		PropertyDetails orderNumber = new PropertyDetails(RequestParameters.ORDER_NUMBER.toString(), payer.getOrderNumber(), "", "", null);
		PropertyDetails transactionType = new PropertyDetails(RequestParameters.TRANSACTION_TYPE.toString(), payer.getTransactionType(), "", "", null);
		PropertyDetails requestedAmount = new PropertyDetails(RequestParameters.REQUESTED_AMOUNT.toString(), payer.getRequestedAmount(),
				RequestParameters.CURRENCY.toString(), payer.getCurrency(), null);

		details.addAll(Arrays.asList(paymentMethods, merchantId, requestId, requestedAmount, transactionType, orderNumber));
		if(null!= payer.getFirstName() && null!=payer.getLastName() && null!= payer.getEmail()) {
			PropertyDetails accountHolder = new PropertyDetails("account-holder", "", "", "", Arrays.asList(new PropertyDetails[]{
					new PropertyDetails(RequestParameters.FIRST_NAME.toString(), payer.getFirstName(), "", "", null),
					new PropertyDetails(RequestParameters.LAST_NAME.toString(), payer.getLastName(), "", "", null),
					new PropertyDetails(RequestParameters.ACCOUNT_NUMBER.toString(), payer.getCreditCardNumber(), "", "", null),
					new PropertyDetails(RequestParameters.EMAIL_ID.toString(), payer.getEmail(), "", "", null)
			}
					));
			details.add(accountHolder);
		}

		if(null!=payer.getPeriodicType()) {
			PropertyDetails periodic = new PropertyDetails("periodic", "", "", "", Arrays.asList(new PropertyDetails[]{
					new PropertyDetails(RequestParameters.PERIODIC_TYPE.toString(), payer.getPeriodicType(), "", "", null)
			}
					));
			details.add(periodic);
		}
		

		if(null!= payer.getCreditCardNumber() && null!= payer.getCreditCardType()){
			PropertyDetails cardDetails = new PropertyDetails("card", "", "", "", Arrays.asList(new PropertyDetails[]{
					new PropertyDetails(RequestParameters.EXPIRATION_MONTH.toString(), payer.getExpiryMonth(), "", "", null),
					new PropertyDetails(RequestParameters.EXPIRATION_YEAR.toString(),  payer.getExpiryYear(), "", "", null) ,
					new PropertyDetails(RequestParameters.CARD_TYPE.toString(),  payer.getCreditCardType(), "", "", null) ,
					new PropertyDetails(RequestParameters.ACCOUNT_NUMBER.toString(),  payer.getCreditCardNumber(), "", "", null) ,
			}
					));
			details.add(cardDetails);
		}

		if(null!=payer.getTokenId()) {
			PropertyDetails cardToken = new PropertyDetails("card-token", "", "", "", Arrays.asList(new PropertyDetails[]{
					new PropertyDetails(RequestParameters.TOKEN_ID.toString(), payer.getTokenId(), "", "", null)
			}));
			details.add(cardToken);
		}
		
		if(null!= payer.getParentTransactionId()) {
			PropertyDetails parentTransactionId = new PropertyDetails(RequestParameters.PARENT_TRANSACTION_ID.toString(), payer.getParentTransactionId(), "", "", null);
			details.add(parentTransactionId);
		}
		
		return details;
	}
	
	/**
	 *returns xml request in form of String 
	 *it creates xml request from {@link PropertyDetails}   
	 * @param paymentDetails
	 * @throws Exception
	 */
	public static String createXMLContent(List<PropertyDetails> paymentDetails) throws Exception {
        	DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder;
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document doc = documentBuilder.newDocument();
            Element mainRootElement = doc.createElementNS("http://www.elastic-payments.com/schema/payment","payment");

            doc.appendChild(mainRootElement);
            // append child elements to root element
            paymentDetails.forEach( paymentDetail -> mainRootElement.appendChild(createElement(doc, paymentDetail)));
            // output DOM XML to console
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
            return writer.toString();
    }

	/**
	 * 
	 * @param xmlContent
	 * @param tagName
	 * @return value of specified tagName from response Xml 
	 * @throws Exception
	 */
	public static  String getValueByElementTag(String xmlContent, String tagName) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xmlContent));
		Document doc = db.parse(is);
		NodeList message = doc.getElementsByTagName(tagName);
		return message.getLength()!=0 ? message.item(0).getFirstChild().getNodeValue(): null;
	}
	
	/**
	 * 
	 * @param xmlContent
	 * @param tagName
	 * @param attributeName
	 * @return attribute value of specified attributeName from response xml
	 * @throws Exception
	 */
	public static  String getValueByElementTagAndAttributeName(String xmlContent, String tagName, String attributeName) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xmlContent));
		Document doc = db.parse(is);
		return doc.getElementsByTagName(tagName).item(0).getAttributes().getNamedItem(attributeName).getNodeValue();
	}

	public static String getResult(String xmlContent, Function<Document, String> selector) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xmlContent));
		Document doc = db.parse(is);
		return selector.apply(doc);
	}
	
    private static Node createElement(Document doc, PropertyDetails propertyDetails) {
        if(null != propertyDetails.getPropertyName() && !propertyDetails.getPropertyName().trim().isEmpty()) {

            Element rootElement = (Element) createNode(doc, propertyDetails);

            if(null != propertyDetails.getChildProperties()) {
            	propertyDetails.getChildProperties().forEach(childPropertyDetail -> 
            					rootElement.appendChild(createNode(doc, childPropertyDetail)));
            }
            return rootElement;
        }
        return null;
    }

    private static Node createNode(Document doc, PropertyDetails propertyDetails){
        Element node = doc.createElement(propertyDetails.getPropertyName());

        if(null != propertyDetails.getPropertyValue() && !propertyDetails.getPropertyValue().trim().isEmpty()){
            node.appendChild(doc.createTextNode(propertyDetails.getPropertyValue()));
        }

        if(null != propertyDetails.getAttributeName() && !propertyDetails.getAttributeName().trim().isEmpty()
                && null != propertyDetails.getAttributeValue() && !propertyDetails.getAttributeValue().trim().isEmpty()){
            node.setAttribute(propertyDetails.getAttributeName(), propertyDetails.getAttributeValue());
        }

        return node;
    }

}
