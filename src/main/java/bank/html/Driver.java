package bank.html;

import bank.*;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Stream;

public class Driver implements BankDriver {
    private Bank bank = null;
    private static HttpClient client = HttpClient.newHttpClient();


    @Override
    public void connect(String[] args) throws IOException, URISyntaxException {
        bank = new Bank(args);
        System.out.println("connected...");
    }

    @Override
    public void disconnect() throws IOException {
        bank = null;
        System.out.println("disconnected...");
    }

    @Override
    public bank.Bank getBank() {
        return bank;
    }

    static class Bank implements bank.Bank {
        private String host;
        private int port;
        String baseURI = "http://localhost:8080/myBank";
        //Map<String, Account> accounts = new HashMap();

        public Bank(String[] args) throws IOException, URISyntaxException {
            host = args[0];
            port = Integer.valueOf(args[1]);

        }

        private HttpResponse<String> makeRequest(String query) throws URISyntaxException, IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(baseURI + query))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());
            return response;
        }

        @Override
        public String createAccount(String owner) throws IOException {
            String query = "?action=createAccount&owner="+owner.replace(" ", "%20");
            try {
                HttpResponse<String> response = makeRequest(query);
                System.out.println(response.body());
                //accounts.put(response.body(), new Account(owner, response.body()));
                return response.body();
            } catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public boolean closeAccount(String number) throws IOException {
            return false;
        }

        @Override
        public Set<String> getAccountNumbers() throws IOException {
            String query = "?action=getAccountNumbers";
            try {
                HttpResponse<String> response = makeRequest(query);
                //System.out.println("getAccountNumbers: " + response.body());
                Set<String> set = new HashSet<>();
                String[] split = response.body().split("\n");
                System.out.println(response.body());
                for (int i = 1; i <= Integer.parseInt(split[0]); i++){
                    set.add(split[i]);
                }
                return set;
            } catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public Account getAccount(String number) throws IOException {
            String newNumber = number.replace("\n", "");
            String query = "?action=getAccount&number="+newNumber.replace(" ", "%20");
            System.out.println(query);
            try{
                HttpResponse<String> response = makeRequest(query);
                String[] split = response.body().split("\n");
                for (int i = 0; i < split.length; i++){
                    System.out.println("Split: " + split[i]);
                }
                Account account = new Account(split[0], newNumber, Double.valueOf(split[1]), Boolean.valueOf(split[2]));
//                if(!accounts.containsKey(number)){
//                    account = new Account(split[0], newNumber);
//                    accounts.put(newNumber, account);
//                }
//                account = accounts.get(number);
//                account.balance = Double.parseDouble(split[1]);
//                account.active = Boolean.parseBoolean(split[2]);

                return account;
            } catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void transfer(bank.Account a, bank.Account b, double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

        }

        public void transfer(Account a, Account b, double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

        }
    }

    static class Account implements bank.Account {
        private final String number;
        private final String owner;
        private double balance;
        private boolean active = true;
        String baseURI = "http://localhost:8080/myBank";

        public Account(String owner, String number) {
            this.owner = owner;
            this.number = number;
        }

        public Account(String owner, String number, Double balance, Boolean active) {
            this.owner = owner;
            this.number = number;
            this.balance = balance;
            this.active = active;
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

        private HttpResponse<String> makeRequest(String query) throws URISyntaxException, IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(baseURI + query))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());
            return response;
        }

        @Override
        public void deposit(double amount) throws IOException, IllegalArgumentException, InactiveException {
            String newNumber = number.replace("\n", "");
            String query = "?action=deposit&number="+newNumber+"&amount="+String.valueOf(amount).replace(" ", "%20");
            try {
                HttpResponse<String> response = makeRequest(query);
                System.out.println(response.body());
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void withdraw(double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

        }

        @Override
        public double getBalance() throws IOException {
            String newNumber = number.replace("\n", "");
            String query = "?action=balance&number="+newNumber.replace(" ", "%20");
            try {
                HttpResponse<String> response = makeRequest(query);
                System.out.println(response.body());
                return Double.valueOf(response.body());
            } catch (Exception e){
                e.printStackTrace();
            }
            return balance;
        }
    }
}
