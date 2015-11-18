package com.hadis.BankTransactions;

import java.net.*;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
			waitingClient.initializeClient();		
			waitingClient.sendTransactionsToServer();
		}
	}
}

class Client extends Thread{
	
	private String serverIP;
	private int port;
	private String clientLogFileAddr;
	private String message;
	private String terminalAddr;
	
	public Client(String path){
		terminalAddr = path;
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
		File terminalInfo = new File ("../../../terminal.xml");
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(terminalInfo);
			
			doc.getDocumentElement().normalize();
			
			NodeList serverInfo = doc.getElementsByTagName("server");
			serverIP = ((Element)(serverInfo.item(0))).getAttribute("ip");
			port = Integer.parseInt(((Element)(serverInfo.item(0))).getAttribute("port"));
			
			NodeList clientLogInfo = doc.getElementsByTagName("outLog");
			clientLogFileAddr = ((Element)(clientLogInfo.item(0))).getAttribute("path");
			
			NodeList transactionList = doc.getElementsByTagName("transaction");
			for (int i = 0 ; i < transactionList.getLength() ; i++)
				addToTransactions((Element)(transactionList.item(i)));
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
	}
	
	
	/*
	
	public void sendTransactionsToServer(){
		try {

			System.out.println("Connecting to " + serverIP + " on port " + port);
			Socket client = new Socket(serverIP, port);
			System.out.println("Just connected to " + client.getRemoteSocketAddress());
			
			OutputStream outToServer = client.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			
			String numOfTransactions =( new Integer(transactions.size())).toString();
			out.writeUTF(numOfTransactions);
			
			InputStream inFromServer = client.getInputStream();
			DataInputStream in = new DataInputStream(inFromServer);
			System.out.println("Server says " + in.readUTF());
			
			client.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	*/
	
	
	public void sendTransactionsToServer(){
		try {
			for (int i = 0 ; i < transactions.size() ; i++){
			
				System.out.println("Connecting to " + serverIP +
				" on port " + port);
				Socket client = new Socket(serverIP, port);
				System.out.println("Just connected to " 
				+ client.getRemoteSocketAddress());
				
				OutputStream outToServer = client.getOutputStream();
				DataOutputStream out = new DataOutputStream(outToServer);
				
				//out.writeUTF("Hello from " + client.getLocalSocketAddress());
				out.writeUTF(transactions.get(i).id + " " + transactions.get(i).type +
						" " + transactions.get(i).amount + " " + transactions.get(i).deposit);
				
				
				InputStream inFromServer = client.getInputStream();
				DataInputStream in =
				                new DataInputStream(inFromServer);
				System.out.println("Server says " + in.readUTF());
				
				
				client.close();
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	
}
