#!/bin/bash

./build.sh

docker-compose -f build/docker-compose.yaml down -v
docker-compose -f build/docker-compose.yaml up
