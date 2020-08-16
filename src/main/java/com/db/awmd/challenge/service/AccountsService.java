package com.db.awmd.challenge.service;

import java.math.BigDecimal;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.AccountUpdate;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.NotEnoughFundsException;
import com.db.awmd.challenge.exception.TransferBetweenSameAccountException;
import com.db.awmd.challenge.repository.AccountsRepository;

import lombok.Getter;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;
  
  @Getter
  private final NotificationService notificationService;

  @Autowired
  private Validator transferValidator;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }
  
  /**
   * This method is used to Transfer some positive amount between accounts
   * @param transferAmount
   * @throws AccountNotFoundException When an account does not exist
   * @throws NotEnoughFundsException When there are not enough funds to complete the transfer
   * @throws TransferBetweenSameAccountException Transfer to self account is not permitted
   */
  public void makeTransfer(Transfer transferAmount) throws AccountNotFoundException, NotEnoughFundsException, TransferBetweenSameAccountException {

      final Account accountFrom = accountsRepository.getAccount(transferAmount.getAccountFromId());
      final Account accountTo = accountsRepository.getAccount(transferAmount.getAccountToId());
      final BigDecimal amount = transferAmount.getAmountToTransfer();

      transferValidator.validate(accountFrom, accountTo, transferAmount);

      //ideally atomic operation in production
      boolean successful = accountsRepository.updateAccountsBatch(Arrays.asList(
              new AccountUpdate(accountFrom.getAccountId(), amount.negate()),
              new AccountUpdate(accountTo.getAccountId(), amount)
              ));

      if (successful){
          notificationService.notifyAboutTransfer(accountFrom, "The transfer to the account with ID " + accountTo.getAccountId() + " is now complete for the amount of " + transferAmount.getAmountToTransfer() + ".");
          notificationService.notifyAboutTransfer(accountTo, "The account with ID + " + accountFrom.getAccountId() + " has transferred " + transferAmount.getAmountToTransfer() + " into your account.");
      }
  }
}
