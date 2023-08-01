#!/bin/bash

output_dir=$1
duration=$2
num_iter=$3
num_rows_list1=(2000 4000 8000 16000 32000 64000 128000 256000 512000)
num_rows_list2=(10000 40000 160000 640000 2560000 10240000 40960000)
num_partitions=448

mkdir -p ${output_dir}
for dataset in bra ebay; do 
  if [ ${dataset} = bra ]; then
    num_queries=132
    num_rows_list=${num_rows_list1[@]}
    div=1000
  else
    num_queries=108
    num_rows_list=${num_rows_list2[@]}
    div=10000
  fi
  for num_rows in ${num_rows_list}; do
    qaad_count=0
    qaad_sum="0.0"
    qaad_avg="0.0"
    for (( i=0; i<${num_iter}; i++ )); do
      bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2>&1
      bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions}
      tmp_file=${output_dir}/tmp-qaad-d-${dataset}-q-${num_queries}-r-${num_rows}-i-${i}
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
          yarn application -kill "$yarn_app_id" > /dev/null
        fi
      else
        time=$(bash /root/QaaD/scripts/get-job-time.sh)
        if [[ ${time:0:1} =~ [0-9] ]]; then
          qaad_sum=$(python -c "print(round(${qaad_sum} + ${time}, 1))")
        fi
        (( qaad_count++ ))
        kill ${sleep_proc_id}
      fi
    done
    if [[ $qaad_count -gt 0 ]]; then
      qaad_avg=$(python -c "print(round(${qaad_sum} / ${qaad_count}, 1))")
    else
      qaad_avg="0.0"
    fi
    power=$(python -c "from math import log2; print(int(log2(${num_rows}/${div})))")
    xaxis=$(python -c "print(2**${power})")
    echo "${xaxis} 2^{${power}} ${qaad_avg}" >> ${output_dir}/scalability-${dataset}-elapsedtime-p-${num_partitions}-q-${num_queries}-qaad.dat
    
    sparks_count=0
    sparks_sum="0.0"
    sparks_avg="0.0" 
    for (( i=0; i<${num_iter}; i++ )); do
      bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2>&1
      bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions}
      tmp_file=${output_dir}/tmp-sparks-d-${dataset}-q-${num_queries}-r-${num_rows}-i-${i}
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
          yarn application -kill "$yarn_app_id" > /dev/null
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
    power=$(python -c "from math import log2; print(int(log2(${num_rows}/${div})))")
    xaxis=$(python -c "print(2**${power})")
    echo "${xaxis} 2^{${power}} ${sparks_avg}" >> ${output_dir}/scalability-${dataset}-elapsedtime-p-${num_partitions}-q-${num_queries}-sparks.dat

    sparku_count=0
    sparku_sum="0.0"
    sparku_avg="0.0" 
    for (( i=0; i<${num_iter}; i++ )); do
      bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2>&1
      bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions}
      tmp_file=${output_dir}/tmp-sparku-d-${dataset}-q-${num_queries}-r-${num_rows}-i-${i}
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
          yarn application -kill "$yarn_app_id" > /dev/null
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
    power=$(python -c "from math import log2; print(int(log2(${num_rows}/${div})))")
    xaxis=$(python -c "print(2**${power})")
    echo "${xaxis} 2^{${power}} ${sparku_avg}" >> ${output_dir}/scalability-${dataset}-elapsedtime-p-${num_partitions}-q-${num_queries}-sparku.dat
    rm -rf ${output_dir}/tmp*
  done
  /root/QaaD/scripts/plot-scripts/scalability-plot.sh ${dataset} ${num_partitions} ${num_queries} ${output_dir}
done
