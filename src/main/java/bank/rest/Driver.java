package bank.rest;

import bank.InactiveException;
import bank.OverdrawException;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

public class Driver implements bank.BankDriver {
    private Bank bank = null;
    private final static String DEFAULT_PATH = "http://localhost:9998/bank/accounts";
    private static String path = DEFAULT_PATH;
    private static Client client;

    @Override
    public void connect(String[] args) throws IOException, URISyntaxException {
        client = ClientBuilder.newClient();
        bank = new Bank(args);
    }

    @Override
    public void disconnect() throws IOException {
        bank = null;
    }

    @Override
    public Bank getBank() {
        return bank;
    }

    static class Bank implements bank.Bank {

        private final String path = "http://localhost:9998/bank/accounts";

        public Bank(String[] args) throws IOException, URISyntaxException {
            //Client client = ClientBuilder.newClient();
            //target = client.target(path);
        }

        @Override
        public String createAccount(String owner) throws IOException {
            WebTarget target = client.target(path);
            Form f = new Form();
            f.param("owner", owner);
            Response response = target.request().post(Entity.entity(f, MediaType.APPLICATION_FORM_URLENCODED));
            if(response.getStatusInfo() != Response.Status.CREATED) return null;
            String loc = response.getHeaderString("Location");
            return loc.substring(loc.lastIndexOf("/") + 1);
        }

        @Override
        public boolean closeAccount(String number) throws IOException {
            return false;
        }

        @Override
        public Set<String> getAccountNumbers() throws IOException {
            WebTarget target = client.target(path);
            String response = target.request().accept("text/plain").get(String.class);
            String[] split = response.split("\n");
            Set<String> set = new HashSet<>();
            for (String s : split){
                set.add(s.substring(s.lastIndexOf("/") + 1));
            }
            return set;
        }

        @Override
        public Account getAccount(String number) throws IOException {
            WebTarget target = client.target(path);
            AccountDTO response = target.request().accept(MediaType.APPLICATION_JSON).get(AccountDTO.class);
            System.out.println("hallo");
            //AccountDTO account = response.readEntity(AccountDTO.class);
            return new Account(response.getOwner(), number);
        }

        @Override
        public void transfer(bank.Account a, bank.Account b, double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

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

        @Override
        public String getNumber() throws IOException {
            return number;
        }

        @Override
        public String getOwner() throws IOException {
            return owner;
        }

        @Override
        public boolean isActive() throws IOException {
            return active;
        }


        @Override
        public void deposit(double amount) throws IOException, IllegalArgumentException, InactiveException {
            WebTarget target = client.target(path + "/" + number);
            Response response = target.request().put(Entity.entity(new AccountDTO(owner, balance+amount), MediaType.APPLICATION_JSON));
            System.out.println("Deposit done: " + response.getStatus());
        }

        @Override
        public void withdraw(double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

        }

        @Override
        public double getBalance() throws IOException {
            return balance;
        }
    }
}
