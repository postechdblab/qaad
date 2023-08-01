m=128
dataset=$1
num_rows=$2
input_path=/root/QaaD/scripts/input/${dataset}-${num_rows}.txt
num_partitions=$3
num_queries=1
/root/QaaD/scripts/set.sh ori-jars
if [ ${dataset} == bra ]; then
  for i in {0/root/QaaD10}; do
    hdfs_dir=/root/QaaD/datasets/synthetic-brazilian-ecommerce/num-rows-${num_rows}/
    /root/dev/hadoop-2.7.7/bin/hdfs dfs -rm -r -f ${hdfs_dir}/*
    /root/dev/hadoop-2.7.7/bin/hdfs dfs -mkdir -p ${hdfs_dir}
    /root/dev/spark-3.2.1-bin-hadoop2.7/bin/spark-shell \
      --master yarn \
      --driver-memory ${m}g \
      --driver-cores 14 \
      --executor-cores 14 \
      --num-executors 4 \
      --executor-memory ${m}g \
      --conf spark.driver.maxResultSize=20g \
      --conf spark.scheduler.listenerbus.eventqueue.capacity=100000 \
      --conf spark.memory.fraction=0.8 \
      --conf spark.serializer=org.apache.spark.serializer.KryoSerializer \
      --conf spark.kryoserializer.buffer.max=1g \
      --conf spark.rpc.message.maxSize=2000 \
      --conf spark.yarn.maxAppAttempts=1 \
      -i <(echo 'val inputPath = "'${input_path}'"') \
      -i <(echo 'val globalNumPartitions = "'${num_partitions}'".toInt') \
      -i <(echo 'val numQueries = "'${num_queries}'".toInt') \
      -i <(echo 'val numRows = "'${num_rows}'".toInt') \
      -i /root/QaaD/src/Partitioners.scala \
      -i /root/QaaD/src/Operation.scala \
      -i /root/QaaD/src/SetupForGenDataset.scala \
      -i /root/QaaD/querysets/brazilian-ecommerce/load.scala \
      -i /root/QaaD/src/GenDataset-bra.scala > /dev/null 2>&1
    dir_count=$(/root/dev/hadoop-2.7.7/bin/hdfs dfs -ls ${hdfs_dir} | wc -l)
    if [[ "${dir_count}" -eq "9" ]]; then
      break
    else
      /root/QaaD/scripts/clear-tmp-files.sh
    fi
  done
else
  for i in {0/root/QaaD10}; do
    hdfs_dir=/root/QaaD/datasets/synthetic-ebay/num-rows-${num_rows}/
    /root/dev/hadoop-2.7.7/bin/hdfs dfs -rm -r -f ${hdfs_dir}/*
    /root/dev/hadoop-2.7.7/bin/hdfs dfs -mkdir -p ${hdfs_dir}
    /root/dev/spark-3.2.1-bin-hadoop2.7/bin/spark-shell \
      --master yarn \
      --driver-memory ${m}g \
      --driver-cores 14 \
      --executor-cores 14 \
      --num-executors 4 \
      --executor-memory ${m}g \
      --conf spark.driver.maxResultSize=20g \
      --conf spark.scheduler.listenerbus.eventqueue.capacity=100000 \
      --conf spark.memory.fraction=0.8 \
      --conf spark.serializer=org.apache.spark.serializer.KryoSerializer \
      --conf spark.kryoserializer.buffer.max=1g \
      --conf spark.rpc.message.maxSize=2000 \
      --conf spark.yarn.maxAppAttempts=1 \
      -i <(echo 'val inputPath = "'${input_path}'"') \
      -i <(echo 'val globalNumPartitions = "'${num_partitions}'".toInt') \
      -i <(echo 'val numQueries = "'${num_queries}'".toInt') \
      -i <(echo 'val numRows = "'${num_rows}'".toInt') \
      -i /root/QaaD/src/Partitioners.scala \
      -i /root/QaaD/src/Operation.scala \
      -i /root/QaaD/src/SetupForGenDataset.scala \
      -i /root/QaaD/querysets/ebay/load.scala \
      -i /root/QaaD/src/GenDataset-ebay.scala > /dev/null 2>&1
    dir_count=$(/root/dev/hadoop-2.7.7/bin/hdfs dfs -ls ${hdfs_dir} | wc -l)
    if [[ "${dir_count}" -eq "2" ]]; then
      break
    else
      /root/QaaD/scripts/clear-tmp-files.sh
    fi
  done
fi
