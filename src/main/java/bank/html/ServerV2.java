//package bank.html;
//
//import bank.InactiveException;
//import bank.OverdrawException;
//import com.sun.net.httpserver.Filter;
//import com.sun.net.httpserver.HttpExchange;
//import org.apache.catalina.Context;
//import org.apache.catalina.LifecycleException;
//import org.apache.catalina.startup.Tomcat;
//
//import javax.servlet.ServletException;
//import javax.servlet.annotation.WebServlet;
//import javax.servlet.http.HttpServlet;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.*;
//import java.net.URI;
//import java.net.URLDecoder;
//import java.util.*;
//
//public class ServerV2 {
//    private Bank bank;
//    public static void main(String[] args) throws LifecycleException {
//        Bank bank = new Bank();
//        Tomcat tomcat = new Tomcat();
//        tomcat.setPort(8080);
//        Context context = tomcat.addContext("/", new File(".").getAbsolutePath());
//        HttpServlet servlet = new CreateServlet(bank);
//        Tomcat.addServlet(context, "CreateServlet", servlet);
//        context.addServletMappingDecoded("/create", "CreateServlet");
//        HttpServlet servlet2 = new DeleteServlet(bank);
//        Tomcat.addServlet(context, "DeleteServlet", servlet2);
//        context.addServletMappingDecoded("/delete", "DeleteServlet");
//        tomcat.start();
//        tomcat.getConnector();
//        tomcat.getServer().await();
////        Context context = tomcat.addContext("/", new File(".").getAbsolutePath());
////
////        HttpServlet servlet = new HttpServlet() {
////            @Override
////            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
////                    throws ServletException, IOException {
////
////                PrintWriter writer = resp.getWriter();
////                writer.println("<html><title>Welcome</title><body>");
////                writer.println("<h1>Bank Application</h1>");
////                writer.println("</body></html>");
////            }
////        };
////
////        String servletName = "Default";
////        String urlPattern = "/";
////
////        // Analog zur Definition in web.xml
////        Tomcat.addServlet(context, servletName, servlet);
////        context.addServletMappingDecoded(urlPattern, servletName);
////
////        tomcat.start();
////        tomcat.getConnector();	// http://tomcat.10.x6.nabble.com/Start-embedded-Tomcat-9-0-1-server-from-java-code-td5068985.html
////        tomcat.getServer().await();
//    }
//
////    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException{
////        PrintWriter writer = response.getWriter();
////        response.setContentType("text/html");
////        writer.println("<html><title>Welcome</title><body>");
////        writer.println("<h1>asdfasfdasfdasdfasdfasdfasdfFSGD</h1>");
////        writer.println("</body></html>");
////    }
//
//    static class CreateServlet extends HttpServlet {
//        private Bank bank;
//
//        public CreateServlet(Bank bank){
//            this.bank = bank;
//        }
//
//        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException{
//            PrintWriter writer = response.getWriter();
//            response.setContentType("text/html");
//            writer.println("<html><title>Welcome</title><body>");
//            writer.println("<h1>Hallo</h1>");
//            writer.println("</body></html>");
//        }
//
//        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
//            String owner = request.getParameter("owner");
//            String error = null;
//            if (owner == null || owner.trim().equals("")) {
//                error = "Owner not set!";
//            } else {
//                String number = bank.createAccount(owner);
//                if (number == null) {
//                    error = "Account not created";
//                } else {
//                    response.sendRedirect("");
//                }
//            }
//            if (error != null) {
//                PrintWriter writer = response.getWriter();
//                response.setContentType("text/html");
//                writer.write("<html><title>Error</title><body>");
//                writer.write(error + "<br/><a href=\"create\">Back</a>");
//                writer.write("</body></html>");
//            }
//        }
//    }
//
//    static class DeleteServlet extends HttpServlet {
//        private Bank bank;
//        public DeleteServlet(Bank bank){
//            this.bank = bank;
//        }
//        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException{
//            PrintWriter writer = response.getWriter();
//            response.setContentType("text/html");
//            writer.println("<html><title>Welcome</title><body>");
//            writer.println("<h1>DELETE</h1>");
//            writer.println("</body></html>");
//        }
//    }
//
//    static class Bank implements bank.Bank {
//        private final Map<String, Server.Account> accounts = new HashMap<>();
//
//        @Override
//        public String createAccount(String owner) throws IOException {
//            String number = "101-47-" + accounts.size();
//            accounts.put(number, new Server.Account(owner, number));
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
//        public Server.Account getAccount(String number) throws IOException {
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
