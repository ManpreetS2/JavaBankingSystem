package com.manpreet.bank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.manpreet.bank.AppContext;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.support.TestAppContext;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TransactionSearchUnicodeTest {

    @TempDir
    Path tempDir;

    private AppContext context;
    private UserSession ada;
    private UserSession grace;

    @BeforeEach
    void setUp() {
        context = TestAppContext.create(tempDir.resolve("search.db"));
        ada = context.getAuthService().register("Ada", "Lovelace", "ada@example.com", "ada", "correct-horse-battery");
        grace = context.getAuthService().register("Grace", "Hopper", "grace@example.com", "grace", "correct-horse-battery");
        for (String description : List.of("Café ÉCOLE lunch", "Zürich ÖV ticket", "ΑΘΗΝΑ trip", "PAYCHECK September",
                "100% match_test")) {
            deposit(ada, description);
        }
        deposit(grace, "École fees");
    }

    @Test
    void matchesUppercaseNonAsciiTextInAnyCase() {
        for (String query : List.of("ÉCOLE", "École", "école", "café", "CAFÉ")) {
            assertEquals(1, count(ada, query), "query \"" + query + "\"");
        }
        for (String query : List.of("ÖV", "öv", "ZÜRICH", "zürich")) {
            assertEquals(1, count(ada, query), "query \"" + query + "\"");
        }
        for (String query : List.of("ΑΘΗΝΑ", "αθηνα", "Αθηνα")) {
            assertEquals(1, count(ada, query), "query \"" + query + "\"");
        }
    }

    @Test
    void keepsAsciiCaseInsensitivityAndLiteralWildcards() {
        assertEquals(1, count(ada, "paycheck"));
        assertEquals(1, count(ada, "PAYCHECK"));
        assertEquals(1, count(ada, "100%"));
        assertEquals(1, count(ada, "_test"));
        assertEquals(0, count(ada, "1000%"));
    }

    @Test
    void searchStaysScopedToTheSignedInUser() {
        assertEquals(1, count(ada, "école"));
        assertEquals(1, count(grace, "école"));
        assertEquals(0, count(grace, "zürich"));
    }

    @Test
    void searchResultsAndCountsAgree() {
        List<?> rows = context.getTransactionService()
                .search(ada.userId(), new TransactionFilter(null, null, null, null, "École", 20, 0));

        assertEquals(1, rows.size());
        assertEquals(1, count(ada, "École"));
    }

    private long count(UserSession user, String query) {
        return context.getTransactionService()
                .count(user.userId(), new TransactionFilter(null, null, null, null, query, 20, 0));
    }

    private void deposit(UserSession user, String description) {
        Account checking = context.getAccountService().getAccountsForUser(user.userId()).stream()
                .filter(account -> account.getAccountType() == AccountType.CHECKING)
                .findFirst()
                .orElseThrow();
        context.getAccountService().deposit(user.userId(), checking.getId(), new BigDecimal("1.00"), description);
    }
}
