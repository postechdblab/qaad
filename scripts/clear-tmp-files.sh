passwd=Asdf123!
echo "${passwd}" > /tmp/passwd
current_date=$(date +'%Y-%m-%d %H:%M:%S')
date -s "$(echo ${current_date})"

for i in {0..10}; do
  while read -u 3 host; do
    echo ${host}
    command="rm -rf /tmp/*; rm -rf /root/QaaD/datanode; mkdir -p /root/QaaD/datanode; rm -rf /root/QaaD/tmp; mkdir -p /root/QaaD/tmp; rm -rf /root/dev/hadoop-2.7.7/logs/*; yes | /root/dev/hadoop-2.7.7/bin/hdfs namenode -format; /root/QaaD/src/AdaptDB/zookeeper-3.4.6/bin/zkServer.sh start"
    echo ${command}
    sshpass -f /root/QaaD/scripts/passwd ssh -tt root@${host} "$command"
    command='date -s "$(echo ${current_date})"'
    sshpass -f /root/QaaD/scripts/passwd ssh -tt root@${host} "$command"
  done 3< /root/dev/hadoop-2.7.7/etc/hadoop/slaves

  rm -rf /tmp/passwd

  rm -rf /tmp/*; rm -rf /root/QaaD/namenode; mkdir -p /root/QaaD/namenode; rm -rf /root/QaaD/tmp; mkdir -p /root/QaaD/tmp; rm -rf /root/dev/hadoop-2.7.7/logs/*

  rm -rf /root/QaaD/scripts/core.*
  rm -rf /root/QaaD/scripts/hs_*log
  rm -rf /root/QaaD/scripts/target
  pkill -9 -f 'java'
  pkill -9 -f 'sleep'

  /root/QaaD/scripts/setup.sh
  /root/QaaD/src/AdaptDB/zookeeper-3.4.6/bin/zkServer.sh start

  num_nodes=$(/root/dev/hadoop-2.7.7/bin/hdfs dfsadmin -report | grep "Normal" | wc -l)
  target=$(cat /root/dev/hadoop-2.7.7/etc/hadoop/slaves | wc -l)
  if [[ ${num_nodes} = ${target} ]]; then
    echo "This is a valid cluster."
    break
  fi
done
if [[ ${i} == "10" ]]; then
  echo "This is a invalid cluster."
fi
