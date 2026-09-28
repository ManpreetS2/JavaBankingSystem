package com.manpreet.bank.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manpreet.bank.AppContext;
import com.manpreet.bank.exception.DuplicateUserException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.model.TransactionType;
import com.manpreet.bank.model.User;
import com.manpreet.bank.support.TestAppContext;
import com.manpreet.bank.util.MoneyUtil;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositoryIntegrationTest {

    @TempDir
    Path tempDir;

    private AppContext context;
    private UserRepository userRepository;
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        context = TestAppContext.create(tempDir.resolve("repo.db"));
        userRepository = context.getUserRepository();
        accountRepository = context.getAccountRepository();
        transactionRepository = context.getTransactionRepository();
    }

    @Test
    void userRepositorySupportsCreateLookupAndCaseInsensitiveQueries() {
        User created = userRepository.create(sampleUser("alice", "alice@example.com"));
        assertTrue(created.getId() > 0);

        assertEquals(created.getId(), userRepository.findById(created.getId()).orElseThrow().getId());
        assertEquals(created.getId(), userRepository.findByUsername("ALICE").orElseThrow().getId());
        assertEquals(created.getId(), userRepository.findByEmail("Alice@Example.com").orElseThrow().getId());
        assertEquals(Optional.empty(), userRepository.findByUsername("missing"));

        assertThrows(DuplicateUserException.class,
                () -> userRepository.create(sampleUser("alice", "other@example.com")));
        assertThrows(DuplicateUserException.class,
                () -> userRepository.create(sampleUser("alice2", "alice@example.com")));
    }

    @Test
    void accountRepositoryPersistsExactMoneyAndOrdersCheckingFirst() {
        User user = userRepository.create(sampleUser("bob", "bob@example.com"));

        Account savings = accountRepository.create(new Account(
                0L, user.getId(), "SAV-1000000001", AccountType.SAVINGS,
                new BigDecimal("1234567890.12"), LocalDateTime.now()
        ));
        Account checking = accountRepository.create(new Account(
                0L, user.getId(), "CHK-1000000001", AccountType.CHECKING,
                MoneyUtil.ZERO, LocalDateTime.now()
        ));

        assertTrue(savings.getId() > 0);
        assertEquals(0, new BigDecimal("1234567890.12").compareTo(
                accountRepository.findById(savings.getId()).orElseThrow().getBalance()));

        List<Account> accounts = accountRepository.findByUserId(user.getId());
        assertEquals(2, accounts.size());
        assertEquals(AccountType.CHECKING, accounts.get(0).getAccountType());
        assertEquals(AccountType.SAVINGS, accounts.get(1).getAccountType());

        Account updated = accountRepository.updateBalance(checking.getId(), new BigDecimal("50.25"));
        assertEquals(0, new BigDecimal("50.25").compareTo(updated.getBalance()));

        assertThrows(Exception.class, () -> accountRepository.create(new Account(
                0L, user.getId(), "CHK-9999999999", AccountType.CHECKING,
                MoneyUtil.ZERO, LocalDateTime.now()
        )));
        assertThrows(Exception.class, () -> accountRepository.create(new Account(
                0L, user.getId(), "CHK-1000000001", AccountType.SAVINGS,
                MoneyUtil.ZERO, LocalDateTime.now()
        )));
    }

    @Test
    void transactionRepositoryOrdersNewestFirstAndScopesUserHistory() {
        User owner = userRepository.create(sampleUser("cara", "cara@example.com"));
        User other = userRepository.create(sampleUser("dan", "dan@example.com"));

        Account ownerAccount = accountRepository.create(new Account(
                0L, owner.getId(), "CHK-2000000001", AccountType.CHECKING,
                new BigDecimal("100.00"), LocalDateTime.now()
        ));
        Account otherAccount = accountRepository.create(new Account(
                0L, other.getId(), "CHK-2000000002", AccountType.CHECKING,
                new BigDecimal("100.00"), LocalDateTime.now()
        ));

        Transaction older = transactionRepository.create(new Transaction(
                0L, ownerAccount.getId(), null, TransactionType.DEPOSIT,
                new BigDecimal("10.00"), "Older", LocalDateTime.of(2026, 1, 1, 10, 0)
        ));
        Transaction newer = transactionRepository.create(new Transaction(
                0L, ownerAccount.getId(), otherAccount.getId(), TransactionType.TRANSFER_OUT,
                new BigDecimal("5.00"), "Newer", LocalDateTime.of(2026, 2, 1, 10, 0)
        ));
        transactionRepository.create(new Transaction(
                0L, otherAccount.getId(), null, TransactionType.DEPOSIT,
                new BigDecimal("20.00"), "Other user", LocalDateTime.of(2026, 3, 1, 10, 0)
        ));

        List<Transaction> byAccount = transactionRepository.findByAccountId(ownerAccount.getId());
        assertEquals(newer.getId(), byAccount.get(0).getId());
        assertEquals(older.getId(), byAccount.get(1).getId());
        assertEquals(otherAccount.getId(), byAccount.get(0).getRelatedAccountId());

        List<Transaction> recent = transactionRepository.findRecentByUserId(owner.getId(), 10);
        assertEquals(2, recent.size());
        assertTrue(recent.stream().noneMatch(tx -> tx.getAccountId() == otherAccount.getId()));
    }

    private static User sampleUser(String username, String email) {
        return new User(
                0L, "Test", "User", email, username,
                "pbkdf2-sha256$1$salt$hash", LocalDateTime.now()
        );
    }
}
