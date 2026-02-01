package com.clipit.api_gateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus; // <--- Added
import org.springframework.http.server.reactive.ServerHttpResponse; // <--- Added
import org.springframework.stereotype.Component;

import com.clipit.api_gateway.util.JwtUtil;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (validator.isSecured.test(exchange.getRequest())) {
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7);
                }

                try {
                    jwtUtil.validateToken(authHeader);

                    String tempUserId = jwtUtil.extractUserId(authHeader);

                    if (tempUserId == null) {
                        tempUserId = jwtUtil.extractUsername(authHeader);
                    }

                    final String finalUserId = tempUserId;

                    return chain.filter(exchange.mutate()
                            .request(r -> r.header("X-User-Id", finalUserId))
                            .build());

                } catch (Exception e) {
                    System.err.println("Unauthorized access: " + e.getMessage());
                    
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return response.setComplete();
                }
            }
            return chain.filter(exchange);
        });
    }

    public static class Config {
    }
}