package com.linkedinagent.util;

import com.linkedinagent.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionUtilTest {

    private EncryptionUtil encryptionUtil;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getEncryption().setAesKey("test-32-char-aes-key-change-me!!");
        encryptionUtil = new EncryptionUtil(properties);
    }

    @Test
    void encryptsAndDecryptsRoundTrip() {
        String plaintext = "linkedin-access-token-secret-value";

        String encrypted = encryptionUtil.encrypt(plaintext);
        String decrypted = encryptionUtil.decrypt(encrypted);

        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void handlesNullAndBlank() {
        assertThat(encryptionUtil.encrypt(null)).isNull();
        assertThat(encryptionUtil.encrypt("")).isEmpty();
        assertThat(encryptionUtil.decrypt(null)).isNull();
    }
}
