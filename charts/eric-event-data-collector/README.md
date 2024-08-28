# Chart Details
## Parser Volume
**Note:** The volumes/volume claims need to be provided by the user of this chart.
This chart does not provision any volumes/volume claims.

The example below shows how to mount multiple volumes:
````
volumes: |
  - name: eric-event-data-collector-pmdata
    nfs:
      server: 131.160.199.197
      path: "/pmdata"
  - name: event-data-collector-volume2
    persistentVolumeClaim:
      claimName: nfs2
volumeMounts: |
  - name: eric-event-data-collector-pmdata
    mountPath: "/ctr_data"
    subPath: 15k1rop
    readOnly: true
  - name: event-data-collector-volume2
    mountPath: "/other_data"
    subPath: some_location
    readOnly: false
````
