#!/bin/bash

dataset=$1
num_rows=$2
num_partitions=$3
output_dir=$4

filename="${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-bar.eps"
qaaddata="${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-qaad.dat"
sparkudata="${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-sparku.dat"
sparksdata="${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-sparks.dat"
scriptname="/root/QaaD/scripts/plot-scripts/queryset-elapsedtime-bar.plt"

gnuplot -e "filename='$filename'; qaaddata='$qaaddata'; sparkudata='$sparkudata'; sparksdata='$sparksdata'" $scriptname 
