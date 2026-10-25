package run.halo.moments.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.moments.Moment;

class DocumentConverterTest {

    private final ReactiveExtensionClient client = mock(ReactiveExtensionClient.class);

    private final ExternalUrlSupplier externalUrlSupplier = mock(ExternalUrlSupplier.class);

    private final DocumentConverter converter =
        new DocumentConverter(client, externalUrlSupplier);

    @BeforeEach
    void setUp() {
        when(externalUrlSupplier.get()).thenReturn(URI.create("http://localhost:8090"));
        when(client.fetch(User.class, "alice")).thenReturn(Mono.empty());
    }

    @Test
    void shouldMarkScheduledMomentAsUnpublished() {
        var haloDoc = converter
            .convert(moment(Instant.now().plus(2, ChronoUnit.HOURS)))
            .block();

        assertThat(haloDoc).isNotNull();
        assertThat(haloDoc.isPublished()).isFalse();
        assertThat(haloDoc.isExposed()).isTrue();
    }

    @Test
    void shouldMarkReleasedMomentAsPublished() {
        var haloDoc = converter
            .convert(moment(Instant.now().minus(2, ChronoUnit.HOURS)))
            .block();

        assertThat(haloDoc).isNotNull();
        assertThat(haloDoc.isPublished()).isTrue();
    }

    @Test
    void shouldMarkMomentWithoutReleaseTimeAsPublished() {
        var haloDoc = converter.convert(moment(null)).block();

        assertThat(haloDoc).isNotNull();
        assertThat(haloDoc.isPublished()).isTrue();
    }

    @Test
    void shouldNotExposePrivateMoment() {
        var moment = moment(Instant.now().minus(2, ChronoUnit.HOURS));
        moment.getSpec().setVisible(Moment.MomentVisible.PRIVATE);

        var haloDoc = converter.convert(moment).block();

        assertThat(haloDoc).isNotNull();
        assertThat(haloDoc.isExposed()).isFalse();
    }

    private static Moment moment(Instant releaseTime) {
        var moment = new Moment();
        var metadata = new Metadata();
        metadata.setName("moment-test");
        metadata.setCreationTimestamp(Instant.now());
        moment.setMetadata(metadata);
        var spec = new Moment.MomentSpec();
        var content = new Moment.MomentContent();
        content.setHtml("<p>hello</p>");
        spec.setContent(content);
        spec.setOwner("alice");
        spec.setApproved(true);
        spec.setVisible(Moment.MomentVisible.PUBLIC);
        spec.setReleaseTime(releaseTime);
        moment.setSpec(spec);
        return moment;
    }
}