FROM eclipse-temurin:21

RUN mkdir /opt/app
RUN mkdir -p /app/data

COPY ../target/scala-3.3.1/rinha-2024-q1-assembly-0.1.0-SNAPSHOT.jar /opt/app

EXPOSE 8080 

CMD java -cp /opt/app/rinha-2024-q1-assembly-0.1.0-SNAPSHOT.jar \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.nio=ALL-UNNAMED \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
  -Dio.netty.eventLoopThreads=25 \
  -Dscala.reflect.classtag.cache.disable=true \
  -Dkyo.scheduler.coordinator.enable=false \
  -Dkyo.scheduler.coreWorkers=1 \
  rinha.Server
