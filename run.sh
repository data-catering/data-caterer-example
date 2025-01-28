#!/usr/bin/env bash

DATA_CATERER_ENV_FILE="$HOME/.data-caterer-env"

data_caterer_version=$(grep dataCatererVersion gradle.properties | cut -d= -f2)
data_caterer_user=${DATA_CATERER_API_USER:-}
data_caterer_token=${DATA_CATERER_API_TOKEN:-}

echo "Checking for Data Caterer user and token..."
if [[ -f "$DATA_CATERER_ENV_FILE" ]]; then
  source "$DATA_CATERER_ENV_FILE"
else
  if [[ -z ${DATA_CATERER_API_USER} ]]; then
    read -p "Data Caterer user: " data_caterer_user
    echo "export DATA_CATERER_API_USER=$data_caterer_user" > "$DATA_CATERER_ENV_FILE"
  fi
  if [[ -z ${DATA_CATERER_API_TOKEN} ]]; then
    read -p "Data Caterer token: " -s data_caterer_token
    echo "export DATA_CATERER_API_TOKEN=$data_caterer_token" >> "$DATA_CATERER_ENV_FILE"
    echo
  fi
fi
source "$DATA_CATERER_ENV_FILE"

if [[ -s ".tmp_prev_class_name" ]]; then
  prev_class_name=$(cat .tmp_prev_class_name)
else
  prev_class_name='DocumentationPlanRun'
fi

if [[ $# -eq 0 ]]; then
  read -p "Class name of plan to run [$prev_class_name]: " class_name
  curr_class_name=${class_name:-$prev_class_name}
else
  curr_class_name=$1
fi

if [[ $curr_class_name == *".yaml"* ]]; then
  full_class_name="PLAN_FILE_PATH=/opt/app/custom/plan/$curr_class_name"
else
  full_class_name="PLAN_CLASS=io.github.datacatering.plan.$curr_class_name"
fi
echo -n "$curr_class_name" > .tmp_prev_class_name

echo "Building jar with plan run"
./gradlew clean build
if [[ $? -ne 0 ]]; then
  echo "Failed to build!"
  exit 1
fi

docker network create --driver bridge docker_default || true

echo "Running Data Caterer via docker, version: $data_caterer_version"
docker network inspect insta-infra_default >/dev/null 2>&1 || docker network create --driver bridge insta-infra_default --label com.docker.compose.network="default"
DOCKER_CMD=(
  docker run -p 4040:4040
  -v "$(pwd)/build/libs/data-caterer-example-0.1.0.jar:/opt/app/job.jar"
  -v "$(pwd)/docker/sample:/opt/app/data"
  -v "$(pwd)/docker/sample/tracking:/opt/app/record-tracking"
  -v "$(pwd)/docker/mount:/opt/app/mount"
  -v "$(pwd)/docker/data/custom:/opt/app/custom"
  -v "$(pwd)/docker/tmp:/tmp"
  -e "APPLICATION_CONFIG_PATH=/opt/app/custom/application.conf"
  -e "$full_class_name"
  -e "DEPLOY_MODE=client"
  -e "DRIVER_MEMORY=2g"
  -e "EXECUTOR_MEMORY=2g"
  -e "DATA_CATERER_API_USER=$DATA_CATERER_API_USER"
  -e "DATA_CATERER_API_TOKEN=$DATA_CATERER_API_TOKEN"
  --network "insta-infra_default"
  datacatering/data-caterer:"$data_caterer_version"
)

eval "${DOCKER_CMD[@]}"
if [[ $? != 0 ]]; then
  echo "Failed to run"
  exit 1
fi
echo "Finished!"
