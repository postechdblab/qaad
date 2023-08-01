m=128
dataset=$1
num_rows=$2
num_partitions=$3
if [ ${dataset} = bra ]; then
  num_templates=33
  dir=brazilian-ecommerce
else
  num_templates=27
  dir=ebay
fi
num_queries=$(python -c "print(int(float($4) / ${num_templates}))")
input_path=/root/QaaD/scripts/input/${dataset}-${num_rows}.txt
echo "sparks ${num_rows} ${num_queries}"
/root/QaaD/scripts/set.sh ori-jars
if [ ${dataset} = bra ]; then
	/root/dev/spark-3.2.1-bin-hadoop2.7/bin/spark-shell \
                --name sparks-d-${dataset}-r-${num_rows}-q-${num_queries}-p-${num_partitions} \
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
		-i <(echo 'var globalStartTime = 0.0f') \
		-i /root/QaaD/src/Partitioners.scala \
		-i /root/QaaD/src/Operation.scala \
		-i /root/QaaD/src/MicroRDD.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp1.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp2.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp3.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp4.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp5.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp6.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp7.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp8.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp9.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp10.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp11.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp12.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp13.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp14.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp15.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp16.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp17.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp18.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp19.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp20.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp21.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp22.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp23.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp24.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp25.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp26.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp27.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp28.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp29.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp30.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp31.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp32.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp33.scala \
		-i /root/QaaD/src/SparkSApp-${dataset}.scala
else
	/root/dev/spark-3.2.1-bin-hadoop2.7/bin/spark-shell \
                --name sparks-d-${dataset}-r-${num_rows}-q-${num_queries}-p-${num_partitions} \
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
		-i <(echo 'var globalStartTime = 0.0f') \
		-i /root/QaaD/src/Partitioners.scala \
		-i /root/QaaD/src/Operation.scala \
		-i /root/QaaD/src/MicroRDD.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp1.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp2.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp3.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp4.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp5.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp6.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp9.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp10.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp11.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp12.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp13.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp14.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp15.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp16.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp17.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp18.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp19.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp20.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp24.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp26.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp27.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp28.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp29.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp30.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp31.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp32.scala \
		-i /root/QaaD/querysets/${dir}/DashboardApp33.scala \
		-i /root/QaaD/src/SparkSApp-${dataset}.scala
fi
