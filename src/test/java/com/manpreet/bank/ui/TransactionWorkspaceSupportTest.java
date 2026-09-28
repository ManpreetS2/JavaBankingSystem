package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.model.TransactionType;
import com.manpreet.bank.service.TransactionFilter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TransactionWorkspaceSupportTest {

    @Test
    void paginationHandlesEdgesAndClamping() {
        PaginationState empty = new PaginationState();
        assertEquals(0, empty.getPageIndex());
        assertEquals(1, empty.getTotalPages());
        assertFalse(empty.hasPrevious());
        assertFalse(empty.hasNext());
        assertEquals(0, empty.getOffset());

        PaginationState onePage = new PaginationState(0, 5);
        assertEquals(1, onePage.getTotalPages());
        assertFalse(onePage.hasNext());

        PaginationState exactTwo = new PaginationState(0, PaginationState.PAGE_SIZE * 2);
        assertEquals(2, exactTwo.getTotalPages());
        assertTrue(exactTwo.hasNext());
        exactTwo.next();
        assertEquals(1, exactTwo.getPageIndex());
        assertFalse(exactTwo.hasNext());
        assertTrue(exactTwo.hasPrevious());
        assertEquals(PaginationState.PAGE_SIZE, exactTwo.getOffset());

        PaginationState partial = new PaginationState(0, PaginationState.PAGE_SIZE + 3);
        assertEquals(2, partial.getTotalPages());
        partial.next();
        assertEquals("Page 2 of 2", partial.pageLabel());

        PaginationState overflow = new PaginationState(9, 25);
        assertEquals(1, overflow.getPageIndex());
        overflow.setTotalCount(5);
        assertEquals(0, overflow.getPageIndex());
        overflow.reset();
        assertEquals(0, overflow.getPageIndex());
    }

    @Test
    void mapperBuildsTransferLabelsAndSignedAmounts() {
        Account checking = account(10L, AccountType.CHECKING, "CHK-1111222233334444");
        Account savings = account(20L, AccountType.SAVINGS, "SAV-5555666677778888");
        Map<Long, Account> lookup = Map.of(10L, checking, 20L, savings);

        Transaction transferOut = new Transaction(
                1L, 10L, 20L, TransactionType.TRANSFER_OUT, new BigDecimal("500.00"),
                "Move", LocalDateTime.of(2026, 1, 2, 10, 0)
        );
        Transaction transferIn = new Transaction(
                2L, 20L, 10L, TransactionType.TRANSFER_IN, new BigDecimal("500.00"),
                "Move", LocalDateTime.of(2026, 1, 2, 10, 0)
        );
        Transaction deposit = new Transaction(
                3L, 10L, null, TransactionType.DEPOSIT, new BigDecimal("25.00"),
                "Pay", LocalDateTime.of(2026, 1, 3, 9, 0)
        );

        TransactionRowViewModel outRow = TransactionViewMapper.toRow(transferOut, lookup);
        assertEquals("Transfer to Savings", outRow.typeLabel());
        assertEquals("Checking •••• 4444", outRow.accountLabel());
        assertEquals("-$500.00", outRow.signedAmount());
        assertFalse(outRow.credit());
        assertEquals("Savings", outRow.relatedAccountLabel());

        TransactionRowViewModel inRow = TransactionViewMapper.toRow(transferIn, lookup);
        assertEquals("Transfer from Checking", inRow.typeLabel());
        assertEquals("+$500.00", inRow.signedAmount());
        assertTrue(inRow.credit());

        TransactionRowViewModel depositRow = TransactionViewMapper.toRow(deposit, lookup);
        assertEquals("Deposit", depositRow.typeLabel());
        assertEquals("+$25.00", depositRow.signedAmount());

        Transaction orphan = new Transaction(
                4L, 10L, 99L, TransactionType.TRANSFER_OUT, new BigDecimal("1.00"),
                "x", LocalDateTime.now()
        );
        assertEquals("Transfer out", TransactionViewMapper.toRow(orphan, lookup).typeLabel());
        assertNull(TransactionViewMapper.toRow(orphan, lookup).relatedAccountLabel());
    }

    @Test
    void filterNormalizesSearchWithLocaleRoot() {
        TransactionFilter filter = new TransactionFilter(
                null, null, null, null, "  Café  ", null, null
        ).withDefaults(20);
        assertEquals("café", filter.searchText());
        assertEquals(20, filter.limit());
        assertEquals(0, filter.offset());

        String turkishI = "I".toLowerCase(Locale.ROOT);
        assertEquals("i", turkishI);
        TransactionFilter upper = new TransactionFilter(
                null, null, null, null, "ALPHA", 10, 5
        ).withDefaults(20);
        assertEquals("alpha", upper.searchText());
        assertEquals(10, upper.limit());
        assertEquals(5, upper.offset());
    }

    private static Account account(long id, AccountType type, String number) {
        Account account = new Account();
        account.setId(id);
        account.setUserId(1L);
        account.setAccountType(type);
        account.setAccountNumber(number);
        account.setBalance(BigDecimal.ZERO);
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }
}
