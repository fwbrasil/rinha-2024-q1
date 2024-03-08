FROM registry.access.redhat.com/ubi9/ubi:9.3-1610

RUN yum -y update && \
    yum -y --skip-broken install zip unzip && \
    yum clean all

RUN curl -s "https://get.sdkman.io" | bash
RUN source /root/.sdkman/bin/sdkman-init.sh && sdk install java 21.0.2-oracle

ENV JAVA_HOME=/root/.sdkman/candidates/java/current
ENV PATH="$JAVA_HOME/bin:$PATH"

RUN curl -sL "https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/3.10.3/gatling-charts-highcharts-bundle-3.10.3-bundle.zip" -o /tmp/gatling-charts-highcharts-bundle-3.10.3-bundle.zip \
    && unzip /tmp/gatling-charts-highcharts-bundle-3.10.3-bundle.zip -d /opt \
    && rm /tmp/gatling-charts-highcharts-bundle-3.10.3-bundle.zip

ENV GATLING_HOME=/opt/gatling-charts-highcharts-bundle-3.10.3
ENV PATH="$GATLING_HOME/bin:$PATH"
ENV GATLING_BIN_DIR=$GATLING_HOME/bin

RUN mkdir -p /opt/app
COPY build/warmup/RinhaBackendCrebitosSimulation.scala /opt/app/

EXPOSE 8080 

CMD ["sh", "-c", "timeout 40s $GATLING_BIN_DIR/gatling.sh -rm local -s RinhaBackendCrebitosSimulation \
        -rd 'Rinha de Backend - 2024/Q1: Warmup' \
        -sf '/opt/app/' > /dev/null 2>&1 || true"]
