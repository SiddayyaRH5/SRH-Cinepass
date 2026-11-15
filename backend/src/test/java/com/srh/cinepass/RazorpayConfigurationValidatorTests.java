package com.srh.cinepass;

import com.srh.cinepass.config.RazorpayConfigurationValidator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

class RazorpayConfigurationValidatorTests {

    @Test
    void productionWithAllCredentialsStartsNormally() {
        assertThatCode(() -> validator("production", "key", "secret", "webhook")
                .afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void productionFailsFastWhenCredentialsAreMissing() {
        Throwable failure = catchThrowable(() -> validator("production", "", "", "")
                .afterPropertiesSet());

        assertThat(failure)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RAZORPAY_KEY_ID")
                .hasMessageContaining("RAZORPAY_KEY_SECRET")
                .hasMessageContaining("RAZORPAY_WEBHOOK_SECRET");
    }

    @Test
    void productionErrorNeverEchoesConfiguredSecretValues() {
        Throwable failure = catchThrowable(() -> validator(
                "production",
                "rzp_live_visible_key_id",
                "",
                "")
                .afterPropertiesSet());

        assertThat(failure).isInstanceOf(IllegalStateException.class);
        assertThat(failure.getMessage())
                .doesNotContain("rzp_live_visible_key_id")
                .doesNotContain("secret");
    }

    @Test
    void developmentAndTestDoNotRequireRazorpayCredentials() {
        assertThatCode(() -> validator("development", "", "", "")
                .afterPropertiesSet())
                .doesNotThrowAnyException();

        assertThatCode(() -> validator("test", "", "", "")
                .afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void environmentNameIsCaseInsensitive() {
        Throwable failure = catchThrowable(() -> validator("PRODUCTION", "", "", "")
                .afterPropertiesSet());

        assertThat(failure).isInstanceOf(IllegalStateException.class);
    }

    private RazorpayConfigurationValidator validator(
            String environment,
            String keyId,
            String keySecret,
            String webhookSecret) {

        return new RazorpayConfigurationValidator(
                environment,
                keyId,
                keySecret,
                webhookSecret);
    }
}
