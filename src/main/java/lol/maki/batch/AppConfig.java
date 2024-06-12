package lol.maki.batch;

import java.time.Clock;

import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AppConfig {

	@Bean
	public RestClientCustomizer restClientCustomizer(
			LogbookClientHttpRequestInterceptor logbookClientHttpRequestInterceptor) {
		return restClientBuilder -> restClientBuilder.requestInterceptor(logbookClientHttpRequestInterceptor);
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

}
