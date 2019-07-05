package bank.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;

public class AccountDTO {
    @JsonProperty("owner")
    private String owner;
    @JsonProperty("balance")
    private Double balance;

    public AccountDTO(){

    }

    public AccountDTO(String owner, Double balance){
        this.owner = owner;
        this.balance = balance;
    }

    public AccountDTO(BankResource.Account a) throws IOException {
        this.owner = a.getOwner();
        this.balance = a.getBalance();
    }

    @JsonProperty("owner")
    public String getOwner() {
        return owner;
    }

    @JsonProperty("balance")
    public Double getBalance() {
        return balance;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }
}
