package com.farm.fpms.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordServiceTest {

    @Test
    void matchesPlainPassword() {
        PasswordService service = new PasswordService();

        assertThat(service.matches("123456", "123456")).isTrue();
        assertThat(service.matches("bad-password", "123456")).isFalse();
    }

    @Test
    void rejectsBlankPassword() {
        PasswordService service = new PasswordService();

        assertThat(service.matches("", "123456")).isFalse();
        assertThat(service.matches("123456", "")).isFalse();
    }
}
