package no.shhsoft.kafka.auth;

import java.util.Set;

interface UserToGroupsFetcher {

    Set<String> fetchGroups(String username);

}
