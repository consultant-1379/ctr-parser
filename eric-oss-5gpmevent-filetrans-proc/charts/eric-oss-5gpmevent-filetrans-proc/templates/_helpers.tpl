{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.name" }}
  {{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.version" }}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.fullname" -}}
{{- if .Values.fullnameOverride -}}
  {{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
  {{- $name := default .Chart.Name .Values.nameOverride -}}
  {{- printf "%s" $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

Create image registry url
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.registryUrl" -}}
{{- if index .Values "imageCredentials" "eric-oss-5gpmevent-filetrans-proc" "registry" "url" -}}
{{- index .Values "imageCredentials" "eric-oss-5gpmevent-filetrans-proc" "registry" "url" -}}
{{- else if .Values.global.registry.url -}}
{{- print .Values.global.registry.url -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.chart" }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create image pull secrets for global (outside of scope)
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.pullSecret.global" -}}
{{- $pullSecret := "" -}}
{{- if .Values.global -}}
  {{- if .Values.global.pullSecret -}}
    {{- $pullSecret = .Values.global.pullSecret -}}
  {{- end -}}
  {{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{/*
Create image pull secret, service level parameter takes precedence
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.pullSecret" -}}
{{- $pullSecret := (include "eric-oss-5gpmevent-filetrans-proc.pullSecret.global" . ) -}}
{{- if .Values.imageCredentials -}}
  {{- if .Values.imageCredentials.pullSecret -}}
    {{- $pullSecret = .Values.imageCredentials.pullSecret -}}
  {{- end -}}
{{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{- define "eric-oss-5gpmevent-filetrans-proc.testImagePath" }}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := (index $productInfo "images" "eric-oss-5gpmevent-filetrans-procTest" "registry") -}}
    {{- $repoPath := (index $productInfo "images" "eric-oss-5gpmevent-filetrans-procTest" "repoPath") -}}
    {{- $name := (index $productInfo "images" "eric-oss-5gpmevent-filetrans-procTest" "name") -}}
    {{- $tag := (index $productInfo "images" "eric-oss-5gpmevent-filetrans-procTest" "tag") -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.url -}}
                {{- $registryUrl = .Values.global.registry.url -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if (index .Values "imageCredentials" "eric-oss-5gpmevent-filetrans-procTest") -}}
            {{- if (index .Values "imageCredentials" "eric-oss-5gpmevent-filetrans-procTest" "registry") -}}
                {{- if (index .Values "imageCredentials" "eric-oss-5gpmevent-filetrans-procTest" "registry" "url") -}}
                    {{- $registryUrl = (index .Values "imageCredentials" "eric-oss-5gpmevent-filetrans-procTest" "registry" "url") -}}
                {{- end -}}
            {{- end -}}
            {{- if not (kindIs "invalid" (index .Values "imageCredentials" "eric-oss-5gpmevent-filetrans-procTest" "repoPath")) -}}
                {{- $repoPath = (index .Values "imageCredentials" "eric-oss-5gpmevent-filetrans-procTest" "repoPath") -}}
            {{- end -}}
            {{- if not (kindIs "invalid" (index .Values "imageCredentials" "eric-oss-5gpmevent-filetrans-procTest" "name")) -}}
                {{- $name = (index .Values "imageCredentials" "eric-oss-5gpmevent-filetrans-procTest" "name") -}}
            {{- end -}}
            {{- if not (kindIs "invalid" (index .Values "imageCredentials" "eric-oss-5gpmevent-filetrans-procTest" "tag")) -}}
                {{- $tag = (index .Values "imageCredentials" "eric-oss-5gpmevent-filetrans-procTest" "tag") -}}
            {{- end -}}
        {{- end -}}
        {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
            {{- $repoPath = .Values.imageCredentials.repoPath -}}
        {{- end -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- printf "%s/%s%s:%s" $registryUrl $repoPath $name $tag -}}
{{- end -}}

{{/*
Timezone variable
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.timezone" }}
  {{- $timezone := "UTC" }}
  {{- if .Values.global }}
    {{- if .Values.global.timezone }}
      {{- $timezone = .Values.global.timezone }}
    {{- end }}
  {{- end }}
  {{- print $timezone | quote }}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.labels" }}
app.kubernetes.io/name: {{ include "eric-oss-5gpmevent-filetrans-proc.name" . }}
helm.sh/chart: {{ include "eric-oss-5gpmevent-filetrans-proc.chart" . }}
{{ include "eric-oss-5gpmevent-filetrans-proc.selectorLabels" . }}
app.kubernetes.io/version: {{ include "eric-oss-5gpmevent-filetrans-proc.version" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Return the fsgroup set via global parameter if it's set, otherwise 10000
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.fsGroup.coordinated" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.fsGroup -}}
      {{- if .Values.global.fsGroup.manual -}}
        {{ .Values.global.fsGroup.manual }}
      {{- else -}}
        {{- if eq .Values.global.fsGroup.namespace true -}}
          # The 'default' defined in the Security Policy will be used.
        {{- else -}}
          10000
      {{- end -}}
    {{- end -}}
  {{- else -}}
    10000
  {{- end -}}
  {{- else -}}
    10000
  {{- end -}}
{{- end -}}

{{/*
Selector labels
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.selectorLabels" -}}
app.kubernetes.io/name: {{ include "eric-oss-5gpmevent-filetrans-proc.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.serviceAccountName" -}}
  {{- if .Values.serviceAccount.create }}
    {{- default (include "eric-oss-5gpmevent-filetrans-proc.fullname" .) .Values.serviceAccount.name }}
  {{- else }}
    {{- default "default" .Values.serviceAccount.name }}
  {{- end }}
{{- end }}

{{/*
Create a user defined annotation
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.config-annotations" }}
  {{- if .Values.annotations -}}
    {{- range $name, $config := .Values.annotations }}
{{ $name }}: {{ tpl $config $ }}
    {{- end }}
  {{- end }}
{{- end}}

{{/*
Annotations for Product Name and Product Number (DR-D1121-064).
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.product-info" }}
ericsson.com/product-name: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productName | quote }}
ericsson.com/product-number: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productNumber | quote }}
ericsson.com/product-revision: {{regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end }}

{{/*
Create prometheus info
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.prometheus" -}}
prometheus.io/path: {{ .Values.prometheus.path | quote }}
prometheus.io/port: {{ .Values.service.port | quote }}
prometheus.io/scrape: {{ .Values.prometheus.scrape | quote }}
{{- end -}}

{{/*
Define the role reference for security policy
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.securityPolicy.reference" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.security -}}
      {{- if .Values.global.security.policyReferenceMap -}}
        {{ $mapped := index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" }}
        {{- if $mapped -}}
          {{ $mapped }}
        {{- else -}}
          default-restricted-security-policy
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

{{/*
Define the annotations for security policy
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.securityPolicy.annotations" -}}
# Automatically generated annotations for documentation purposes.
{{- end -}}

{{/*
Define Pod Disruption Budget value taking into account its type (int or string)
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.pod-disruption-budget" -}}
  {{- if kindIs "string" .Values.podDisruptionBudget.minAvailable -}}
    {{- print .Values.podDisruptionBudget.minAvailable | quote -}}
  {{- else -}}
    {{- print .Values.podDisruptionBudget.minAvailable | atoi -}}
  {{- end -}}
{{- end -}}

{{/*
Define upper limit for TerminationGracePeriodSeconds
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.terminationGracePeriodSeconds" -}}
  {{- if .Values.terminationGracePeriodSeconds -}}
    {{- toYaml .Values.terminationGracePeriodSeconds -}}
  {{- end -}}
{{- end -}}

{{/*
Define tolerations to comply with DR-D1120-060
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.tolerations" -}}
  {{- if .Values.tolerations -}}
    {{- toYaml .Values.tolerations -}}
  {{- end -}}
{{- end -}}

{{/*
Create a merged set of nodeSelectors from global and service level.
*/}}
{{- define "eric-oss-5gpmevent-filetrans-proc.nodeSelector" -}}
{{- $globalValue := (dict) -}}
{{- if .Values.global -}}
    {{- if .Values.global.nodeSelector -}}
      {{- $globalValue = .Values.global.nodeSelector -}}
    {{- end -}}
{{- end -}}
{{- if .Values.nodeSelector -}}
  {{- range $key, $localValue := .Values.nodeSelector -}}
    {{- if hasKey $globalValue $key -}}
         {{- $Value := index $globalValue $key -}}
         {{- if ne $Value $localValue -}}
           {{- printf "nodeSelector \"%s\" is specified in both global (%s: %s) and service level (%s: %s) with differing values which is not allowed." $key $key $globalValue $key $localValue | fail -}}
         {{- end -}}
     {{- end -}}
    {{- end -}}
    nodeSelector: {{- toYaml (merge $globalValue .Values.nodeSelector) | trim | nindent 2 -}}
{{- else -}}
  {{- if not ( empty $globalValue ) -}}
    nodeSelector: {{- toYaml $globalValue | trim | nindent 2 -}}
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
configmap volumemounts
*/}}
{{- define "eric-event-data-collector.volumeMounts" -}}
{{ if .Values.volumeMounts -}}
{{ .Values.volumeMounts -}}
{{ end -}}
{{ end -}}

{{/*
configmap volumes
*/}}
{{- define "eric-event-data-collector.volumes" -}}
{{ if .Values.volumes -}}
{{ .Values.volumes -}}
{{ end -}}
{{ end -}}
