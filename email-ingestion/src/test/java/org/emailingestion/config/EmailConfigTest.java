package org.emailingestion.config;

import org.emailingestion.service.EmailPollingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EmailConfigTest {

    @Mock
    private EmailPollingService emailPollingService;

    @Test
    void emailInboundFlow_WhenPollingEnabled_ShouldReturnIntegrationFlow() {
        // Arrange
        EmailConfig emailConfig = new EmailConfig();
        ReflectionTestUtils.setField(emailConfig, "host", "imap.gmail.com");
        ReflectionTestUtils.setField(emailConfig, "port", 993);
        ReflectionTestUtils.setField(emailConfig, "username", "test@gmail.com");
        ReflectionTestUtils.setField(emailConfig, "password", "password123");
        ReflectionTestUtils.setField(emailConfig, "protocol", "imaps");
        ReflectionTestUtils.setField(emailConfig, "pollingInterval", 30000L);
        ReflectionTestUtils.setField(emailConfig, "folder", "INBOX");
        ReflectionTestUtils.setField(emailConfig, "markAsRead", true);
        ReflectionTestUtils.setField(emailConfig, "deleteAfterProcessing", false);
        ReflectionTestUtils.setField(emailConfig, "pollingEnabled", true);

        // Act
        IntegrationFlow flow = emailConfig.emailInboundFlow(emailPollingService);

        // Assert
        assertThat(flow).isNotNull();
    }

    @Test
    void emailInboundFlow_WhenPollingDisabled_ShouldReturnNull() {
        // Arrange
        EmailConfig emailConfig = new EmailConfig();
        ReflectionTestUtils.setField(emailConfig, "pollingEnabled", false);

        // Act
        IntegrationFlow flow = emailConfig.emailInboundFlow(emailPollingService);

        // Assert
        assertThat(flow).isNull();
    }

    @Test
    void buildEmailUrl_ShouldEncodeSpecialCharacters() {
        // Arrange
        EmailConfig emailConfig = new EmailConfig();
        ReflectionTestUtils.setField(emailConfig, "host", "imap.gmail.com");
        ReflectionTestUtils.setField(emailConfig, "port", 993);
        ReflectionTestUtils.setField(emailConfig, "username", "test+user@gmail.com");
        ReflectionTestUtils.setField(emailConfig, "password", "p@ssw0rd!");
        ReflectionTestUtils.setField(emailConfig, "protocol", "imaps");
        ReflectionTestUtils.setField(emailConfig, "folder", "INBOX");

        // Act - use reflection to call private method
        String url = (String) ReflectionTestUtils.invokeMethod(emailConfig, "buildEmailUrl");

        // Assert
        assertThat(url).isNotNull();
        assertThat(url).startsWith("imaps://");
        assertThat(url).contains("imap.gmail.com:993/INBOX");
        // Username and password should be URL encoded
        assertThat(url).doesNotContain("@gmail.com@imap"); 
    }

    @Test
    void javaMailProperties_ForImaps_ShouldSetCorrectProperties() {
        // Arrange
        EmailConfig emailConfig = new EmailConfig();
        ReflectionTestUtils.setField(emailConfig, "protocol", "imaps");

        // Act - explicit cast to Properties
        Properties props = (Properties) ReflectionTestUtils.invokeMethod(emailConfig, "javaMailProperties");

        // Assert
        assertThat(props).isNotNull();
        assertThat(props.getProperty("mail.imaps.ssl.enable")).isEqualTo("true");
        assertThat(props.getProperty("mail.imaps.ssl.trust")).isEqualTo("*");
        assertThat(props.getProperty("mail.imaps.auth")).isEqualTo("true");
        assertThat(props.getProperty("mail.imaps.connectiontimeout")).isEqualTo("10000");
        assertThat(props.getProperty("mail.imaps.timeout")).isEqualTo("10000");
    }

    @Test
    void javaMailProperties_ForImap_ShouldSetCorrectProperties() {
        // Arrange
        EmailConfig emailConfig = new EmailConfig();
        ReflectionTestUtils.setField(emailConfig, "protocol", "imap");

        // Act - explicit cast to Properties
        Properties props = (Properties) ReflectionTestUtils.invokeMethod(emailConfig, "javaMailProperties");

        // Assert
        assertThat(props).isNotNull();
        assertThat(props.getProperty("mail.imap.auth")).isEqualTo("true");
        assertThat(props.getProperty("mail.imap.connectiontimeout")).isEqualTo("10000");
        assertThat(props.getProperty("mail.imap.timeout")).isEqualTo("10000");
        // SSL properties should not be set for plain imap
        assertThat(props.getProperty("mail.imap.ssl.enable")).isNull();
    }
}