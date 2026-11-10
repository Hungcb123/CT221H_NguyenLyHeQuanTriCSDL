package com.ctu.freshgrocery.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class MoMoSignature {
    private MoMoSignature() {}
    public static String sign(String raw, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException failure) { throw new IllegalStateException("Cannot initialize payment signature", failure); }
    }
    public static boolean verify(String raw, String signature, String secret) {
        if (signature == null || !signature.matches("[0-9a-fA-F]{64}")) return false;
        return MessageDigest.isEqual(HexFormat.of().parseHex(sign(raw,secret)), HexFormat.of().parseHex(signature));
    }
}
