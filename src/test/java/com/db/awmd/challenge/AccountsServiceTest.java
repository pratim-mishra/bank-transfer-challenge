package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.NotEnoughFundsException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;
  
  @Autowired
  private NotificationService notificationService;

  @Test
  public void addAccount() throws Exception {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  public void addAccount_failsOnDuplicateId() throws Exception {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }

  }
  
  /*
   * Transfer between accounts fail when Account Does not exists
   */
  @Test
  public void makeTransferFailAccountDoesNotExist() {
      final String accountFromId = UUID.randomUUID().toString();
      final String accountToId = UUID.randomUUID().toString();
      this.accountsService.createAccount(new Account(accountFromId));
      Transfer transfer = new Transfer(accountFromId, accountToId, new BigDecimal(1200));
      try {
          this.accountsService.makeTransfer(transfer);
          fail("Transfer failed because Account does not exist");
      } catch (AccountNotFoundException anfe) {
          assertThat(anfe.getMessage()).isEqualTo("Account " + accountToId + " not found.");
      }
      verifyZeroInteractions(notificationService);
  }

  /*
   * Exception while transfer of amount due to insufficient balance
   */
  @Test
  public void makeTransferExceptionWhenNotEnoughFunds() {
      final String accountFromId = UUID.randomUUID().toString();
      final String accountToId = UUID.randomUUID().toString();
      this.accountsService.createAccount(new Account(accountFromId));
      this.accountsService.createAccount(new Account(accountToId));
      Transfer transfer = new Transfer(accountFromId, accountToId, new BigDecimal(1100));
      try {
          this.accountsService.makeTransfer(transfer);
          fail("Transfer failed because of insuffecient balance");
      } catch (NotEnoughFundsException nbe) {
          assertThat(nbe.getMessage()).isEqualTo("Not enough funds on account " + accountFromId + " balance=0");
      }
      verifyZeroInteractions(notificationService);
  }

  /*
   * transfer of amount from Account1 to Account2
   */
  @Test
  public void makeTransferTest() {
      final String accountFromId = UUID.randomUUID().toString();
      final String accountToId = UUID.randomUUID().toString();
      final Account accountFrom = new Account(accountFromId, new BigDecimal("1300.74"));
      final Account accountTo = new Account(accountToId, new BigDecimal("500.00"));

      this.accountsService.createAccount(accountFrom);
      this.accountsService.createAccount(accountTo);

      Transfer transfer = new Transfer(accountFromId, accountToId, new BigDecimal("200.74"));

      this.accountsService.makeTransfer(transfer);

      assertThat(this.accountsService.getAccount(accountFromId).getBalance()).isEqualTo(new BigDecimal("1100.00"));
      assertThat(this.accountsService.getAccount(accountToId).getBalance()).isEqualTo(new BigDecimal("700.74"));

      verifyNotifications(accountFrom, accountTo, transfer);
  }

  /*
   * transfer of amount from Account1 to Account2 on boundary condition where ammount left is 0.0 after transfer
   */
  @Test
  public void makeTransfer_should_transferFunds_when_balanceJustEnough() {

      final String accountFromId = UUID.randomUUID().toString();
      final String accountToId = UUID.randomUUID().toString();
      final Account accountFrom = new Account(accountFromId, new BigDecimal("1250.50"));
      final Account accountTo = new Account(accountToId, new BigDecimal("100.00"));

      this.accountsService.createAccount(accountFrom);
      this.accountsService.createAccount(accountTo);

      Transfer transfer = new Transfer(accountFromId, accountToId, new BigDecimal("1250.50"));

      this.accountsService.makeTransfer(transfer);

      assertThat(this.accountsService.getAccount(accountFromId).getBalance()).isEqualTo(new BigDecimal("0.00"));
      assertThat(this.accountsService.getAccount(accountToId).getBalance()).isEqualTo(new BigDecimal("1350.50"));
      verifyNotifications(accountFrom, accountTo, transfer);
  }

  private void verifyNotifications(final Account accountFrom, final Account accountTo, final Transfer transfer) {
      verify(notificationService, Mockito.times(1)).notifyAboutTransfer(accountFrom, "The transfer to the account with ID " + accountTo.getAccountId() + " is now complete for the amount of " + transfer.getAmountToTransfer() + ".");
      verify(notificationService, Mockito.times(1)).notifyAboutTransfer(accountTo, "The account with ID + " + accountFrom.getAccountId() + " has transferred " + transfer.getAmountToTransfer() + " into your account.");
  }
}
