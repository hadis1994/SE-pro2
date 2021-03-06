package com.hadis.BankTransactions;

import java.io.*;
import java.net.*;
import java.util.ArrayList;


import java.math.BigDecimal;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Server extends Thread{

	private ServerSocket serverSocket;
	private int port;
	private static ArrayList<Deposit> deposits = new ArrayList<Deposit>();
	private String serverLogFileAddr;
	private static boolean ifInit = false;
	
	public Server() throws IOException {
		try {
			if (!ifInit){
				initializeServer();
				ifInit = true;
			}
			serverSocket = new ServerSocket(port);
			serverSocket.setSoTimeout(100000);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
	private Deposit makeDeposit(JSONObject jsonDeposit){
		String name = jsonDeposit.get(new String("customer")).toString();
		String id = jsonDeposit.get(new String("id")).toString();
		BigDecimal initialBalance = new BigDecimal(jsonDeposit.get(new String("initialBalance")).toString().replace(",", ""));
		BigDecimal upperBound = new BigDecimal (jsonDeposit.get(new String("upperBound")).toString().replace(",", ""));

		return new Deposit(name, id, initialBalance, upperBound);
	}
	
	private Deposit findDepositById(String id)
			throws DefinedException{
		for (int i = 0 ; i < deposits.size() ; i++)
			if (deposits.get(i).getId().equals(id))
				return deposits.get(i);
		throw new InvalidDepositIdException(id);
	}
	
	private void initializeServer(){
		JSONParser parser = new JSONParser();
		try {
			JSONObject coreJSON = (JSONObject) parser.parse(new FileReader("core.json"));
			port = Integer. parseInt(coreJSON.get(new String("port")).toString());
			
			JSONArray jsonDeposits = (JSONArray) coreJSON.get(new String ("deposits"));
			for (int i = 0; i< jsonDeposits.size(); i++){
				Deposit newDeposit = makeDeposit((JSONObject)jsonDeposits.get(i));
				deposits.add(newDeposit);
			}
			
			serverLogFileAddr = coreJSON.get(new String ("outLog")).toString();
			
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e2){
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}
	
	private void carryOutTransaction(String idOrSync,String depositId, String type, String amount)
			throws DefinedException{
		
		Deposit currentDeposit = findDepositById(depositId);
		if (type.equals("deposit"))
			currentDeposit.depositIntoAccount(new BigDecimal(amount));
		else if (type.equals("withdraw"))
			currentDeposit.withdrawFromAccount(new BigDecimal(amount));
		else
			throw new UnknownTransactionType(depositId);
	}
	
	
	@SuppressWarnings("unchecked")
	private synchronized void sync(){
		
		JSONParser parser = new JSONParser();
		try {
			JSONObject coreJSON = (JSONObject) parser.parse(new FileReader("core.json"));
			port = Integer. parseInt(coreJSON.get(new String("port")).toString());			
			serverLogFileAddr = coreJSON.get(new String ("outLog")).toString();
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e2){
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		
		System.out.println("sync being executed...");
		JSONObject core = new JSONObject();
		core.put("port", port);
		JSONArray jsonDeposits = new JSONArray();
		for (int i = 0 ; i < deposits.size() ; i++){
			JSONObject tempDeposit = new JSONObject();
			tempDeposit.put("customer", deposits.get(i).getName());
			tempDeposit.put("id", deposits.get(i).getId());
			try {
				tempDeposit.put("initialBalance", findDepositById(deposits.get(i).getId()).getInitialBalance().toString());
				
			} catch (DefinedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			tempDeposit.put("upperBound", deposits.get(i).getUpperBound().toString());
			jsonDeposits.add(tempDeposit);
			
		}
		core.put("deposits", jsonDeposits);
			
		core.put(new String ("outLog"), serverLogFileAddr);
		
		try {

			FileWriter file = new FileWriter("core.json",false);
			file.write(core.toJSONString());
			file.flush();
			file.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void getServerCommand()
			throws DefinedException{
		BufferedReader bufferReader = new BufferedReader(new InputStreamReader(System.in));
		try {
			String command = bufferReader.readLine();
			if (command.equals("sync"))
				sync();
			else
				throw new InvalidServerCommand(command);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public synchronized void updateLogFile(String transaction){
		
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(serverLogFileAddr, true)))) {
		    out.println(transaction);

		}catch (IOException e) {
		    e.printStackTrace();
		}

	}
	
	public void run() {
		while(true) {
			try {
				System.out.println("Waiting for client on port " +
				serverSocket.getLocalPort() + "...");
				
				Socket server = serverSocket.accept();
				System.out.println("Just connected to " + server.getRemoteSocketAddress());
				
				DataInputStream in = new DataInputStream(server.getInputStream());
				String []terminalInfo = in.readUTF().split(" ");
				int numOfTransactions = Integer.parseInt(terminalInfo[0]);
				DataOutputStream out = new DataOutputStream(server.getOutputStream());
				out.writeUTF(" requested for " + numOfTransactions + " transactions...");
				
				for(int i = 0 ; i <numOfTransactions ; i++){
					in = new DataInputStream(server.getInputStream());
					String error = "transaction done successfully";
					String []clientRequest = in.readUTF().split(" ");
					
					BigDecimal balance = new BigDecimal("0"); 
					String outToLog = "";
					outToLog = "terminalid:" + terminalInfo[1]
							+ " terminalType:" + terminalInfo[2]
							+ " transactionId:" + clientRequest[0]
							+ " transactionType: " + clientRequest[1]
							+ " depositId:" + clientRequest[3]
							+ " changedAmount:" + clientRequest[2];
					try {
						carryOutTransaction(clientRequest[0] ,clientRequest[3], clientRequest[1], clientRequest[2]);
						balance = findDepositById(clientRequest[3]).getInitialBalance();
						outToLog += " depositBalance:" +balance.toString()
								+ " success:true" ;
						
					} catch (DefinedException e) {
						error = e.sendMessage();
						outToLog += " success:false" ;
					}
							
					updateLogFile(outToLog);
					
					out = new DataOutputStream(server.getOutputStream());
					out.writeUTF(error);
				}
				
				server.close();
				
			 } catch(SocketTimeoutException s) {
				System.out.println("Socket timed out!");
				break;
			 }catch(IOException e) {
				e.printStackTrace();
				break;
			 }
		}
	}
	
	public static void main(String [] args){
		try {
			Thread server = new Server();
			server.start();
			Thread console =new Server(){
				@Override
				public void run(){
					while (true){
						try {
							getServerCommand();
						} catch (DefinedException e1) {
							System.out.println(e1.sendMessage());
						}
					}
				}
			};
			console.start();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}

