
#!/bin/bash

YARN_RM_ADDRESS="http://0.0.0.0:30000"

app_id=$(curl -s "$YARN_RM_ADDRESS/ws/v1/cluster/apps" | jq -r '.apps | .app[] | select(.applicationType == "SPARK") | .id' | sort -r | head -n1)

log_dir="hdfs://master:9000/spark-logs"

# Find the log file in HDFS using app_id
input_file=$(/root/dev/hadoop-2.7.7/bin/hdfs dfs -ls "${log_dir}" | grep "${app_id}" | awk '{print $NF}')

if [ -z "$input_file" ]; then
  echo "Error: Log file for app_id not found in HDFS."
  exit 1
fi

# Create a temporary local file
tmp_file=$(mktemp)

# Remove the temporary file if it exists
if [ -e "$tmp_file" ]; then
  rm "$tmp_file"
fi

# Download the log file to the temporary local file
/root/dev/hadoop-2.7.7/bin/hdfs dfs -get "$input_file" "$tmp_file"

submission_time=$(grep "SparkListenerJobStart" "$tmp_file" | tail -n1 | jq -r '.["Submission Time"]')
completion_time=$(grep "SparkListenerJobEnd" "$tmp_file" | tail -n1 | jq -r '.["Completion Time"]')

# Clean up the temporary file
rm "$tmp_file"

elapsed_time_seconds=$(python -c "print(round(($completion_time - $submission_time) / 1000, 1))")
echo $elapsed_time_seconds

