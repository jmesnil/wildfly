[1mdiff --git a/build/build.xml b/build/build.xml[m
[1mindex 381e517..b463d6c 100644[m
[1m--- a/build/build.xml[m
[1m+++ b/build/build.xml[m
[36m@@ -679,6 +679,8 @@[m
 [m
         <module-def name="org.hornetq">[m
             <maven-resource group="org.hornetq" artifact="hornetq-core"/>[m
[32m+[m[32m            <maven-resource group="org.hornetq" artifact="hornetq-commons"/>[m
[32m+[m[32m            <maven-resource group="org.hornetq" artifact="hornetq-journal"/>[m
             <maven-resource group="org.hornetq" artifact="hornetq-jms"/>[m
         </module-def>[m
 [m
[1mdiff --git a/build/src/main/resources/configuration/examples/subsystems/messaging-hornetq-colocated.xml b/build/src/main/resources/configuration/examples/subsystems/messaging-hornetq-colocated.xml[m
[1mindex d84d8a6..62aa0fb 100644[m
[1m--- a/build/src/main/resources/configuration/examples/subsystems/messaging-hornetq-colocated.xml[m
[1m+++ b/build/src/main/resources/configuration/examples/subsystems/messaging-hornetq-colocated.xml[m
[36m@@ -5,7 +5,6 @@[m
    <extension-module>org.jboss.as.messaging</extension-module>[m
    <subsystem xmlns="urn:jboss:domain:messaging:1.3">[m
        <hornetq-server>[m
[31m-           <clustered>true</clustered>[m
            <persistence-enabled>true</persistence-enabled>[m
            <shared-store>true</shared-store>[m
            <journal-type>ASYNCIO</journal-type>[m
[36m@@ -117,7 +116,6 @@[m
        </hornetq-server>[m
 [m
        <hornetq-server name="backup-server">[m
[31m-           <clustered>true</clustered>[m
            <persistence-enabled>true</persistence-enabled>[m
            <backup>true</backup>[m
            <shared-store>true</shared-store>[m
[1mdiff --git a/build/src/main/resources/configuration/subsystems/messaging.xml b/build/src/main/resources/configuration/subsystems/messaging.xml[m
[1mindex 85f5b7a..2ef664c 100644[m
[1m--- a/build/src/main/resources/configuration/subsystems/messaging.xml[m
[1m+++ b/build/src/main/resources/configuration/subsystems/messaging.xml[m
[36m@@ -85,7 +85,6 @@[m
    </subsystem>[m
    <supplement name="ha">[m
        <replacement placeholder="CLUSTERED">[m
[31m-           <clustered>true</clustered>[m
            <cluster-password>${jboss.messaging.cluster.password:CHANGE ME!!}</cluster-password>[m
        </replacement>[m
        <replacement placeholder="BROADCAST-GROUPS">[m
[1mdiff --git a/build/src/main/resources/docs/schema/jboss-as-messaging_1_3.xsd b/build/src/main/resources/docs/schema/jboss-as-messaging_1_3.xsd[m
[1mindex 56f5eb0..3f0aaea 100644[m
[1m--- a/build/src/main/resources/docs/schema/jboss-as-messaging_1_3.xsd[m
[1m+++ b/build/src/main/resources/docs/schema/jboss-as-messaging_1_3.xsd[m
[36m@@ -53,7 +53,6 @@[m
           </xs:documentation>[m
       </xs:annotation>[m
       <xs:all>[m
[31m-          <xs:element maxOccurs="1" minOccurs="0" name="clustered" type="xs:boolean" />[m
           <!-- no file system deployment in AS[m
           <xs:element maxOccurs="1" minOccurs="0" type="file-deployment-enabled"/>[m
            -->[m
[1mdiff --git a/build/src/main/resources/modules/org/hornetq/main/module.xml b/build/src/main/resources/modules/org/hornetq/main/module.xml[m
[1mindex be59f34..c4f3f33 100644[m
[1m--- a/build/src/main/resources/modules/org/hornetq/main/module.xml[m
[1m+++ b/build/src/main/resources/modules/org/hornetq/main/module.xml[m
[36m@@ -32,6 +32,7 @@[m
         <module name="javax.api"/>[m
         <module name="javax.jms.api" />[m
         <module name="org.jboss.jts"/>[m
[32m+[m[32m        <module name="org.jboss.logging"/>[m
         <module name="org.jboss.netty"/>[m
         <module name="javax.resource.api"/>[m
         <module name="org.jboss.jboss-transaction-spi"/>[m
[1mdiff --git a/build/src/main/resources/modules/org/hornetq/ra/main/module.xml b/build/src/main/resources/modules/org/hornetq/ra/main/module.xml[m
[1mindex a039282..59de1a7 100644[m
[1m--- a/build/src/main/resources/modules/org/hornetq/ra/main/module.xml[m
[1m+++ b/build/src/main/resources/modules/org/hornetq/ra/main/module.xml[m
[36m@@ -35,6 +35,7 @@[m
         <module name="org.hornetq"/>[m
         <module name="org.jboss.as.transactions"/>[m
         <module name="org.jboss.jboss-transaction-spi"/>[m
[32m+[m[32m        <module name="org.jboss.logging"/>[m
         <module name="javax.api"/>[m
         <module name="javax.jms.api" />[m
         <module name="org.jboss.jts"/>[m
