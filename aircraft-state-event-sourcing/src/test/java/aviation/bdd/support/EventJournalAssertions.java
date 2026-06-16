package aviation.bdd.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.assertj.core.api.Assertions;

public final class EventJournalAssertions {

    private static final int MAX_EVENT_WAIT_ITERATIONS = 120;
    private static final int EVENT_WAIT_MILLIS = 250;
    private static final String PERSISTENCE_ID_PREFIX = "aircraft-";

    private final TestcontainersSupport infrastructure;

    public EventJournalAssertions(final TestcontainersSupport infrastructure) {
        this.infrastructure = infrastructure;
    }

    public void waitForEvents(final int expectedCount) throws Exception {
        for (int i = 0; i < MAX_EVENT_WAIT_ITERATIONS; i++) {
            try (Connection conn = openConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM event_journal")) {
                rs.next();
                if (rs.getInt(1) >= expectedCount) {
                    return;
                }
            }
            Thread.sleep(EVENT_WAIT_MILLIS);
        }
        throw new AssertionError("Expected " + expectedCount + " events in event_journal but did not arrive in time");
    }

    public void assertEventsPersistedFor(final List<String> icao24s) throws Exception {
        try (Connection conn = openConnection()) {
            for (final String icao24 : icao24s) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                             "SELECT COUNT(*) FROM event_journal WHERE persistence_id = '" + PERSISTENCE_ID_PREFIX + icao24 + "'")) {
                    rs.next();
                    Assertions.assertThat(rs.getInt(1)).isEqualTo(1);
                }
            }
        }
    }

    public void truncate() throws Exception {
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE event_journal, event_tag RESTART IDENTITY");
        }
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(
                infrastructure.jdbcUrl(),
                infrastructure.databaseCredentials().user(),
                infrastructure.databaseCredentials().password());
    }
}
