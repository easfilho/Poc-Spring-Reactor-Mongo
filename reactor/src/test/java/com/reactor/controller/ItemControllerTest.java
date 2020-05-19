package com.reactor.controller;

import com.github.javafaker.Faker;
import com.reactor.document.Item;
import com.reactor.repository.ItemReactiveRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
@DirtiesContext
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public class ItemControllerTest {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private ItemReactiveRepository itemReactiveRepository;
    private List<Item> items;

    public List<Item> generateItems() {
        Faker faker = new Faker();
        List<Item> items = new ArrayList<>();
        items.add(new Item("Id",
                faker.commerce().productName(),
                new BigDecimal(faker.commerce().price().replaceAll(",", "."))));
        for (int i = 0; i < 3; i++) {
            items.add(new Item(null,
                    faker.commerce().productName(),
                    new BigDecimal(faker.commerce().price().replaceAll(",", "."))));
        }
        return items;
    }

    @Before
    public void setUp() throws Exception {
        items = generateItems();
        itemReactiveRepository.deleteAll()
                .thenMany(Flux.fromIterable(items))
                .flatMap(itemReactiveRepository::save)
                .doOnNext(item -> System.out.println("Inserted item is: " + item))
                .blockLast();
    }

    @Test
    public void shouldReturnAllItems() {
        webTestClient.get()
                .uri("/v1/items")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Item.class)
                .hasSize(4)
                .consumeWith(response -> {
                    Assert.assertNotNull(response.getResponseBody());
                    boolean matches = items.stream().allMatch(item -> response.getResponseBody().stream()
                            .anyMatch(responseItem -> responseItem.getDescription().equals(item.getDescription()) &&
                                    responseItem.getPrice().compareTo(item.getPrice()) == 0 &&
                                    responseItem.getId() != null));
                    Assert.assertTrue(matches);
                });
    }

    @Test
    public void shouldGetItem() {
        webTestClient.get()
                .uri("/v1/items/Id")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Item.class)
                .consumeWith(response -> {
                    Assert.assertNotNull(response.getResponseBody());
                    Assert.assertEquals(items.get(0).getId(), response.getResponseBody().getId());
                    Assert.assertEquals(items.get(0).getDescription(), response.getResponseBody().getDescription());
                    Assert.assertEquals(items.get(0).getPrice().toString(), response.getResponseBody().getPrice().toString());
                });
    }

    @Test
    public void shouldReturnNoItem() {
        webTestClient.get()
                .uri("/v1/items/1903")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    public void shouldSaveItem() {
        Item item = new Item(null,
                "Description",
                BigDecimal.ONE);

        webTestClient.post()
                .uri("/v1/items")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .body(Mono.just(item), Item.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.description").isEqualTo("Description")
                .jsonPath("$.price").isEqualTo(1.00);

    }

    @Test
    public void shouldDeleteItem() {
        webTestClient.delete()
                .uri("/v1/items/id")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Void.class);

        StepVerifier.create(itemReactiveRepository.findById("id"))
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    public void shouldUpdateItem() {
        Item item = new Item(null,
                "Description",
                BigDecimal.ONE);
        webTestClient.put()
                .uri("/v1/items/Id")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .body(Mono.just(item), Item.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Item.class)
                .consumeWith(response -> {
                    Assert.assertNotNull(response.getResponseBody());
                    Assert.assertEquals("Id", response.getResponseBody().getId());
                    Assert.assertEquals(item.getDescription(), response.getResponseBody().getDescription());
                    Assert.assertEquals(item.getPrice().toString(), response.getResponseBody().getPrice().toString());
                });

        StepVerifier.create(itemReactiveRepository.findById("Id"))
                .expectSubscription()
                .expectNextMatches(currentItem ->
                        currentItem.getDescription().equals("Description") &&
                                currentItem.getPrice().equals(BigDecimal.ONE)
                );
    }

    @Test
    public void shouldValidateNoExistentItemOnUpdate() {
        Item item = new Item(null,
                "Description",
                BigDecimal.ONE);
        webTestClient.put()
                .uri("/v1/items/1903")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .body(Mono.just(item), Item.class)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    public void shouldValidateRunTimeException() {
        webTestClient.get()
                .uri("/v1/items/runTimeException")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(String.class)
                .isEqualTo("RunTimeException :o");
    }
}