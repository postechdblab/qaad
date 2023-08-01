#!/usr/bin/gnuplot --persist


reset

set terminal postscript enhanced eps "Helvetica" 30
set encoding utf8
set size 0.8,0.9

set xlabel "# of queries" offset 0,1.2
set ylabel "Elapsed time (sec.)" offset 0
set logscale y 10
#set format x "2^{%T}"
set format y "10^{%T}"
set boxwidth 0.15
set xrange[log(27)/log(2)-0.4:log(864)/log(2)+0.4]
#set yrange[0.1:*]
#set ytics add ("0" 0.1)
set xtics format " "
set xtics font ", 25"
set xtics scale 0
set xtics offset 0,0.5
set xtics add ("27" log(27)/log(2), "54" log(54)/log(2), "108" log(108)/log(2), "216" log(216)/log(2), "432" log(432)/log(2), "864" log(864)/log(2))

set key left top samplen 0.8 spacing 0.8 at graph 0.0, 0.95
#set key at 0.0, 10
set style fill solid noborder

# bfbfbf 805a90 cf597e eeb479 9ccb86 009392 669900 000000

set output filename 
plot \
  sparksdata using (log($1)/log(2)-0.225):3 title "SparkS" with boxes lc rgb "#bfbfbf", \
  middata using (log($1)/log(2)-0.075):3 title "mID\\@QaaD" with boxes lc rgb "#eeb479", \
  aqedata using (log($1)/log(2)+0.075):3 title "AQE\\@QaaD" with boxes lc rgb "#009392", \
  qaaddata using (log($1)/log(2)+0.225):3 title "microPart\\@QaaD" with boxes lc rgb "#000000"

