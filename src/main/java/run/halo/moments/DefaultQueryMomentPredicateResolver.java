package run.halo.moments;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.index.query.Condition;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.infra.AnonymousUserConst;

import java.security.Principal;
import java.time.Instant;
import java.util.function.Predicate;

import static run.halo.app.extension.index.query.Queries.and;
import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.index.query.Queries.isNull;
import static run.halo.app.extension.index.query.Queries.lessThan;
import static run.halo.app.extension.index.query.Queries.or;


/**
 * The default implementation of {@link ReactiveQueryMomentPredicateResolver}.
 *
 */
@Component
public class DefaultQueryMomentPredicateResolver implements ReactiveQueryMomentPredicateResolver {

    @Override
    public Mono<Predicate<Moment>> getPredicate() {
        Predicate<Moment> predicate = moment -> moment.isApproved()
            && !ExtensionUtil.isDeleted(moment)
            && moment.isReleased();
        Predicate<Moment> visiblePredicate = Moment::isPubliclyVisible;
        return currentUserName()
            .map(username -> predicate.and(
                visiblePredicate.or(moment -> username.equals(moment.getSpec().getOwner())))
            )
            .defaultIfEmpty(predicate.and(visiblePredicate));
    }

    @Override
    public Mono<ListOptions> getListOptions() {
        var listOptions = new ListOptions();
        var fieldQuery = and(
            isNull("metadata.deletionTimestamp"),
            equal("spec.approved", Boolean.TRUE),
            releasedQuery()
        );
        var visibleQuery = equal("spec.visible", Moment.MomentVisible.PUBLIC.name());
        return currentUserName()
            .map(username -> and(fieldQuery,
                or(visibleQuery, equal("spec.owner", username)))
            )
            .defaultIfEmpty(and(fieldQuery, visibleQuery))
            .map(query -> {
                listOptions.setFieldSelector(FieldSelector.of(query));
                return listOptions;
            });
    }

    /**
     * Builds the query that only matches moments whose release time has arrived. Moments
     * without release time are matched as well for compatibility with legacy data.
     *
     * @return the release time condition
     */
    static Condition releasedQuery() {
        return or(isNull("spec.releaseTime"),
            lessThan("spec.releaseTime", Instant.now().toString(), true));
    }

    Mono<String> currentUserName() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Principal::getName)
            .filter(name -> !AnonymousUserConst.isAnonymousUser(name));
    }
}
