package com.reactor.ItemClient.controller;

import com.reactor.ItemClient.domain.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class ItemClientController {

    private final WebClient webClient = WebClient.create("http://localhost:8080");

    @GetMapping("/client/retrieve")
    public Flux<Item> getAllItemsUsingRetrieve() {
        return webClient.get().uri("/v1/items")
                .retrieve()
                .bodyToFlux(Item.class)
                .log();
    }

    @GetMapping("/client/exchange")
    public Flux<Item> getAllItemsUsingExchange() {
        return webClient.get().uri("/v1/items")
                .exchange()
                .flatMapMany(clientResponse -> clientResponse.bodyToFlux(Item.class))
                .log();
    }

    @GetMapping("/client/retrieve/{id}")
    public Mono<Item> getSingleItemsUsingRetrieve(@PathVariable String id) {
        return webClient.get().uri("/v1/items/{id}", id)
                .retrieve()
                .bodyToMono(Item.class)
                .log();
    }

    @GetMapping("/client/exchange/{id}")
    public Mono<Item> getSingleItemsUsingExchange(@PathVariable String id) {
        return webClient.get().uri("/v1/items/{id}", id)
                .exchange()
                .flatMap(clientResponse -> clientResponse.bodyToMono(Item.class))
                .log();
    }

    @PostMapping("/client/create-item")
    public Mono<Item> createItem(@RequestBody Item item) {
        return webClient.post().uri("/v1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(item), Item.class)
                .retrieve()
                .bodyToMono(Item.class)
                .log();
    }

    @PutMapping("/client/update-item/{id}")
    public Mono<Item> updateItem(@PathVariable String id, @RequestBody Item item) {
        return webClient.put().uri("/v1/items/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(item), Item.class)
                .retrieve()
                .bodyToMono(Item.class)
                .log();
    }

    @DeleteMapping("/client/delete-item/{id}")
    public Mono<Void> delete(@PathVariable String id) {
        return webClient.delete().uri("/v1/items/{id}", id)
                .retrieve()
                .bodyToMono(Void.class)
                .log();
    }

    @GetMapping("/client/retrieve/error")
    public Flux<Item> getRetrieveError() {
        return webClient.get().uri("/v1/items/runTimeException")
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .flatMap(message -> {
                            log.error("The error message: " + message);
                            throw new RuntimeException(message);
                        }))
                .bodyToFlux(Item.class)
                .log();
    }

    @GetMapping("/client/exchange/error")
    public Flux<Item> getExchangeError() {
        return webClient.get().uri("/v1/items/runTimeException")
                .exchange()
                .flatMapMany(clientResponse -> {
                    if (clientResponse.statusCode().is5xxServerError()) {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(message -> {
                                    log.error("The error message: " + message);
                                    throw new RuntimeException(message);
                                });
                    }
                    return clientResponse.bodyToFlux(Item.class);
                })
                .log();
    }
}
