#!/usr/bin/gnuplot --persist


reset

set terminal postscript enhanced eps "Helvetica" 30
set size 0.8,0.8

set xlabel "# of rows (K)" offset 0,1.1
set xtics offset 0,0.4
set ylabel "Elapsed time (sec.)" offset 2.0,0
#set logscale y 10
#set format x "2^{%T}"
#set format y "10^{%T}"
set boxwidth 0.3
set yrange[0:*]

set key left top samplen 1 spacing 0.98
#set key at 4.3, 7.3
set style fill solid noborder

set output filename 
plot \
  sparksdata using (log($1)/log(2)-0.3):3 title "SparkS" with boxes lc rgb "#bfbfbf", \
  sparkudata using (log($1)/log(2)):3:xtic(2) title "SparkU" with boxes lc rgb "#808080", \
  qaaddata using (log($1)/log(2)+0.3):3 title "QaaD" with boxes lc rgb "#000000"

