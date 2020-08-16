package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Before
  public void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  public void createAccount() throws Exception {
    createAccountWithContent("{\"accountId\":\"Id-123\",\"balance\":1000}").andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  public void createDuplicateAccount() throws Exception {
    createAccountWithContent("{\"accountId\":\"Id-123\",\"balance\":1000}").andExpect(status().isCreated());

    createAccountWithContent("{\"accountId\":\"Id-123\",\"balance\":1000}").andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoAccountId() throws Exception {
    createAccountWithContent("{\"balance\":1000}")
            .andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBalance() throws Exception {
    createAccountWithContent("{\"accountId\":\"Id-123\"}").andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNegativeBalance() throws Exception {
    createAccountWithContent("{\"accountId\":\"Id-123\",\"balance\":-1000}")
            .andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountEmptyAccountId() throws Exception {
    createAccountWithContent("{\"accountId\":\"\",\"balance\":1000}").andExpect(status().isBadRequest());
  }

  private ResultActions createAccountWithContent(final String content) throws Exception {
    return this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content(content));
  }

  @Test
  public void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    verifyAccountBalance(uniqueAccountId, new BigDecimal("123.45"));
  }

  private void verifyAccountBalance(final String accountId, final BigDecimal balance) throws Exception {
    this.mockMvc.perform(get("/v1/accounts/" + accountId))
            .andExpect(status().isOk())
            .andExpect(
                    content().string("{\"accountId\":\"" + accountId + "\",\"balance\":"+balance+"}"));
  }

  @Test
  public void makeTransferAccountFromNotFound() throws Exception {
    createAccountWithContent("{\"accountId\":\"Id-2\",\"balance\":1000}").andExpect(status().isCreated());

    tranferURIBuilderWithContents("{\"accountFromId\":\"Id-1\",\"accountToId\":\"Id-2\",\"amountToTransfer\":1000}")
            .andExpect(status().isNotFound());
  }

  @Test
  public void makeTransferAccountToNotFound() throws Exception {
    createAccountWithContent("{\"accountId\":\"Id-1\",\"balance\":1000.20}").andExpect(status().isCreated());

    tranferURIBuilderWithContents("{\"accountFromId\":\"Id-1\",\"accountToId\":\"Id-2\",\"amountToTransfer\":1000}")
            .andExpect(status().isNotFound());
  }

  private ResultActions tranferURIBuilderWithContents(String content) throws Exception {
    return this.mockMvc.perform(
            put("/v1/accounts/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(content));
  }

  @Test
  public void makeTransferSameAccount() throws Exception {
    createAccountWithContent("{\"accountId\":\"Id-1\",\"balance\":1000}").andExpect(status().isCreated());
    tranferURIBuilderWithContents("{\"accountFromId\":\"Id-1\",\"accountToId\":\"Id-1\",\"amountToTransfer\":1000}")
            .andExpect(status().isBadRequest());
  }

  @Test
  public void makeTransferOverdraft() throws Exception {
    createAccountWithContent("{\"accountId\":\"Id-1\",\"balance\":20.50}").andExpect(status().isCreated());
    createAccountWithContent("{\"accountId\":\"Id-2\",\"balance\":1000}").andExpect(status().isCreated());

    tranferURIBuilderWithContents("{\"accountFromId\":\"Id-1\",\"accountToId\":\"Id-2\",\"amountToTransfer\":21}")
            .andExpect(status().isUnprocessableEntity());
  }

  @Test
  public void makeTransferNegativeAmount() throws Exception {
    createAccountWithContent("{\"accountId\":\"Id-1\",\"balance\":100.50}").andExpect(status().isCreated());
    createAccountWithContent("{\"accountId\":\"Id-2\",\"balance\":1000.50}").andExpect(status().isCreated());

    tranferURIBuilderWithContents("{\"accountFromId\":\"Id-1\",\"accountToId\":\"Id-2\",\"amountToTransfer\":-50}")
            .andExpect(status().isBadRequest());
  }

  @Test
  public void makeTransferEmptyBody() throws Exception {
    tranferURIBuilderWithContents("{}")
            .andExpect(status().isBadRequest());
  }

  @Test
  public void makeTransferNoBody() throws Exception {
    this.mockMvc.perform(put("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void makeTransferZeroBalanceAfterTransfer() throws Exception {
    createAccountWithContent("{\"accountId\":\"Id-1\",\"balance\":2000.20}").andExpect(status().isCreated());
    createAccountWithContent("{\"accountId\":\"Id-2\",\"balance\":100}").andExpect(status().isCreated());

    tranferURIBuilderWithContents("{\"accountFromId\":\"Id-1\",\"accountToId\":\"Id-2\",\"amountToTransfer\":2000.20}")
            .andExpect(status().isOk());

    verifyAccountBalance("Id-1", new BigDecimal("0.00"));
    verifyAccountBalance("Id-2", new BigDecimal("2100.20"));
  }

  @Test
  public void makeTransferBetweenAccountsPositiveBalanceAfterTransfer() throws Exception {
    createAccountWithContent("{\"accountId\":\"Id-1\",\"balance\":5100.21}").andExpect(status().isCreated());
    createAccountWithContent("{\"accountId\":\"Id-2\",\"balance\":6000}").andExpect(status().isCreated());

    tranferURIBuilderWithContents("{\"accountFromId\":\"Id-1\",\"accountToId\":\"Id-2\",\"amountToTransfer\":5000}")
            .andExpect(status().isOk());

    verifyAccountBalance("Id-1", new BigDecimal("100.21"));
    verifyAccountBalance("Id-2", new BigDecimal("11000"));
  }










}
