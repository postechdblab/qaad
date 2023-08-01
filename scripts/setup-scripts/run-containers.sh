#!/bin/bash

docker stop master; docker rm master;

DIR=$1
shift 1

# Input arguments for Master
MASTER=$1
MASTER_PASSWORD=$2
shift 2

docker run -itd --privileged -h master -v ${DIR}:/root/QaaD --name master -p 30000-30010:30000-30010 --net spark-cluster-net base-image-master
docker exec -it master /bin/bash -c "bash -i -c 'which hdfs; exit'"
docker cp ${DIR}/scripts/setup-scripts/init-master.sh master:/root/init.sh
docker exec -it master /bin/bash -c "bash -i -c 'which hdfs; exit'"
docker exec -it master /bin/bash -c "bash -i -c 'bash /root/init.sh; pip install fabric3; exit'"
echo 'echo -e "Host *\n   UserKnownHostsFile /dev/null\n   StrictHostKeyChecking no" >> ~/.ssh/config; exit' | docker exec -i master /bin/bash
docker exec -it master /bin/bash -c "bash -i -c 'which hdfs; exit'"

SLAVES_FILE=${DIR}/scripts/setup-scripts/slaves

if [ -f ${SLAVES_FILE} ]; then
  rm -rf ${SLAVES_FILE}
fi

# Input arguments for Workers
worker_id=0
zookeeper_id=1
while [ "$#" -gt 1 ]; do
  (( worker_id++ ))
  (( zookeeper_id++ ))
  WORKER=$1
  WORKER_PASSWORD=$2
  shift 2
  worker_name="worker"${worker_id}
  echo $worker_name >> ${SLAVES_FILE}

  sshpass -p "$WORKER_PASSWORD" ssh -tt "$WORKER" "docker stop ${worker_name}"
  sshpass -p "$WORKER_PASSWORD" ssh -tt "$WORKER" "docker rm ${worker_name}"
  sshpass -p "$WORKER_PASSWORD" ssh -tt "$WORKER" "docker run -itd --privileged -h ${worker_name} -p 30000-30010:30000-30010 --name ${worker_name} --link master:master --net spark-cluster-net base-image-worker"
  docker exec -it master /bin/bash -c "bash -i -c 'which hdfs; exit'"
  sshpass -p "$WORKER_PASSWORD" scp ${DIR}/scripts/setup-scripts/setup-password.sh $WORKER:/tmp
  sshpass -p "$WORKER_PASSWORD" scp ${DIR}/scripts/setup-scripts/init-worker.sh $WORKER:/tmp
  sshpass -p "$WORKER_PASSWORD" scp ${DIR}/scripts/setup-scripts/conf-worker/yarn-site.xml $WORKER:/tmp
  sshpass -p "$WORKER_PASSWORD" scp ${DIR}/scripts/setup-scripts/conf-worker/core-site.xml $WORKER:/tmp
  sshpass -p "$WORKER_PASSWORD" scp ${DIR}/scripts/setup-scripts/conf-worker/hdfs-site.xml $WORKER:/tmp
  sshpass -p "$WORKER_PASSWORD" scp -r ${DIR}/src/AdaptDB/zookeeper-3.4.6 $WORKER:/tmp
  docker exec -it master /bin/bash -c "bash -i -c 'which hdfs; exit'"

  sshpass -p "$WORKER_PASSWORD" ssh -tt "$WORKER" "docker cp /tmp/setup-password.sh ${worker_name}:/root/setup-password.sh"
  sshpass -p "$WORKER_PASSWORD" ssh -tt "$WORKER" "docker cp /tmp/init-worker.sh ${worker_name}:/root/init.sh"
  sshpass -p "$WORKER_PASSWORD" ssh -tt "$WORKER" "docker cp /tmp/yarn-site.xml ${worker_name}:/root/dev/hadoop-2.7.7/etc/hadoop/yarn-site.xml"
  sshpass -p "$WORKER_PASSWORD" ssh -tt "$WORKER" "docker cp /tmp/core-site.xml ${worker_name}:/root/dev/hadoop-2.7.7/etc/hadoop/core-site.xml"
  sshpass -p "$WORKER_PASSWORD" ssh -tt "$WORKER" "docker cp /tmp/hdfs-site.xml ${worker_name}:/root/dev/hadoop-2.7.7/etc/hadoop/hdfs-site.xml"
  sshpass -p "$WORKER_PASSWORD" ssh -tt "$WORKER" "docker exec -it ${worker_name} /bin/bash -c 'mkdir -p /root/QaaD/src/AdaptDB; exit'"
  sshpass -p "$WORKER_PASSWORD" ssh -tt "$WORKER" "docker cp /tmp/zookeeper-3.4.6 ${worker_name}:/root/QaaD/src/AdaptDB/"

  sshpass -p "$WORKER_PASSWORD" ssh -tt "$WORKER" "docker exec -it ${worker_name} /bin/bash -c \"bash -i -c 'chmod 777 /root/setup-password.sh; bash /root/setup-password.sh Asdf123\!; bash /root/init.sh; echo ${zookeeper_id} > /root/QaaD/src/AdaptDB/zookeeper-3.4.6/data/myid; exit'\""
  docker exec -it master /bin/bash -c "bash -i -c 'which hdfs; exit'"
  echo "sshpass -p Asdf123! ssh-copy-id -i /root/.ssh/id_rsa.pub -o 'StrictHostKeyChecking=no' root@${worker_name}; exit" | docker exec -i master /bin/bash
  docker exec -it master /bin/bash -c "bash -i -c 'which hdfs; exit'"
  
done

docker exec -it master /bin/bash -c "bash -i -c 'which hdfs; exit'"

docker cp ${SLAVES_FILE} master:/root/dev/hadoop-2.7.7/etc/hadoop/slaves
docker cp ${DIR}/scripts/setup-scripts/conf-master/yarn-site.xml master:/root/dev/hadoop-2.7.7/etc/hadoop/yarn-site.xml
docker cp ${DIR}/scripts/setup-scripts/conf-master/core-site.xml master:/root/dev/hadoop-2.7.7/etc/hadoop/core-site.xml
docker cp ${DIR}/scripts/setup-scripts/conf-master/hdfs-site.xml master:/root/dev/hadoop-2.7.7/etc/hadoop/hdfs-site.xml

echo "bash /root/QaaD/scripts/clear-tmp-files.sh; exit" | docker exec -i master /bin/bash
#docker exec -i master /bin/bash -c "bash -i -c 'bash /root/QaaD/scripts/clear-tmp-files.sh; exit'"
