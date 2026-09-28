package com.manpreet.bank;

import com.manpreet.bank.database.DatabaseInitializer;
import com.manpreet.bank.database.DatabaseManager;
import com.manpreet.bank.repository.AccountRepository;
import com.manpreet.bank.repository.TransactionRepository;
import com.manpreet.bank.repository.UserRepository;
import com.manpreet.bank.service.AccountService;
import com.manpreet.bank.service.AuthService;
import com.manpreet.bank.service.TransactionService;
import com.manpreet.bank.session.SessionManager;
import com.manpreet.bank.util.AccountNumberGenerator;
import com.manpreet.bank.util.PasswordHasher;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Composition root that wires application dependencies.
 */
public class AppContext {

    private final DatabaseManager databaseManager;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordHasher passwordHasher;
    private final AccountNumberGenerator accountNumberGenerator;
    private final AuthService authService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final SessionManager sessionManager;

    public AppContext() {
        this(new DatabaseManager());
    }

    public AppContext(Path databasePath) {
        this(new DatabaseManager(databasePath));
    }

    public AppContext(DatabaseManager databaseManager) {
        this(databaseManager, new PasswordHasher());
    }

    public AppContext(DatabaseManager databaseManager, PasswordHasher passwordHasher) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.accountNumberGenerator = new AccountNumberGenerator();
        this.userRepository = new UserRepository(databaseManager);
        this.accountRepository = new AccountRepository(databaseManager);
        this.transactionRepository = new TransactionRepository(databaseManager);
        this.authService = new AuthService(
                databaseManager,
                userRepository,
                accountRepository,
                passwordHasher,
                accountNumberGenerator
        );
        this.accountService = new AccountService(databaseManager, accountRepository, transactionRepository);
        this.transactionService = new TransactionService(accountRepository, transactionRepository);
        this.sessionManager = new SessionManager();
    }

    public void initializeDatabase() {
        new DatabaseInitializer(databaseManager).initialize();
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public AccountRepository getAccountRepository() {
        return accountRepository;
    }

    public TransactionRepository getTransactionRepository() {
        return transactionRepository;
    }

    public PasswordHasher getPasswordHasher() {
        return passwordHasher;
    }

    public AccountNumberGenerator getAccountNumberGenerator() {
        return accountNumberGenerator;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public AccountService getAccountService() {
        return accountService;
    }

    public TransactionService getTransactionService() {
        return transactionService;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
}
