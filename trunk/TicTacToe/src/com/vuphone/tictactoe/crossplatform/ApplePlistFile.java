package com.vuphone.tictactoe.crossplatform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.util.Log;

public class ApplePlistFile {

	HashMap<String, Object> objects;
	
	public ApplePlistFile(InputStream stream)
	{
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true); // never forget this!
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(stream);
			
			NodeList n1 = doc.getChildNodes();
			Node n = doc.getChildNodes().item(3).getChildNodes().item(1);
			
			if (n == null){
				Log.d("ApplePlistFile", "Could not load the plist file. No root node found.");
				return;
			}
			
			// parse key/data pairs
			objects = new HashMap<String, Object>();
			
			for (int i = 0; i < n.getChildNodes().getLength(); i+=4){
				Node keyNode = n.getChildNodes().item(i+1);
				Node dataNode = n.getChildNodes().item(i+3);
				if ((keyNode != null) && (dataNode != null) && (keyNode.getChildNodes() != null) && (keyNode.getChildNodes().getLength() > 0) && 
						(dataNode.getChildNodes() != null) && (dataNode.getChildNodes().getLength() > 0)){
					String key = keyNode.getChildNodes().item(0).getNodeValue();
					String data = dataNode.getChildNodes().item(0).getNodeValue();
					
					Log.d("ApplePlistFile", "Loading key/value pair with key " + key + ". Data type = "+dataNode.getNodeName());
					
					if ((key != null) && (data != null)){
						if (dataNode.getNodeName().equals("integer")){
							objects.put(key, Integer.parseInt(data));
						
						} else {
							byte[] decoded = Base64.decode(data);
							ByteBuffer buffer = ByteBuffer.wrap(decoded);
							objects.put(key, buffer);
						}
					}
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();	
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Object objectForKey(String key)
	{
		return objects.get(key);
	}
}
