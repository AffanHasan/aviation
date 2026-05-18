Feature: Fetch aircraft state vectors from OpenSky Network

  As an aviation data consumer
  I want to periodically fetch aircraft state vectors
  So that I can monitor aircraft positions in real time

  Scenario: Scheduler fetches state vectors every 5 minutes
    Given the OpenSky API is available
    And valid OAuth2 credentials are configured
    When the scheduler triggers a fetch
    Then the service should retrieve state vectors successfully
    And the response should contain valid aircraft data

  Scenario: Service handles API rate limiting gracefully
    Given the OpenSky API is available
    And valid OAuth2 credentials are configured
    When the API returns a 429 Too Many Requests response
    And the scheduler triggers a fetch
    Then the service should log a warning
    And the scheduler should continue running without crashing

  Scenario: Service authenticates using OAuth2 client credentials
    Given the OpenSky API is available
    And valid OAuth2 credentials are configured
    When the scheduler triggers a fetch
    Then the API request should include a valid Bearer token

  Scenario: Fetched state vectors are published to Kafka as Avro
    Given the OpenSky API is available
    And valid OAuth2 credentials are configured
    And Kafka is running
    When the scheduler triggers a fetch via the service
    Then the full state vector response should be published to the "aircraft.state.vectors" topic
    And the message should be Avro serialized with the response time as the key
