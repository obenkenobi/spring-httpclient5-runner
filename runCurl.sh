seq 1 200 | xargs -I $ -n1 -P4  curl "http://localhost:8080/stream"