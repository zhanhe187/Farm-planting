package com.farm.fpms.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    @Test
    void masksPhoneAndCitizenIdBeforeInjectingContext() {
        PromptBuilder builder = new PromptBuilder();

        String prompt = builder.buildContextPrompt("番茄批次负责人手机 13812345678，身份证 110101199001011234");

        assertThat(prompt).contains("138****5678");
        assertThat(prompt).contains("110101********1234");
        assertThat(prompt).doesNotContain("13812345678");
        assertThat(prompt).doesNotContain("110101199001011234");
    }
}
