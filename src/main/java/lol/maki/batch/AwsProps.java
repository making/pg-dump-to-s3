package lol.maki.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public record AwsProps(String accessKeyId, String secretAccessKey, String region) {
}
