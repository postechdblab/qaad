#!/bin/bash

dataset=$1
num_rows=$2
num_partitions=$3
output_dir=$4

filename="${output_dir}/comp-part-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-bar.eps"
sparksdata="${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-sparks.dat"
qaaddata="${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-qaad.dat"
adaptdbdata="${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-adaptdb.dat"
ampdata="${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-smartplan.dat"
copartdata="${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-copart.dat"
aqedata="${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-aqe.dat"
middata="${output_dir}/queryset-${dataset}-elapsedtime-r-${num_rows}-p-${num_partitions}-mid.dat"
scriptname="/root/QaaD/scripts/plot-scripts/comp-part-${dataset}-elapsedtime-bar.plt"

gnuplot -e "filename='$filename'; sparksdata='$sparksdata'; qaaddata='$qaaddata'; adaptdbdata='$adaptdbdata'; smartplandata='$smartplandata'; copartdata='$copartdata'; aqedata='$aqedata'; middata='$middata'" $scriptname 
