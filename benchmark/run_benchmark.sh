#!/usr/bin/env bash

data_caterer_version=$(grep dataCatererVersion gradle.properties | cut -d= -f2)
image_suffix="-basic"
default_job="io.github.datacatering.plan.benchmark.BenchmarkParquetPlanRun"
default_record_count="100000"
driver_memory="DRIVER_MEMORY=2g"
executor_memory="EXECUTOR_MEMORY=2g"
benchmark_result_file="benchmark/results/benchmark_results_${data_caterer_version}.txt"
num_runs=5
uname_out="$(uname -s)"
case "${uname_out}" in
    Darwin*)  sed_option="-E";;
    *)        sed_option="-r";;
esac
spark_query_execution_engines=("default" "gluten")  #"blaze"
data_sizes=(10000 100000 1000000)
job_names=("BenchmarkForeignKeyPlanRun" "BenchmarkJsonPlanRun" "BenchmarkParquetPlanRun") #"BenchmarkAdvancedKafkaPlanRun"

gluten_spark_conf="--conf spark.plugins=io.glutenproject.GlutenPlugin
--conf spark.memory.offHeap.enabled=true
--conf spark.memory.offHeap.size=1024mb
--conf spark.shuffle.manager=org.apache.spark.shuffle.sort.ColumnarShuffleManager"

echo "Benchmark run" > "$benchmark_result_file"
{
  echo "Data Caterer version: $data_caterer_version"
  echo "Date: $(date)"
  echo "System info:"
  docker info | grep -E "OSType|Architecture|CPUs|Total Memory"
  echo "Driver memory: $driver_memory"
  echo "Executor memory: $executor_memory"
  echo "Class name, Num records, Num run, Time taken (s)"
} >> "$benchmark_result_file"

run_docker() {
  for num_run in $(seq 1 $num_runs)
  do
    echo "Run $num_run of $num_runs"

    time_taken=$({
      command time docker run -p 4040:4040 \
        -v "$(pwd)/build/libs/data-caterer-example-0.1.0.jar:/opt/spark/jars/data-caterer.jar" \
        -v "$(pwd)/docker/sample:/opt/app/data" \
        -e "PLAN_CLASS=$1" \
        -e "RECORD_COUNT=$2" \
        -e "DEPLOY_MODE=client" \
        -e "$driver_memory" \
        -e "$executor_memory" \
        --network "docker_default" \
        datacatering/data-caterer"$image_suffix":"$data_caterer_version";
    } 2>&1 | tail -1 | sed "$sed_option" "s/^ +([0-9\.]+) real.*$/\1/")
    if [[ $1 == *BenchmarkForeignKeyPlanRun* ]]; then
      final_record_count=$(($2 * 5))
    else
      final_record_count=$2
    fi
    echo "$1, $final_record_count, $num_run, $time_taken" >> "$benchmark_result_file"
  done
}

echo "Building jar with plan run"
./gradlew clean build -q
if [[ $? -ne 0 ]]; then
  echo "Failed to build!"
  exit 1
fi

echo "Pulling image before starting benchmarks"
docker pull datacatering/data-caterer"$image_suffix":"$data_caterer_version"

echo "Running benchmarks"
echo "Running Spark query execution engine benchmarks"
#for spark_qe in "${spark_query_execution_engines[@]}"; do
#  echo "Running for Spark query execution engine: $spark_qe"
#  case "$spark_qe" in
#    gluten*) spark_conf=gluten_spark_conf;;
#    *) ;;
#  esac
#  run_docker "$default_job" "$default_record_count" "$spark_conf"
#done

echo "Running data size benchmarks"
for record_count in "${data_sizes[@]}"; do
  echo "Running for data size: $record_count"
  run_docker "$default_job" "$record_count"
done

echo "Running data sink benchmarks"
for job in "${job_names[@]}"; do
  echo "Running for job: $job"
  full_class_name="io.github.datacatering.plan.benchmark.$job"
  if [[ "$job" == *"Advanced"* ]]; then
    image_suffix=""
  fi
  run_docker "$full_class_name" "$default_record_count"
done

echo "Cleaning docker runs..."
docker ps -a | grep "datacatering/data-caterer-basic" | awk -F " " '{print $1}' | xargs docker rm
echo "Done!"

#configuration
#- generate data
#- generate schema and data
#- generate schema, data and validations
#- generate schema, data and validations with report

