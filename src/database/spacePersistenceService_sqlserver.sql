-- $Revision$
-- $Date$

CREATE TABLE ofSpacePersistenceData (
  objectId              NVARCHAR(255)   NOT NULL,
  spaceId               NVARCHAR(255)   NOT NULL,
  expirationDate        DATETIME,
  xmlElement            NVARCHAR(65535) NOT NULL,
  CONSTRAINT ofSpacePersistenceData_pk PRIMARY KEY (objectId)
);

CREATE TABLE ofSpacePersistenceDependencies (
  referrer              NVARCHAR(255) NOT NULL,
  reference             NVARCHAR(255) NOT NULL,
  spaceId               NVARCHAR(255) NOT NULL,
  CONSTRAINT ofSpacePersistenceDependencies_pk PRIMARY KEY (referrer, reference)
);

INSERT INTO ofVersion (name, version) VALUES ('spacePersistenceService', 0);