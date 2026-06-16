DROP TABLE IF EXISTS event_tag;
DROP TABLE IF EXISTS event_journal;

CREATE TABLE IF NOT EXISTS event_journal (
    ordering BIGSERIAL,
    persistence_id VARCHAR(255) NOT NULL,
    sequence_number BIGINT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    writer VARCHAR(255) NOT NULL,
    write_timestamp BIGINT NOT NULL,
    adapter_manifest VARCHAR(255) NOT NULL,
    event_payload BYTEA NOT NULL,
    event_ser_id INTEGER NOT NULL,
    event_ser_manifest VARCHAR(255) NOT NULL,
    meta_payload BYTEA,
    meta_ser_id INTEGER,
    meta_ser_manifest VARCHAR(255),
    PRIMARY KEY(persistence_id, sequence_number)
);

CREATE UNIQUE INDEX event_journal_ordering_idx ON event_journal(ordering);

CREATE TABLE IF NOT EXISTS event_tag (
    event_id BIGINT,
    tag VARCHAR(255),
    PRIMARY KEY(event_id, tag),
    CONSTRAINT fk_event_journal
        FOREIGN KEY(event_id)
        REFERENCES event_journal(ordering)
        ON DELETE CASCADE
);

CREATE INDEX event_tag_tag_idx ON event_tag(tag);

DROP TABLE IF EXISTS snapshot;

CREATE TABLE IF NOT EXISTS snapshot (
    persistence_id VARCHAR(255) NOT NULL,
    sequence_number BIGINT NOT NULL,
    created BIGINT NOT NULL,
    snapshot_ser_id INTEGER NOT NULL,
    snapshot_ser_manifest VARCHAR(255) NOT NULL,
    snapshot_payload BYTEA NOT NULL,
    meta_payload BYTEA,
    meta_ser_id INTEGER,
    meta_ser_manifest VARCHAR(255),
    PRIMARY KEY(persistence_id, sequence_number)
);
