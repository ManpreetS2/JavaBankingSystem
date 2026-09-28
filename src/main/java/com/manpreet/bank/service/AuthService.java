package com.manpreet.bank.service;

import com.manpreet.bank.database.DatabaseManager;
import com.manpreet.bank.exception.AuthenticationException;
import com.manpreet.bank.exception.BankingOperationException;
import com.manpreet.bank.exception.DuplicateUserException;
import com.manpreet.bank.exception.ValidationException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.model.User;
import com.manpreet.bank.repository.AccountRepository;
import com.manpreet.bank.repository.UserRepository;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.util.AccountNumberGenerator;
import com.manpreet.bank.util.InputValidator;
import com.manpreet.bank.util.MoneyUtil;
import com.manpreet.bank.util.PasswordHasher;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class AuthService {

    private final DatabaseManager databaseManager;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordHasher passwordHasher;
    private final AccountNumberGenerator accountNumberGenerator;

    public AuthService(DatabaseManager databaseManager,
                       UserRepository userRepository,
                       AccountRepository accountRepository,
                       PasswordHasher passwordHasher,
                       AccountNumberGenerator accountNumberGenerator) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.accountNumberGenerator = Objects.requireNonNull(accountNumberGenerator);
    }

    public UserSession register(String firstName,
                                String lastName,
                                String email,
                                String username,
                                String password) {
        String normalizedFirstName = InputValidator.normalizeName(firstName, "First name");
        String normalizedLastName = InputValidator.normalizeName(lastName, "Last name");
        String normalizedEmail = InputValidator.normalizeEmail(email);
        String normalizedUsername = InputValidator.normalizeUsername(username);
        String validatedPassword = InputValidator.validatePassword(password);

        try {
            return databaseManager.executeInTransaction(connection -> {
                if (userRepository.existsByUsername(connection, normalizedUsername)) {
                    throw new DuplicateUserException("An account with this username already exists");
                }
                if (userRepository.existsByEmail(connection, normalizedEmail)) {
                    throw new DuplicateUserException("An account with this email already exists");
                }

                String passwordHash = passwordHasher.hash(validatedPassword);
                LocalDateTime createdAt = LocalDateTime.now();

                User user = new User(
                        0L,
                        normalizedFirstName,
                        normalizedLastName,
                        normalizedEmail,
                        normalizedUsername,
                        passwordHash,
                        createdAt
                );
                userRepository.create(connection, user);

                createAccount(connection, user.getId(), AccountType.CHECKING, createdAt);
                createAccount(connection, user.getId(), AccountType.SAVINGS, createdAt);

                return toSession(user);
            });
        } catch (DuplicateUserException | ValidationException e) {
            throw e;
        } catch (SQLException e) {
            throw new BankingOperationException("Registration failed. Please try again.", e);
        }
    }

    public UserSession authenticate(String usernameOrEmail, String password) {
        String identifier = InputValidator.normalizeLoginIdentifier(usernameOrEmail);
        if (password == null) {
            throw new AuthenticationException("Invalid username/email or password");
        }

        User user = userRepository.findByUsernameOrEmail(identifier)
                .orElseThrow(() -> new AuthenticationException("Invalid username/email or password"));

        if (!passwordHasher.verify(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username/email or password");
        }

        return toSession(user);
    }

    public List<Account> getAccountsForRegisteredUser(long userId) {
        return accountRepository.findByUserId(userId);
    }

    private void createAccount(java.sql.Connection connection,
                               long userId,
                               AccountType accountType,
                               LocalDateTime createdAt) throws SQLException {
        SQLException lastFailure = null;
        for (int attempt = 1; attempt <= accountNumberGenerator.maxAttempts(); attempt++) {
            String accountNumber = accountNumberGenerator.generate(accountType);
            Account account = new Account(
                    0L,
                    userId,
                    accountNumber,
                    accountType,
                    MoneyUtil.ZERO,
                    createdAt
            );
            try {
                accountRepository.create(connection, account);
                return;
            } catch (SQLException e) {
                lastFailure = e;
                String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                if (!(message.contains("unique") && message.contains("account_number"))) {
                    throw e;
                }
            }
        }
        throw new BankingOperationException(
                "Unable to generate a unique account number",
                lastFailure
        );
    }

    private static UserSession toSession(User user) {
        return new UserSession(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
        );
    }
}
