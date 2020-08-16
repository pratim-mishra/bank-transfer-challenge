package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.AccountUpdate;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

  private final Map<String, Account> accounts = new ConcurrentHashMap<>();

  @Override
  public void createAccount(Account account) throws DuplicateAccountIdException {
    Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
    if (previousAccount != null) {
      throw new DuplicateAccountIdException(
        "Account id " + account.getAccountId() + " already exists!");
    }
  }

  @Override
  public Account getAccount(String accountId) {
    return accounts.get(accountId);
  }

  @Override
  public void clearAccounts() {
    accounts.clear();
  }
  
  @Override
  public boolean updateAccountsBatch(List<AccountUpdate> accountUpdates) {
      accountUpdates
              .stream()
              .forEach(this::updateAccount);

      return true;
  }

  private void updateAccount(final AccountUpdate accountUpdate) {
      final String accountId = accountUpdate.getAccountId();
      accounts.computeIfPresent(accountId, (key, account) -> {
          account.setBalance(account.getBalance().add(accountUpdate.getAmount()));
          return account;
      });
  }

}
