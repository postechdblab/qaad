import math
import random
import sys
import os

from subprocess import PIPE, run

def out(command):
  result = run(command, stdout=PIPE, stderr=PIPE, universal_newlines=True, shell=True)
  return result.stdout

random.seed(0)

arrival_rate = float(sys.argv[1])
method = sys.argv[2]
dataset = sys.argv[3]
batch_size = float(sys.argv[4])
output_dir = sys.argv[5]

#sparku_query_time = float(sys.argv[2])
#qaad_query_time = float(sys.argv[3])
#file_path = sys.argv[4]

def next_time(rateParameter):
    return -math.log(1.0 - random.random()) / rateParameter

def get_response_time():
  n = 1056
  delta = int(batch_size) / 33
  if dataset == "ebay":
    n = 864
    delta = int(batch_size) / 27
  arrival_time = 0.0
  delivered_time = 0.0
  start_line = 1
  arrival_time_list = list()
  response_time = 0.0
  for qid in range(1, n + 1):
    arrival_time = arrival_time + next_time(arrival_rate)
    arrival_time_list.append(arrival_time)
    if qid % batch_size == 0:
      cmd = '''tail -n 1 %s/%s-%s-r-0-p-448-q-%s-b-%s-s-%s.txt | awk '{sum += $1} END {print sum}' '''%(output_dir, method, dataset, n, int(batch_size), int(start_line))
      query_time = float(out(cmd)) # float(os.system(cmd))
      start_line = start_line + delta
      if delivered_time < arrival_time:
        delivered_time = arrival_time + query_time
      else:
        delivered_time = delivered_time + query_time
      assert(len(arrival_time_list) == batch_size)
      for t in arrival_time_list:
        #print("delivered_time: {}, requested: {}".format(delivered_time, t))
        response_time = response_time + (delivered_time - t)
      arrival_time_list = list()
  response_time = response_time / n
  return response_time

def get_response_time_sparks():
  query_times = []
  with open(output_dir + 'sparks-' + dataset + '-batch.txt', 'r') as file:
    for line in file:
      query_time = float(line.strip())
      query_times.append(query_time)
  n = 1056
  if dataset == "ebay":
    n = 864
  assert(len(query_times) == n)
  arrival_time = 0.0
  delivered_time = 0.0
  start_line = 1
  arrival_time_list = list()
  response_time = 0.0
  for qid in range(1, n + 1):
    arrival_time = arrival_time + next_time(arrival_rate)
    query_time = query_times[qid - 1]
    if delivered_time < arrival_time:
      delivered_time = arrival_time + query_time
    else:
      delivered_time = delivered_time + query_time
    response_time = response_time + (delivered_time - arrival_time)
  response_time = response_time / n
  return response_time

if method == "sparks":
  print(str(get_response_time_sparks()))
else:
  print(str(get_response_time()))
