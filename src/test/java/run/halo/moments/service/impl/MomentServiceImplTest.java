package run.halo.moments.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Counter;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.moments.Moment;
import run.halo.moments.exception.NotFoundException;

class MomentServiceImplTest {

    private static final String MOMENT_NAME = "moment-test";

    private static final String COUNTER_NAME = "moments.moment.halo.run/" + MOMENT_NAME;

    private final ReactiveExtensionClient client = mock(ReactiveExtensionClient.class);

    private final MomentServiceImpl momentService = new MomentServiceImpl(client);

    @Test
    void shouldCreateCounterWhenItIsAbsent() {
        when(client.fetch(Moment.class, MOMENT_NAME)).thenReturn(Mono.just(moment()));
        when(client.fetch(Counter.class, COUNTER_NAME)).thenReturn(Mono.empty());
        when(client.create(any(Counter.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        var stats = momentService.updateUpvote(MOMENT_NAME, 128).block();

        var counter = ArgumentCaptor.forClass(Counter.class);
        verify(client).create(counter.capture());
        verify(client, never()).update(any(Counter.class));
        assertThat(counter.getValue().getMetadata().getName()).isEqualTo(COUNTER_NAME);
        assertThat(counter.getValue().getUpvote()).isEqualTo(128);
        assertThat(stats).isNotNull();
        assertThat(stats.getUpvote()).isEqualTo(128);
    }

    @Test
    void shouldUpdateExistingCounter() {
        var existing = Counter.emptyCounter(COUNTER_NAME);
        existing.getMetadata().setVersion(3L);
        existing.setUpvote(1);
        existing.setTotalComment(5);
        when(client.fetch(Moment.class, MOMENT_NAME)).thenReturn(Mono.just(moment()));
        when(client.fetch(Counter.class, COUNTER_NAME)).thenReturn(Mono.just(existing));
        when(client.update(any(Counter.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        var stats = momentService.updateUpvote(MOMENT_NAME, 0).block();

        var counter = ArgumentCaptor.forClass(Counter.class);
        verify(client).update(counter.capture());
        verify(client, never()).create(any(Counter.class));
        assertThat(counter.getValue().getUpvote()).isZero();
        assertThat(stats).isNotNull();
        assertThat(stats.getUpvote()).isZero();
        assertThat(stats.getTotalComment()).isEqualTo(5);
    }

    @Test
    void shouldRejectNegativeUpvote() {
        assertThatThrownBy(() -> momentService.updateUpvote(MOMENT_NAME, -1).block())
            .isInstanceOf(ServerWebInputException.class);

        verify(client, never()).fetch(Moment.class, MOMENT_NAME);
    }

    @Test
    void shouldFailWhenMomentIsAbsent() {
        when(client.fetch(Moment.class, MOMENT_NAME)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> momentService.updateUpvote(MOMENT_NAME, 10).block())
            .isInstanceOf(NotFoundException.class);

        verify(client, never()).create(any(Counter.class));
    }

    private static Moment moment() {
        var moment = new Moment();
        var metadata = new Metadata();
        metadata.setName(MOMENT_NAME);
        moment.setMetadata(metadata);
        var spec = new Moment.MomentSpec();
        spec.setContent(new Moment.MomentContent());
        spec.setTags(Set.of());
        spec.setOwner("alice");
        moment.setSpec(spec);
        return moment;
    }
}