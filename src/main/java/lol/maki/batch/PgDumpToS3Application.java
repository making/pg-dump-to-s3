package lol.maki.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ PgDumpProps.class, S3Props.class, AwsProps.class })
public class PgDumpToS3Application {

	public static void main(String[] args) {
		SpringApplication.run(PgDumpToS3Application.class, args);
	}

}
