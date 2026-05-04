package com.example.apigateway.route;

import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.setPath;

@Configuration
public class OrderServiceRoutes {

    @Bean
    public RouterFunction<ServerResponse> orderRoutes() {
        return GatewayRouterFunctions.route("order-service")
                .route(RequestPredicates.path("/orders/**"),
                        HandlerFunctions.http("http://localhost:8082"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> orderServiceApiDocs() {
        return GatewayRouterFunctions.route("order-service-api-docs")
                .route(RequestPredicates.path("/docs/orderservice/v3/api-docs"),
                        HandlerFunctions.http("http://localhost:8082"))
                .filter(setPath("/v3/api-docs"))
                .build();
    }
}
