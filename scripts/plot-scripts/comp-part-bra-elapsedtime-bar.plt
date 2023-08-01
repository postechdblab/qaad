#!/usr/bin/gnuplot --persist



reset

set terminal postscript enhanced eps "Helvetica" 30
set encoding utf8
set size 0.8,0.9

set autoscale x
set autoscale y

set xlabel "# of queries" offset 0,1.2
set ylabel "Elapsed time (sec.)" offset 0
set logscale y 10
#set format x "2^{%T}"
set format y "10^{%T}"
set boxwidth 0.1
set xrange[log(33)/log(2)-0.45:log(1056)/log(2)+0.45]
set yrange[10:40000]
#set ytics add ("0" 0.1)
unset xtics
set xtics format " "
set xtics font ", 25"
set xtics scale 0
set xtics offset 0,0.5
set xtics add ("33" log(33)/log(2), "66" log(66)/log(2), "132" log(132)/log(2), "264" log(264)/log(2), "528" log(528)/log(2), "1056" log(1056)/log(2))


set key left top samplen 0.5 spacing 0.8 at graph 0, 0.97
#set key at 4.3, 7.3
set style fill solid noborder
# bfbfbf 805a90 cf597e eeb479 9ccb86 009392 006699 000000

set output filename 
plot \
  sparksdata using (log($1)/log(2)-0.3):3 title "SparkS" with boxes lc rgb "#bfbfbf", \
  adaptdbdata using (log($1)/log(2)-0.2):3 title "AdaptDB\\@SparkS" with boxes lc rgb "#805a90", \
  smartplandata using (log($1)/log(2)-0.1):3 title "SmartPlan\\@SparkS" with boxes lc rgb "#cf597e", \
  middata using (log($1)/log(2)):3 title "mID\\@QaaD" with boxes lc rgb "#eeb479", \
  copartdata using (log($1)/log(2)+0.1):3 title "Copart\\@QaaD" with boxes lc rgb "#9ccb86", \
  aqedata using (log($1)/log(2)+0.2):3 title "AQE\\@QaaD" with boxes lc rgb "#009392", \
  qaaddata using (log($1)/log(2)+0.3):3 title "microPart\\@QaaD" with boxes lc rgb "#000000"

