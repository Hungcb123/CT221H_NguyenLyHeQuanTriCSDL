package com.ctu.freshgrocery.service;

import com.ctu.freshgrocery.dto.TopUpWalletRequest;
import com.ctu.freshgrocery.dto.WalletResponse;
import com.ctu.freshgrocery.model.WalletTransaction;

import java.util.List;

public interface WalletService {
    WalletResponse topUpWallet(TopUpWalletRequest request);
    WalletResponse getBalance(Integer customerId);
    List<WalletTransaction> getTransactionHistory(Integer customerId);
}
