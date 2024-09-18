FROM registry.access.redhat.com/ubi8/openjdk-17@sha256:632e78989471859ca4ed1148b951a911f7e3b6e6516482be20967c4171612c55 AS builder

USER 0

WORKDIR /work/
COPY ./ .

RUN mvn -V -B package -DskipTests

FROM registry.access.redhat.com/ubi8/openjdk-17-runtime@sha256:aa92e16bbe5fe1fafc8621de5a1a7a88fb96f5b3db73e5a3218c48f98a49552f
ENV TZ UTC
ENV LANG en_US.UTF-8

USER 0
RUN microdnf install -y gettext wget && microdnf clean all 

WORKDIR /work/

COPY --from=builder /work/cli/target/bacon.jar /home/jboss/

USER 185

