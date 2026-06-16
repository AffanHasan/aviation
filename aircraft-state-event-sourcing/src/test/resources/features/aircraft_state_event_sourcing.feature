Feature: Aircraft state event sourcing

  Scenario: Consuming three aircraft state messages creates three persisted actors
    Given Kafka and PostgreSQL are running
    And the event-sourcing actor system is started
    When 3 sample aircraft state messages are consumed from the "aircraft.state.vectors" topic
    Then 3 aircraft state events are persisted in the database
    And 3 event sourced persistent actors exist in memory
    And each actor represents the latest aircraft state
