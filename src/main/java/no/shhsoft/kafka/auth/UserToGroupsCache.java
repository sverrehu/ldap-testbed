package no.shhsoft.kafka.auth;

import no.shhsoft.time.SystemTimeProvider;
import no.shhsoft.time.TimeProvider;
import no.shhsoft.utils.cache.TimeoutCache;

import java.util.Set;
import java.util.function.Function;

public final class UserToGroupsCache
implements UserToGroupsMapper {

    private static final UserToGroupsCache INSTANCE = new UserToGroupsCache();
    static final long TTL = 10L * 60L * 1000L;
    static final long REFRESH_WHEN_LESS_THAN_MS = 30L * 1000L;
    private final TimeoutCache<String, Set<String>> cache;
    private final TimeProvider timeProvider;

    private UserToGroupsCache() {
        this(new SystemTimeProvider());
    }

    /**
     * Only intended to be used for unit tests.
     */
    UserToGroupsCache(final TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
        cache = new TimeoutCache<>(timeProvider);
    }

    public static UserToGroupsCache getInstance() {
        return INSTANCE;
    }


    @Override
    public void setGroupsForUser(final String userName, final Set<String> groups) {
        cache.put(userName, groups, timeProvider.currentTimeMillis() + TTL);
    }

    @Override
    public Set<String> getGroupsForUser(final String userName) {
        return cache.get(userName);
    }

    @Override
    public void fetchGroupsForUserIfNeeded(final String user, final Function<String, Set<String>> fetcher) {
        if (cache.getExpiresInMs(user) >= REFRESH_WHEN_LESS_THAN_MS) {
            return;
        }
        setGroupsForUser(user, fetcher.apply(user));
    }

}
