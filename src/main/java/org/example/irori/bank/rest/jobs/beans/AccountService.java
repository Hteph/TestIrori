package org.example.irori.bank.rest.jobs.beans;

import org.example.irori.bank.rest.jobs.Account;

import java.util.Collections;
import java.util.List;

public class AccountService {
    public Account findAccountById(Integer accountId) {return new Account();}
    
    public List<Account> findAccountsByBusinessCustomerId(Integer businessCustomerId) {
        return Collections.emptyList();
    }
    
    public void save(Account account) {
    
    }
}
