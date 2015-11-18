#!/bin/sh
javac com/hadis/BankTransactions/Main.java
for i in $(seq 1 100); do
	echo item: $i
	java com/hadis/BankTransactions/Main "../../../terminal.xml "& java com/hadis/BankTransactions/Main "../../../terminal1.xml" & java com/hadis/BankTransactions/Main "../../../terminal2.xml"

done
