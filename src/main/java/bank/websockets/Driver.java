package bank.websockets;

import bank.*;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.DeploymentException;
import javax.websocket.Session;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class Driver implements BankDriver2 {
    private Bank bank;
    private Session session;
    @Override
    public void registerUpdateHandler(UpdateHandler handler) throws IOException {

    }

    @Override
    public void connect(String[] args) throws IOException, URISyntaxException {
        try {
            URI uri = new URI("ws://" + args[0] + ":" + args[1] + "/websockets/bank");
            System.out.println("Connecting to server...");
            ClientManager client = ClientManager.createClient();
            session = client.connectToServer(this, uri);
        } catch (URISyntaxException | DeploymentException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void disconnect() throws IOException {
        bank = null;
        session.close();
        System.out.println("Disconnected...");
    }

    @Override
    public Bank getBank() {
        return bank;
    }


    static class Bank implements bank.Bank {

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


    static class Account implements bank.Account {

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
