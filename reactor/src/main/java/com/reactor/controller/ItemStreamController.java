package com.reactor.controller;

import com.reactor.document.ItemCapped;
import com.reactor.repository.ItemReactiveCappedRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ItemStreamController {

    private final ItemReactiveCappedRepository itemReactiveCappedRepository;

    public ItemStreamController(ItemReactiveCappedRepository itemReactiveCappedRepository) {
        this.itemReactiveCappedRepository = itemReactiveCappedRepository;
    }

    @GetMapping(value = "/v1/stream/items", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<ItemCapped> listItemStream() {
        return itemReactiveCappedRepository.findItemsBy();
    }
}
