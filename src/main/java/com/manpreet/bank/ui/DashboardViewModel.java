package com.manpreet.bank.ui;

import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.session.UserSession;
import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable snapshot used to refresh authenticated banking screens.
 */
public record DashboardViewModel(
        UserSession session,
        BigDecimal totalBalance,
        Account checking,
        Account savings,
        List<Transaction> recentTransactions
) {
}
