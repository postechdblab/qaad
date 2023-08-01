#!/bin/bash

output_dir=$1
duration=$2
num_iter=$3
num_rows=0
num_partitions=448
arrival_rate_list=(1 10 100 1000)

mkdir -p ${output_dir}
for dataset in bra ebay; do
  if [ ${dataset} = bra ]; then
    num_queries=1056
  else
    num_queries=864
  fi
  bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2&>1
  bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions} > /dev/null 2&>1
  bash /root/QaaD/scripts/run-sparks-yarn-batch.sh ${dataset} ${num_rows} ${num_partitions} ${num_queries} ${output_dir} > /dev/null 2&>1
  qaad_avg=$(tail -n 1 ${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-qaad.dat | awk '{sum += $3} END {print sum}')
  sparku_avg=$(tail -n 1 ${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-sparku.dat | awk '{sum += $3} END {print sum}')
  for arrival_rate in ${arrival_rate_list[@]}; do
    output=$(python3.8 /root/QaaD/scripts/arrival-rate.py ${arrival_rate} ${sparku_avg} ${qaad_avg} ${output_dir}/sparks-${dataset}-batch.txt)
    sparks_time=$(echo "$output" | awk -F': ' '/sparks_response_time/{print $2}')
    sparku_time=$(echo "$output" | awk -F': ' '/sparku_response_time/{print $2}')
    qaad_time=$(echo "$output" | awk -F': ' '/qaad_response_time/{print $2}')
    echo "${arrival_rate} ${arrival_rate} ${qaad_time}" >> ${output_dir}/arrivalrate-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-q-${num_queries}-qaad.dat
    echo "${arrival_rate} ${arrival_rate} ${sparks_time}" >> ${output_dir}/arrivalrate-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-q-${num_queries}-sparks.dat
    echo "${arrival_rate} ${arrival_rate} ${sparku_time}" >> ${output_dir}/arrivalrate-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-q-${num_queries}-sparku.dat
    if [ ${arrival_rate} = 100 ]; then
      echo "${arrival_rate} ${arrival_rate} ${qaad_time}" > ${output_dir}/_arrivalrate-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-q-${num_queries}-qaad.dat
      echo "${arrival_rate} ${arrival_rate} ${sparks_time}" > ${output_dir}/_arrivalrate-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-q-${num_queries}-sparks.dat
      echo "${arrival_rate} ${arrival_rate} ${sparku_time}" > ${output_dir}/_arrivalrate-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-q-${num_queries}-sparku.dat
    fi
  done
  /root/QaaD/scripts/plot-scripts/arrivalrate-plot.sh ${dataset} ${num_rows} ${num_partitions} ${num_queries} ${output_dir}
done
