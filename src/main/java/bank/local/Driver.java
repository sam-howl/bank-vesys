/*
 * Copyright (c) 2019 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved. 
 */

package bank.local;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import bank.InactiveException;
import bank.OverdrawException;

public class Driver implements bank.BankDriver {
	private Bank bank = null;

	@Override
	public void connect(String[] args) {
		bank = new Bank();
		System.out.println("connected...");
	}

	@Override
	public void disconnect() {
		bank = null;
		System.out.println("disconnected...");
	}

	@Override
	public Bank getBank() {
		return bank;
	}

	static class Bank implements bank.Bank {

		private final Map<String, Account> accounts = new HashMap<>();

		@Override
		public Set<String> getAccountNumbers() throws IOException{
			Set<String> activeAccounts = new HashSet<>();
			accounts.forEach((n, a) -> {
				if(a.isActive()) activeAccounts.add(n);
			});
			return activeAccounts;
		}

		@Override
		public String createAccount(String owner) {
			String number = "101-47-" + accounts.size();
			if (accounts.containsKey(number)){
				return null;
			}
			accounts.put(number, new Account(owner, number));
			return number;
		}

		@Override
		public boolean closeAccount(String number) {
			if (accounts.containsKey(number) && accounts.get(number).isActive() && accounts.get(number).getBalance() == 0.0){
				accounts.get(number).setPassive();
				return true;
			}
			return false;
		}

		@Override
		public bank.Account getAccount(String number) {
			return accounts.get(number);
		}

		@Override
		public void transfer(bank.Account from, bank.Account to, double amount)
				throws IOException, InactiveException, OverdrawException {
			if(!to.isActive() || !from.isActive()) throw new InactiveException();
			double frombalance = from.getBalance();
			double tobalance = to.getBalance();
			try {
				from.withdraw(amount);
				to.deposit(amount);
			} catch (OverdrawException o){
				if (from.getBalance() != frombalance){
					from.deposit(amount);
				}
				if (to.getBalance() != tobalance){
					to.withdraw(amount);
				}
			}
		}

	}

	static class Account implements bank.Account {
		private String number;
		private String owner;
		private double balance;
		private boolean active = true;

		Account(String owner, String number) {
			this.owner = owner;
			this.number = number;
		}

		@Override
		public double getBalance() {
			return balance;
		}

		@Override
		public String getOwner() {
			return owner;
		}

		@Override
		public String getNumber() {
			return number;
		}

		@Override
		public boolean isActive() {
			return active;
		}

		public void setPassive() {
			this.active = false;
		}

		@Override
		public void deposit(double amount) throws InactiveException {
			if(!isActive()) throw new InactiveException();
			if (amount < 0.0) throw new IllegalArgumentException();
			balance+=amount;
		}

		@Override
		public void withdraw(double amount) throws InactiveException, OverdrawException {
			if (amount < 0.0) throw new IllegalArgumentException();
			if(!isActive()) throw new InactiveException();
			if (balance < amount){
				throw new OverdrawException();
			}
			balance-=amount;
		}

	}

}