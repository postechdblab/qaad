/root/dev/hadoop-2.7.7/sbin/stop-dfs.sh
/root/dev/hadoop-2.7.7/sbin/stop-yarn.sh
/root/dev/spark-3.2.1-bin-hadoop2.7/sbin/stop-all.sh  
/root/dev/spark-3.2.1-bin-hadoop2.7/sbin/stop-history-server.sh 

yes | /root/dev/hadoop-2.7.7/bin/hadoop namenode -format hdfs-namenode
/root/dev/hadoop-2.7.7/sbin/start-dfs.sh  
/root/dev/hadoop-2.7.7/sbin/start-yarn.sh  
/root/dev/hadoop-2.7.7/bin/hadoop fs -mkdir -p /spark-logs/  
/root/dev/spark-3.2.1-bin-hadoop2.7/sbin/start-all.sh  
/root/dev/spark-3.2.1-bin-hadoop2.7/sbin/start-history-server.sh

/root/dev/hadoop-2.7.7/bin/hdfs dfsadmin -safemode leave
