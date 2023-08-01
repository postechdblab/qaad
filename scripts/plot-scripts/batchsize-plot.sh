#!/bin/bash

dataset=$1
num_rows=$2
num_partitions=$3
output_dir=$4

filename="${output_dir}/batchsize-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-bar.eps"
qaaddata="${output_dir}/batchsize-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-qaad.dat"
sparkudata="${output_dir}/batchsize-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-sparku.dat"
sparksdata="${output_dir}/batchsize-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-sparks.dat"
_qaaddata="${output_dir}/_batchsize-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-qaad.dat"
_sparkudata="${output_dir}/_batchsize-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-sparku.dat"
_sparksdata="${output_dir}/_batchsize-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-sparks.dat"
scriptname="/root/QaaD/scripts/plot-scripts/batchsize-elapsedtime-bar.plt"

gnuplot -e "filename='$filename'; qaaddata='$qaaddata'; sparkudata='$sparkudata'; sparksdata='$sparksdata'; _qaaddata='$_qaaddata'; _sparkudata='$_sparkudata'; _sparksdata='$_sparksdata'" $scriptname 
