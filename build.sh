#!/bin/bash

sbt assembly

docker build -t fwbrasil/rinha-2024-q1:latest -f build/Dockerfile .

docker push fwbrasil/rinha-2024-q1:latest
