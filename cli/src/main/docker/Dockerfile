FROM registry.access.redhat.com/ubi8/openjdk-17 AS builder

USER 0

WORKDIR /work/
COPY ./ .

RUN mvn -V -B package -DskipTests

FROM registry.access.redhat.com/ubi8/openjdk-17-runtime
ENV TZ UTC
ENV LANG en_US.UTF-8

USER 0
RUN microdnf install -y gettext wget jq && microdnf clean all

COPY --from=builder /work/cli/target/bacon.jar /home/jboss/

USER 185

