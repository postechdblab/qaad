#!/bin/bash

output_dir=$1
duration=$2
num_iter=$3
num_rows=0
num_partitions=448
num_queries_list1=(33 66 132 264 528 1056)
num_queries_list2=(27 54 108 216 432 864)

mkdir -p ${output_dir}
for dataset in bra ebay; do
  if [ ${dataset} = bra ]; then
    num_queries_list=${num_queries_list1[@]}
  else
    num_queries_list=${num_queries_list2[@]}
  fi
  for num_queries in ${num_queries_list}; do
    if [ ${dataset} = bra ]; then
      adaptdb_count=0
      adaptdb_sum="0.0"
      adaptdb_avg="0.0"
      for (( i=0; i<${num_iter}; i++ )); do
        num_params=$(python -c "print(int(${num_queries} / 33))")
        bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2>&1
        bash /root/QaaD/src/AdaptDB/scripts/fabfile/run-workload.sh ${i} ${num_params} > /dev/null 2>&1
        time=$(grep "Elapsed time:" /root/QaaD/src/AdaptDB/logs/${dataset}_workload_q_${num_params}_${i}.log | awk '{sum += $5} END {print sum}')
        if [[ ${time:0:1} =~ [0-9] ]]; then
          adaptdb_sum=$(python -c "print(round(${adaptdb_sum} + ${time}, 1))")
        else
          (( i-- ))
          continue
        fi
        (( adaptdb_count++ ))
      done
      if [ $adaptdb_count -gt 0 ]; then
        adaptdb_avg=$(python -c "print(round(${adaptdb_sum} / ${adaptdb_count}, 1))")
      else
        adaptdb_avg="0.0"
      fi
      echo "${num_queries} ${num_queries} ${adaptdb_avg}" >> ${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-adaptdb.dat
      rm -rf /root/QaaD/src/AdaptDB/logs/*

      smartplan_count=0
      smartplan_sum=0
      smartplan_avg="0.0"
      for (( i=0; i<${num_iter}; i++ )); do
        bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2>&1
        bash /root/QaaD/scripts/run-gen-opt-partitions.sh ${dataset} ${num_rows} ${num_partitions}
        tmp_file=${output_dir}/tmp-smartplan-d-${dataset}-q-${num_queries}-i-${i}
        result_file=${tmp_file}.log
        bash /root/QaaD/scripts/run-sparks-yarn.sh ${dataset} ${num_rows} ${num_partitions} ${num_queries} > ${result_file}
        num_params=$(python -c "print(int(${num_queries} / 33))")
        time=$(tail -n 1 ${result_file} | grep "Elapsed time" | cut -d' ' -f4)
        if [[ ${time:0:1} =~ [0-9] ]]; then
          smartplan_sum=$(python -c "print(round(${smartplan_sum} + ${time}, 1))")
        else
          (( i-- ))
          continue
        fi
        (( smartplan_count++ ))
      done
      if [ $smartplan_count -gt 0 ]; then
        smartplan_avg=$(python -c "print(round(${smartplan_sum} / ${smartplan_count}, 1))")
      else
        smartplan_avg="0.0"
      fi
      echo "${num_queries} ${num_queries} ${smartplan_avg}" >> ${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-smartplan.dat

      copart_count=0
      copart_sum="0.0"
      copart_avg="0.0"
      for (( i=0; i<${num_iter}; i++ )); do
        bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2>&1
        bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions}
        tmp_file=${output_dir}/tmp-copart-d-${dataset}-q-${num_queries}-i-${i}
        result_file=${tmp_file}.log
        {
          bash /root/QaaD/scripts/run-copart-yarn.sh ${dataset} ${num_rows} ${num_partitions} ${num_queries} > ${result_file};
          echo "done" > ${tmp_file}
        } &
        spark_proc_id=$!
        {
          sleep ${duration}m; echo "timeout" > ${tmp_file}
        } &
        sleep_proc_id=$!
        wait -n ${spark_proc_id} ${sleep_proc_id}
        result=$(cat ${tmp_file})
        if [ ${result} == "timeout" ]; then
          kill ${spark_proc_id}
          yarn_app_id=$(grep -oP "application_\d{13}_\d{4}" ${result_file} | head -1)
          if [ -n "$yarn_app_id" ]; then
            yarn application -kill "$yarn_app_id" > /dev/null 2>&1
          fi
        else
          kill ${sleep_proc_id} > /dev/null 2>&1
          if [ -z ${result} ]; then
            bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2>&1
            bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions}
          else 
            time=$(bash /root/QaaD/scripts/get-job-time.sh)
            if [[ ${time:0:1} =~ [0-9] ]]; then
              copart_sum=$(python -c "print(round(${copart_sum} + ${time}, 1))")
            else
              (( i-- ))
              continue
            fi
            (( copart_count++ ))
          fi
        fi
      done
      if [ $copart_count -gt 0 ]; then
        copart_avg=$(python -c "print(round(${copart_sum} / ${copart_count}, 1))")
      else
        copart_avg="0.0"
      fi
      echo "${num_queries} ${num_queries} ${copart_avg}" >> ${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-copart.dat
    fi

    mid_count=0
    mid_sum="0.0"
    mid_avg="0.0"
    for (( i=0; i<${num_iter}; i++ )); do
      bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2>&1
      bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions}
      tmp_file=${output_dir}/tmp-mid-d-${dataset}-q-${num_queries}-i-${i}
      result_file=${tmp_file}.log
      {
        bash /root/QaaD/scripts/run-mid-yarn.sh ${dataset} ${num_rows} ${num_partitions} ${num_queries} > ${result_file};
        echo "done" > ${tmp_file}
      } &
      spark_proc_id=$!
      {
        sleep ${duration}m; echo "timeout" > ${tmp_file}
      } &
      sleep_proc_id=$!
      wait -n ${spark_proc_id} ${sleep_proc_id}
      result=$(cat ${tmp_file})
      if [ ${result} == "timeout" ]; then
        kill ${spark_proc_id}
        yarn_app_id=$(grep -oP "application_\d{13}_\d{4}" ${result_file} | head -1)
        if [ -n "$yarn_app_id" ]; then
          yarn application -kill "$yarn_app_id" > /dev/null 2>&1
        fi
      else
        kill ${sleep_proc_id} > /dev/null 2>&1
        if [ -z ${result} ]; then
          bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2>&1
          bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions}
        else 
          time=$(bash /root/QaaD/scripts/get-job-time.sh)
          if [[ ${time:0:1} =~ [0-9] ]]; then
            mid_sum=$(python -c "print(round(${mid_sum} + ${time}, 1))")
          else
            (( i-- ))
            continue
          fi
          (( mid_count++ ))
        fi
      fi
    done
    if [ $mid_count -gt 0 ]; then
      mid_avg=$(python -c "print(round(${mid_sum} / ${mid_count}, 1))")
    else
      mid_avg="0.0"
    fi
    echo "${num_queries} ${num_queries} ${mid_avg}" >> ${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-mid.dat

    aqe_count=0
    aqe_sum="0.0"
    aqe_avg="0.0"
    for (( i=0; i<${num_iter}; i++ )); do
      bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2>&1
      bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions}
      tmp_file=${output_dir}/tmp-aqe-d-${dataset}-q-${num_queries}-i-${i}
      result_file=${tmp_file}.log
      {
        bash /root/QaaD/scripts/run-aqe-yarn.sh ${dataset} ${num_rows} ${num_partitions} ${num_queries} > ${result_file};
        echo "done" > ${tmp_file}
      } &
      spark_proc_id=$!
      {
        sleep ${duration}m; echo "timeout" > ${tmp_file}
      } &
      sleep_proc_id=$!
      wait -n ${spark_proc_id} ${sleep_proc_id}
      result=$(cat ${tmp_file})
      if [ ${result} == "timeout" ]; then
        kill ${spark_proc_id}
        yarn_app_id=$(grep -oP "application_\d{13}_\d{4}" ${result_file} | head -1)
        if [ -n "$yarn_app_id" ]; then
          yarn application -kill "$yarn_app_id" > /dev/null 2>&1
        fi
      else
        kill ${sleep_proc_id} > /dev/null 2>&1
        if [ -z ${result} ]; then
          bash /root/QaaD/scripts/clear-tmp-files.sh > /dev/null 2>&1
          bash /root/QaaD/scripts/run-gen-partitions.sh ${dataset} ${num_rows} ${num_partitions}
        else 
          (( aqe_count++ ))
          time=$(bash /root/QaaD/scripts/get-job-time.sh)
          if [[ ${time:0:1} =~ [0-9] ]]; then
            aqe_sum=$(python -c "print(round(${aqe_sum} + ${time}, 1))")
          else
            (( i-- ))
            continue
          fi
        fi
      fi
    done
    if [ $aqe_count -gt 0 ]; then # check if count is greater than 0
      aqe_avg=$(python -c "print(round(${aqe_sum} / ${aqe_count}, 1))")
    else
      aqe_avg="0.0" # set avg to 0 if count is 0
    fi
    echo "${num_queries} ${num_queries} ${aqe_avg}" >> ${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-aqe.dat
  done
  
  rm -rf ${result_file}
  rm -rf ${output_dir}/tmp*
  /root/QaaD/scripts/plot-scripts/queryset-plot.sh ${dataset} ${num_rows} ${num_partitions} ${output_dir}
done
