#!/bin/bash

dataset=$1
num_rows=$2
num_partitions=$3
num_queries=$4
output_dir=$5

filename="${output_dir}/arrivalrate-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-q-${num_queries}-bar.eps"
qaaddata="${output_dir}/arrivalrate-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-q-${num_queries}-qaad.dat"
sparkudata="${output_dir}/arrivalrate-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-q-${num_queries}-sparku.dat"
sparksdata="${output_dir}/arrivalrate-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-q-${num_queries}-sparks.dat"
_qaaddata="${output_dir}/_arrivalrate-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-q-${num_queries}-qaad.dat"
_sparkudata="${output_dir}/_arrivalrate-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-q-${num_queries}-sparku.dat"
_sparksdata="${output_dir}/_arrivalrate-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-q-${num_queries}-sparks.dat"
scriptname="/root/QaaD/scripts/plot-scripts/arrivalrate-elapsedtime-bar.plt"


gnuplot -e "filename='$filename'; qaaddata='$qaaddata'; sparkudata='$sparkudata'; sparksdata='$sparksdata'; _sparksdata='$_sparksdata'; _sparkudata='$_sparkudata'; _qaaddata='$_qaaddata'" $scriptname 
