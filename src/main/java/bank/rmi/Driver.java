package bank.rmi;

import bank.Bank;
import bank.BankDriver;
import bank.InactiveException;
import bank.OverdrawException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Driver implements BankDriver {
    private bank.Bank bank;
    @Override
    public void connect(String[] args) throws IOException, URISyntaxException {
        String host = args[0];
        try {
            this.bank = (bank.Bank) Naming.lookup("rmi://" + host + ":12345/Bank");
        } catch (NotBoundException e){
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() throws IOException {
        bank = null;
    }

    @Override
    public bank.Bank getBank() {
        return bank;
    }

    static class Bank implements bank.Bank {
        private final Map<String, Server.Account> accounts = new HashMap<>();

        @Override
        public String createAccount(String owner) throws IOException {
            String number = "101-47-" + accounts.size();
            accounts.put(number, new Server.Account(owner, number));
            return number;
        }

        @Override
        public boolean closeAccount(String number) throws IOException {
            if (accounts.containsKey(number) && accounts.get(number).isActive() && accounts.get(number).getBalance() == 0.0){
                accounts.get(number).setPassive();
                return true;
            }
            return false;
        }

        @Override
        public Set<String> getAccountNumbers() throws IOException {
            Set<String> activeAccounts = new HashSet<>();
            accounts.forEach((n, a) -> {
                if(a.isActive()) activeAccounts.add(n);
            });
            return activeAccounts;
        }

        @Override
        public Server.Account getAccount(String number) throws IOException {
            return accounts.get(number);
        }

        @Override
        public void transfer(bank.Account from, bank.Account to, double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {
            if (!to.isActive() || !from.isActive()) throw new InactiveException();

            double balanceTo = to.getBalance();
            double balanceFrom = from.getBalance();
            try {
                from.withdraw(amount);
                to.deposit(amount);
            } catch (OverdrawException e) {
                if (from.getBalance() != balanceFrom) {
                    from.deposit(amount);
                }
                if (to.getBalance() != balanceTo) {
                    to.withdraw(amount);
                }
            }
        }
    }

    static class Account implements bank.Account {
        private final String number;
        private final String owner;
        private double balance;
        private boolean active = true;

        Account(String owner, String number) {
            this.owner = owner;
            this.number = number;
        }

        @Override
        public String getNumber() throws IOException {
            return number;
        }

        @Override
        public String getOwner() throws IOException {
            return owner;
        }

        @Override
        public boolean isActive(){
            return active;
        }

        public void setPassive() {
            this.active = false;
        }

        @Override
        public void deposit(double amount) throws IOException, IllegalArgumentException, InactiveException {
            if (!isActive()) {
                throw new InactiveException();
            }
            if (amount < 0.0) {
                throw new IllegalArgumentException();
            }
            balance = balance + amount;
        }

        @Override
        public void withdraw(double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {
            if (!isActive()) {
                throw new InactiveException();
            }
            if (amount < 0.0) {
                throw new IllegalArgumentException();
            }
            if (balance - amount < 0.0) {
                throw new OverdrawException();
            }
            balance = balance - amount;
        }

        @Override
        public double getBalance() throws IOException {
            return balance;
        }
    }
}
