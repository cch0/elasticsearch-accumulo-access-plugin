# elasticsearch-accumulo-access-plugin

This repository contains the implementation of an Elasticsearch plugin for applying Accumulo's attribute-based-access-control (ABAC) to documents in Elasticsearch.

<br>

## Support for different Elasticsearch and JDK Versions

The CIDCD pipeline for this project can be configured to support multiple different Elasticsearch and JDK versions.
Each combination of Elasticsearch and JDK version will have its own plugin zip file created.

Based on your Elasticsearch version, you are free to choose the one that works best for your environment.

<br>

## Usage

See [usage](./docs/usage.md) for details.

<br>

## How To Build

<b>Prerequisites</b>

- Gradle: 8.x
- JDK: 17 or above

<br>

<b>Build</b>

```bash
./gradlew clean build
```

<br>

<b>Build Artifact</b>

Build artifact is located under `./build/distributions` directory.

```bash
>ls -l build/distributions

-rw-r--r--  1 xxx  staff  107036 Oct  9 07:59 accumulo-access-es_8.15.0-jdk_17-0.1.0.zip
```

<br>

## Release Artifacts

When a new Github release is created, plugin zip files will be included as part of the release assets.
Go to Release [page](https://github.com/Koverse/elasticsearch-accumulo-access-plugin/releases) and download the appropriate zip file.

<br>

<b>Artifact naming convention</b>

Plugin zip file has the following naming convention:


accumulo-access-es_`${elasticsearch_version}`-jdk_`${java_major_version}`-`${plugin_version}`.zip



<br>

## Installation

Copy the zip file to the Elasticsearch server, for example, `/plugins/accumulo-access.zip`.

Execute the command to install the plugin

```bash
>pwd
/usr/share/elasticsearch

>bin/elasticsearch-plugin install file:///plugins/accumulo-access.zip
```
<br>

## CICD Process

See [cicd](./docs/cicd.md) for details.

<br>

## Acknowledgement

This project is inspired by the following works
- [elasticsearch-accumulo-security](https://github.com/jstoneham/elasticsearch-accumulo-security) -
This repository from 2013 provides a possible solution brining Accumulo ABAC into Elasticsearch via custom plugin.
- [Let’s Write a Dang ElasticSearch Plugin](https://www.viget.com/articles/lets-write-a-dang-elasticsearch-plugin/) - This tutorial is an excellent resource for learning how to create an Elasticsearch plugin.


<br>
