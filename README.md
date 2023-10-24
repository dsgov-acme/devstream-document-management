# dsgov-acme Document Management

Reusable document upload, scanning, and retrieval service.

This service provides an API to upload documents to a cloud storage bucket. It also supports
adding metadata to documents like description and document type.

Documents are uploaded into an "unscanned" bucket, then moved to either a "scanned" or "quarantined" bucket
based on results of a malware scan. Only documents that successfully pass a malware scan can be retrieved.

- [dsgov-acme Document Management](#dsgov-acme-document-management)
  - [Prerequisites](#prerequisites)
  - [Run Locally](#run-locally)
  - [Develop Locally](#develop-locally)
  - [Local Pull Request Readiness Check](#local-pull-request-readiness-check)
    - [Upload a document](#upload-a-document)
    - [Check document status](#check-document-status)
    - [Get download url](#get-download-url)
  - [Metadata](#metadata)
  - [Malware Scanning](#malware-scanning)
    - [Malware Database](#malware-database)
    - [Scanning Service](#scanning-service)
  - [GCloud Pub/Sub Emulator](#gcloud-pubsub-emulator)
  - [Google Cloud Storage Emulator](#google-cloud-storage-emulator)
  - [Configuration Parameters](#configuration-parameters)
    - [Helm](#helm)
      - [Postgres](#postgres)
      - [Network](#network)
      - [Environment Variables](#environment-variables)
    - [Gradle](#gradle)
      - [settings.gradle](#settingsgradle)
      - [gradle.properties](#gradleproperties)
      - [gradle-wrapper.properties](#gradle-wrapperproperties)


## Prerequisites

Make sure you have the following installed:

1. Java 11+
2. Docker
3. Setup and configure minikube (using [This setup](https://github.com/dsgov-acme/devstream-local-environment))

## Run Locally

**NOTE: For M1 Mac users, run this instead: `skaffold run -p m1-minikube`**
This will disable the ClamAV portion of the Kubernetes deployment because the base `clamav/clamav:stable` image is not designed to be run on non-`linux/amd64` environments and will cause a deployment failure.

1. To just spin up the service in `minikube`, run this command: `skaffold run`
2. [view docs](http://api.devstream.test/dm/swagger-ui/index.html)

## Develop Locally

1. In a standalone terminal, run: `skaffold dev`
2. You should eventually have console output similar to this:
![Skaffold Dev Log](docs/assets/skaffold-dev-log.png)
3. As you make code changes, Skaffold will rebuild the container image and deploy it to your local `minikube` cluster.
4. Once the new deployment is live, you can re-generate your Postman collection to test your new API changes!

To exit `skaffold dev`, in the terminal where you executed the command, hit `Ctrl + C`.

**NOTE: This will terminate your existing app deployment in minikube.**

## Local Pull Request Readiness Check

When a pull request is raised, the following commands are executed in Google Cloud Build to help with validating a pull request's readiness for merge:

```bash
# Java-app focused
./gradlew check

# Kubernetes manifest focused
skaffold render -p minikube --digest-source=tag
skaffold render -p dev --digest-source=tag
```

These commands can also be executed locally to have a quicker developer feedback loop! Take advantage of it.

### Upload a document

Make a **POST** request to the **/docs** endpoint. Request body should be `multipart/form-data`, with the uploaded
file in the `file` key. The service generates a **document_id**, save this for subsequent requests.

Request:

```bash
curl -F file=@thumb.jpg api.devstream.test/dm/docs
```

Response:

```json
{"document_id":"dbb28058-5208-4b34-a0ec-104667ff6978"}
```

### Check document status

Make a **GET** request to the **/docs/{document_id}/status** endpoint. This checks the status of the document to see
if malware scan is finished.

Request:

```bash
curl api.devstream.test/dm/docs/40a9fe1d-aeeb-4d55-94b8-ca8c251f6b0e/status
```

Response:

```json
{
"status": 200,
"message": "Document is available for download"
}
```

### Get download url

Make a **GET** request to the **/docs/{document_id}/url** endpoint. This creates a download url that is valid for a
limited time(also known as signed url).

Request:

```bash
curl api.devstream.test/dm/docs/40a9fe1d-aeeb-4d55-94b8-ca8c251f6b0e/url
```

Response:

```json
{
"expires":900,
"signed_url":"https://storage.googleapis.com/example-bucket/cat.jpeg?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=example%40example-project.iam.gserviceaccount.com"}
```

* **expires**: time in seconds before the download link expires.
* **signed_url**: download link for file.

## Metadata

Metadata can be added to a document during upload, either using form-data keys or query parameters.
Metadata is stored using Cloud Storage Object Metadata, simple key-value pairs of strings.
You can retrieve metadata by making a **GET** request on the **/docs/{document_id}/metadata** endpoint.

Example Response:

```json
{
"groupId": "",
"documentCategory": "",
"documentType": "",
"uploadedBy": "",
"uploaderDescription": "",
"agentDescription": ""
}
```

Refer to API documentation in [openapi.yaml](swagger.yaml) for details about each metadata field.

## Malware Scanning

(Mac M1 users please check [Run Locally](#run-locally) section)

The malware scan is performed with a local service based on [ClamAV](https://www.clamav.net/), a trusted and open source solution led by Cisco.\
It is adapted from a [Google Cloud Design](https://cloud.google.com/architecture/automate-malware-scanning-for-documents-uploaded-to-cloud-storage) to work well in the context of dsg-document-management, and works with K8s PersistentStorage to achieve cloud portability.

The architecture for the malware database and service is shown in the following diagram:\
![malwaredb](docs/assets/malwaredb.png)

### Malware Database

The malware database is checked for updates every hour, but only database increments are downloaded if present to make good use of network resources.

Running document-management for the first time may take longer than usual for the malware database initialization (around 2 minutes with a 300Mb download connection), but once the first malware database signatures are persisted during the first run, subsequent starts have no delay related to it, since malware scanning service will run immediately based on existing signatures and perform an incremental update concurrently on application start.

For testing it is recommended to change the `clamav.database.hourlyRefreshMinute` [config](helm/document-management/values.local.yaml) to a value of "*" instead of a number, in order to trigger the CronJobs every minute instead of every hour, and doing so being able to test that the initialization Job and the updater CronJob pods do not present errors in attempting the database update at the same time.

The K8s node can be explored in the /mnt/clamavdb path, which should be empty or non-existing before app start, and is going to be used to persist the malware signatures database.

It is also recommended to explore the logs of the temporally preserved initialization Job and CronJobs pods to see the successful execution trace of the clamav updater.

### Scanning Service

The actual ClamAV scanner process listens on a TCP socket from port 3310, and files can be streamed to its ClamAV daemon which has malware signatures preloaded in memory for fast checking.
It is worth noting that the clamav-service container requires around 4GB of memory due to those preload signatures, and for quick database verification and swapping during signatures updating ([ref](https://docs.clamav.net/manual/Installing/Docker.html#memory-ram-requirements)).

The malware scanning of files can then be tested manually from a different container, with the ClamDScan binary configured to run against the TCP socket daemon.

The following script can achieve that testing from the local-document-management container:

```bash
mkdir /testingdir
cd /testingdir
printf "TCPSocket 3310 \nTCPAddr localhost \n" > config
apt update
apt install libclamav9 -y
apt download clamdscan # download package with no dependencies
dpkg -i clamdscan*.deb # (this last command will report some errors that are fine)

# downloading test files:
# getting a healthy png file
wget https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_92x30dp.png -O image.png

# getting safe malware tester files
wget https://secure.eicar.org/eicar.com.txt
wget https://secure.eicar.org/eicar_com.zip
wget https://secure.eicar.org/eicarcom2.zip

# testing the image file
clamdscan -c config --stream image.png

# then test the other files with:
# clamdscan -c config --stream [THE-FILE-YOU-WANT-TO-TEST]
```

This kind of scanning is performed by document-management when files are uploaded.


## GCloud Pub/Sub Emulator
This service includes a Google Cloud Pub/Sub Emulator which is also used for malware scanning in isolated testing environments and minikube, but can be tried with the following scripts, inside a document-management container.

You need to open two terminals to the same pod:
1. In the first terminal run:
```bash
# First terminal
apt update
apt install git python3 python3-pip -y
pip install virtualenv
mkdir pubsubtester
cd pubsubtester
git clone https://github.com/googleapis/python-pubsub.git .
cd samples/snippets
virtualenv venv
source venv/bin/activate
pip install -r requirements.txt
export PUBSUB_EMULATOR_HOST=pubsub-emulator-service:8085
python publisher.py devstream-dev create test-topic
python publisher.py devstream-dev publish test-topic
```
2. After completing the previous command execution, in the second terminal run:
```bash
# Second Terminal
cd pubsubtester/samples/snippets
virtualenv venv
source venv/bin/activate
export PUBSUB_EMULATOR_HOST=pubsub-emulator-service:8085
python subscriber.py devstream-dev create test-topic test-subscription
python subscriber.py devstream-dev receive test-subscription
```

3. Go back to first terminal and run:
```bash
# Firts terminal again (pretty much just run the very last command):
python publisher.py devstream-dev publish test-topic
```
You should see how messages published from first terminal are shown in the second.

* We've found sometimes Minikube has trouble pulling the PubSub emulator image from Google Cloud registry.\
If there are ImagePullBackOff errors in Minikube the following steps solve it:
```bash
docker pull gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators
minikube image load gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators
```

## Google Cloud Storage Emulator
Local and ephemeral environments count with Storage Buckets service, that can be either ephemeral or persistent based on the `.Values.gcloudStorageEmulator.persistent` boolean value.\
Those buckets are exposed for this and other services at the cluster internal URL `http://gcloud-storage-emulator-service.devstream:4443/storage/v1/b`, which can be queried for existing buckets with a single `curl` call to that URL from any pod.

## Configuration Parameters
Here are the key configuration parameters for the application:
### Helm

#### Postgres
- POSTGRES_HOST: `<db-host-instance-name>`
- POSTGRES_DB: `<db-name>`
- POSTGRES_PASSWORD: `<db-password>`
- POSTGRES_PORT: `<db-port>`
- POSTGRES_USER: `<db-user>`

#### Network
- host: `<api-domain-name>`
- applicationPort: `<k8s-application-container-port>`
- servicePort: `<k8s-service-port>`
- contextPath: `<k8s-ingress-context-path>`
- readinessProbe.path: `<k8s-readiness-probe-path>`

#### Environment Variables
- CLAMAV_ENABLED: `<bool>`
- DOCUMENT_PROCESSING_ENABLED: `<bool>`
- GOOGLE_STORAGE_EMULATED: `<bool>`
- PUBSUB_EMULATOR_HOST: `<bool>`
- PUBSUB_ENABLED: `<bool>`
- DOCUMENT_QUALITY_PROCESSOR_ID: `<document-quality-processor-id>`
- DOCUMENT_ID_PROOFING_PROCESSOR_ID: `<document-id-proofing-processor-id>`
- ALLOWED_ORIGINS: `<allowed-origins>`
- CERBOS_URI: `<cerbos-uri>`
- DB_CONNECTION_URL: `<db-connection-url>`
- DB_USERNAME: `<db-username>`
- DB_PASSWORD: `<db-password>`
- GCP_PROJECT_ID: `<gcp-project-id>`
- SELF_SIGN_PUBLIC_KEY: `<secret-manager-path-to-rsa-public-key>`
- SELF_SIGN_PRIVATE_KEY: `<secret-manager-path-to-rsa-private-key>`
- TOKEN_PRIVATE_KEY_SECRET: `<token-private-key-secret-name>`
- TOKEN_PRIVATE_KEY_VERSION: `<token-private-key-secret-version>`
- TOKEN_ISSUER: `<token-issuer-name>`
- USER_MANAGEMENT_BASE_URL: `<user-management-base-url>`
- STORAGE_PROVIDER: `<storage-provider>`
- UNSCANNED_BUCKET_NAME: `<unscanned-bucket-name>`
- QUARANTINED_BUCKET_NAME: `<quarantined-bucket-name>`
- SCANNED_BUCKET_NAME: `<scanned-bucket-name>`
- PUBSUB_CLAMAV_TOPIC: `<pubsub-clamav-topic-path>`
- PUBSUB_DOCUMENT_PROCESSING_TOPIC: `<pubsub-document-processing-topic-path>`
- PUBSUB_DOCUMENT_PROCESSING_RESULT_TOPIC: `<pubsub-document-processing-result-topic-path>`
- PUBSUB_CLAMAV_SUBSCRIPTION: `<pubsub-clamav-subscription-path>`
- PUBSUB_DOCUMENT_PROCESSING_SUBSCRIPTION: `<pubsub-document-processing-subscription-path>`
- ALLOWED_MIME_TYPES: `<list-of-allowed-mime-types>`
- ALLOWED_OCTET_STREAM_EXTENSIONS: `<list-of-allowed-octet-stream-extensions>`
- OTEL_SAMPLER_PROBABILITY: `<opentelemetry-sampler-probability`


### Gradle

#### settings.gradle
- rootProject.name = `<project-name>`

#### gradle.properties
profile=`<profile-name>`

#### gradle-wrapper.properties
- distributionBase=`<distribution-base>`
- distributionPath=`<distribution-path>`
- distributionUrl=`<distribution-url>`
- zipStoreBase=`<zip-store-base>`
- zipStorePath=`<zip-store-path>`
