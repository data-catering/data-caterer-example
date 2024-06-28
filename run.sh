#!/usr/bin/env bash

data_caterer_version=$(grep dataCatererVersion gradle.properties | cut -d= -f2)
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

image_suffix="-basic"
additional_env_vars=""
if [[ "$curr_class_name" == *"Advanced"* ]]; then
  if [[ -z "$DATA_CATERING_API_KEY" ]]; then
    image_suffix=""
  else
    image_suffix="-trial"
    additional_env_vars=" -e API_KEY=$DATA_CATERING_API_KEY"
  fi
fi

echo "Building jar with plan run"
./gradlew clean build
if [[ $? -ne 0 ]]; then
  echo "Failed to build!"
  exit 1
fi

docker network create --driver bridge docker_default || true

echo "Running Data Caterer via docker, version: $data_caterer_version"
DOCKER_CMD=(
  docker run -p 4040:4040
  -v "$(pwd)/build/libs/data-caterer-example-0.1.0.jar:/opt/app/job.jar"
  -v "$(pwd)/docker/sample:/opt/app/data"
  -v "$(pwd)/docker/sample/tracking:/opt/app/record-tracking"
  -v "$(pwd)/docker/mount:/opt/app/mount"
  -v "$(pwd)/docker/data/custom:/opt/app/custom"
  -e "APPLICATION_CONFIG_PATH=/opt/app/custom/application.conf"
  -e "$full_class_name"
  -e "DEPLOY_MODE=client"
  -e "DRIVER_MEMORY=2g"
  -e "EXECUTOR_MEMORY=2g"
  "$additional_env_vars"
  --network "docker_default"
  datacatering/data-caterer"$image_suffix":"$data_caterer_version"
)

eval "${DOCKER_CMD[@]}"
if [[ $? != 0 ]]; then
  echo "Failed to run"
  exit 1
fi
echo "Finished!"
