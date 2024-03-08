#!/bin/bash

sbt assembly
docker build -t fwbrasil/rinha-2024-q1-app:latest -f build/app/Dockerfile .
docker build -t fwbrasil/rinha-2024-q1-warmup:latest -f build/warmup/Dockerfile .

# docker push fwbrasil/rinha-2024-q1-app:latest
# docker push fwbrasil/rinha-2024-q1-warmup:latest
