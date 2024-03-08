FROM registry.access.redhat.com/ubi9/ubi:9.3-1610

RUN yum -y update && \
    yum -y --skip-broken install zip unzip && \
    yum clean all

RUN curl -s "https://get.sdkman.io" | bash
RUN source /root/.sdkman/bin/sdkman-init.sh && sdk install java  21.0.2-graal

ENV JAVA_HOME=/root/.sdkman/candidates/java/current
ENV PATH="$JAVA_HOME/bin:$PATH"

RUN mkdir /opt/app
RUN mkdir -p /app/data

COPY target/scala-3.3.1/rinha-2024-q1-assembly-0.1.0-SNAPSHOT.jar /opt/app

EXPOSE 8080 

CMD java -cp /opt/app/rinha-2024-q1-assembly-0.1.0-SNAPSHOT.jar \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.nio=ALL-UNNAMED \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
  -XX:+UseZGC \
  -Xmx150M -Xms150M \
  -Dio.netty.eventLoopThreads=8 \
  -Dio.netty.leakDetection.level=DISABLED \
  -Dscala.reflect.classtag.cache.disable=true \
  -Dkyo.scheduler.coreWorkers=1 \
  -Dkyo.scheduler.virtualizeWorkers=false \
  rinha.Server
