package com.hadis.BankTransactions;

import java.math.BigDecimal;

public class Deposit {
	public String name;
	public String id;
	public BigDecimal initialBalance;
	public BigDecimal upperBound;
	
	public String getId(){
		return id;
	}
	
	public String getName(){
		return name;
	}
	
	public BigDecimal getInitialBalance(){
		return initialBalance;
	}
	
	public BigDecimal getUpperBound(){
		return upperBound;
	}
	
	public Deposit(String n, String i, BigDecimal ib, BigDecimal ub){
		name = n;
		id = i;
		initialBalance = ib;
		upperBound = ub;
	}
	
	public void depositIntoAccount(BigDecimal addedDeposit)
			throws DefinedException{
		if (addedDeposit.compareTo(new BigDecimal(0))< 0)
			throw new InvaliDepositdAmountType(id);
		
		synchronized (this){
			BigDecimal depositAmount = new BigDecimal(0);
			depositAmount = depositAmount.add(initialBalance);
			depositAmount = depositAmount.add(addedDeposit);
			if (upperBound.compareTo(depositAmount) < 0)
				throw new DepositOutOfUpperBound(id);
			initialBalance = initialBalance.add(addedDeposit);
		}
	}
	
	public void withdrawFromAccount(BigDecimal withdrawnAmount)
			throws DefinedException{
		if (withdrawnAmount.compareTo(new BigDecimal(0))< 0)
			throw new InvaliDepositdAmountType(id);
		
		synchronized (this){
			BigDecimal depositAmount = new BigDecimal(0);
			depositAmount = depositAmount.add(initialBalance);
			depositAmount = depositAmount.subtract(withdrawnAmount);
			if (depositAmount.compareTo(new BigDecimal(0)) <0)
				throw new DepositNotEnough(id);
			initialBalance = initialBalance.subtract(withdrawnAmount);
		}
	}
}

