package com.db.awmd.challenge.domain;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountUpdate {

    private final String accountId;
    private final BigDecimal amount;

}
