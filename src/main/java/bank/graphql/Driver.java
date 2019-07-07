package bank.graphql;

import bank.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;

public class Driver implements BankDriver {
    static private Bank bank;
    static private URI uri;

    @Override
    public void connect(String[] args) throws URISyntaxException {
        bank = new Bank();
        uri = new URI("http://" + args[0] + ":" + args[1] + "/graphql");
    }

    @Override
    public void disconnect() throws IOException {

    }

    @Override
    public Bank getBank() {
        return bank;
    }

    static private JSONObject sendRequest (String request) throws IOException {
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(new Query(request));
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(uri)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());
            System.out.println(json);
            return json;
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    static class Query {
        private final String query;
        private final String variables;

        public Query(String query, String variables) {
            this.query = query;
            this.variables = variables;
        }

        public Query(String query) {
            this(query, null);
        }

        public String getQuery() {
            return query;
        }

        public String getVariables() {
            return variables;
        }
    }

    static class Bank implements bank.Bank{

        @Override
        public String createAccount(String owner) throws IOException {
            JSONObject json = sendRequest(
                    "mutation {createAccount (owner : \"" + owner + "\" ) }");
            String number = json.getJSONObject("data").getString("createAccount");
            new Account(number, owner);
            return number;
        }

        @Override
        public boolean closeAccount(String number) throws IOException {
            return false;
        }

        @Override
        public Set<String> getAccountNumbers() throws IOException {
            JSONObject json = sendRequest(
                    "query {getAllAccounts { number } }");
            JSONArray accounts = json.getJSONObject("data").getJSONArray("getAllAccounts");
            System.out.println(accounts);
            Set<String> set = new HashSet<>();
            for (int i = 0; i < accounts.length(); i++){
                set.add(accounts.getJSONObject(i).getString("number"));
            }
            return set;
        }

        @Override
        public Account getAccount(String number) throws IOException {
            JSONObject json = sendRequest(
                    "query {getAccount (number : \"" + number + "\" ) { number, owner, balance, active } }");
            JSONObject account = json.getJSONObject("data").getJSONObject("getAccount");
            return new Account(account.getString("number"), account.getString("owner"), account.getBoolean("active"), account.getDouble("balance"));
        }

        @Override
        public void transfer(bank.Account a, bank.Account b, double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

        }
    }

    static class Account implements bank.Account{
        String number;
        String owner;
        Boolean active;
        Double balance;

        public Account (String number, String owner){
            this.number = number;
            this.owner = owner;
            this.active = true;
            this.balance = 0.0;
        }

        public Account (String number, String owner, Boolean active, Double balance){
            this.number = number;
            this.owner = owner;
            this.active = active;
            this.balance = balance;
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
            JSONObject json = sendRequest(
                    "mutation {deposit (number : \"" + getNumber() + "\", amount : " + amount + " ) }");
            String res = json.getJSONObject("data").getString("deposit");
            if("INACTIVE".equals(res))
                throw new InactiveException();
            else if ("ILLEGAL".equals(res))
                throw new IllegalArgumentException();
        }

        @Override
        public void withdraw(double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

        }

        @Override
        public double getBalance() throws IOException {
            JSONObject json = sendRequest(
                    "query {getAccount (number : \"" + number + "\" ) { balance } }");
            Double bal = json.getJSONObject("data").getJSONObject("getAccount").getDouble("balance");
            return bal;
        }
    }
}
