package com.manpreet.bank.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manpreet.bank.AppContext;
import com.manpreet.bank.exception.ValidationException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.model.TransactionType;
import com.manpreet.bank.session.UserSession;
import com.manpreet.bank.support.TestAppContext;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TransactionExportLimitTest {

    private static final String PASSWORD = "StrongTestPass1";
    private static final int OWNER_MATCHES = 135;
    private static final int OTHER_MATCHES = 40;
    private static final String HEADER = "Date,Account,Type,Description,Amount";

    @TempDir
    Path tempDir;

    private AppContext context;
    private UserSession owner;
    private Account ownerChecking;
    private Account ownerSavings;
    private Account otherChecking;

    @BeforeEach
    void setUp() {
        context = TestAppContext.create(tempDir.resolve("export-limit.db"));
        owner = context.getAuthService().register("Export", "Owner", "exportowner@example.com", "exportowner", PASSWORD);
        UserSession other = context.getAuthService()
                .register("Export", "Other", "exportother@example.com", "exportother", PASSWORD);
        ownerChecking = find(owner.userId(), AccountType.CHECKING);
        ownerSavings = find(owner.userId(), AccountType.SAVINGS);
        otherChecking = find(other.userId(), AccountType.CHECKING);

        for (int i = 0; i < OWNER_MATCHES; i++) {
            context.getAccountService().deposit(
                    owner.userId(), ownerChecking.getId(), new BigDecimal("1.00"), "Bulk row " + i
            );
        }
        // Non-matching rows for the owner (different account / type).
        context.getAccountService().deposit(owner.userId(), ownerSavings.getId(), new BigDecimal("5.00"), "Bulk savings");
        context.getAccountService().withdraw(owner.userId(), ownerChecking.getId(), new BigDecimal("1.00"), "Bulk withdraw");
        // Matching text for another user must never be counted or exported.
        for (int i = 0; i < OTHER_MATCHES; i++) {
            context.getAccountService().deposit(
                    other.userId(), otherChecking.getId(), new BigDecimal("1.00"), "Bulk row other " + i
            );
        }
    }

    @Test
    void underLimitExportsAllFilteredMatchesRegardlessOfUiPage() {
        LocalDate today = LocalDate.now();
        // Simulates the UI sitting on page 3 (limit 20, offset 40).
        TransactionFilter uiPageThree = new TransactionFilter(
                ownerChecking.getId(), TransactionType.DEPOSIT, today, today, "bulk row", 20, 40
        );

        String csv = context.getTransactionExportService().exportCsv(owner.userId(), uiPageThree);

        assertEquals(OWNER_MATCHES, dataRowCount(csv));
        assertTrue(csv.startsWith(HEADER + "\n"));
        assertFalse(csv.contains("Bulk savings"));
        assertFalse(csv.contains("Bulk withdraw"));
        assertFalse(csv.contains("other"));
        assertFalse(csv.contains(ownerChecking.getAccountNumber()));
        assertFalse(csv.contains(otherChecking.getAccountNumber()));
        assertFalse(csv.contains(PASSWORD));
        assertFalse(csv.contains("pbkdf2-sha256"));
    }

    @Test
    void exactlyAtLimitExportsEveryRow() {
        TransactionExportService atLimit = new TransactionExportService(
                context.getTransactionService(), context.getAccountRepository(), OWNER_MATCHES
        );
        TransactionFilter filter = new TransactionFilter(
                ownerChecking.getId(), TransactionType.DEPOSIT, null, null, "bulk row", 20, 0
        );

        String csv = assertDoesNotThrow(() -> atLimit.exportCsv(owner.userId(), filter));

        assertEquals(OWNER_MATCHES, dataRowCount(csv));
        assertFalse(csv.contains("other"));
    }

    @Test
    void aboveLimitFailsWithoutProducingPartialExport() {
        int max = OWNER_MATCHES - 1;
        TransactionExportService belowMatches = new TransactionExportService(
                context.getTransactionService(), context.getAccountRepository(), max
        );
        TransactionFilter filter = new TransactionFilter(
                ownerChecking.getId(), TransactionType.DEPOSIT, null, null, "bulk row", 20, 0
        );

        ValidationException csvError = assertThrows(ValidationException.class,
                () -> belowMatches.exportCsv(owner.userId(), filter));
        assertEquals("Export supports up to 134 transactions. Narrow your filters and try again.",
                csvError.getMessage());
        assertThrows(ValidationException.class, () -> belowMatches.exportCsvBytes(owner.userId(), filter));

        // Narrowing the filter below the limit succeeds.
        TransactionFilter narrowed = new TransactionFilter(
                ownerChecking.getId(), TransactionType.DEPOSIT, null, null, "bulk row 1", 20, 0
        );
        String csv = belowMatches.exportCsv(owner.userId(), narrowed);
        assertTrue(dataRowCount(csv) > 0);
        assertTrue(dataRowCount(csv) <= max);
        assertFalse(csv.contains("other"));
    }

    @Test
    void productionLimitBoundaryIsInclusive() {
        assertEquals(10_000, TransactionExportService.MAX_EXPORT_ROWS);
        assertDoesNotThrow(() -> TransactionExportService.ensureWithinExportLimit(0, TransactionExportService.MAX_EXPORT_ROWS));
        assertDoesNotThrow(() -> TransactionExportService.ensureWithinExportLimit(
                TransactionExportService.MAX_EXPORT_ROWS, TransactionExportService.MAX_EXPORT_ROWS));

        ValidationException error = assertThrows(ValidationException.class,
                () -> TransactionExportService.ensureWithinExportLimit(
                        TransactionExportService.MAX_EXPORT_ROWS + 1L, TransactionExportService.MAX_EXPORT_ROWS));
        assertEquals("Export supports up to 10,000 transactions. Narrow your filters and try again.",
                error.getMessage());
    }

    @Test
    void formulaLikeDescriptionsAreNeutralizedWithoutBreakingEscaping() {
        assertEquals("'=SUM(A1:A2)", TransactionExportService.neutralizeFormula("=SUM(A1:A2)"));
        assertEquals("'+1", TransactionExportService.neutralizeFormula("+1"));
        assertEquals("'-5", TransactionExportService.neutralizeFormula("-5"));
        assertEquals("'@cmd", TransactionExportService.neutralizeFormula("@cmd"));
        assertEquals("'\tTab", TransactionExportService.neutralizeFormula("\tTab"));
        assertEquals("'\rReturn", TransactionExportService.neutralizeFormula("\rReturn"));
        assertEquals("Coffee", TransactionExportService.neutralizeFormula("Coffee"));
        assertEquals("Pay -5 later", TransactionExportService.neutralizeFormula("Pay -5 later"));
        assertEquals("Café — ਪੰਜਾਬ", TransactionExportService.neutralizeFormula("Café — ਪੰਜਾਬ"));
        assertEquals("", TransactionExportService.neutralizeFormula(""));
        assertEquals("", TransactionExportService.neutralizeFormula(null));

        deposit("=HYPERLINK(\"http://x\",\"y\")");
        deposit("+cmd|' /C calc'!A0");
        deposit("@SUM(1,2)");
        deposit("-2+3");
        deposit("Normal, \"quoted\"\nline");

        String csv = new String(context.getTransactionExportService().exportCsvBytes(
                owner.userId(),
                new TransactionFilter(ownerChecking.getId(), TransactionType.DEPOSIT, null, null, null, 20, 0)
        ), StandardCharsets.UTF_8);

        assertTrue(csv.contains(",\"'=HYPERLINK(\"\"http://x\"\",\"\"y\"\")\","));
        assertTrue(csv.contains(",'+cmd|' /C calc'!A0,"));
        assertTrue(csv.contains(",\"'@SUM(1,2)\","));
        assertTrue(csv.contains(",'-2+3,"));
        assertTrue(csv.contains(",\"Normal, \"\"quoted\"\"\nline\","));
        // Signed amounts are generated values and remain unprefixed.
        assertTrue(csv.contains(",+$1.00\n"));
        assertFalse(csv.contains(",'+$1.00"));
    }

    private void deposit(String description) {
        context.getAccountService().deposit(owner.userId(), ownerChecking.getId(), new BigDecimal("1.00"), description);
    }

    private static long dataRowCount(String csv) {
        // Descriptions in this test class never contain newlines except the explicit escaping case.
        return csv.lines().skip(1).filter(line -> !line.isBlank()).count();
    }

    private Account find(long userId, AccountType type) {
        return context.getAccountService().getAccountsForUser(userId).stream()
                .filter(account -> account.getAccountType() == type)
                .findFirst()
                .orElseThrow();
    }
}
