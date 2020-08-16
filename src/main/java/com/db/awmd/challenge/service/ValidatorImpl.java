package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.NotEnoughFundsException;
import com.db.awmd.challenge.exception.TransferBetweenSameAccountException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ValidatorImpl implements Validator {

    /**
     * Validation for account existance, overdraft and same account transfer
     *
     * @param currAccountFrom source account
     * @param currAccountTo destination account
     * @param transfer The transfer object as requested
     * @throws AccountNotFoundException
     * @throws NotEnoughFundsException
     * @throws TransferBetweenSameAccountException
     */
    public void validate(final Account currAccountFrom, final Account currAccountTo, final Transfer transfer)
            throws AccountNotFoundException, NotEnoughFundsException, TransferBetweenSameAccountException{

        if (currAccountFrom == null){
            throw new AccountNotFoundException("Account " + transfer.getAccountFromId() + " not found.");
        }

        if (currAccountTo == null) {
            throw new AccountNotFoundException("Account " + transfer.getAccountToId() + " not found.");
        }

        if (sameAccount(transfer)){
            throw new TransferBetweenSameAccountException("Transfer to self not permitted.");
        }

        if (!enoughFunds(currAccountFrom, transfer.getAmountToTransfer())){
            throw new NotEnoughFundsException("Not enough funds on account " + currAccountFrom.getAccountId() + " balance="+currAccountFrom.getBalance());
        }
    }

    private boolean sameAccount(final Transfer transfer) {
        return transfer.getAccountFromId().equals(transfer.getAccountToId());
    }


    private boolean enoughFunds(final Account account, final BigDecimal amount) {
        return account.getBalance().subtract(amount).compareTo(BigDecimal.ZERO) >= 0;
    }

}
