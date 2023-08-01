output_dir=$1
echo "(1/6) Running the experiments for Fig. 9 (estimated time remaining: 330 hours)"
bash /root/QaaD/scripts/test-num-queries.sh ${output_dir} 120 1
echo "(1/6) Complete the experiments for Fig. 9"
echo "(2/6) Running the experiments for Fig. 10 (estimated time remaining: 270 hours)"
bash /root/QaaD/scripts/test-ap.sh ${output_dir} 120 1
echo "(2/6) Complete the experiments for Fig. 10"
echo "(3/6) Running the experiments for Fig. 11 (estimated time remaining: 225 hours)"
bash /root/QaaD/scripts/test-num-partitions.sh ${output_dir} 15 1
echo "(3/6) Complete the experiments for Fig. 11"
echo "(4/6) Running the experiments for Fig. 12 (estimated time remaining: 210 hours)"
bash /root/QaaD/scripts/test-scalability.sh ${output_dir} 120 1
echo "(4/6) Complete the experiments for Fig. 12"
echo "(5/6) Running the experiments for Fig. 13 (estimated time remaining: 150 hours)"
bash /root/QaaD/scripts/test-arrival-rate.sh ${output_dir} 60 1
echo "(5/6) Complete the experiments for Fig. 13"
echo "(6/6) Running the experiments for Fig. 14 (estimated time remaining: 150 hours)"
bash /root/QaaD/scripts/test-batch-size.sh ${output_dir} 60 1
echo "(6/6) Complete the experiments for Fig. 14"
echo "All experiments are complete, and see eps files in ${output_dir} for the results."
