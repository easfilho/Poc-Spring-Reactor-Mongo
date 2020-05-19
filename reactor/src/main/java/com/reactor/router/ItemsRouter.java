package com.reactor.router;

import com.reactor.handler.ItemsHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;


@Configuration
public class ItemsRouter {

    @Bean
    public RouterFunction<ServerResponse> itemsRoute(ItemsHandler itemsHandler) {
        return RouterFunctions
                .route(GET("/v1/fun/items").and(accept(MediaType.APPLICATION_JSON)), itemsHandler::getAllItems)
                .andRoute(GET("/v1/fun/items/{id}").and(accept(MediaType.APPLICATION_JSON)), itemsHandler::getOneItem)
                .andRoute(POST("/v1/fun/items").and(accept(MediaType.APPLICATION_JSON)), itemsHandler::save)
                .andRoute(DELETE("/v1/fun/items/{id}").and(accept(MediaType.APPLICATION_JSON)), itemsHandler::delete)
                .andRoute(PUT("/v1/fun/items/{id}").and(accept(MediaType.APPLICATION_JSON)), itemsHandler::update);
    }

    @Bean
    public RouterFunction<ServerResponse> errorRoute(ItemsHandler itemsHandler) {
        return RouterFunctions
                .route(GET("/v1/fun/runTimeException").and(accept(MediaType.APPLICATION_JSON)), itemsHandler::runTimeException);
    }

    @Bean
    public RouterFunction<ServerResponse> itemsStreamRoute(ItemsHandler itemsHandler) {
        return RouterFunctions
                .route(GET("/v1/fun/stream/items").and(accept(MediaType.APPLICATION_JSON)), itemsHandler::getAllItemsStream);
    }
}
