package com.ielts.speaking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Placeholder for Aliyun NLS SDK client configuration.
 * The Aliyun NLS SDK is not available in Maven Central.
 * In production, download the SDK JAR and add it as a system-scoped dependency,
 * then replace this placeholder with the actual NlsClient bean.
 */
@Configuration
public class NlsClientConfig {

    @ConfigurationProperties(prefix = "aliyun.nls")
    public record NlsProperties(String accessKeyId, String accessKeySecret, String appKey) {}
}
