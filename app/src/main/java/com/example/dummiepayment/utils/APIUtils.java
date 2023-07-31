package com.example.dummiepayment.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class APIUtils
{
	/**
	 * Process int value that uses Payment Gateway
	 * 
	 * @param value
	 * @return
	 */
	public static BigDecimal parseAPIAmount(String value)
	{
		try
		{
			String integerPart  = value.substring(0, value.length()-2);
			String decimalPart  = value.substring(value.length()-2, value.length());
			
			return new BigDecimal(Double.parseDouble(integerPart + "." + decimalPart));
		}
		catch (Exception e)
		{
			return BigDecimal.ZERO;
		}
		
	}
	
	/**
	 * Formats decimal number to communication specified API format.
	 * 
	 * @param bigDecimal
	 * @return
	 */
	public static String serializeAPIAmount(BigDecimal bigDecimal)
	{
		DecimalFormat df    = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
		String result       = df.format(bigDecimal.doubleValue());
		
		try
		{
			return result.replaceAll("\\.", "");
		}
		catch (NumberFormatException nfe)
		{
			return "000";
		}
	}
	
	/**
	 * Parse initialize action input params to key - value structure.
	 * 
	 * @param serializedPropeties
	 * @return
	 */
	public static Properties parseInitializeParameters(String serializedPropeties)
	{
		Properties properties = new Properties();
		
		try
		{
			if (serializedPropeties != null && !serializedPropeties.isEmpty())
			{
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				
				Document document = dBuilder.parse(new ByteArrayInputStream(serializedPropeties.getBytes()));
				document.normalize();
				
				// Obtaining all params
				NodeList params = document.getElementsByTagName("Param");
				for (int i = 0; i < params.getLength(); i++)
				{
					Element param = (Element)params.item(i);
					String key = param.getAttribute("Key");
					String value = param.getTextContent();
					
					properties.put(key, value);
				}
			}
		}
		catch (Exception e) {}
		
		
		return properties;
	}
}
