echo "sync time"
rdate -s time.bora.net
echo "done."

echo "set variables"
echo "export YARN_CONF_DIR=$HADOOP_HOME/etc/hadoop" >> /etc/bash.bashrc && /bin/bash -c "source /etc/bash.bashrc"
echo "export HADOOP_CLASSPATH=${JAVA_HOME}/lib/tools.jar" >> /etc/bash.bashrc && /bin/bash -c "source /etc/bash.bashrc"
echo "export PYSPARK_PYTHON=python3.8" >> /etc/bash.bashrc && /bin/bash -c "source /etc/bash.bashrc"
echo "export PYSPARK_DRIVER_PYTHON=python3.8" >> /etc/bash.bashrc && /bin/bash -c "source /etc/bash.bashrc"
echo "export PATH=$PATH:$SPARK_HOME/bin" >> /etc/bash.bashrc && /bin/bash -c "source /etc/bash.bashrc"
echo "export PATH=$PATH:$HADOOP_HOME/bin" >> /etc/bash.bashrc && /bin/bash -c "source /etc/bash.bashrc"
echo "export ZOOKEEPER_HOME=/root/QaaD/src/AdaptDB/zookeeper-3.4.6" >> /etc/bash.bashrc && /bin/bash -c "source /etc/bash.bashrc"
echo "done."

echo "ssh keygen"
cd /root/ && yes | ssh-keygen -t rsa -P "" -f /root/.ssh/id_rsa
cat /root/.ssh/id_rsa.pub >> /root/.ssh/authorized_keys
echo "done."

echo "ssh start"
service ssh start
echo "done."

echo "jupyter start"
/bin/bash -c "jupyter notebook --MultiKernelManager.default_kernel_name=plotly --allow-root > /dev/null 2>&1 &"
echo "done."

current_path=$(pwd)
cd /root/QaaD/scripts/gnuplot-4.6.2; make clean; ./configure; make; make install; cd ${current_path}

chmod 777 -R /root/QaaD
