#!/bin/bash

dataset=$1
num_rows=$2
num_queries=$3
output_dir=$4

filename="${output_dir}/partitioning-${dataset}-elapsedtime-r-${num_rows}-q-${num_queries}-bar.eps"
qaaddata="${output_dir}/partitioning-${dataset}-elapsedtime-r-${num_rows}-q-${num_queries}-qaad.dat"
sparkudata="${output_dir}/partitioning-${dataset}-elapsedtime-r-${num_rows}-q-${num_queries}-sparku.dat"
sparksdata="${output_dir}/partitioning-${dataset}-elapsedtime-r-${num_rows}-q-${num_queries}-sparks.dat"
scriptname="/root/QaaD/scripts/plot-scripts/partitioning-elapsedtime-bar.plt"

gnuplot -e "filename='$filename'; qaaddata='$qaaddata'; sparkudata='$sparkudata'; sparksdata='$sparksdata'" $scriptname 
