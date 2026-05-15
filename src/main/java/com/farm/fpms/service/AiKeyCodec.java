package com.farm.fpms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

@Component
public class AiKeyCodec {

    private static final byte[] PLAIN_PREFIX = new byte[]{'P', '1'};
    private static final byte[] AES_PREFIX = new byte[]{'A', '1'};
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;

    private final String masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AiKeyCodec(@Value("${fpms.ai.master-key:}") String masterKey) {
        this.masterKey = masterKey == null ? "" : masterKey.trim();
    }

    public byte[] encode(String apiKey) {
        String value = apiKey == null ? "" : apiKey.trim();
        if (masterKey.isEmpty()) {
            byte[] raw = value.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate(PLAIN_PREFIX.length + raw.length);
            buffer.put(PLAIN_PREFIX).put(raw);
            return buffer.array();
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(AES_PREFIX.length + iv.length + encrypted.length);
            buffer.put(AES_PREFIX).put(iv).put(encrypted);
            return buffer.array();
        } catch (Exception ex) {
            throw new IllegalStateException("AI API Key 加密失败", ex);
        }
    }

    public String decode(byte[] stored) {
        if (stored == null || stored.length == 0) {
            return "";
        }
        if (startsWith(stored, PLAIN_PREFIX)) {
            return new String(Arrays.copyOfRange(stored, PLAIN_PREFIX.length, stored.length), StandardCharsets.UTF_8);
        }
        if (!startsWith(stored, AES_PREFIX)) {
            return new String(stored, StandardCharsets.UTF_8);
        }
        if (masterKey.isEmpty()) {
            throw new IllegalStateException("已加密的 AI API Key 需要配置 FPMS_AI_MASTER_KEY");
        }
        try {
            byte[] iv = Arrays.copyOfRange(stored, AES_PREFIX.length, AES_PREFIX.length + IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(stored, AES_PREFIX.length + IV_LENGTH, stored.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("AI API Key 解密失败", ex);
        }
    }

    public static String mask(String apiKey) {
        String value = apiKey == null ? "" : apiKey.trim();
        if (value.length() <= 8) {
            return "********";
        }
        return value.substring(0, Math.min(3, value.length())) + "********" + value.substring(value.length() - 4);
    }

    private SecretKeySpec keySpec() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return new SecretKeySpec(digest.digest(masterKey.getBytes(StandardCharsets.UTF_8)), "AES");
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
