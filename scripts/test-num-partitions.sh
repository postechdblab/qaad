#!/bin/bash

output_dir=$1
duration=$2
num_iter=$3
num_rows=0
num_partitions_list=(56 112 224 448)

mkdir -p ${output_dir}
for dataset in bra ebay; do
  num_queries=108
  if [ ${dataset} = bra ]; then
    num_queries=132
  fi
  for num_partitions in ${num_partitions_list[@]}; do
    qaad_count=0
    qaad_sum="0.0"
    qaad_avg="0.0"
    for (( i=0; i<${num_iter}; i++ )); do
      bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2&>1
      bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions} > /dev/null 2&>1
      tmp_file=${output_dir}/tmp-qaad-d-${dataset}-q-${num_queries}-p-${num_partitions}-i-${i}
      result_file=${tmp_file}.log
      {
        bash /root/QaaD/scripts/run-qaad-yarn.sh ${dataset} ${num_rows} ${num_partitions} ${num_queries} > ${result_file};
        echo "done" > ${tmp_file}
      } &
      spark_proc_id=$!
      {
        sleep ${duration}m; echo "timeout" > ${tmp_file}
      } &
      sleep_proc_id=$!
      wait -n ${spark_proc_id} ${sleep_proc_id}
      result=$(cat ${tmp_file})
      if [[ ${result} == "timeout" ]]; then
        kill ${spark_proc_id}
        yarn_app_id=$(grep -oP "application_\d{13}_\d{4}" ${result_file} | head -1)
        if [ -n "$yarn_app_id" ]; then
          yarn application -kill "$yarn_app_id" > /dev/null 2&>1
        fi
      else
        (( qaad_count++ ))
        time=$(bash /root/QaaD/scripts/get-job-time.sh)
        if [[ ${time:0:1} =~ [0-9] ]]; then
          qaad_sum=$(python -c "print(round(${qaad_sum} + ${time}, 1))")
        fi
        kill ${sleep_proc_id}
      fi
    done
    if [[ $qaad_count -gt 0 ]]; then
      qaad_avg=$(python -c "print(round(${qaad_sum} / ${qaad_count}, 1))")
    else
      qaad_avg="0.0" # set avg to 0 if count is 0
    fi
    echo "${num_partitions} ${num_partitions} ${qaad_avg}" >> ${output_dir}/partitioning-${dataset}-elapsedtime-r-${num_rows}-q-${num_queries}-qaad.dat
    
    bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2&>1
    bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions} > /dev/null 2&>1
    sparks_count=0
    sparks_sum="0.0"
    sparks_avg="0.0" 
    for (( i=0; i<${num_iter}; i++ )); do
      tmp_file=${output_dir}/tmp-sparks-d-${dataset}-q-${num_queries}-p-${num_partitions}-i-${i}
      result_file=${tmp_file}.log
      {
        bash /root/QaaD/scripts/run-sparks-yarn.sh ${dataset} ${num_rows} ${num_partitions} ${num_queries} > ${result_file}
        time=$(tail -n 1 ${result_file} | grep "Elapsed time" | cut -d' ' -f4)
        echo $time > ${tmp_file}
      } &
      spark_proc_id=$!
      {
        sleep ${duration}m; echo "timeout" > ${tmp_file}
      } &
      sleep_proc_id=$!
      wait -n ${spark_proc_id} ${sleep_proc_id}
      result=$(cat ${tmp_file})
      if [[ ${result} == "timeout" ]]; then
        kill ${spark_proc_id}
        yarn_app_id=$(grep -oP "application_\d{13}_\d{4}" ${result_file} | head -1)
        if [ -n "$yarn_app_id" ]; then
          yarn application -kill "$yarn_app_id" > /dev/null 2>&1
        fi
      else
        (( sparks_count++ ))
        time=$(cat ${tmp_file})
        if [[ ${time:0:1} =~ [0-9] ]]; then
          sparks_sum=$(python -c "print(round(${sparks_sum} + ${time}, 1))")
        fi
        kill ${sleep_proc_id}
      fi
    done
    if [[ $sparks_count -gt 0 ]]; then
      sparks_avg=$(python -c "print(round(${sparks_sum} / ${sparks_count}, 1))")
    else
      sparks_avg="0.0"
    fi
    echo "${num_partitions} ${num_partitions} ${sparks_avg}" >> ${output_dir}/partitioning-${dataset}-elapsedtime-r-${num_rows}-q-${num_queries}-sparks.dat

    bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2&>1
    bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions} > /dev/null 2&>1
    sparku_count=0
    sparku_sum="0.0"
    sparku_avg="0.0" 
    for (( i=0; i<${num_iter}; i++ )); do
      tmp_file=${output_dir}/tmp-sparku-d-${dataset}-q-${num_queries}-p-${num_partitions}-i-${i}
      result_file=${tmp_file}.log
      {
        bash /root/QaaD/scripts/run-sparku-yarn.sh ${dataset} ${num_rows} ${num_partitions} ${num_queries} > ${result_file}
        time=$(tail -n 1 ${result_file} | grep "Elapsed time" | cut -d' ' -f4)
        echo $time > ${tmp_file}
      } &
      spark_proc_id=$!
      {
        sleep ${duration}m; echo "timeout" > ${tmp_file}
      } &
      sleep_proc_id=$!
      wait -n ${spark_proc_id} ${sleep_proc_id}
      result=$(cat ${tmp_file})
      if [[ ${result} == "timeout" ]]; then
        kill ${spark_proc_id}
        yarn_app_id=$(grep -oP "application_\d{13}_\d{4}" ${result_file} | head -1)
        if [ -n "$yarn_app_id" ]; then
          yarn application -kill "$yarn_app_id" > /dev/null 2>&1
        fi
      else
        (( sparku_count++ ))
        time=$(cat ${tmp_file})
        if [[ ${time:0:1} =~ [0-9] ]]; then
          sparku_sum=$(python -c "print(round(${sparku_sum} + ${time}, 1))")
        fi
        kill ${sleep_proc_id}
      fi
    done
    if [[ $sparku_count -gt 0 ]]; then
      sparku_avg=$(python -c "print(round(${sparku_sum} / ${sparku_count}, 1))")
    else
      sparku_avg="0.0"
    fi
    echo "${num_partitions} ${num_partitions} ${sparku_avg}" >> ${output_dir}/partitioning-${dataset}-elapsedtime-r-${num_rows}-q-${num_queries}-sparku.dat
    rm -rf ${output_dir}/tmp*
  done
  /root/QaaD/scripts/plot-scripts/partitioning-plot.sh ${dataset} ${num_rows} ${num_queries} ${output_dir}
done
