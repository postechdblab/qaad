#!/bin/bash

output_dir=$1
duration=$2
num_iter=$3
num_rows=0
num_partitions=448
batch_size_list1=(33 66 132 264 528)
batch_size_list2=(27 54 108 216 432)

mkdir -p ${output_dir}
for dataset in ebay bra; do
  if [ ${dataset} = bra ]; then
    num_queries=1056
    num_queries_per_param=33
    batch_size_list=${batch_size_list1[@]}
  else
    num_queries=864
    num_queries_per_param=27
    batch_size_list=${batch_size_list2[@]}
  fi
  for batch_size in ${batch_size_list}; do
    start_line=1
    delta=$(python -c "print(int(float(${batch_size}) / ${num_queries_per_param}))")
    end_line=$(expr ${start_line} + ${delta})
    last_line=$(python -c "print(int(float(${num_queries}) / ${num_queries_per_param}) + 1)")
    while [ "${end_line}" -le ${last_line} ]; do
      qaad_count=0
      qaad_sum="0.0"
      qaad_avg="0.0"
      for (( i=0; i<${num_iter}; i++ )); do
        bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2>&1
        bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions} > /dev/null 2>&1
        tmp_file=${output_dir}/tmp-qaad-d-${dataset}-b-${batch_size}-s-${start_line}-i-${i}
        result_file=${tmp_file}.log
        {
          bash /root/QaaD/scripts/run-qaad-yarn-batch.sh ${dataset} ${num_rows} ${num_partitions} ${batch_size} ${start_line} ${end_line} > ${result_file};
          echo "done" > ${output_dir}/tmp-qaad-d-${dataset}-b-${batch_size}-s-${start_line}-i-${i}
        } &
        spark_proc_id=$!
        {
          sleep ${duration}m; echo "timeout" > ${output_dir}/tmp-qaad-d-${dataset}-b-${batch_size}-s-${start_line}-i-${i}
        } &
        sleep_proc_id=$!
        wait -n ${spark_proc_id} ${sleep_proc_id}
        result=$(cat ${output_dir}/tmp-qaad-d-${dataset}-b-${batch_size}-s-${start_line}-i-${i})
        if [[ ${result} == "timeout" ]]; then
          kill ${spark_proc_id}
          yarn_app_id=$(grep -oP "application_\d{13}_\d{4}" ${result_file} | head -1)
          if [ -n "$yarn_app_id" ]; then
            yarn application -kill "$yarn_app_id" > /dev/null 2>&1
          fi
        else
          kill ${sleep_proc_id}
          if [[ -z ${result} ]]; then
            bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2>&1
            bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions} > /dev/null 2>&1
          else 
            (( qaad_count++ ))
            time=$(bash /root/QaaD/scripts/get-job-time.sh)
            if [[ ${time:0:1} =~ [0-9] ]]; then
              qaad_sum=$(python -c "print(round(${qaad_sum} + ${time}, 1))")
            fi
          fi
        fi
      done
      if [[ $qaad_count -gt 0 ]]; then
        qaad_avg=$(python -c "print(round(${qaad_sum} / ${qaad_count}, 1))")
      else
        qaad_avg="0.0"
      fi
      echo "${qaad_avg}" > ${output_dir}/qaad-${dataset}-r-${num_rows}-p-${num_partitions}-q-${num_queries}-b-${batch_size}-s-${start_line}.txt
      sparku_count=0
      sparku_sum="0.0"
      sparku_avg="0.0" 
      bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2>&1
      bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions} > /dev/null 2>&1
      for (( i=0; i<${num_iter}; i++ )); do
        tmp_file=${output_dir}/tmp-sparku-d-${dataset}-b-${batch_size}-s-${start_line}-i-${i}
        result_file=${tmp_file}.log
        {
          bash /root/QaaD/scripts/run-sparku-yarn-batch.sh ${dataset} ${num_rows} ${num_partitions} ${batch_size} ${start_line} ${end_line} > ${result_file};
          time=$(tail -n 1 ${result_file} | grep "Elapsed time" | cut -d' ' -f4)
          echo $time > ${output_dir}/tmp-sparku-d-${dataset}-b-${batch_size}-s-${start_line}-i-${i}
        } &
        spark_proc_id=$!
        {
          sleep ${duration}m; echo "timeout" > ${output_dir}/tmp-sparku-d-${dataset}-b-${batch_size}-s-${start_line}-i-${i}
        } &
        sleep_proc_id=$!
        wait -n ${spark_proc_id} ${sleep_proc_id}
        result=$(cat ${output_dir}/tmp-sparku-d-${dataset}-b-${batch_size}-s-${start_line}-i-${i})
        if [[ ${result} == "timeout" ]]; then
          kill ${spark_proc_id}
          yarn_app_id=$(grep -oP "application_\d{13}_\d{4}" ${result_file} | head -1)
          if [ -n "$yarn_app_id" ]; then
            yarn application -kill "$yarn_app_id" > /dev/null 2>&1
          fi
        else
          kill ${sleep_proc_id}
          if [[ -z ${result} ]]; then
            bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2>&1
            bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions} > /dev/null 2>&1
          else 
            (( sparku_count++ ))
            time=$(cat ${output_dir}/tmp-sparku-d-${dataset}-b-${batch_size}-s-${start_line}-i-${i})
            if [[ ${time:0:1} =~ [0-9] ]]; then
              sparku_sum=$(python -c "print(round(${sparku_sum} + ${time}, 1))")
            fi
          fi
        fi
      done
      if [[ $sparku_count -gt 0 ]]; then
        sparku_avg=$(python -c "print(round(${sparku_sum} / ${sparku_count}, 1))")
      else
        sparku_avg="0.0"
      fi
      echo "${sparku_avg}" > ${output_dir}/sparku-${dataset}-r-${num_rows}-p-${num_partitions}-q-${num_queries}-b-${batch_size}-s-${start_line}.txt
      rm -rf ${result_file}
      rm -rf ${output_dir}/tmp*
      start_line=$(expr ${start_line} + ${delta})
      end_line=$(expr ${start_line} + ${delta})
    done
    qaad_time=$(python /root/QaaD/scripts/batch.py 100 qaad ${dataset} ${batch_size} ${output_dir})
    echo "${batch_size} ${batch_size} ${qaad_time}" >> ${output_dir}/batchsize-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-qaad.dat
    sparku_time=$(python /root/QaaD/scripts/batch.py 100 sparku ${dataset} ${batch_size} ${output_dir})
    echo "${batch_size} ${batch_size} ${sparku_time}" >> ${output_dir}/batchsize-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-sparku.dat

  done

  qaad_time=$(cat ${output_dir}/_arrivalrate-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-q-${num_queries}-qaad.dat | awk '{print $3}')
  echo "${num_queries} ${num_queries} ${qaad_time}" >> ${output_dir}/batchsize-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-qaad.dat
  echo "${num_queries} ${num_queries} ${qaad_time}" > ${output_dir}/_batchsize-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-qaad.dat

  sparku_time=$(cat ${output_dir}/_arrivalrate-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-q-${num_queries}-sparku.dat | awk '{print $3}')
  echo "${num_queries} ${num_queries} ${sparku_time}" >> ${output_dir}/batchsize-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-sparku.dat
  echo "${num_queries} ${num_queries} ${sparku_time}" > ${output_dir}/_batchsize-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-sparku.dat

  sparks_time=$(python /root/QaaD/scripts/batch.py 100 sparks ${dataset} 0 ${output_dir})
  for batch_size in ${batch_size_list}; do
    echo "${batch_size} ${batch_size} ${sparks_time}" >> ${output_dir}/batchsize-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-sparks.dat
  done

  sparks_time=$(cat ${output_dir}/_arrivalrate-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-q-${num_queries}-sparks.dat | awk '{print $3}')
  echo "${num_queries} ${num_queries} ${sparks_time}" >> ${output_dir}/batchsize-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-sparks.dat
  echo "${num_queries} ${num_queries} ${sparks_time}" > ${output_dir}/_batchsize-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-sparks.dat

  /root/QaaD/scripts/plot-scripts/batchsize-plot.sh ${dataset} ${num_rows} ${num_partitions} ${output_dir}
done
