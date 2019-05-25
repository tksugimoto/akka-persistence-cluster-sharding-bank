FROM openjdk:8-jre-alpine3.9

ARG SBT_VERSION=1.2.8
ARG SBT_HOME=/usr/local/sbt

ENV PATH ${PATH}:${SBT_HOME}/bin

RUN mkdir -p $SBT_HOME
RUN apk add --no-cache --update-cache bash
RUN wget -qO - "https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz" \
    | tar xz -C $SBT_HOME --strip-components=1

VOLUME /root/.ivy2
VOLUME /root/.sbt
WORKDIR /app

RUN apk add --no-cache --update-cache tini
ENTRYPOINT ["/sbin/tini", "--"]
