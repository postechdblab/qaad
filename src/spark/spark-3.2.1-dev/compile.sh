export MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true"
/root/QaaD/src/spark/spark-3.2.1-dev/build/mvn -DskipTests -Dmaven.wagon.http.ssl.ignore.validity.dates=true -pl :spark-core_2.12 package
cp /root/QaaD/src/spark/spark-3.2.1-dev/core/target/spark-core_2.12-3.2.1*.jar /root/dev/spark-3.2.1-bin-hadoop2.7/jars/
/root/dev/spark-3.2.1-bin-hadoop2.7/sbin/stop-all.sh > /dev/null 2>&1
/root/dev/spark-3.2.1-bin-hadoop2.7/sbin/start-all.sh > /dev/null 2>&1
