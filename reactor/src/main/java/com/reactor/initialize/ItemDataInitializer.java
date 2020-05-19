package com.reactor.initialize;

import com.github.javafaker.Faker;
import com.reactor.document.Item;
import com.reactor.document.ItemCapped;
import com.reactor.repository.ItemReactiveCappedRepository;
import com.reactor.repository.ItemReactiveRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("!test")
@Slf4j
public class ItemDataInitializer implements CommandLineRunner {

    private final ItemReactiveRepository itemReactiveRepository;
    private final MongoOperations mongoOperations;
    private final ItemReactiveCappedRepository itemReactiveCappedRepository;

    public ItemDataInitializer(ItemReactiveRepository itemReactiveRepository,
                               MongoOperations mongoOperations,
                               ItemReactiveCappedRepository itemReactiveCappedRepository) {
        this.itemReactiveRepository = itemReactiveRepository;
        this.mongoOperations = mongoOperations;
        this.itemReactiveCappedRepository = itemReactiveCappedRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        initialSetupData();
        createCappedCollection();
        dataSetupForCappedCollection();
    }

    private void createCappedCollection() {
        mongoOperations.dropCollection(ItemCapped.class);
        mongoOperations.createCollection(ItemCapped.class,
                CollectionOptions.empty().maxDocuments(20).size(50000).capped());
    }

    private void initialSetupData() {
        itemReactiveRepository.deleteAll()
                .thenMany(Flux.fromIterable(data()))
                .flatMap(itemReactiveRepository::save)
                .subscribe();
    }

    public List<Item> data() {
        Faker faker = new Faker();
        List<Item> items = new ArrayList<>();
        for(int i = 0; i < 10; i ++) {
            items.add(new Item(null,
                    faker.commerce().productName(),
                    new BigDecimal(faker.commerce().price().replaceAll(",", "."))));
        }
        items.add(new Item("Id",
                faker.commerce().productName(),
                new BigDecimal(faker.commerce().price().replaceAll(",", "."))));
        return items;
    }

    public void dataSetupForCappedCollection() {
        Flux<ItemCapped> itemCappedFlux = Flux.interval(Duration.ofSeconds(1))
                .map(i -> new ItemCapped(null, "Random Item " + i, new BigDecimal(i)));

        itemReactiveCappedRepository.insert(itemCappedFlux)
                .subscribe(itemCapped -> log.info("Inserted item capped " + itemCapped));
    }
}
