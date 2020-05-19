package com.reactor.controller;

import com.reactor.document.Item;
import com.reactor.repository.ItemReactiveRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class ItemController {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRunTimeException(RuntimeException exception) {
        log.error("Exception caught in handleRunTimeException: " + exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
    }

    private final ItemReactiveRepository itemReactiveRepository;

    public ItemController(ItemReactiveRepository itemReactiveRepository) {
        this.itemReactiveRepository = itemReactiveRepository;
    }

    @GetMapping("/v1/items")
    public Flux<Item> list() {
        return itemReactiveRepository.findAll();
    }

    @GetMapping("/v1/items/{id}")
    public Mono<ResponseEntity<Item>> get(@PathVariable String id) {
        return itemReactiveRepository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }

    @PostMapping("/v1/items")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Item> save(@RequestBody Item item) {
        return itemReactiveRepository.save(item);
    }

    @DeleteMapping("/v1/items/{id}")
    public Mono<Void> delete(@PathVariable String id) {
        return itemReactiveRepository.deleteById(id);
    }

    @PutMapping("/v1/items/{id}")
    public Mono<ResponseEntity<Item>> update(@PathVariable String id, @RequestBody Item item) {
        return itemReactiveRepository.findById(id)
                .flatMap(currentItem -> {
                    currentItem.setDescription(item.getDescription());
                    currentItem.setPrice(item.getPrice());
                    return itemReactiveRepository.save(currentItem);
                }).map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }

    @GetMapping("/v1/items/runTimeException")
    public Flux<Item> runTimeException() {
        return itemReactiveRepository.findAll()
                .concatWith(Mono.error(new RuntimeException("RunTimeException :o")));
    }
}
