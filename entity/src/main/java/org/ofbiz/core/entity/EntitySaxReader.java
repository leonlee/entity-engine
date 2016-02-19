/*
 * $Id: EntitySaxReader.java,v 1.1 2005/04/01 05:58:03 sfarquhar Exp $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.core.entity;

import org.ofbiz.core.util.Debug;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * SAX XML Parser Content Handler for Entity Engine XML files
 *
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version $Revision: 1.1 $
 * @since 2.0
 */
public class EntitySaxReader implements org.xml.sax.ContentHandler, ErrorHandler {

    public static final String module = EntitySaxReader.class.getName();

    protected org.xml.sax.Locator locator;
    protected GenericDelegator delegator;
    protected GenericValue currentValue = null;
    protected String currentFieldName = null;
    protected String currentFieldValue = null;
    protected long numberRead = 0;

    protected int valuesPerWrite = 100;
    protected int valuesPerMessage = 1000;
    protected int transactionTimeout = 7200;

    protected List<GenericValue> valuesToWrite = new ArrayList<GenericValue>(valuesPerWrite);

    protected EntitySaxReader() {
    }

    public EntitySaxReader(GenericDelegator delegator) {
        this.delegator = delegator;
    }

    public int getValuesPerWrite() {
        return this.valuesPerWrite;
    }

    public void setValuesPerWrite(int valuesPerWrite) {
        this.valuesPerWrite = valuesPerWrite;
    }

    public int getValuesPerMessage() {
        return this.valuesPerMessage;
    }

    public void setValuesPerMessage(int valuesPerMessage) {
        this.valuesPerMessage = valuesPerMessage;
    }

    public int getTransactionTimeout() {
        return this.transactionTimeout;
    }

    public void setTransactionTimeout(int transactionTimeout) throws GenericTransactionException {
        if (this.transactionTimeout != transactionTimeout) {
            TransactionUtil.setTransactionTimeout(transactionTimeout);
            this.transactionTimeout = transactionTimeout;
        }
    }

    public long parse(String content) throws SAXException, java.io.IOException {
        if (content == null) {
            Debug.logWarning("content was null, doing nothing", module);
            return 0;
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(content.getBytes());

        return this.parse(bis, "Internal Content");
    }

    public long parse(URL location) throws SAXException, java.io.IOException {
        if (location == null) {
            Debug.logWarning("location URL was null, doing nothing", module);
            return 0;
        }
        return this.parse(location.openStream(), location.toString());
    }

    public long parse(InputStream is, String docDescription) throws SAXException, java.io.IOException {

        /* NOTE: this method is not used because it doesn't work with various parsers...
         String orgXmlSaxDriver = System.getProperty("org.xml.sax.driver");
         if (UtilValidate.isEmpty(orgXmlSaxDriver)) orgXmlSaxDriver = "org.apache.xerces.parsers.SAXParser";
         XMLReader reader = XMLReaderFactory.createXMLReader(orgXmlSaxDriver);
         */

        XMLReader reader = null;

        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            SAXParser parser = parserFactory.newSAXParser();

            reader = parser.getXMLReader();
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            Debug.logError(e, "Failed to get a SAX XML parser");
            throw new IllegalStateException("Failed to get a SAX XML parser");
        }

        reader.setContentHandler(this);
        reader.setErrorHandler(this);
        // LocalResolver lr = new UtilXml.LocalResolver(new DefaultHandler());
        // reader.setEntityResolver(lr);

        numberRead = 0;
        try {
            boolean beganTransaction = TransactionUtil.begin();

            TransactionUtil.setTransactionTimeout(transactionTimeout);
            Debug.logImportant("Transaction Timeout set to " + transactionTimeout / 3600 + " hours (" + transactionTimeout + " seconds)");
            try {
                reader.parse(new InputSource(is));
                // make sure all of the values to write got written...
                if (valuesToWrite.size() > 0) {
                    delegator.storeAll(valuesToWrite);
                    valuesToWrite.clear();
                }
                TransactionUtil.commit(beganTransaction);
            } catch (Exception e) {
                Debug.logError(e, "An error occurred saving the data, rolling back transaction");
                TransactionUtil.rollback(beganTransaction);
                throw new SAXException("A transaction error occurred reading data", e);
            }
        } catch (GenericTransactionException e) {
            throw new SAXException("A transaction error occurred reading data", e);
        }
        Debug.logImportant("Finished writing " + numberRead + " values to the database from " + docDescription);
        return numberRead;
    }

    public void characters(char[] values, int offset, int count) throws org.xml.sax.SAXException {
        if (currentValue != null && currentFieldName != null) {
            String value = new String(values, offset, count);

            // Debug.logInfo("characters: value=" + value);
            if (currentFieldValue == null) {
                currentFieldValue = value;
            } else {
                currentFieldValue += value;
            }
        }
    }

    public void endDocument() throws org.xml.sax.SAXException {
    }

    public void endElement(String namespaceURI, String localName, String fullName) throws org.xml.sax.SAXException {
        // Debug.logInfo("endElement: localName=" + localName + ", fullName=" + fullName + ", numberRead=" + numberRead);
        if ("entity-engine-xml".equals(fullName)) {
            return;
        }
        if (currentValue != null) {
            if (currentFieldName != null) {
                if (currentFieldValue != null && currentFieldValue.length() > 0) {
                    currentValue.setString(currentFieldName, currentFieldValue);
                    currentFieldValue = null;
                }
                currentFieldName = null;
            } else {
                try {
                    valuesToWrite.add(currentValue);
                    if (valuesToWrite.size() >= valuesPerWrite) {
                        delegator.storeAll(valuesToWrite);
                        valuesToWrite.clear();
                    }
                    numberRead++;
                    if ((numberRead % valuesPerMessage) == 0) {
                        Debug.logImportant("Another " + valuesPerMessage + " values imported: now up to " + numberRead);
                    }
                    currentValue = null;
                } catch (GenericEntityException e) {
                    throw new SAXException("Error storing value", e);
                }
            }
        }
    }

    public void endPrefixMapping(String prefix) throws org.xml.sax.SAXException {
    }

    public void ignorableWhitespace(char[] values, int offset, int count) throws org.xml.sax.SAXException {
        String value = new String(values, offset, count);
        // Debug.logInfo("ignorableWhitespace: value=" + value);
    }

    public void processingInstruction(String target, String instruction) throws org.xml.sax.SAXException {
    }

    public void setDocumentLocator(org.xml.sax.Locator locator) {
        this.locator = locator;
    }

    public void skippedEntity(String name) throws org.xml.sax.SAXException {
    }

    public void startDocument() throws org.xml.sax.SAXException {
    }

    public void startElement(String namepsaceURI, String localName, String fullName, org.xml.sax.Attributes attributes) throws org.xml.sax.SAXException {
        // Debug.logInfo("startElement: localName=" + localName + ", fullName=" + fullName + ", attributes=" + attributes);
        if ("entity-engine-xml".equals(fullName)) {
            return;
        }
        if (currentValue != null) {
            // we have a nested value/CDATA element
            currentFieldName = fullName;
        } else {
            String entityName = fullName;

            // if a dash or colon is in the tag name, grab what is after it
            if (entityName.indexOf('-') > 0) {
                entityName = entityName.substring(entityName.indexOf('-') + 1);
            }
            if (entityName.indexOf(':') > 0) {
                entityName = entityName.substring(entityName.indexOf(':') + 1);
            }

            try {
                currentValue = delegator.makeValue(entityName, null);
            } catch (Exception e) {
            }

            if (currentValue != null) {
                int length = attributes.getLength();

                for (int i = 0; i < length; i++) {
                    String name = attributes.getLocalName(i);
                    String value = attributes.getValue(i);

                    if (name == null || name.length() == 0) {
                        name = attributes.getQName(i);
                    }
                    try {
                        // treat empty strings as nulls
                        if (value != null && value.length() > 0) {
                            currentValue.setString(name, value);
                        }
                    } catch (Exception e) {
                        Debug.logWarning(e, "Could not set field " + name + " to the value " + value);
                    }
                }
            }
        }
    }

    public void startPrefixMapping(String prefix, String uri) throws org.xml.sax.SAXException {
    }

    // ======== ErrorHandler interface implementations ========

    public void error(org.xml.sax.SAXParseException exception) throws org.xml.sax.SAXException {
        Debug.logWarning(exception, "Error reading XML on line " + exception.getLineNumber() + ", column " + exception.getColumnNumber(), module);
    }

    public void fatalError(org.xml.sax.SAXParseException exception) throws org.xml.sax.SAXException {
        Debug.logError(exception, "Fatal Error reading XML on line " + exception.getLineNumber() + ", column " + exception.getColumnNumber(), module);
        throw new SAXException("Fatal Error reading XML on line " + exception.getLineNumber() + ", column " + exception.getColumnNumber(), exception);
    }

    public void warning(org.xml.sax.SAXParseException exception) throws org.xml.sax.SAXException {
        Debug.logWarning(exception, "Warning reading XML on line " + exception.getLineNumber() + ", column " + exception.getColumnNumber(), module);
    }
}
