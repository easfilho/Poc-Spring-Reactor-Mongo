package com.reactor.fluxmonoplayground;

import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class FluxMonoTest {

    @Test
    public void FluxTestElementsWithoutError() {
        Flux<String> stringFlux = Flux.just("A", "B", "C");

        StepVerifier.create(stringFlux)
                .expectNext("A")
                .expectNext("B")
                .expectNext("C")
                .verifyComplete();
    }

    @Test
    public void FluxTestElementsWithError() {
        Flux<String> stringFlux = Flux.just("A", "B", "C")
                .concatWith(Flux.error(new RuntimeException("Error")));

        StepVerifier.create(stringFlux)
                .expectNext("A")
                .expectNext("B")
                .expectNext("C")
                .expectErrorMessage("Error")
                //.expectError(RuntimeException.class)
                .verify();
    }

    @Test
    public void FluxTestCountWithError() {
        Flux<String> stringFlux = Flux.just("A", "B", "C")
                .concatWith(Flux.error(new RuntimeException("Error")));

        StepVerifier.create(stringFlux)
                .expectNextCount(3)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    public void FluxTestIterable() {
        List<String> strings = Arrays.asList("A", "B", "C");
        Flux<String> stringFlux = Flux.fromIterable(strings)
                .log();

        StepVerifier.create(stringFlux)
                .expectNext("A", "B", "C")
                .verifyComplete();
    }

    @Test
    public void monoTest() {
        Mono<String> stringMono = Mono.just("A");

        StepVerifier.create(stringMono)
                .expectNext("A")
                .verifyComplete();
    }

    @Test
    public void monoTestError() {
        StepVerifier.create(Mono.error(new RuntimeException("Error")))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    public void monoJustOrEmpty() {
        Mono<String> stringMono = Mono.justOrEmpty(null);
        StepVerifier.create(stringMono)
                .verifyComplete();
    }

    @Test
    public void fluxErrorHandlerResume() {
        Flux<String> stringFlux = Flux.just("A", "B", "C")
                .concatWith(Flux.error(new RuntimeException("Erro")))
                .concatWith(Flux.just("D"))
                .onErrorResume(e -> {
                    return Flux.just("Error handled 1", "Error handled 2");
                });

        StepVerifier.create(stringFlux)
                .expectSubscription()
                .expectNext("A", "B", "C")
                .expectNext("Error handled 1", "Error handled 2")
                .verifyComplete();
    }

    @Test
    public void fluxErrorHandlerReturn() {
        Flux<String> stringFlux = Flux.just("A", "B", "C")
                .concatWith(Flux.error(new RuntimeException("Erro")))
                .concatWith(Flux.just("D"))
                .onErrorReturn("Default");

        StepVerifier.create(stringFlux)
                .expectSubscription()
                .expectNext("A", "B", "C")
                .expectNext("Default")
                .verifyComplete();
    }

    @Test
    public void fluxErrorHandlerMap() {
        Flux<String> stringFlux = Flux.just("A", "B", "C")
                .concatWith(Flux.error(new RuntimeException("Erro")))
                .concatWith(Flux.just("D"))
                .onErrorMap(CustomException::new);

        StepVerifier.create(stringFlux)
                .expectSubscription()
                .expectNext("A", "B", "C")
                .expectError(CustomException.class)
                .verify();
    }

    @Test
    public void fluxErrorHandlerMapWithRetry() {
        Flux<String> stringFlux = Flux.just("A", "B", "C")
                .concatWith(Flux.error(new RuntimeException("Erro")))
                .concatWith(Flux.just("D"))
                .onErrorMap(CustomException::new)
                .retry(2);

        StepVerifier.create(stringFlux)
                .expectSubscription()
                .expectNext("A", "B", "C")
                .expectNext("A", "B", "C")
                .expectNext("A", "B", "C")
                .expectError(CustomException.class)
                .verify();
    }

    @Test
    public void fluxErrorHandlerMapWithRetryBackOff() {
        Flux<String> stringFlux = Flux.just("A", "B", "C")
                .concatWith(Flux.error(new RuntimeException("Erro")))
                .concatWith(Flux.just("D"))
                .onErrorMap(CustomException::new)
                .retryBackoff(2, Duration.ofSeconds(3));

        StepVerifier.create(stringFlux.log())
                .expectSubscription()
                .expectNext("A", "B", "C")
                .expectNext("A", "B", "C")
                .expectNext("A", "B", "C")
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    public void infiniteFlux() throws InterruptedException {
        Flux<Long> infiniteFlux = Flux.interval(Duration.ofMillis(100))
                .log();

        infiniteFlux
                .subscribe(System.out::println);

        Thread.sleep(3000);
    }

    @Test
    public void finiteFluxTest() throws InterruptedException {
        Flux<Long> finiteFlux = Flux.interval(Duration.ofMillis(100))
                .take(3)
                .log();

        finiteFlux
                .subscribe(System.out::println);

        StepVerifier.create(finiteFlux)
                .expectSubscription()
                .expectNext(0L, 1L, 2L)
                .verifyComplete();
    }

    @Test
    public void backPressureTest() {
        Flux<Integer> integerFlux = Flux.range(1, 10)
                .log();

        StepVerifier.create(integerFlux)
                .expectSubscription()
                .thenRequest(1)
                .expectNext(1)
                .thenRequest(1)
                .expectNext(2)
                .thenCancel()
                .verify();
    }

    @Test
    public void backPressureSubscribe() {
        Flux<Integer> integerFlux = Flux.range(1, 10)
                .log();

        integerFlux.subscribe(
                element -> System.out.println("Element is " + element),
                error -> System.err.println("Error is " + error.getMessage()),
                () -> System.out.println("Completed")
                , subscription -> subscription.request(2));
    }

    @Test
    public void backPressureCancel() {
        Flux<Integer> integerFlux = Flux.range(1, 10)
                .log();

        integerFlux.subscribe(
                element -> System.out.println("Element is " + element),
                error -> System.err.println("Error is " + error.getMessage()),
                () -> System.out.println("Completed")
                , Subscription::cancel);
    }

    @Test
    public void backPressureCustom() {
        Flux<Integer> integerFlux = Flux.range(1, 10)
                .log();

        integerFlux.subscribe(new BaseSubscriber<Integer>() {
            @Override
            protected void hookOnNext(Integer value) {
                request(1);
                System.out.println("Received value is " + value);
                if(value == 4) {
                    cancel();
                }
            }
        });
    }

    @Test
    public void coldSubscriberTest() throws InterruptedException {
        Flux<Integer> integerFLux = Flux.range(1, 10)
                .delayElements(Duration.ofSeconds(1));
        integerFLux.subscribe(e -> System.out.println("Subscriber 1: " + e));
        Thread.sleep(2000);
        integerFLux.subscribe(e -> System.out.println("Subscriber 2: " + e));
        Thread.sleep(8000);
    }

    @Test
    public void hotSubscriberTest() throws InterruptedException {
        Flux<Integer> integerFLux = Flux.range(1, 10)
                .delayElements(Duration.ofSeconds(1));

        ConnectableFlux<Integer> connectableFlux = integerFLux.publish();
        connectableFlux.connect();
        connectableFlux.subscribe(e -> System.out.println("Subscriber 1 " + e));
        Thread.sleep(4000);
        connectableFlux.subscribe(e -> System.out.println("Subscriber 2 " + e));
        Thread.sleep(7000);
    }

    @Test
    public void testWithVirtualTime() {
        VirtualTimeScheduler.getOrSet();

        Flux<Long> longFlux = Flux.interval(Duration.ofSeconds(1))
                .take(5);

        StepVerifier.withVirtualTime(longFlux::log)
                .expectSubscription()
                .thenAwait(Duration.ofSeconds(5))
                .expectNext(0L, 1L, 2L, 3L, 4L)
                .verifyComplete();
    }
}