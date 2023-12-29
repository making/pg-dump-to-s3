package lol.maki.batch;

import java.time.Clock;

import io.micrometer.core.instrument.binder.okhttp3.OkHttpObservationInterceptor;
import io.micrometer.observation.ObservationRegistry;
import io.minio.MinioClient;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AppConfig {

	@Bean
	public OkHttpClient okHttpClient(ObservationRegistry observationRegistry) {
		final HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
		httpLoggingInterceptor.setLevel(Level.BASIC);
		return new OkHttpClient.Builder()
			.addInterceptor(OkHttpObservationInterceptor.builder(observationRegistry, "okhttp.requests")
				.uriMapper(request -> "/" + String.join("/", request.url().pathSegments()))
				.build())
			.addInterceptor(httpLoggingInterceptor)
			.build();
	}

	@Bean
	public MinioClient minioClient(AwsProps awsProps, S3Props s3Props, OkHttpClient okHttpClient) {
		return MinioClient.builder()
			.endpoint(s3Props.hostname())
			.credentials(awsProps.accessKeyId(), awsProps.secretAccessKey())
			.region(awsProps.region())
			.httpClient(okHttpClient)
			.build();
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

}
