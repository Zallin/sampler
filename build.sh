make clj

docker build -t local/sampler:latest .

docker rm -f sampler

docker run -d --name sampler \
 -p 8088:8088 \
 -e PGHOST=sampler-db \
 -e PGPORT=5432 \
 -e PGUSER=postgres \
 -e PW=bums \
 -e DB=postgres \
 --network=sampler_default \
 local/sampler:latest
