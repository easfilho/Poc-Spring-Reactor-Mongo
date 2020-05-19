package com.reactor.repository;

import com.reactor.document.Item;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Arrays;

@DataMongoTest
@RunWith(SpringRunner.class)
@DirtiesContext
public class ItemReactiveRepositoryTest {

    @Autowired
    private ItemReactiveRepository itemReactiveRepository;

    @Before
    public void setUp() throws Exception {
        itemReactiveRepository.deleteAll()
                .thenMany(Flux.fromIterable(Arrays.asList(new Item(null, "Item 1", BigDecimal.TEN),
                        new Item("IdNotGenerated", "Item 2", BigDecimal.ONE))))
        .flatMap(itemReactiveRepository::save)
        .blockLast();
    }

    @Test
    public void getAllItems() {
        StepVerifier.create(itemReactiveRepository.findAll())
                .expectSubscription()
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    public void getById() {
        StepVerifier.create(itemReactiveRepository.findById("IdNotGenerated"))
                .expectSubscription()
                .expectNextMatches(item -> item.getDescription().equals("Item 2"))
                .verifyComplete();
    }

    @Test
    public void findByDescription() {
        StepVerifier.create(itemReactiveRepository.findByDescriptionContaining("Item"))
                .expectSubscription()
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    public void saveItem() {
        Mono<Item> savedItem = itemReactiveRepository.save(new Item(null, "Description", BigDecimal.ONE));

        StepVerifier.create(savedItem)
                .expectSubscription()
                .expectNextMatches(item -> item.getId() != null)
                .verifyComplete();
    }

    @Test
    public void updateItem() {
        Flux<Item> fluxItem = itemReactiveRepository.findByDescriptionContaining("Item 2")
                .map(item -> {
                    item.setDescription("Description Updated");
                    return item;
                })
                .flatMap(itemReactiveRepository::save);


        StepVerifier.create(fluxItem)
                .expectSubscription()
                .expectNextMatches(item -> item.getDescription().equals("Description Updated"))
                .verifyComplete();
    }

    @Test
    public void deleteItem() {
        Mono<Void> deletedItem = itemReactiveRepository.findById("IdNotGenerated")
                .map(Item::getId)
                .flatMap(itemReactiveRepository::deleteById);

        StepVerifier.create(deletedItem)
                .expectSubscription()
                .verifyComplete();

        StepVerifier.create(itemReactiveRepository.findAll())
                .expectSubscription()
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    public void shouldUpdateItem() {

    }
}