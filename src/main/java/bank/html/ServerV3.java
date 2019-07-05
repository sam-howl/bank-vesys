package bank.html;

import bank.InactiveException;
import bank.OverdrawException;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.util.*;

public class ServerV3 {
    static private Bank bank;
    static{
        bank = new Bank();
    }
    public static void main(String[] args) throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        Context context = tomcat.addContext("/", new File(".").getAbsolutePath());
        HttpServlet servlet = new MyServlet(bank);
        Tomcat.addServlet(context, "MyServlet", servlet);
        context.addServletMappingDecoded("/myBank", "MyServlet");
        tomcat.start();
        tomcat.getConnector();
        tomcat.getServer().await();
    }

    static class MyServlet extends HttpServlet {
        private Bank bank;

        public MyServlet(Bank bank){
            this.bank = bank;
        }

        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException{
            String action = request.getParameter("action");
            StringBuilder buf = new StringBuilder();
            PrintWriter writer = response.getWriter();
            String number = null;
            Account account;

            switch(action) {
                case "createAccount":
                    number = bank.createAccount(request.getParameter("owner"));
                    if(number != null) {
                        buf.append(number + "\n");
                    } else {
                        buf.append("\n");
                    }
                    writer.write(buf.toString());
                    writer.flush();
                    writer.close();
                    break;
                case "getAccountNumbers":
                    Set<String> s = bank.getAccountNumbers();
                    buf.append("" + s.size() + "\n");
                    for (String acc : s){
                        buf.append(acc + "\n");
                    }
                    writer.write(buf.toString());
                    writer.flush();
                    writer.close();
                    break;
                case "getAccount":
                    account = bank.getAccount(request.getParameter("number"));
                    System.out.println(account.getOwner() + " " + account.getBalance());
                    buf.append(account.getOwner() + "\n");
                    buf.append(account.getBalance() + "\n");
                    buf.append(account.isActive() + "\n");
                    writer.write(buf.toString());
                    writer.flush();
                    writer.close();
                    break;
                case "deposit":
                    account = bank.getAccount(request.getParameter("number"));
                    try {
                        account.deposit(Double.parseDouble(request.getParameter("amount")));
                        System.out.println(account.getBalance());
                        buf.append("\n");
                    } catch (Exception e){
                        buf.append("error\n");
                    }
                    writer.write(buf.toString());
                    writer.flush();
                    writer.close();
                    break;
                case "balance":
                    account = bank.getAccount(request.getParameter("number"));
                    try {
                        buf.append(account.getBalance());
                    } catch (Exception e){
                        buf.append("error\n");
                    }
                    writer.write(buf.toString());
                    writer.flush();
                    writer.close();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    static class Bank implements bank.Bank {
        private final Map<String, Account> accounts = new HashMap<>();

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

    static class ParameterParser extends Filter {

        @Override
        public String description() {
            return "Parses the requested URI for parameters";
        }

        @Override
        public void doFilter(HttpExchange exchange, Chain chain)
                throws IOException {
            parseGetParameters(exchange);
            parsePostParameters(exchange);
            chain.doFilter(exchange);
        }

        private void parseGetParameters(HttpExchange exchange)
                throws UnsupportedEncodingException {
            Map<String, Object> parameters = new HashMap<>();
            URI requestedUri = exchange.getRequestURI();
            String query = requestedUri.getRawQuery();
            parseQuery(query, parameters);
            exchange.setAttribute("parameters", parameters);
        }

        private void parsePostParameters(HttpExchange exchange)
                throws IOException {
            if ("post".equalsIgnoreCase(exchange.getRequestMethod())) {
                @SuppressWarnings("unchecked")
                Map<String, Object> parameters = (Map<String, Object>) exchange
                        .getAttribute("parameters");
                InputStreamReader isr = new InputStreamReader(
                        exchange.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                String query = br.readLine();
                parseQuery(query, parameters);
            }
        }

        @SuppressWarnings("unchecked")
        public static void parseQuery(String query,
                                      Map<String, Object> parameters)
                throws UnsupportedEncodingException {
            if (query != null) {
                StringTokenizer st = new StringTokenizer(query, "&");
                while (st.hasMoreTokens()) {
                    String keyValue = st.nextToken();
                    StringTokenizer st2 = new StringTokenizer(keyValue, "=");
                    String key = null;
                    String value = "";
                    if (st2.hasMoreTokens()) {
                        key = st2.nextToken();
                        key = URLDecoder.decode(key, "UTF-8");
                    }

                    if (st2.hasMoreTokens()) {
                        value = st2.nextToken();
                        value = URLDecoder.decode(value, "UTF-8");
                    }

                    if (parameters.containsKey(key)) {
                        Object o = parameters.get(key);
                        if (o instanceof List) {
                            List<String> values = (List<String>) o;
                            values.add(value);
                        } else if (o instanceof String) {
                            List<String> values = new ArrayList<String>();
                            values.add((String) o);
                            values.add(value);
                            parameters.put(key, values);
                        }
                    } else {
                        parameters.put(key, value);
                    }
                }
            }
        }
    }
}
