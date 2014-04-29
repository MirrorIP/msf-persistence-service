# MIRROR Persistence Service
The MIRROR Persistence Service adds an sophisticated persistence layer to the [MIRROR Spaces Framework][1]. The service is registered as component of the XMPP domain and therefore acts on the same level as the MIRROR Spaces Service. Whilst the Spaces Service is mandatory within the MIRROR Spaces Framework (MSF), the Persistence Service is optional.
In its current implementation, the service is realized as plug-in for the [Openfire XMPP server][2].

## Build
An documentation how to setup an development environment for Openfire is described [here][3]. A developer guide for Openfire plugins including information how to build it is available [here][4].

## Installation
The MIRROR Persistence Service requires Openfire 3.8 and depends on a Spaces Service of version 0.5 or higher.

To install the plugin perform the following steps:

1. Open the administration console of Openfire and click on the "Plugins" tab.
2. In the "Upload Plugin" section of the page, select the persistenceService.jar file and submit it by pressing on the "Upload Plugin" button.
3. After a few seconds, new entry "MIRROR Persistence Service" should show up in the plugins list. You can validate the installation by opening the info log ("Server" > "Server Manager" > "Logs" > "Info") , which should contain a line "Starting MIRROR Persistence Service.".
4. Connect the service with the MIRROR Spaces Service:
 * Open the new "MIRROR Spaces" tab and click on "Settings" to open the general settings for the service.
 * Select the checkbox "Connect to the MIRROR Persistence Service." in the persistence settings.
 * Submit the change by pressing the "Save" button

## Update
To update an existing installation follow steps 1) - 3) of the installation. Stored data will remain unaffected.

## Usage
The service can be configured using the Openfire administration console: "MIRROR Spaces" > "Settings" > "Persistence Settings"

The documentation of the service is available [online](https://github.com/MirrorIP/msf-persistence-service/wiki).

## License
The MIRROR Spaces Service is released under the [Apache License 2.0][5].

## Changelog

v0.3.0 -- April 2, 2014

* [NEW] Added an option to authorize the publisher of a data object to delete it as well. Requires the publisher to be stored in the data object.

v0.2.1 -- January 9, 2014

* [FIX] Fixed namespace used in packages.
* [FIX] Fixed error stanza format.

v0.2 -- October 2, 2013

* [NEW] Compatible with Openfire 3.8.x and MIRROR Spaces Service 0.6.
* [UPDATED] Changed plugin identifier.

v0.1 -- April 15, 2013

* Initial version.


  [1]: https://github.com/MirrorIP
  [2]: http://www.igniterealtime.org/projects/openfire/
  [3]: http://community.igniterealtime.org/docs/DOC-1020
  [4]: http://www.igniterealtime.org/builds/openfire/docs/latest/documentation/plugin-dev-guide.html
  [5]: http://www.apache.org/licenses/LICENSE-2.0.html
