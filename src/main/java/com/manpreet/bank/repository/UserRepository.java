package com.manpreet.bank.repository;

import com.manpreet.bank.database.DatabaseManager;
import com.manpreet.bank.exception.BankingOperationException;
import com.manpreet.bank.exception.DuplicateUserException;
import com.manpreet.bank.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public class UserRepository {

    private final DatabaseManager databaseManager;

    public UserRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
    }

    public User create(User user) {
        try {
            return databaseManager.executeInTransaction(connection -> create(connection, user));
        } catch (DuplicateUserException e) {
            throw e;
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to create user", e);
        }
    }

    public User create(Connection connection, User user) throws SQLException {
        Objects.requireNonNull(connection, "connection must not be null");
        Objects.requireNonNull(user, "user must not be null");

        String sql = """
                INSERT INTO users (first_name, last_name, email, username, password_hash, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getFirstName());
            statement.setString(2, user.getLastName());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getUsername());
            statement.setString(5, user.getPasswordHash());
            statement.setString(6, formatTimestamp(user.getCreatedAt()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating user failed: no generated id");
                }
                user.setId(keys.getLong(1));
                return user;
            }
        } catch (SQLException e) {
            throwDuplicateIfUniqueViolation(e);
            throw e;
        }
    }

    public Optional<User> findById(long id) {
        try (Connection connection = databaseManager.getConnection()) {
            return findById(connection, id);
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to load user", e);
        }
    }

    public Optional<User> findById(Connection connection, long id) throws SQLException {
        String sql = """
                SELECT id, first_name, last_name, email, username, password_hash, created_at
                FROM users
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapUser(resultSet));
            }
        }
    }

    public Optional<User> findByUsername(String username) {
        try (Connection connection = databaseManager.getConnection()) {
            return findByUsername(connection, username);
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to load user by username", e);
        }
    }

    public Optional<User> findByUsername(Connection connection, String username) throws SQLException {
        String sql = """
                SELECT id, first_name, last_name, email, username, password_hash, created_at
                FROM users
                WHERE username = ? COLLATE NOCASE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapUser(resultSet));
            }
        }
    }

    public Optional<User> findByEmail(String email) {
        try (Connection connection = databaseManager.getConnection()) {
            return findByEmail(connection, email);
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to load user by email", e);
        }
    }

    public Optional<User> findByEmail(Connection connection, String email) throws SQLException {
        String sql = """
                SELECT id, first_name, last_name, email, username, password_hash, created_at
                FROM users
                WHERE email = ? COLLATE NOCASE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapUser(resultSet));
            }
        }
    }

    public Optional<User> findByUsernameOrEmail(String identifier) {
        try (Connection connection = databaseManager.getConnection()) {
            return findByUsernameOrEmail(connection, identifier);
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to load user by username or email", e);
        }
    }

    public Optional<User> findByUsernameOrEmail(Connection connection, String identifier) throws SQLException {
        Optional<User> byUsername = findByUsername(connection, identifier);
        if (byUsername.isPresent()) {
            return byUsername;
        }
        return findByEmail(connection, identifier);
    }

    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    public boolean existsByUsername(Connection connection, String username) throws SQLException {
        return findByUsername(connection, username).isPresent();
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    public boolean existsByEmail(Connection connection, String email) throws SQLException {
        return findByEmail(connection, email).isPresent();
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getLong("id"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("email"),
                resultSet.getString("username"),
                resultSet.getString("password_hash"),
                LocalDateTime.parse(resultSet.getString("created_at"))
        );
    }

    private static String formatTimestamp(LocalDateTime value) {
        return Objects.requireNonNullElseGet(value, LocalDateTime::now).toString();
    }

    private static void throwDuplicateIfUniqueViolation(SQLException e) {
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (!(message.contains("unique") || message.contains("constraint"))) {
            return;
        }
        if (message.contains("email")) {
            throw new DuplicateUserException("An account with this email already exists");
        }
        if (message.contains("username")) {
            throw new DuplicateUserException("An account with this username already exists");
        }
        throw new DuplicateUserException("An account with these details already exists");
    }
}
