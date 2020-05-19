package com.reactor.handler;

import com.reactor.document.Item;
import com.reactor.document.ItemCapped;
import com.reactor.repository.ItemReactiveCappedRepository;
import com.reactor.repository.ItemReactiveRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

@Component
public class ItemsHandler {

    private final ItemReactiveRepository itemReactiveRepository;
    private final ItemReactiveCappedRepository itemReactiveCappedRepository;

    public ItemsHandler(ItemReactiveRepository itemReactiveRepository, ItemReactiveCappedRepository itemReactiveCappedRepository) {
        this.itemReactiveRepository = itemReactiveRepository;
        this.itemReactiveCappedRepository = itemReactiveCappedRepository;
    }

    public Mono<ServerResponse> getAllItems(ServerRequest serverRequest) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(itemReactiveRepository.findAll(), Item.class);
    }

    public Mono<ServerResponse> getOneItem(ServerRequest serverRequest) {
        return Stream.of(serverRequest.pathVariable("id"))
                .map(itemReactiveRepository::findById)
                .map(itemMono -> itemMono.flatMap(item -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromObject(item))
                        .switchIfEmpty(ServerResponse.noContent().build())))
                .findFirst()
                .orElseGet(() -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    public Mono<ServerResponse> save(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(Item.class)
                .flatMap(itemReactiveRepository::save)
                .flatMap(item -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromObject(item)));
    }

    public Mono<ServerResponse> delete(ServerRequest serverRequest) {
        return Stream.of(serverRequest.pathVariable("id"))
                .map(itemReactiveRepository::deleteById)
                .map(voidMono -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .build())
                .findFirst()
                .orElseGet(() -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    public Mono<ServerResponse> update(ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");
        Mono<Item> updatedItem = serverRequest.bodyToMono(Item.class)
                .flatMap(item -> itemReactiveRepository.findById(id)
                        .flatMap(currentItem -> {
                            currentItem.setPrice(item.getPrice());
                            currentItem.setDescription(item.getDescription());
                            return itemReactiveRepository.save(currentItem);
                        })
                );

        return updatedItem.flatMap(item -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(item))
                .switchIfEmpty(ServerResponse.noContent().build()));
    }

    public Mono<ServerResponse> runTimeException(ServerRequest serverRequest) {
        throw new RuntimeException("RunTimeException :o");
    }

    public Mono<ServerResponse> getAllItemsStream(ServerRequest serverRequest) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .body(itemReactiveCappedRepository.findItemsBy(), ItemCapped.class);

    }
}
