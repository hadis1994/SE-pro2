package com.hadis.BankTransactions;

public abstract class DefinedException extends Exception{
	private static final long serialVersionUID = 1L;
	public DefinedException(){
		
	}
	
	abstract public String sendMessage();
}

abstract class ClientException extends DefinedException{

	private static final long serialVersionUID = 1L;

	protected String customerId;
	public ClientException(String id) {
		customerId = id;
	}
	
}


class InvalidDepositIdException extends ClientException{
	private static final long serialVersionUID = 1L;

	public InvalidDepositIdException(String id) {
		super(id);
	}
	
	@Override
	public String sendMessage() {
		return ("Error 1 :: Customer with id: \"" + customerId + "\" does not exist.");
	}
}


class DepositOutOfUpperBound extends ClientException{
	private static final long serialVersionUID = 1L;

	public DepositOutOfUpperBound(String id) {
		super(id);
	}

	@Override
	public String sendMessage() {
		return ("Error 2 :: Cannot make changes to Customer with id: \"" 
							+ customerId + "\" added amount exceeds upper bound.");
	}
}


class DepositNotEnough extends ClientException{
	private static final long serialVersionUID = 1L;

	public DepositNotEnough(String id) {
		super(id);
	}

	@Override
	public String sendMessage() {
		return ("Error 3 :: Cannot make changes to Customer with id: \"" 
							+ customerId + "\" withdrawn amount exceeds deposit balance.");
	}
}

class InvaliDepositdAmountType extends ClientException{
	private static final long serialVersionUID = 1L;
	
	public InvaliDepositdAmountType(String id){
		super(id);
	}
	
	@Override
	public String sendMessage(){
		return ("Error 4 :: in changed amount of deposit for Customer with id: \"" 
				+ customerId + "\", amount cannot be negative.");
	}
}

class UnknownTransactionType extends ClientException {
	private static final long serialVersionUID = 1L;

	public UnknownTransactionType(String id){
		super(id);
	}
	
	@Override
	public String sendMessage(){
		return ("Error 5 :: Cannot make changes for Customer with id: \""
							+ customerId + "\" due to unknown transaction type.");
						
	}
}

class InvalidServerCommand extends DefinedException{

	private static final long serialVersionUID = 1L;
	private String command;
	
	public InvalidServerCommand(String c) {
		command = c;
	}
	
	@Override
	public String sendMessage() {
		return "Error 6 :: Unknown server command: \""+ command + '\"';
	}

}
