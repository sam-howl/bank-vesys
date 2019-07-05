package bank.sockets;

import bank.*;

import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class Driver implements BankDriver {
    private bank.sockets.Driver.Bank bank = null;
    private static DataOutputStream out;
    private static DataInputStream in;
    private static Socket socket;

    @Override
    public void connect(String[] args) throws IOException {
        bank = new Bank(args);
        System.out.println("connected...");
    }

    @Override
    public void disconnect() throws IOException {
        bank = null;
        System.out.println("disconnected...");
    }

    private static void parseException(String msg) throws InactiveException, OverdrawException {
        if ("InactiveException".equals(msg)) {
            throw new InactiveException();
        } else if ("OverdrawnException".equals(msg)) {
            throw new OverdrawException();
        } else if ("IllegalException".equals(msg)) {
            throw new IllegalArgumentException();
        } else if ("NullPointerException".equals(msg)){
            throw new NullPointerException();
        }
    }

    @Override
    public Bank getBank() {
        return bank;
    }

    static class Bank implements bank.Bank {
        private String host;
        private int port;
        private Socket socket;

        public static final int GETACCOUNTNUMBERS = 10;
        public static final int CREATEACC = 20;
        public static final int CLOSEACC = 30;
        public static final int GETACC = 40;
        public static final int TRANSFER = 50;
        public static final int SETINACITVE = 60;
        public static final int DEPOSIT = 70;
        public static final int WITHDRAW = 80;
        public static final int INACITVEEX = 500;
        public static final int OVERDRAWEX = 510;
        public static final int NULLPTR = 520;

        public Bank(String[] args) throws IOException{
            host = args[0];
            port = Integer.valueOf(args[1]);
            socket = new Socket(host, port, null, 0);
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        }

        @Override
        public String createAccount(String owner) throws IOException {
            out.writeUTF("create");
            out.writeUTF(owner);
            out.flush();
            out.flush();
            return in.readUTF();
        }

        @Override
        public boolean closeAccount(String number) throws IOException {
            out.writeUTF("close-account");
            out.writeUTF(number);
            out.flush();
            return in.readBoolean();
        }

        @Override
        public Set<String> getAccountNumbers() throws IOException {
            out.writeUTF("get-accountnumbers");
            out.flush();
            Set<String> set = new HashSet<>();
            int size = in.readInt();
            while (set.size() < size){
                set.add(in.readUTF());
            }
            return set;
        }

        @Override
        public Account getAccount(String number) throws IOException {
            out.writeUTF("get-account");
            out.writeUTF(number);
            out.flush();
            String owner = in.readUTF();
            if (!owner.equals("null")){
                double balance = in.readDouble();
                boolean active = in.readBoolean();
                return new Account(owner, number, balance, active);
            }
            return null;
        }

        @Override
        public void transfer(bank.Account a, bank.Account b, double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {
            out.writeUTF("transfer");
            out.writeUTF(a.getNumber());
            out.writeUTF(b.getNumber());
            out.writeDouble(amount);
            out.flush();
            String msg = in.readUTF();
            parseException(msg);
        }
    }

    static class Account implements bank.Account {
        private final String number;
        private final String owner;
        private double balance;
        private boolean active = true;

        public Account(String owner, String number) {
            this.owner = owner;
            this.number = number;
        }

        public Account(String owner, String number, double balance, boolean active){
            this.owner = owner;
            this.number = number;
            this.balance = balance;
            this.active = active;
        }

        @Override
        public double getBalance() throws IOException{
            out.writeUTF("get-balance");
            out.writeUTF(number);
            out.flush();
            return in.readDouble();
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
        public boolean isActive() throws IOException {
            out.writeUTF("is-active");
            out.writeUTF(number);
            out.flush();
            return in.readBoolean();
        }

        public void setPassive() {
            this.active = false;
        }

        @Override
        public void deposit(double amount) throws IOException, InactiveException, IllegalArgumentException {
            if (amount < 0){
                throw new IllegalArgumentException();
            }
            out.writeUTF("deposit");
            out.writeUTF(number);
            out.writeDouble(amount);
            out.flush();
            String msg = in.readUTF();
            try {
                parseException(msg);
            } catch (OverdrawException e){

            }
        }

        @Override
        public void withdraw(double amount) throws IOException, InactiveException, OverdrawException {
            out.writeUTF("withdraw");
            out.writeUTF(number);
            out.writeDouble(amount);
            out.flush();
            String msg = in.readUTF();
            parseException(msg);
        }

    }
}
