package com.manpreet.bank;

import com.manpreet.bank.database.DatabaseInitializer;
import com.manpreet.bank.database.DatabaseManager;
import com.manpreet.bank.repository.AccountRepository;
import com.manpreet.bank.repository.TransactionRepository;
import com.manpreet.bank.repository.UserRepository;
import com.manpreet.bank.service.AccountService;
import com.manpreet.bank.service.AuthService;
import com.manpreet.bank.service.DemoDataSeeder;
import com.manpreet.bank.service.TransactionExportService;
import com.manpreet.bank.service.TransactionService;
import com.manpreet.bank.session.SessionManager;
import com.manpreet.bank.ui.ThemeManager;
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
    private final TransactionExportService transactionExportService;
    private final DemoDataSeeder demoDataSeeder;
    private final SessionManager sessionManager;
    private final ThemeManager themeManager;

    public AppContext() {
        this(ApplicationPaths.resolveDatabasePath());
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
        this.transactionExportService = new TransactionExportService(transactionService, accountRepository);
        this.demoDataSeeder = new DemoDataSeeder(authService, accountService);
        this.sessionManager = new SessionManager();
        this.themeManager = new ThemeManager();
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

    public TransactionExportService getTransactionExportService() {
        return transactionExportService;
    }

    public DemoDataSeeder getDemoDataSeeder() {
        return demoDataSeeder;
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }
}
