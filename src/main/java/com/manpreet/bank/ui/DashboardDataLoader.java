package com.manpreet.bank.ui;

import com.manpreet.bank.AppContext;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.session.UserSession;
import java.util.List;

/**
 * Loads dashboard state from production services.
 */
public class DashboardDataLoader {

    private final AppContext appContext;

    public DashboardDataLoader(AppContext appContext) {
        this.appContext = appContext;
    }

    public DashboardViewModel load(UserSession session) {
        List<Account> accounts = appContext.getAccountService().getAccountsForUser(session.userId());
        Account checking = accounts.stream()
                .filter(account -> account.getAccountType() == AccountType.CHECKING)
                .findFirst()
                .orElse(null);
        Account savings = accounts.stream()
                .filter(account -> account.getAccountType() == AccountType.SAVINGS)
                .findFirst()
                .orElse(null);
        List<Transaction> recent = appContext.getTransactionService()
                .getRecentActivity(session.userId(), 10);
        return new DashboardViewModel(
                session,
                appContext.getAccountService().getTotalBalance(session.userId()),
                checking,
                savings,
                recent
        );
    }
}
