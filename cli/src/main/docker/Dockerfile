FROM registry.access.redhat.com/ubi8/openjdk-17 AS builder

USER 0

WORKDIR /work/
COPY ./ .

RUN mvn -V -B package -DskipTests

FROM registry.access.redhat.com/ubi8/openjdk-17-runtime
ENV TZ UTC
ENV LANG en_US.UTF-8

ARG IT_ROOT_CA_2015
ARG IT_ROOT_CA_2022

USER 0
RUN microdnf install -y gettext wget jq && \
    LATEST_YQ_VERSION=$(wget -qO- https://api.github.com/repos/mikefarah/yq/releases/latest | jq -r '.tag_name') && \
    wget https://github.com/mikefarah/yq/releases/download/${LATEST_YQ_VERSION}/yq_linux_amd64 -O /usr/local/bin/yq && \
    chmod +x /usr/local/bin/yq && \
    microdnf clean all

# Install the certs
RUN if [ -n "$IT_ROOT_CA_2015" ]; then \
      echo "$IT_ROOT_CA_2015" > /tmp/IT_ROOT_CA_2015.pem && \
      keytool -importcert -trustcacerts -cacerts \
        -storepass changeit -noprompt \
        -alias 2015-IT-Root-CA \
        -file /tmp/IT_ROOT_CA_2015.pem && \
      rm /tmp/IT_ROOT_CA_2015.pem; \
    fi

RUN if [ -n "$IT_ROOT_CA_2022" ]; then \
      echo "$IT_ROOT_CA_2022" > /tmp/IT_ROOT_CA_2022.pem && \
      keytool -importcert -trustcacerts -cacerts \
        -storepass changeit -noprompt \
        -alias 2022-IT-Root-CA \
        -file /tmp/IT_ROOT_CA_2022.pem && \
      rm /tmp/IT_ROOT_CA_2022.pem; \
    fi
COPY --from=builder /work/cli/target/bacon.jar /home/jboss/

USER 185

