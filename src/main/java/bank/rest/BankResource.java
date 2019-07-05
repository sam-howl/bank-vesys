package bank.rest;

import bank.InactiveException;
import bank.OverdrawException;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.inject.Singleton;
import javax.portlet.ProcessAction;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Singleton
@Path("accounts")
public class BankResource {
    private final Bank bank;

    public BankResource(){
        bank = new Bank();
    }

    @GET
    @Produces("text/plain")
    public String getAccountNumbers(@Context UriInfo uriInfo)
            throws IOException {
        StringBuffer buf = new StringBuffer();
        for(String acc : bank.getAccountNumbers()) {
            buf.append(uriInfo.getAbsolutePathBuilder().path(acc).build());
            buf.append("\n");
        }
        return buf.toString();
    }

    @GET
    @Produces("application/json")
    public Set<String> getAccountNumbers() throws IOException {
        return bank.getAccountNumbers();
    }

    @GET
    @Produces("application/json")
    //@Produces(MediaType.TEXT_PLAIN)
    @Path("{id}")
    //@Consumes({MediaType.APPLICATION_JSON})
    public Response getAccount(@PathParam("id") String id) throws IOException {
        Account account = bank.getAccount(id);
        if(account == null)
            throw new NotFoundException("Account with number " + id + " was not found!");
        //GenericEntity entity = new GenericEntity<Account>(account){};
        return Response.ok(new AccountDTO(account)).build();
        //return Response.ok(new AccountDTO(account)).tag(""+account.hashCode()).build();
    }

    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response create(
            @Context UriInfo uriInfo,
            @FormParam("owner") String owner) throws IOException
    {
        String id = bank.createAccount(owner);
        URI location = uriInfo.getRequestUriBuilder().path(id).build();
        return Response.created(location).build();
    }

    @PUT
    @Produces("application/json")
    @Consumes("application/json")
    @Path("{id}")
    public Response putAccount(@Context Request request, @PathParam("id") String id, AccountDTO dto) throws IOException {
        Account account = bank.getAccount(id);
        if (account == null)
            throw new NotFoundException("Account with number " + id + " was not found!");
        Response.ResponseBuilder builder = request.evaluatePreconditions(new EntityTag(""+account.hashCode()));
        System.out.println("line 79");
        if (builder != null){
            return builder.build();
        }
        System.out.println("line 83");
        double delta = dto.getBalance() - account.getBalance();
        System.out.println(delta);
        if(delta != 0){
            try{
                if(delta > 0) account.deposit(delta); else account.withdraw(-delta);
                System.out.println("line 88");
            } catch (IllegalArgumentException e) {
                System.out.println("bla");
                throw new WebApplicationException(e,
                        Response.Status.BAD_REQUEST); // 400
            } catch (InactiveException e) {
                throw new WebApplicationException(e,
                        Response.Status.GONE); // 410
            } catch (OverdrawException e) {
                throw new WebApplicationException(e,
                        Response.Status.FORBIDDEN); // 403
            }
        }
        System.out.println("before return line 100");
        return Response.ok().build();
        //return Response.ok(new AccountDTO(account)).tag(""+account.hashCode()).build();
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
}
