-- $Revision$
-- $Date$

CREATE TABLE ofSpacePersistenceData (
  objectId              VARCHAR2(255) NOT NULL,
  spaceId               VARCHAR2(255) NOT NULL,
  expirationDate        TIMESTAMP,
  xmlElement            XMLTYPE       NOT NULL,
  CONSTRAINT ofSpacePersistenceData_pk PRIMARY KEY (objectId)
);

CREATE TABLE ofSpacePersistenceDependencies (
  referrer              VARCHAR2(255) NOT NULL,
  reference             VARCHAR2(255) NOT NULL,
  spaceId               VARCHAR2(255) NOT NULL,
  CONSTRAINT ofSpacePersistenceDependencies_pk PRIMARY KEY (referrer, reference)
);

INSERT INTO ofVersion (name, version) VALUES ('spacePersistenceService', 0);