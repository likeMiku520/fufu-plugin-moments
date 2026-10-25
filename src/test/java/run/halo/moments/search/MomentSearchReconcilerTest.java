package run.halo.moments.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import run.halo.app.extension.Metadata;
import run.halo.moments.Moment;

class MomentSearchReconcilerTest {

    @Test
    void shouldRequeueAtReleaseTimeWhenMomentIsScheduled() {
        var result = MomentSearchReconciler.requeueIfNotReleased(
            moment(Instant.now().plus(30, ChronoUnit.MINUTES)));

        assertThat(result.reEnqueue()).isTrue();
        assertThat(result.retryAfter()).isPositive();
        assertThat(result.retryAfter()).isLessThan(java.time.Duration.ofHours(1));
    }

    @Test
    void shouldNotRequeueWhenMomentIsAlreadyReleased() {
        var result = MomentSearchReconciler.requeueIfNotReleased(
            moment(Instant.now().minus(1, ChronoUnit.MINUTES)));

        assertThat(result.reEnqueue()).isFalse();
    }

    @Test
    void shouldNotRequeueWhenReleaseTimeIsAbsent() {
        var result = MomentSearchReconciler.requeueIfNotReleased(moment(null));

        assertThat(result.reEnqueue()).isFalse();
    }

    private static Moment moment(Instant releaseTime) {
        var moment = new Moment();
        var metadata = new Metadata();
        metadata.setName("moment-test");
        moment.setMetadata(metadata);
        var spec = new Moment.MomentSpec();
        spec.setContent(new Moment.MomentContent());
        spec.setOwner("alice");
        spec.setApproved(true);
        spec.setVisible(Moment.MomentVisible.PUBLIC);
        spec.setReleaseTime(releaseTime);
        moment.setSpec(spec);
        return moment;
    }
}