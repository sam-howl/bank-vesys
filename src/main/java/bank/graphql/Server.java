package bank.graphql;

import bank.InactiveException;
import bank.OverdrawException;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import graphql.servlet.SimpleGraphQLHttpServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Server extends HttpServlet {
    private SimpleGraphQLHttpServlet graphQLServlet;
    private Bank bank = new Bank();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        graphQLServlet.service(req, resp);
    }

    @Override
    public void init() throws ServletException {
        graphQLServlet = SimpleGraphQLHttpServlet.newBuilder(buildSchema()).build();
    }

    enum Result {
        OK, INACTIVE, OVERDRAW, ILLEGAL
    }

    private GraphQLSchema buildSchema() {
        Reader streamReader = new InputStreamReader(Server.class.getResourceAsStream("/bank.graphqls"));
        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(streamReader);

        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                .type(TypeRuntimeWiring.newTypeWiring("Query")
                        .dataFetcher("getAccount", env -> {
                            try {
                                return bank.getAccount(env.getArgument("number"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            //Account was not found
                            return null;
                        })
                        .dataFetcher("getAllAccounts", env -> {
                            try {
                                return bank.getAccountNumbers().stream()
                                        .map(id -> {
                                            try {
                                                return bank.getAccount(id);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            return null;
                                        })
                                        .collect(Collectors.toList());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return null;
                        })
                )
                .type(TypeRuntimeWiring.newTypeWiring("Mutation")
                    .dataFetcher("createAccount", env -> {
                        try {
                            return bank.createAccount(env.getArgument("owner"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    })
                    .dataFetcher("deposit", env -> {
                        try {
                            bank.getAccount(env.getArgument("number"))
                                    .deposit(env.getArgument("amount"));
                            return Result.OK;
                        } catch (InactiveException e) {
                            return Result.INACTIVE;
                        } catch (Exception e) {
                            return Result.ILLEGAL;
                        }
                    })
                )
                .build();
        return new SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, wiring);
    }

    static class Bank implements bank.Bank{
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
