package com.ctu.freshgrocery.payment;

import java.sql.ResultSet;
import java.sql.SQLException;

public record MoMoPayment(String paymentId, int maDH, int maKH, String requestId, String partnerCode,
                          long amount, String orderInfo, String status, String payUrl, String transId) {
    public static MoMoPayment read(ResultSet rs, int row) throws SQLException {
        return new MoMoPayment(rs.getString("PaymentId"), rs.getInt("MaDH"), rs.getInt("MaKH"),
                rs.getString("RequestId"), rs.getString("PartnerCode"), rs.getLong("Amount"),
                rs.getString("OrderInfo"), rs.getString("Status"), rs.getString("PayUrl"), rs.getString("TransId"));
    }
    public boolean pending() { return status.equals("CREATING") || status.equals("PENDING"); }
}
