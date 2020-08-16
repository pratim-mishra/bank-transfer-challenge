package com.db.awmd.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.NotEnoughFundsException;
import com.db.awmd.challenge.exception.TransferBetweenSameAccountException;
import com.db.awmd.challenge.service.ValidatorImpl;


public class TransferValidatorImplTest {

    private Validator transferValidator;

    @Before
    public void setUp() {
        transferValidator = new ValidatorImpl();
    }

    @Test
    public void validate_should_throwException_when_accountFromNotFound() throws Exception {
        final Account accountTo = new Account("Id-2");
        final Transfer transfer = new Transfer("Id-1", accountTo.getAccountId(), new BigDecimal("2.00"));

        try {
            transferValidator.validate(null, new Account("Id-2"), transfer);
            fail("Account with Id-1 should not be found");
        } catch (AccountNotFoundException ace) {
            assertThat(ace.getMessage()).isEqualTo("Account Id-1 not found.");
        }
    }

    @Test
    public void validate_should_throwException_when_accountToNotFound() throws Exception {
        final Account accountFrom = new Account("Id-1");
        final Transfer transfer = new Transfer("Id-1", "Id-5342", new BigDecimal("2.00"));

        try {
            transferValidator.validate(accountFrom, null, transfer);
            fail("Account with Id-5342 should not be found");
        } catch (AccountNotFoundException ace) {
            assertThat(ace.getMessage()).isEqualTo("Account Id-5342 not found.");
        }
    }

    @Test
    public void validate_should_throwException_when_NotEnoughFunds() throws Exception {
        final Account accountFrom = new Account("Id-1");
        final Account accountTo = new Account("Id-2");
        final Transfer transfer = new Transfer("Id-1", "Id-2", new BigDecimal("2.00"));

        try {
            transferValidator.validate(accountFrom, accountTo, transfer);
            fail("Not enough funds");
        } catch (NotEnoughFundsException nbe) {
            assertThat(nbe.getMessage()).isEqualTo("Not enough funds on account Id-1 balance=0");
        }
    }

    @Test
    public void validate_should_throwException_when_transferBetweenSameAccount() throws Exception {
        final Account accountFrom = new Account("Id-1", new BigDecimal("20.00"));
        final Account accountTo = new Account("Id-1");
        final Transfer transfer = new Transfer("Id-1", "Id-1", new BigDecimal("2.00"));

        try {
            transferValidator.validate(accountFrom, accountTo, transfer);
            fail("Same account transfer");
        } catch (TransferBetweenSameAccountException sae) {
            assertThat(sae.getMessage()).isEqualTo("Transfer to self not permitted.");
        }
    }

    @Test
    public void validate_should_allowValidTransferBetweenAccounts() throws Exception {
        final Account accountFrom = new Account("Id-1", new BigDecimal("20.00"));
        final Account accountTo = new Account("Id-2");
        final Transfer transfer = new Transfer("Id-1", "Id-2", new BigDecimal("2.00"));

        transferValidator.validate(accountFrom, accountTo, transfer);
    }

}