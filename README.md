# Event Data Collector Service

## Description
The **Event Data Collector** service parses events so that their data is readable by consuming applications. It works with Cell Trace Record (CTR) Recording Output Period (ROP) events that network nodes generate. These events are recorded in CTR ROP files. The process flow is as follows:

1. Processed CTR ROP files are added to a **Message Bus KF** input topic that stores the paths to the ROP files on a shared storage location.
2. The **Event Data Collector** consumes the CTR ROP file paths on the input topic, decodes the CTR events, and pushes the CTR events to the **Message Bus KF** output topic.   
  a. It references the **CM Mediator** service to filter by CTR event.   
  b. It references the relevant schemas in the **Schema Registry SR** service to serialize the CTR events.
  c. The **Message Bus KF** output topic receives the CTR events.

## Contact Information

The service guardians are:

- Prem Kumar B
- Donnacha Bushe (Technical Writer)
- Mohamed Ibrahim C

High-level decisions are made by:   

- PO: Mohamed Ibrahim C
- SPM: Kenneth O'Neill

The e-mail distribution list for the service is **coming soon**.

## Source Artifacts

- [Docker](https://gerrit.ericsson.se/plugins/gitiles/AIA/microservices/ctr-parser//master/Docker/)
- [Helm](https://gerrit.ericsson.se/plugins/gitiles/AIA/microservices/ctr-parser//master/charts/eric-event-data-collector)  

## Deployment Instructions

1. Secure an ADP environment from the ADP DEVENV group. For more information, see this [wiki page](
https://openalm.lmera.ericsson.se/plugins/mediawiki/wiki/adp/index.php/Main_Page).
1. Locate the helm chart from the [Helm Repository](https://arm.epk.ericsson.se/artifactory/proj-helm_aia-generic-local/releases/eric-event-data-collector/).
1. Follow the steps in the [Event Data Collector - Service Deployment Guide](https://adp.ericsson.se/marketplace/documentation/2a515e3ec58f19be78505f6890009795/Deployment%20Guide%20).



## Bug Tracker (JIRA)

Use the following [link](https://jira-nam.lmera.ericsson.se/projects/OSSBSS/summary).
