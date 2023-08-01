import math
import random
import sys

random.seed(0)

arrival_rate = float(sys.argv[1])
sparku_query_time = float(sys.argv[2])
qaad_query_time = float(sys.argv[3])
file_path = sys.argv[4]

def next_time(rateParameter):
    return -math.log(1.0 - random.random()) / rateParameter

def get_response_time(query_time_arr):
  n = len(query_time_arr)
  arrival_time = 0.0
  sparks_response_time = 0.0
  sparku_response_time = 0
  delivered_time = 0.0
  s = 0
  for qid in range(0, n):
    query_time = query_time_arr[qid]
    arrival_time = arrival_time + next_time(arrival_rate)
    if delivered_time < arrival_time:
      delivered_time = arrival_time + query_time
    else:
      delivered_time = delivered_time + query_time
    sparks_response_time += (delivered_time - arrival_time)
    s += arrival_time
  #print('last arrival time: ' + str(arrival_time))
  return sparks_response_time / n, (arrival_time + sparku_query_time) - s / n, (arrival_time + qaad_query_time) - s / n

query_time_arr = list()
file = open(file_path)
for line in file.readlines():
  query_time_arr.append(float(line))

sparks_response_time, sparku_response_time, qaad_response_time = get_response_time(query_time_arr)
print('sparks_response_time: ' + str(sparks_response_time))
print('sparku_response_time: ' + str(sparku_response_time))
print('qaad_response_time: ' + str(qaad_response_time))
