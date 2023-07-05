package lol.maki.batch;

import lol.maki.batch.alert.AlertProps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ PgDumpProps.class, S3Props.class, AwsProps.class, AlertProps.class })
public class PgDumpToS3Application {

	public static void main(String[] args) {
		SpringApplication.run(PgDumpToS3Application.class, args);
	}

}
