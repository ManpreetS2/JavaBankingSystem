package com.manpreet.bank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manpreet.bank.AppContext;
import com.manpreet.bank.database.DatabaseManager;
import com.manpreet.bank.exception.AuthenticationException;
import com.manpreet.bank.exception.BankingOperationException;
import com.manpreet.bank.exception.DuplicateUserException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.repository.AccountRepository;
import com.manpreet.bank.repository.UserRepository;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.support.TestAppContext;
import com.manpreet.bank.util.AccountNumberGenerator;
import com.manpreet.bank.util.MoneyUtil;
import com.manpreet.bank.util.PasswordHasher;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuthServiceTest {

    private static final String PASSWORD = "StrongTestPass1";

    @TempDir
    Path tempDir;

    private AppContext context;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        context = TestAppContext.create(tempDir.resolve("auth.db"));
        authService = context.getAuthService();
    }

    @Test
    void registerCreatesUserAndZeroBalanceAccounts() {
        UserSession session = authService.register(
                "Test", "Banker", "test@example.com", "testbanker", PASSWORD
        );

        assertEquals("testbanker", session.username());
        assertEquals("test@example.com", session.email());

        List<Account> accounts = context.getAccountRepository().findByUserId(session.userId());
        assertEquals(2, accounts.size());
        assertEquals(AccountType.CHECKING, accounts.get(0).getAccountType());
        assertEquals(AccountType.SAVINGS, accounts.get(1).getAccountType());
        assertEquals(0, MoneyUtil.ZERO.compareTo(accounts.get(0).getBalance()));
        assertEquals(0, MoneyUtil.ZERO.compareTo(accounts.get(1).getBalance()));

        String storedHash = context.getUserRepository().findById(session.userId()).orElseThrow().getPasswordHash();
        assertTrue(storedHash.startsWith("pbkdf2-sha256$"));
        assertFalse(storedHash.contains(PASSWORD));
    }

    @Test
    void loginWorksWithUsernameOrEmailCaseInsensitively() {
        authService.register("Test", "Banker", "login@example.com", "loginuser", PASSWORD);

        assertEquals("loginuser", authService.authenticate("LOGINUSER", PASSWORD).username());
        assertEquals("loginuser", authService.authenticate("Login@Example.com", PASSWORD).username());
    }

    @Test
    void wrongPasswordAndDuplicatesAreRejected() {
        authService.register("Test", "Banker", "dup@example.com", "dupuser", PASSWORD);

        assertThrows(AuthenticationException.class,
                () -> authService.authenticate("dupuser", "WrongPassword!!"));
        assertThrows(AuthenticationException.class,
                () -> authService.authenticate("missing-user", PASSWORD));
        assertThrows(DuplicateUserException.class,
                () -> authService.register("Test", "Banker", "other@example.com", "dupuser", PASSWORD));
        assertThrows(DuplicateUserException.class,
                () -> authService.register("Test", "Banker", "dup@example.com", "otheruser", PASSWORD));
        assertThrows(DuplicateUserException.class,
                () -> authService.register("Test", "Banker", "DUP@example.com", "anotheruser", PASSWORD));
    }

    @Test
    void registrationRollsBackWhenAccountCreationFails() {
        DatabaseManager databaseManager = new DatabaseManager(tempDir.resolve("rollback.db"));
        new com.manpreet.bank.database.DatabaseInitializer(databaseManager).initialize();

        AccountNumberGenerator collidingGenerator = new AccountNumberGenerator() {
            @Override
            public String generate(AccountType accountType) {
                return "ACC-1111111111";
            }
        };

        AuthService failingAuth = new AuthService(
                databaseManager,
                new UserRepository(databaseManager),
                new AccountRepository(databaseManager),
                new PasswordHasher(TestAppContext.TEST_PBKDF2_ITERATIONS),
                collidingGenerator
        );

        assertThrows(BankingOperationException.class,
                () -> failingAuth.register("Test", "Banker", "roll@example.com", "rolluser", PASSWORD));

        assertTrue(new UserRepository(databaseManager).findByUsername("rolluser").isEmpty());
        assertEquals(0, new AccountRepository(databaseManager).findByUserId(1L).size());
    }
}
