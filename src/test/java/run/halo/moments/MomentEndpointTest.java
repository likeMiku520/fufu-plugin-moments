package run.halo.moments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.moments.service.MomentService;

class MomentEndpointTest {

    @Test
    void shouldUpdateUpvoteOfMoment() {
        var service = mock(MomentService.class);
        when(service.updateUpvote("moment-test", 42)).thenReturn(Mono.just(Stats.builder()
            .upvote(42)
            .totalComment(0)
            .approvedComment(0)
            .build()));
        var client = WebTestClient.bindToRouterFunction(new MomentEndpoint(service).endpoint())
            .build();
        var request = new MomentUpvoteRequest();
        request.setUpvote(42);

        client.put().uri("/moments/moment-test/upvote").bodyValue(request).exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.upvote").isEqualTo(42);

        verify(service).updateUpvote("moment-test", 42);
    }

    @Test
    void shouldListTagsOfMomentsOfEveryOwner() {
        var service = mock(MomentService.class);
        when(service.listAllTags(any())).thenReturn(Flux.just("日常", "游戏", "三角洲行动"));
        var client = WebTestClient.bindToRouterFunction(new MomentEndpoint(service).endpoint())
            .build();

        client.get().uri("/tags").exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.length()").isEqualTo(3)
            .jsonPath("$[2]").isEqualTo("三角洲行动");

        var query = ArgumentCaptor.forClass(MomentQuery.class);
        verify(service).listAllTags(query.capture());
        assertThat(query.getValue().toListOptions().getFieldSelector().query().toString())
            .doesNotContain("spec.owner");
    }

    @Test
    void shouldFilterTagsByKeyword() {
        var service = mock(MomentService.class);
        when(service.listAllTags(any())).thenReturn(Flux.just("日常", "游戏", "三角洲行动"));
        var client = WebTestClient.bindToRouterFunction(new MomentEndpoint(service).endpoint())
            .build();

        client.get().uri("/tags?name=游戏").exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.length()").isEqualTo(1)
            .jsonPath("$[0]").isEqualTo("游戏");
    }
}