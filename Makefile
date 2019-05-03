PGHOST = localhost
PGPORT = 6003
PGUSER = postgres
PW = bums
DB = postgres

TEST_DB = bams
PROD_DB = postgres

.EXPORT_ALL_VARIABLES:
.PHONY: test

repl:
	clj -A:dev -m "tools.repl" -p 3002

test:
	clj -A:dev -m "tools.test"

clj:
	clj -m "tools.build" "target/app.jar"

start:
	clj -A:dev -m sampler.rest	
