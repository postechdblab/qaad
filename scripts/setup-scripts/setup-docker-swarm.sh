#!/bin/bash

docker swarm leave -f

# Input arguments for Master
MASTER=$1
MASTER_PASSWORD=$2
shift 2

# Clear known_hosts file
ssh-keygen -R $(echo $MASTER | cut -d'@' -f2)

# Initiate Docker swarm on the master
SWARM_INIT=$(docker swarm init --advertise-addr $(echo $MASTER | cut -d'@' -f2):2377)

# Extract join token from the output
JOIN_TOKEN=$(echo "$SWARM_INIT" | grep "\-\-token" | awk '{print $5}')

# Input arguments for Workers
while [ "$#" -gt 1 ]; do
  WORKER=$1
  WORKER_PASSWORD=$2
  shift 2

  # Clear known_hosts file
  ssh-keygen -R $(echo $WORKER | cut -d'@' -f2)

  # Join each worker to the swarm
  sshpass -p "$WORKER_PASSWORD" ssh -tt "$WORKER" "docker swarm leave -f; docker swarm join --token $JOIN_TOKEN $(echo $MASTER | cut -d'@' -f2):2377"
done


docker network create -d overlay --attachable spark-cluster-net
