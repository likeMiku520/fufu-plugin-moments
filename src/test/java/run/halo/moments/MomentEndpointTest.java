package run.halo.moments;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
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
}