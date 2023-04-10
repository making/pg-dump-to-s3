package lol.maki.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pg-dump")
public record PgDumpProps(String host, int port, String username, String password,
						  String database) {
}
