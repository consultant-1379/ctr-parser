# Contributing to the Event Data Collector Service

This document describes how to contribute artifacts for the **Event Data Collector** service.

## Gerrit Project Details
**Event Data Collector** artifacts are stored in the following Gerrit Project: [AIA/microservices/ctr-parser](https://gerrit.ericsson.se/#/admin/projects/AIA/microservices/ctr-parser)

## Artifacts

### Source Artifacts

- [Docker](https://gerrit.ericsson.se/plugins/gitiles/AIA/microservices/ctr-parser/+/master/Docker/)
- [Helm](https://gerrit.ericsson.se/plugins/gitiles/AIA/microservices/ctr-parser/+/master/charts/eric-event-data-collector)

### Documents

  - *[Event Data Collector - Service Overview](https://adp.ericsson.se/marketplace/documentation/2a515e3ec58f19be78505f6890009795/Service%20Overview%20)*    
    - *Format:* asciidoc    
    - *Git Path:* `Documentation/Event_Data_Collector_ServiceOverview.adoc`
  - *[Event Data Collector - Deployment Guide](https://adp.ericsson.se/marketplace/documentation/2a515e3ec58f19be78505f6890009795/Deployment%20Guide%20)*    
    - *Format:* asciidoc    
    - *Git Path:* `Documentation/Event_Data_Collector_DeploymentGuide.adoc`

To update documents that are not listed, contact the service guardian mentioned in the [README.md](https://gerrit.ericsson.se/plugins/gitiles/AIA/microservices/ctr-parser/+/refs/heads/master/README.md)file.

## Contribution Workflow
1. The **contributor** updates the artifact in the local repository.
1. The **contributor** pushes the update to Gerrit for review.
1. The **contributor** invites the **service guardian** (mandatory) and **other relevant parties** (optional) to the Gerrit review, and makes no further changes to the document until it is reviewed.
1. The **service guardian** reviews the document and gives a code-review score.
The code-review scores and corresponding workflow activities are as follows:    
    - Score is +1        
        A **reviewer** is happy with the changes but approval is required from another reviewer.            
    - Score is +2        
        The **service guardian** accepts the change and ensures publication to Calstore and to the ADP marketplace occurs.            
    - Score is -1 or -2        
        The **service guardian** and the **contributor** align to determine when and how the change is published.     
