package com.hadis.BankTransactions;

import java.net.*;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.*;


public class Main{
	public static void main(String [] args) {
		System.out.println("Client running...");
		
		if (args.length <1)
			System.out.println("Please give terminal.xml address");
		else{
			Client waitingClient = new Client(args[0]);	
			waitingClient.start();
		}
	}
}

class Client extends Thread{
	
	private String serverIP;
	private int port;
	private String message;
	private String clientLogFileAddr;
	private String terminalId;
	private String terminalType;
	private String terminalXMLAddr;
	
	public Client(String path){
		terminalXMLAddr = path;
		initializeClient();
	}
	
	private ArrayList<Transaction> transactions = new ArrayList<Transaction>();
	private class Transaction{
		public Transaction (String i, String t, String a, String d){
			id = i;
			type = t;
			amount = a;
			deposit = d;
		}
		public String id;
		public String type;
		public String amount;
		public String deposit;
		public String errorType = "";
		public boolean success;
		
	}
	
	public String getIP(){
		return serverIP;
	}
	
	public int getPort (){
		return port;
	}
	
	public void addToTransactions(Element tempTransaction){
		String id = tempTransaction.getAttribute("id");
		String type = tempTransaction.getAttribute("type");
		String amount = tempTransaction.getAttribute("amount");
		String deposit = tempTransaction.getAttribute("deposit");

		transactions.add(new Transaction (id, type, amount, deposit));
	}
	
	public void initializeClient (){
		File terminalInfo = new File (terminalXMLAddr);
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(terminalInfo);
			
			doc.getDocumentElement().normalize();
			
			NodeList terminalXMLInfo = doc.getElementsByTagName("terminal");
			terminalId = ((Element)(terminalXMLInfo.item(0))).getAttribute("id");
			terminalType =  ((Element)(terminalXMLInfo.item(0))).getAttribute("type");
			
			NodeList serverInfo = doc.getElementsByTagName("server");
			serverIP = ((Element)(serverInfo.item(0))).getAttribute("ip");
			port = Integer.parseInt(((Element)(serverInfo.item(0))).getAttribute("port"));
			
			NodeList clientLogInfo = doc.getElementsByTagName("outLog");
			clientLogFileAddr = ((Element)(clientLogInfo.item(0))).getAttribute("path");
			
			NodeList transactionList = doc.getElementsByTagName("transaction");
			for (int i = 0 ; i < transactionList.getLength() ; i++)
				addToTransactions((Element)(transactionList.item(i)));
			
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}			
	}
	
	public void createLogFile(){
		DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder icBuilder;
		Document doc;
		Element e;
		try{
			icBuilder = icFactory.newDocumentBuilder();
			doc = icBuilder.newDocument();
			Element rootElement = doc.createElement("terminal");
			rootElement.setAttribute("id", terminalId);
			rootElement.setAttribute("type", terminalType);
			for (int i = 0 ; i < transactions.size() ; i++){
				e = doc.createElement("transaction");
				e.setAttribute("id", transactions.get(i).id);
				e.setAttribute("type", transactions.get(i).type);
				e.setAttribute("deposit", transactions.get(i).deposit);
				e.setAttribute("success", new Boolean (transactions.get(i).success).toString());
				if (! transactions.get(i).success)
					e.setAttribute("errorType", transactions.get(i).errorType );
				rootElement.appendChild(e);
			}

			doc.appendChild(rootElement);
			try {
				Transformer tr = TransformerFactory.newInstance().newTransformer();
				tr.setOutputProperty(OutputKeys.INDENT, "yes");
				tr.setOutputProperty(OutputKeys.METHOD, "xml");
				tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "roles.dtd");
				tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
				
				tr.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(clientLogFileAddr)));

			} catch (TransformerException te) {
				System.out.println(te.getMessage());
			} catch (IOException ioe) {
	        	System.out.println(ioe.getMessage());
	        }
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	@Override
	public void run(){
		try {
			
			System.out.println("Connecting to " + serverIP +
			" on port " + port);
			Socket client = new Socket(serverIP, port);
			System.out.println("Just connected to " 
			+ client.getRemoteSocketAddress());
			
			
			OutputStream outToServer = client.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			String terminalInfoForServer = (new Integer (transactions.size())).toString() + 
										" " + terminalId.toString() + " " + terminalType;
			out.writeUTF(terminalInfoForServer);
			InputStream inFromServer = client.getInputStream();
			DataInputStream in = new DataInputStream(inFromServer);
			System.out.println("Servers says" + in.readUTF());
				
			for (int i = 0; i < transactions.size(); i++){
				
				outToServer = client.getOutputStream();
				out = new DataOutputStream(outToServer);
				out.writeUTF(transactions.get(i).id + " " + transactions.get(i).type +
						" " + transactions.get(i).amount + " " + transactions.get(i).deposit);
				
				inFromServer = client.getInputStream();
				in = new DataInputStream(inFromServer);
				message = in.readUTF();	
				System.out.println("Server says " + message);
				String[] tempMessage = message.split(" ");
				if (tempMessage[0].equals("Error")) {
					transactions.get(i).success = false;
					transactions.get(i).errorType = tempMessage[1];
				}
				else 
					transactions.get(i).success = true;
			}
				
			client.close();
			createLogFile();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	
}
