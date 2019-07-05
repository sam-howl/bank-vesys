//package bank.html;
//
//import bank.InactiveException;
//import bank.OverdrawException;
//import com.sun.net.httpserver.Filter;
//import com.sun.net.httpserver.HttpExchange;
//import com.sun.net.httpserver.HttpHandler;
//import com.sun.net.httpserver.HttpServer;
//
//import java.io.*;
//import java.net.*;
//import java.nio.charset.Charset;
//import java.util.*;
//
//public class Server {
//    public static void main(String[] args) throws IOException {
//        Bank bank = new Bank();
//        int port = 8080;
//
//        HttpServer server = HttpServer.create(
//                new InetSocketAddress(port),
//                0);
//        server.createContext("/bank", new BankHandler(bank)).getFilters().add(new ParameterParser());
//        server.start();
//
//        System.out.println("WebServer listening at port " + port);
//    }
//
//    static class BankHandler implements HttpHandler {
//        private final Bank bank;
//
//        public BankHandler (Bank b){
//            this.bank = b;
//        }
//
//        @Override
//        public void handle(HttpExchange exchange) throws IOException {
//            String response = "";
//            if(exchange.getRequestURI().getPath().endsWith("register")) {
//                StringBuilder buf = new StringBuilder();
//                buf.append("<HTML><BODY><H1>Create Account</H1>");
//                buf.append("<form name=\"register\" action=\"/bank\" method=POST>");
//                buf.append("<TABLE>");
//                buf.append("<TR><TD>Name:</TD> <TD><input size=40 maxlength=40 name=\"user\"></TD></TR>");
//                buf.append("<TR><TD>Amount:</TD><TD><input size=40 maxlength=40 type=\"number\" step=0.01 name=\"amount\"></TD></TR>");
//                buf.append("</TABLE>");
//                buf.append("<p>");
//                buf.append("<input name=\"submit\" type=submit value=\"Absenden\">");
//                buf.append("</form>");
//                buf.append("</body></html>");
//                response = buf.toString();
//                System.out.println(response);
//            } else if ("GET".equals(exchange.getRequestMethod())){
//                StringBuilder buf = new StringBuilder();
//                buf.append("<HTML><BODY><H1>Accounts</H1>");
//                buf.append("<table border=1>");
//                for (String number : bank.getAccountNumbers()){
//                    Account acc = bank.getAccount(number);
//                    buf.append(String.format("<tr><td width=300>%s</td><td width=100 align=right>%20.2f</td></tr>", acc.getNumber(), acc.getBalance()));
//                }
//                buf.append("</table>");
//                buf.append("<a href=\"bank/register\">create new account    </a>");
//                buf.append("<a href=\"bank/delete\">delete account</a>");
//                buf.append("</body></html>");
//                response = buf.toString();
//            } else if ("POST".equals(exchange.getRequestMethod())) {
//                Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
//                String user = (String)params.get("user");
//                double amount = Double.parseDouble((String)params.get("amount"));
//                String number = bank.createAccount(user);
//                Account acc = bank.getAccount(number);
//                acc.balance = amount;
//                exchange.getResponseHeaders().add("Location", "/bank");
//                exchange.sendResponseHeaders(301, -1);
//                return;
//            }
//            exchange.getResponseHeaders().add("Content-type", "text/html; charset=UTF-8");
//            exchange.sendResponseHeaders(200,0);
//            OutputStream os = exchange.getResponseBody();
//            os.write(response.getBytes(Charset.forName("UTF-8")));
//            os.close();
//        }
//    }
//
//    static class Bank implements bank.Bank {
//        private final Map<String, Account> accounts = new HashMap<>();
//
//        @Override
//        public String createAccount(String owner) throws IOException {
//            String number = "101-47-" + accounts.size();
//            accounts.put(number, new Account(owner, number));
//            return number;
//        }
//
//        @Override
//        public boolean closeAccount(String number) throws IOException {
//            if (accounts.containsKey(number) && accounts.get(number).isActive() && accounts.get(number).getBalance() == 0.0){
//                accounts.get(number).setPassive();
//                return true;
//            }
//            return false;
//        }
//
//        @Override
//        public Set<String> getAccountNumbers() throws IOException {
//            Set<String> activeAccounts = new HashSet<>();
//            accounts.forEach((n, a) -> {
//                if(a.isActive()) activeAccounts.add(n);
//            });
//            return activeAccounts;
//        }
//
//        @Override
//        public Account getAccount(String number) throws IOException {
//            return accounts.get(number);
//        }
//
//        @Override
//        public void transfer(bank.Account from, bank.Account to, double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {
//            if (!to.isActive() || !from.isActive()) throw new InactiveException();
//
//            double balanceTo = to.getBalance();
//            double balanceFrom = from.getBalance();
//            try {
//                from.withdraw(amount);
//                to.deposit(amount);
//            } catch (OverdrawException e) {
//                if (from.getBalance() != balanceFrom) {
//                    from.deposit(amount);
//                }
//                if (to.getBalance() != balanceTo) {
//                    to.withdraw(amount);
//                }
//            }
//        }
//    }
//
//    static class Account implements bank.Account {
//        private final String number;
//        private final String owner;
//        private double balance;
//        private boolean active = true;
//
//        Account(String owner, String number) {
//            this.owner = owner;
//            this.number = number;
//        }
//
//        @Override
//        public String getNumber() throws IOException {
//            return number;
//        }
//
//        @Override
//        public String getOwner() throws IOException {
//            return owner;
//        }
//
//        @Override
//        public boolean isActive(){
//            return active;
//        }
//
//        public void setPassive() {
//            this.active = false;
//        }
//
//        @Override
//        public void deposit(double amount) throws IOException, IllegalArgumentException, InactiveException {
//            if (!isActive()) {
//                throw new InactiveException();
//            }
//            if (amount < 0.0) {
//                throw new IllegalArgumentException();
//            }
//            balance = balance + amount;
//        }
//
//        @Override
//        public void withdraw(double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {
//            if (!isActive()) {
//                throw new InactiveException();
//            }
//            if (amount < 0.0) {
//                throw new IllegalArgumentException();
//            }
//            if (balance - amount < 0.0) {
//                throw new OverdrawException();
//            }
//            balance = balance - amount;
//        }
//
//        @Override
//        public double getBalance() throws IOException {
//            return balance;
//        }
//    }
//
//    static class ParameterParser extends Filter {
//
//        @Override
//        public String description() {
//            return "Parses the requested URI for parameters";
//        }
//
//        @Override
//        public void doFilter(HttpExchange exchange, Chain chain)
//                throws IOException {
//            parseGetParameters(exchange);
//            parsePostParameters(exchange);
//            chain.doFilter(exchange);
//        }
//
//        private void parseGetParameters(HttpExchange exchange)
//                throws UnsupportedEncodingException {
//            Map<String, Object> parameters = new HashMap<>();
//            URI requestedUri = exchange.getRequestURI();
//            String query = requestedUri.getRawQuery();
//            parseQuery(query, parameters);
//            exchange.setAttribute("parameters", parameters);
//        }
//
//        private void parsePostParameters(HttpExchange exchange)
//                throws IOException {
//            if ("post".equalsIgnoreCase(exchange.getRequestMethod())) {
//                @SuppressWarnings("unchecked")
//                Map<String, Object> parameters = (Map<String, Object>) exchange
//                        .getAttribute("parameters");
//                InputStreamReader isr = new InputStreamReader(
//                        exchange.getRequestBody(), "utf-8");
//                BufferedReader br = new BufferedReader(isr);
//                String query = br.readLine();
//                parseQuery(query, parameters);
//            }
//        }
//
//        @SuppressWarnings("unchecked")
//        public static void parseQuery(String query,
//                                      Map<String, Object> parameters)
//                throws UnsupportedEncodingException {
//            if (query != null) {
//                StringTokenizer st = new StringTokenizer(query, "&");
//                while (st.hasMoreTokens()) {
//                    String keyValue = st.nextToken();
//                    StringTokenizer st2 = new StringTokenizer(keyValue, "=");
//                    String key = null;
//                    String value = "";
//                    if (st2.hasMoreTokens()) {
//                        key = st2.nextToken();
//                        key = URLDecoder.decode(key, "UTF-8");
//                    }
//
//                    if (st2.hasMoreTokens()) {
//                        value = st2.nextToken();
//                        value = URLDecoder.decode(value, "UTF-8");
//                    }
//
//                    if (parameters.containsKey(key)) {
//                        Object o = parameters.get(key);
//                        if (o instanceof List) {
//                            List<String> values = (List<String>) o;
//                            values.add(value);
//                        } else if (o instanceof String) {
//                            List<String> values = new ArrayList<String>();
//                            values.add((String) o);
//                            values.add(value);
//                            parameters.put(key, values);
//                        }
//                    } else {
//                        parameters.put(key, value);
//                    }
//                }
//            }
//        }
//    }
//}
