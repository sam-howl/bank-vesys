package bank.sockets;

import bank.InactiveException;
import bank.OverdrawException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Server {
    public static void main(String[] args) throws IOException {
        //Bank hier erstellen, damit mehrere Client auf die Bank zugreiffen können
        Bank bank = new Bank();
        try (ServerSocket server = new ServerSocket(1000)) {
            System.out.println("Started..." + server.getLocalPort());
            while (true) {
                Socket socket = server.accept();
                Thread thread = new Thread(new BankHandler(socket, bank));
                thread.start();
            }
        }
    }

    static class BankHandler implements Runnable{
        private final Socket socket;
        private final Bank bank;
        private final DataInputStream in;
        private final DataOutputStream out;

        public BankHandler(Socket s, Bank b) throws IOException{
            socket = s;
            this.bank = b;
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
//            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        }

        @Override
        public void run() {
            try {
                Account acc;
                while (true){
                    String input = in.readUTF();
                    switch (input) {
                        case "create" :
                            out.writeUTF(bank.createAccount(in.readUTF()));
                            out.flush();
                            break;
                        case "get-account" :
                            Account account = bank.getAccount(in.readUTF());
                            if (account == null){
                                System.out.println("Is null: ");
                                out.writeUTF("null");
                                out.flush();
                            } else {
                                out.writeUTF(account.getOwner());
                                out.writeDouble(account.getBalance());
                                out.writeBoolean(account.isActive());
                                out.flush();
                            }
                            break;
                        case "close-account":
                            out.writeBoolean(bank.closeAccount(in.readUTF()));
                            out.flush();
                            break;
                        case "get-accountnumbers":
                            Set<String> set = bank.getAccountNumbers();
                            out.writeInt(set.size());
                            out.flush();
                            for (String number : set) {
                                out.writeUTF(number);
                                out.flush();
                            }
                            break;
                        case "is-active":
                            acc = bank.getAccount(in.readUTF());
                            if (acc != null){
                                out.writeBoolean(acc.isActive());
                            } else {
                                out.writeBoolean(false);
                            }
                            out.flush();
                            break;
                        case "get-balance":
                            acc = bank.getAccount(in.readUTF());
                            if (acc != null){
                                out.writeDouble(acc.getBalance());
                            } else {
                                out.writeDouble(0.0);
                            }
                            out.flush();
                            break;
                        case "deposit":
                            try {
                                bank.getAccount(in.readUTF()).deposit(in.readDouble());
                                out.writeUTF("done");
                                out.flush();
                            } catch (InactiveException e) {
                                out.writeUTF("InactiveException");
                                out.flush();
                            } catch (IllegalArgumentException i){
                                out.writeUTF("IllegalArgumentException");
                                out.flush();
                            } catch (NullPointerException n){
                                out.writeUTF("NullPointerException");
                                out.flush();
                            }
                            break;
                        case "withdraw":
                            try {
                                bank.getAccount(in.readUTF()).withdraw(in.readDouble());
                                out.writeUTF("done");
                                out.flush();
                            } catch (InactiveException i){
                                out.writeUTF("InactiveException");
                                out.flush();
                            } catch (OverdrawException o){
                                out.writeUTF("OverdrawnException");
                                out.flush();
                            } catch (NullPointerException n){
                                out.writeUTF("NullPointerException");
                                out.flush();
                            } catch (IllegalArgumentException ia){
                                out.writeUTF("IllegalArgumentException");
                                out.flush();
                            }
                            break;
                        case "transfer":
                            try {
                                bank.transfer(bank.getAccount(in.readUTF()), bank.getAccount(in.readUTF()), in.readDouble());
                                out.writeUTF("done");
                                out.flush();
                            } catch (OverdrawException o){
                                out.writeUTF("OverdrawnException");
                                out.flush();
                            } catch (InactiveException i){
                                out.writeUTF("InactiveException");
                                out.flush();
                            } catch (IllegalArgumentException iae) {
                                out.writeUTF("IllegalException");
                                out.flush();
                            } catch (NullPointerException n){
                                out.writeUTF("NullPointerException");
                                out.flush();
                            }
                            break;
                    }
                }

            } catch (IOException e){
                System.out.println(e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }



    static class Bank implements bank.Bank {
        private final Map<String, Server.Account> accounts = new HashMap<>();

        @Override
        public String createAccount(String owner) throws IOException {
            String number = "101-47-" + accounts.size();
            accounts.put(number, new Account(owner, number));
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
        public Account getAccount(String number) throws IOException {
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
