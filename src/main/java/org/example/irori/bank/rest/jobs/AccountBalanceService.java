package org.example;

import static se.irori.bank.rest.jobs.AccountBalanceService.Currency.GBP;
import static se.irori.bank.rest.jobs.AccountBalanceService.Currency.SEK;
import static se.irori.bank.rest.jobs.AccountBalanceService.Currency.USD;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


/**
 * Returns the balance of the customer's account.
 *
 * @author Trazan Apansson
 */
@Path("/account/balance")
@Service
public class AccountBalanceService {
    
    @Autowired
    AccountService accountService;
    
    @Autowired
    AlertServiceImpl alertService;
    
    @Autowired
    private ConverterService converterService;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    
    /**
     * Returns the balance of the customer's account in the given currency.
     *
     * @param accountIdentifier The account id.
     * @param selectedCurrency Can be either USD, EURO or SEK
     * @return the current balance for in the specified currency
     */
    @Path("/{accountIdentifier}/{selectedCurrency}/{email}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Balance getBalance(
            @PathParam("accountIdentifier") String accountIdentifier,
            @PathParam("selectedCurrency") String selectedCurrency,
            @PathParam("email") String email) {
        
        Integer businessCustomerId = null;
        Integer businessAccountNumber = null;
        Integer accountId = null;
        if (accountIdentifier.startsWith("BA")) {
            // business account, format BA1234567890-4
            String[] parts = accountIdentifier.split("-");
            String businessCustomerIdStr = parts[0].substring(2);
            businessCustomerId = Integer.parseInt(businessCustomerIdStr);
            String businessAccountNumberStr = parts[1];
            businessAccountNumber = Integer.parseInt(businessAccountNumberStr);
        } else {
            accountId = Integer.parseInt(accountIdentifier);
        }
        
        Currency currency;
        try {
            currency = Currency.valueOf(selectedCurrency);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
        
        Account account = null;
        if (accountId != null) {
            account = accountService.findAccountById(accountId);
        } else {
            List<Account> businessAccounts = accountService.findAccountsByBusinessCustomerId(businessCustomerId);
            final Integer finalBusinessAccountNumber = businessAccountNumber;
            account = businessAccounts.stream()
                                      .filter(a -> a.getAccountNumber().equals(finalBusinessAccountNumber)).findFirst().get();
        }
        if (email != null && email.length() > 0) {
            account.setContactInformation(email);
            accountService.save(account);
        }
        
        double currentBalanceInSek = account.getCurrentBalanceInSek();
        Double balance = null;
        
        switch (currency) {
            case USD:
                balance = converterService.convert(currentBalanceInSek, SEK, USD);
                break;
            case EURO:
                balance = converterService.convert(currentBalanceInSek, SEK, GBP);
            case GBP:
                throw new NotImplementedException("GBP format not implemented");
            case SEK:
                balance = currentBalanceInSek;
                break;
        }
        if ((currency.equals(USD) && balance > 10000) || (currency.equals(Currency.EURO) && balance > 8400)
                || (currency.equals(SEK) && balance > 86000)) {
            
            alertService.triggerAlert(accountId, "investment_opportunity");
        }
    /*
        if ((currency.equals(USD) && balance < 10) || (currency.equals(Currency.EURO) && balance < 84)
            || (currency.equals(SEK) && balance < 86)) {
          alertService.triggerAlert(accountId, "low_balance");
        }
    */
        
        Date lastTransaction = account.getLastTransaction();
        String dateString = DATE_FORMAT.format(lastTransaction);
        return new Balance(accountId, balance, account.getAccountHolder(), dateString);
    }
    
    public static class Balance {
        public final Integer accountId;
        public final double balance;
        public final String accountHolder;
        public final String lastTransaction;
        
        public Balance(Integer accountId, double balance, String accountHolder, String lastTransaction) {
            this.accountId = accountId;
            this.balance = balance;
            this.accountHolder = accountHolder;
            this.lastTransaction = lastTransaction;
        }
    }
    
    public static enum Currency {
        USD,
        GBP,
        EURO,
        SEK
    }
    
}
