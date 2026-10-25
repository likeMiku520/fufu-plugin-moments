package run.halo.moments;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import run.halo.app.extension.Metadata;

class DefaultQueryMomentPredicateResolverTest {

    private final DefaultQueryMomentPredicateResolver resolver =
        new DefaultQueryMomentPredicateResolver();

    @Test
    void shouldHideMomentBeforeReleaseTimeFromAnonymous() {
        var predicate = resolver.getPredicate().block();

        assertThat(predicate).isNotNull();
        assertThat(predicate.test(moment(Instant.now().plus(1, ChronoUnit.HOURS),
            true, Moment.MomentVisible.PUBLIC))).isFalse();
        assertThat(predicate.test(moment(Instant.now().minus(1, ChronoUnit.HOURS),
            true, Moment.MomentVisible.PUBLIC))).isTrue();
        assertThat(predicate.test(moment(null, true, Moment.MomentVisible.PUBLIC))).isTrue();
    }

    @Test
    void shouldKeepOtherVisibilityRules() {
        var predicate = resolver.getPredicate().block();

        assertThat(predicate).isNotNull();
        assertThat(predicate.test(moment(Instant.now().minus(1, ChronoUnit.HOURS),
            false, Moment.MomentVisible.PUBLIC))).isFalse();
        assertThat(predicate.test(moment(Instant.now().minus(1, ChronoUnit.HOURS),
            true, Moment.MomentVisible.PRIVATE))).isFalse();
    }

    @Test
    void shouldHideMomentBeforeReleaseTimeFromOwner() {
        var predicate = resolver.getPredicate()
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "")))
            .block();

        assertThat(predicate).isNotNull();
        assertThat(predicate.test(moment(Instant.now().plus(1, ChronoUnit.HOURS),
            true, Moment.MomentVisible.PUBLIC))).isFalse();
        assertThat(predicate.test(moment(Instant.now().plus(1, ChronoUnit.HOURS),
            true, Moment.MomentVisible.PRIVATE))).isFalse();
        assertThat(predicate.test(moment(Instant.now().minus(1, ChronoUnit.HOURS),
            true, Moment.MomentVisible.PRIVATE))).isTrue();
    }

    @Test
    void shouldFilterMomentsBeforeReleaseTimeInListOptions() {
        var listOptions = resolver.getListOptions().block();

        assertThat(listOptions).isNotNull();
        assertThat(listOptions.getFieldSelector()).isNotNull();
        assertThat(listOptions.getFieldSelector().query().toString())
            .contains("metadata.deletionTimestamp")
            .contains("spec.approved")
            .contains("spec.releaseTime");
    }

    private static Moment moment(Instant releaseTime, boolean approved,
        Moment.MomentVisible visible) {
        var moment = new Moment();
        var metadata = new Metadata();
        metadata.setName("moment-test");
        moment.setMetadata(metadata);
        var spec = new Moment.MomentSpec();
        spec.setContent(new Moment.MomentContent());
        spec.setOwner("alice");
        spec.setApproved(approved);
        spec.setVisible(visible);
        spec.setReleaseTime(releaseTime);
        moment.setSpec(spec);
        return moment;
    }
}