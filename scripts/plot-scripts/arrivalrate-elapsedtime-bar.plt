#!/usr/bin/gnuplot --persist


reset

set terminal postscript enhanced eps "Helvetica" 35
set size 0.8,0.8

set xlabel "Avg. # of queries per sec." offset 0,1.2
set xtics offset 0,0.5
set ylabel "Response time (sec.)" offset 1.5,0
set logscale y 10
#set format x "2^{%T}"
set format y "10^{%T}"
set yrange[0.1:4000000]
set ytics add ("0" 0.1)
set boxwidth 0.3

set key right top samplen 1 spacing 0.98
#set key at 4.3, 7.3
set style fill solid noborder

set output filename 
plot \
  sparksdata using (log($1)/log(10)-0.3):3 title "SparkS" with boxes lc rgb "#bfbfbf", \
  _sparksdata using (log($1)/log(10)-0.3):3 notitle with boxes lc rgb "#bfefff", \
  sparkudata using (log($1)/log(10)):3:xtic(2) title "SparkU" with boxes lc rgb "#808080", \
  _sparkudata using (log($1)/log(10)):3:xtic(2) notitle with boxes lc rgb "#669acc", \
  qaaddata using (log($1)/log(10)+0.3):3 title "QaaD" with boxes lc rgb "#000000", \
  _qaaddata using (log($1)/log(10)+0.3):3 notitle with boxes lc rgb "#003366"

