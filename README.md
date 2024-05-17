# data-caterer-example

![Data Catering](misc/logo/logo_landscape_banner.svg)

Data Caterer is a metadata driven test data management tool that aids in creating production like data across batch and 
event data systems. Run data validations to ensure your systems have ingested it as expected. Use the Java, Scala API, 
UI, or YAML files to help with setup or customisation that are all run via Docker.

This repo contains example Java and Scala API usage for Data Caterer.

## How

Check out any of [the Scala examples](src/main/scala/io/github/datacatering/plan) or 
[Java examples](src/main/java/io/github/datacatering/plan). If you are looking for more information, you can follow 
[**detailed documentation found here**](https://data.catering/setup/).

### Java

1. Create new Java class similar
   to [DocumentationJavaPlanRun.java](src/main/java/io/github/datacatering/plan/DocumentationJavaPlanRun.java)
   1. Needs to extend `io.github.datacatering.datacaterer.javaapi.api.PlanRun`

### Scala

1. Create new Scala class similar
   to [DocumentationPlanRun.scala](src/main/scala/io/github/datacatering/plan/DocumentationPlanRun.scala)
   1. Needs to extend `io.github.datacatering.datacaterer.api.PlanRun`

## Run

Requires:

- Docker

```shell
./run.sh
#check results under docker/sample/report/index.html folder
```

## Docker

Create your own Docker image via:

```shell
./gradlew clean build
docker build -t <my_image_name>:<my_image_tag> .
docker run -e PLAN_CLASS=io.github.datacatering.plan.DocumentationPlanRun -v ${PWD}/docs/run:/opt/app/data <my_image_name>:<my_image_tag>
#check results under docs/run folder
```

## Docker Compose

Run with own class from either Java or Scala API:

```shell
./gradlew clean build
cd docker
PLAN_CLASS=io.github.datacatering.plan.DocumentationPlanRun DATA_SOURCE=postgres docker-compose up -d datacaterer
```

[Details from docs](https://data.catering/get-started/docker/).  
Docker compose sample found under `docker` folder.

```shell
cd docker
docker-compose up -d datacaterer
```

Check result under [here](docker/data/custom).

Change to another data source via:

- postgres
- mysql
- cassandra
- solace
- kafka
- http

```shell
DATA_SOURCE=cassandra docker-compose up -d datacaterer
```

If you want to use a [different YAML plan](docker/data/custom/plan) for the data source, you can run:

```shell
PLAN=plan/postgres-multiple-tables DATA_SOURCE=postgres docker-compose up -d datacaterer
```

## Helm

```shell
helm install data-caterer ./data-caterer-example/helm/data-caterer
```

## Benchmarks

Base benchmark tests can be run via:

```shell
bash benchmark/run_benchmark.sh
```

Results can be found under [benchmark/results](benchmark/results).
