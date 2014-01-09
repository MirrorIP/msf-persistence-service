-- $Revision$
-- $Date$

CREATE TABLE ofSpacePersistenceData (
  objectId              VARCHAR(255)  NOT NULL,
  spaceId               VARCHAR(255)  NOT NULL,
  expirationDate        TIMESTAMP,
  xmlElement            TEXT          NOT NULL,
  PRIMARY KEY (objectId)
);

CREATE TABLE ofSpacePersistenceDependencies (
  referrer              VARCHAR(255)  NOT NULL,
  reference             VARCHAR(255)  NOT NULL,
  spaceId               VARCHAR(255)  NOT NULL,
  PRIMARY KEY (referrer, reference)
);

INSERT INTO ofVersion (name, version) VALUES ('spacePersistenceService', 0);