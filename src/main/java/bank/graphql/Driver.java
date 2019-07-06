package bank.graphql;

import bank.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

public class Driver implements BankDriver {
    static private Bank bank;

    @Override
    public void connect(String[] args) throws IOException, URISyntaxException {
        bank = new Bank();
    }

    @Override
    public void disconnect() throws IOException {

    }

    @Override
    public Bank getBank() {
        return null;
    }

    static class Bank implements bank.Bank{

        @Override
        public String createAccount(String owner) throws IOException {
            return null;
        }

        @Override
        public boolean closeAccount(String number) throws IOException {
            return false;
        }

        @Override
        public Set<String> getAccountNumbers() throws IOException {
            return null;
        }

        @Override
        public Account getAccount(String number) throws IOException {
            return null;
        }

        @Override
        public void transfer(bank.Account a, bank.Account b, double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

        }
    }

    static class Account implements bank.Account{

        @Override
        public String getNumber() throws IOException {
            return null;
        }

        @Override
        public String getOwner() throws IOException {
            return null;
        }

        @Override
        public boolean isActive() throws IOException {
            return false;
        }

        @Override
        public void deposit(double amount) throws IOException, IllegalArgumentException, InactiveException {

        }

        @Override
        public void withdraw(double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

        }

        @Override
        public double getBalance() throws IOException {
            return 0;
        }
    }
}
