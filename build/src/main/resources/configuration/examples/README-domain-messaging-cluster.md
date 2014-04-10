# Domain Configuration for Messaging Cluster

The domain-messaging-cluster.xml contains an example of a managed domain configuration to create a messaging cluster with HA.
The messaging subsystem are configured in subsystems/messaging-cluster.xml

The cluster is composed of two nodes (server-one and server-two), each hosting 2 hornetq-servers (one live and one backup).
The live hornetq-server is backed up (using replication) by the backup hornetq-server _on the other server_.

Since the two servers have different profiles, they are inside two different server-groups (main-server-group for server-one, and other-server-group for server-two).


    +-----------------------+                             +-------------------------+
    |   main-server-group   |                             |   other-server-group    |
    | main-full-ha profile  |                             |  other-full-ha profile  |
    |                       |                             |                         |
    | +-------------------+ |                             | +---------------------+ |
    | |    server-one     | |                             | |     server-two      | |
    | |-------------------| |                             | |---------------------| |
    | |                   | |                             | |                     | |
    | | hornetq-server    | |                             | | hornetq-server      | |
    | |                   | |                             | |                     | |
    | |    liveA  <------------- messaging-group1 -------------> backupA          | |
    | |                   | |    (backup-group-name)      | |    (backup=true)    | |
    | |                   | |                             | |                     | |
    | |                   | |                             | |                     | |
    | |    backupB  <----------- messaging-group2 ------------> liveB             | |
    | |    (backup=true)  | |   (backup-group-name)       | |                     | |
    | |                   | |                             | |                     | |
    | +-------------------+ |                             | +---------------------+ |
    +-----------------------+                             +-------------------------+


No shared store:

      <shared-store>false</shared-store>

Live/backup pair grouped by `backup-group-name` and use replication through `mycluster` cluster-connection.


Distinct directories for all four hornetq-server:

     <journal-directory path="liveA/messagingjournal" />
     <paging-directory path="baliveAckupA/messagingpaging" />
     <bindings-directory path="liveA/messagingbindings" />
     <large-messages-directory path="liveA/messaginglargemessages" />
