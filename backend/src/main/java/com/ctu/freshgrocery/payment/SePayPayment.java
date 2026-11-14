package com.ctu.freshgrocery.payment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public record SePayPayment(String paymentId,int maDH,int maKH,String accountNumber,long amount,
                           String status,String transId,String bankCode,String accountName,LocalDateTime expiresAt) {
    public static SePayPayment read(ResultSet rs,int row) throws SQLException {
        return new SePayPayment(rs.getString("PaymentId"),rs.getInt("MaDH"),rs.getInt("MaKH"),rs.getString("AccountNumber"),
                rs.getLong("Amount"),rs.getString("Status"),rs.getString("TransId"),rs.getString("BankCode"),
                rs.getString("AccountName"),rs.getTimestamp("ExpiresAt").toLocalDateTime());
    }
    public boolean pending() {return "PENDING".equals(status);}
}
