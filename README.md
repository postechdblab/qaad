# QaaD (Query-as-a-Data): Scalable Execution of Massive Number of Small Queries in Spark

This is a guide for running QaaD on Apache Spark in a distributed Docker environment.

## 1. Prerequisites

This manual is structured on the assumption that the following environment has been established. If you do not have the following environment, configure it and follow the manual below.

- Multiple Linux-based (CentOS-based) machines.
- This requires one master machines and at least four worker machines. All scripts assume that a cluster consists of one master mahine and four worker machines.
- All machines must have at least 16 cores and 192 GB of RAM.
- Each server must have [docker](https://docs.docker.com/get-docker/) installed, and the user must have access to this docker. Each machine should be able to run docker without the sudo command.
- Each server must be able to communicate with each other over the network and must be connected to the Internet.
- Among machines, the machine to be used as the Master must be selected, and the machine's IP must be identified.
- The ssh connection from the master to each worker must be possible.
- The master machine must have [sshpass](https://centos.pkgs.org/7/centos-extras-x86_64/sshpass-1.06-2.el7.x86_64.rpm.html) installed.
- All machines must have at least 256 GB of free disk space.

## 2. Caution
##### (0) Testing environment.
A cluster consists of one master mahcine and four worker machines.
Each machine of the cluster has Intel Xeon E5-2450 CPU with 16 cores and 192 GB of RAM.
All machines are equipped with 1 TB of HDDs and interconnected by InfiniBand QDR 4x network switch.
On the software side, we installed Spark 3.0 and Hadoop 2.7.7 on Ubuntu 20.04.3 LTS.
We configured Spark to have four executors. Each executor used 14 cores and 128 GB RAM to run Spark aaplications.
Note that it is impossible to initialize Spark and continue to run experiments if one of machines in the cluster has no disk space.

##### (1) Code execution location.
The codes to be executed are written in a code block and have the following format.
```
[MACHINE]$ command
```
Here, `MACHINE` has the following types. You can proceed by following the manual while checking whether the location where the code is executed is running in the correct location.

- master : Master node.
- master-docker : Inside docker container running on master node.
- worker : Worker node.
- worker-docker : Inside docker container running on worker node.

Users put the path of the repository into `PATH_TO_DIR` in the following instructions.

##### (2) Repeat the same task for all workers.
In a distributed programming environment, in most cases there are multiple workers, so you have to iterate over each worker machine with the same setup. Name each machine in the list of worker machines in the following order: `worker1`, `worker2`, `worker3`,... In addition, the part marked with `workerX` in the manual below means to repeatedly execute the instruction by substituting a worker number of `X = 1,2,3...` for each worker machine.

## 2. Docker Swarm
This step is to group each machine where Docker is installed into one cluster.

1. Connect via ssh to the master node.

2. Run `PATH_TO_DIR/scripts/setup-scripts/setup-docker-swarm.sh`
Here, use USER_NAME (Linux user name), IP (machine's IP address), and PASSWORD (password for USER_NAME and IP) for ssh connection.
```
[master]$ cd PATH_TO_DIR/scripts/setup-scripts; bash setup-docker-swarm.sh MASTER_USER_NAME@MASTER_IP MASTER_PASSWORD WORKER1_USER_NAME@WORKER1_IP WORKER1_PASSWORD WORKER2_USER_NAME@WORKER2_IP WORKER2_PASSWORD WORKER3_USER_NAME@WORKER3_IP WORKER3_PASSWORD WORKER4_USER_NAME@WORKER4_IP WORKER4_PASSWORD ...

(example)
[master]$ cd /root/qaad/scripts/setup-scripts; bash setup-docker-swarm.sh root@0.0.0.0 EXPW123! root@0.0.0.1 EXPW123! root@0.0.0.2 EXPW123! root@0.0.0.3 EXPW123! root@0.0.0.4 EXPW123!
```

3. (Back to the master node) Enter the following command.
```
[master]$ docker node ls
```

When you do this, all machines (including master) to be included in the cluster should be displayed.


## 3. Create Docker Image and Run Container
This step is the process of downloading the configuration file/external source codes, creating a Docker image, and running the container.

#### For Master, do the following:

1. Download this repository on the master machine.

2. Download two datasets (i.e., BRA and eBay datasets) on `qaad/datasets`.

- Download [BRA dataset directory](https://drive.google.com/drive/folders/1sJXnzc4BUqfU0-P6my7Xsb6ZYaheC2qC?usp=sharing) on `qaad/datasets`

- Download [eBay dataset directory](https://drive.google.com/drive/folders/16p3vF-tPsSzmVQj2Xg_31Fe9Tvf8PeBP?usp=sharing) on `qaad/datasets`

3. Download two query sets and their parameters on `qaad/querysets`

- Download [brazilian-ecommerce directory](https://drive.google.com/drive/folders/1WFddeCYADEZtQiI1WyjZRB7kU8cTZy7A?usp=sharing) on `qaad/querysets`

- Download [eBay directory](https://drive.google.com/drive/folders/1ZkbhEUqSjfF-kxksr7apMVzFnSexa9I6?usp=sharing) on `qaad/querysets`

4. Download the master docker image [base-image-master.tar](https://drive.google.com/file/d/15zpJUCLjk--bhMqd_oYo2IES7hVVQS9-/view?usp=sharing) and load the docker image.
```
[master]$ docker load < base-image-master.tar
```

5. Download [AdaptDB](https://drive.google.com/file/d/1PwQc7HIxFxO-ie-MSNikhjx1G77vb51X/view?usp=share_link) on `qaad/src` and uncompress this tar file.
```
[master]$ cd PATH_TO_DIR/src; tar -xvf AdaptDB.tar
```

6. Download the worker docker image [base-image-worker.tar](https://drive.google.com/file/d/1OuD5q8e1XWkB--UzNV1bl1-IfhXF8aRr/view?usp=sharing) on each worker machine.

7. Load the docker image on each worker machine.
```
[worker]$ docker load < base-image-worker.tar
```

8. If you want to use more than four worker machines, add additional worker machines to `PATH_TO_DIR/scripts/setup-scripts/slaves` as follows.

```
worker1
worker2
worker3
worker4
.....
```

9. Run the docker containers using `PATH_TO_DIR/scripts/setup-scripts/run-containers.sh` on master machine.
```
[master]$ cd PATH_TO_DIR/scripts/setup-scripts; bash run-containers.sh PATH_TO_DIR MASTER_USER_NAME@MASTER_IP MASTER_PASSWORD WORKER1_USER_NAME@WORKER1_IP WORKER1_PASSWORD WORKER2_USER_NAME@WORKER2_IP WORKER2_PASSWORD WORKER3_USER_NAME@WORKER3_IP WORKER3_PASSWORD WORKER4_USER_NAME@WORKER4_IP WORKER4_PASSWORD ...

(example)
[master]$ cd /root/qaad/scripts/setup-scripts; bash run-containers.sh PATH_TO_DIR root@0.0.0.0 EXPW123! root@0.0.0.1 EXPW123! root@0.0.0.2 EXPW123! root@0.0.0.3 EXPW123! root@0.0.0.4 EXPW123!
```

## 4. Run Experiments

1. Enter the master docker container.
```
[master]$ docker exec -it master /bin/bash
```

2. Check the ssh connection on the master machine.
The following commands should be performed without entering passwords on master machine.
```
[master-docker]$ ssh worker1
[master-docker]$ ssh worker2
[master-docker]$ ssh worker3
[master-docker]$ ssh worker4
...
```

3. Compile source codes.
```
[master-docker]$ cd /root/QaaD/src/spark/spark-3.2.1-dev/; bash compile.sh
```

4. Run `test-all.sh` in `QaaD/scripts`.
`PATH_TO_OUTPUT_DIR` indicates the output directory.

```
[master-docker]$ cd /root/QaaD/scripts; bash test-all.sh PATH_TO_OUTPUT_DIR
(example)
[master-docker]$ cd /root/QaaD/scripts; bash test-all.sh /root/QaaD/results
```

`test-all.sh` generates result files for figures in the paper as follows.

Fig. 9(a):
`PATH_TO_OUTPUT_DIR/queryset-bra-elapsedtime-r-0-p-448-bar.eps`

Fig. 9(b):
`PATH_TO_OUTPUT_DIR/queryset-ebay-elapsedtime-r-0-p-448-bar.eps`

Fig. 10(a):
`PATH_TO_OUTPUT_DIR/comp-part-bra-elapsedtime-r-0-p-448-bar.eps`

Fig. 10(b):
`PATH_TO_OUTPUT_DIR/comp-part-ebay-elapsedtime-r-0-p-448-bar.eps`

Fig. 11(a):
`PATH_TO_OUTPUT_DIR/partitioning-bra-elapsedtime-r-0-q-132-bar.eps`

Fig. 11(b):
`PATH_TO_OUTPUT_DIR/partitioning-bra-elapsedtime-r-0-q-108-bar.eps`

Fig. 12(a):
`PATH_TO_OUTPUT_DIR/scalability-bra-elapsedtime-p-448-q-132-bar.eps`

Fig. 12(b):
`PATH_TO_OUTPUT_DIR/scalability-ebay-elapsedtime-p-448-q-108-bar.eps`

Fig. 13(a):
`PATH_TO_OUTPUT_DIR/arrivalrate-bra-elapsedtime-r-0-p-448-q-1056-bar.eps`

Fig. 13(b):
`PATH_TO_OUTPUT_DIR/arrivalrate-ebay-elapsedtime-r-0-p-448-q-864-bar.eps`

Fig. 14(a):
`PATH_TO_OUTPUT_DIR/batchsize-bra-elapsedtime-r-0-p-448-bar.eps`

Fig. 14(b):
`PATH_TO_OUTPUT_DIR/batchsize-ebay-elapsedtime-r-0-p-448-bar.eps`

## 5. Contact Us

If you have any inquiry or bug report, please send emails to me at yspark@dblab.postech.ac.kr.
