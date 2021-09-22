package no.shhsoft.kafka.auth;

import java.util.Set;
import java.util.function.Function;

public interface UserToGroupsMapper {

    void setGroupsForUser(String userName, Set<String> groups);

    /**
     * @return <code>null</code> if user is not found.
     */
    Set<String> getGroupsForUser(String userName);

    void fetchGroupsForUserIfNeeded(String user, Function<String, Set<String>> fetcher);

    void clear();

}
