package lol.maki.batch;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "s3")
public record S3Props(String hostname, String bucket, Duration retention) {
}
