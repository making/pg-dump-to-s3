package lol.maki.batch;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "s3")
public record S3Props(String hostname, String bucket, @DefaultValue("") String filePrefix, Duration retention) {
}
