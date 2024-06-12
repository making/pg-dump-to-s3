package lol.maki.batch;

import java.time.Clock;
import java.time.Duration;

import am.ik.spring.http.client.RetryableClientHttpRequestInterceptor;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration(proxyBeanMethods = false)
public class AppConfig {

	@Bean
	public RestClientCustomizer restClientCustomizer(
			LogbookClientHttpRequestInterceptor logbookClientHttpRequestInterceptor) {
		return restClientBuilder -> restClientBuilder
			.requestFactory(ClientHttpRequestFactories.get(JdkClientHttpRequestFactory::new,
					ClientHttpRequestFactorySettings.DEFAULTS.withReadTimeout(Duration.ofSeconds(5))))
			.requestInterceptor(logbookClientHttpRequestInterceptor)
			.requestInterceptor(new RetryableClientHttpRequestInterceptor(new ExponentialBackOff()));
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

}
