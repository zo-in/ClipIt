package com.clipit.api_gateway;

import java.util.Collections;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	@Bean
	public CorsWebFilter corsWebFilter() {
		CorsConfiguration corsConfig = new CorsConfiguration();
		// Allow your frontend origin
		corsConfig.setAllowedOrigins(Collections.singletonList("http://localhost:5173"));
		// Allow all HTTP methods (GET, POST, PUT, DELETE, etc.)
		corsConfig.addAllowedMethod("*");
		// Allow all headers (Authorization, Content-Type, etc.)
		corsConfig.addAllowedHeader("*");
		// Allow credentials (needed if we ever use cookies, though we use JWT headers)
		corsConfig.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", corsConfig);

		return new CorsWebFilter(source);
	}
}