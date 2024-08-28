{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-event-data-collector.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-event-data-collector.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-event-data-collector.version" -}}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
DR-D1121-064
*/}}
{{- define "eric-event-data-collector.product-info" }}
ericsson.com/product-name: "eric-event-data-collector"
ericsson.com/product-number: "CXC 201 1896"
ericsson.com/product-revision: {{regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end }}

{{/*
Expand the host name of parser.
*/}}
{{- define "eric-event-data-collector.host" -}}
{{- printf "eps-%s-0.schema.default.svc.cluster.local" .Release.Name | trunc 24 -}}
{{- end -}}

{{/*
Expand the host name of schema-registry.
*/}}
{{- define "eric-event-data-collector.schemaRegistry.host" -}}
{{- printf "%s://%s:%s" .Values.ctrParser.schemaRegistry.serviceProtocol .Values.ctrParser.schemaRegistry.serviceName  .Values.ctrParser.schemaRegistry.port -}}
{{- end -}}

{{/*
Define the schema registry URL
*/}}
{{- define "eric-event-data-collector.schema-registry.url" -}}
{{-  printf "%s:%s" .Values.ctrParser.schemaRegistry.serviceName  .Values.ctrParser.schemaRegistry.port -}}
{{- end -}}

{{/*
configmap volumes
*/}}
{{- define "eric-event-data-collector.volumes" -}}
{{ if .Values.volumes -}}
{{ .Values.volumes -}}
{{ end -}}
{{ end -}}

{{/*
configmap volumemounts
*/}}
{{- define "eric-event-data-collector.volumeMounts" -}}
{{ if .Values.volumeMounts -}}
{{ .Values.volumeMounts -}}
{{ end -}}
{{ end -}}

{{/*
Create image pull secrets (DR-D1123-115)
*/}}
{{- define "eric-event-data-collector.pullSecrets" -}}
{{- if .Values.imageCredentials.pullSecret -}}
{{- print .Values.imageCredentials.pullSecret -}}
{{- else if .Values.global.pullSecret -}}
{{- print .Values.global.pullSecret -}}
{{- end -}}
{{- end -}}

{{/*
Support user defined labels (DR-D1121-068)
*/}}
{{- define "eric-event-data-collector.user-labels" }}
{{- if .Values.labels }}
{{ toYaml .Values.labels }}
{{- end }}
{{- end }}

{{/*
Create annotations for roleBinding. (DR-D1123-124)
*/}}
{{- define "eric-event-data-collector.securityPolicy.annotations" }}
ericsson.com/security-policy.name: "restricted/default"
ericsson.com/security-policy.privileged: "false"
ericsson.com/security-policy.capabilities: "N/A"
{{- end -}}

{{/*
Create roleBinding reference. (DR-D1123-124)
*/}}
{{- define "eric-event-data-collector.securityPolicy.reference" -}}
    {{- if .Values.global -}}
        {{- if .Values.global.security -}}
            {{- if .Values.global.security.policyReferenceMap -}}
              {{ $mapped := index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" }}
              {{- if $mapped -}}
                {{ $mapped }}
              {{- else -}}
                {{ $mapped }}
              {{- end -}}
            {{- else -}}
              default-restricted-security-policy
            {{- end -}}
        {{- else -}}
          default-restricted-security-policy
        {{- end -}}
    {{- else -}}
      default-restricted-security-policy
    {{- end -}}
{{- end -}}