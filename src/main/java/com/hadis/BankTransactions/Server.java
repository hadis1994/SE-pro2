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
	private ArrayList<Deposit> deposits = new ArrayList<Deposit>();
	private String serverLogFileAddr;
	
	public Server() throws IOException {
		initializeServer();
		serverSocket = new ServerSocket(port);
		serverSocket.setSoTimeout(100000);
		
	}
	
	public Server(String s){
		
	}
	
	private Deposit makeDeposit(JSONObject jsonDeposit){
		String name = jsonDeposit.get(new String("customer")).toString();
		String id = jsonDeposit.get(new String("id")).toString();
		BigDecimal initialBalance = new BigDecimal(jsonDeposit.get(new String("initialBalance")).toString());
		BigDecimal upperBound = new BigDecimal (jsonDeposit.get(new String("upperBound")).toString());

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
	private void sync(){
		JSONObject core = new JSONObject();
		core.put("port", port);
		JSONArray jsonDeposits = new JSONArray();
		for (int i = 0 ; i < deposits.size() ; i++){
			JSONObject tempDeposit = new JSONObject();
			tempDeposit.put("customer", deposits.get(i).getName());
			tempDeposit.put("id", deposits.get(i).getId());
			tempDeposit.put("initialBalance", deposits.get(i).getInitialBalance());
			tempDeposit.put("upperBound", deposits.get(i).getUpperBound());
			jsonDeposits.add(tempDeposit);
			
		}
		core.put("deposits", jsonDeposits);
			
		core.put(new String ("outLog"), serverLogFileAddr);
		
		try {

			FileWriter file = new FileWriter("test.json");
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
			if (command.equals("sync")){
				System.out.println("sync being executed...");//TODO: server log
				sync();
			}
			else{
				System.out.println("here");
				throw new InvalidServerCommand(command);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
					try {
						carryOutTransaction(clientRequest[0] ,clientRequest[3], clientRequest[1], clientRequest[2]);
					} catch (DefinedException e) {
						error = e.sendMessage();
					}
					
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
			
			Thread console = new Server("input"){
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

