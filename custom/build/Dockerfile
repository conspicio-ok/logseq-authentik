FROM node:24-bookworm AS node
FROM eclipse-temurin:21-jdk-jammy AS jdk
FROM ocaml/opam:ubuntu-22.04-ocaml-5.1

USER root

RUN apt-get update \
  && apt-get install -y --no-install-recommends ca-certificates curl git python3 build-essential pkg-config libsecret-1-dev rlwrap \
  && rm -rf /var/lib/apt/lists/*

COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules
COPY --from=jdk /opt/java/openjdk /opt/java/openjdk

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH=/opt/java/openjdk/bin:${PATH}

RUN ln -s ../lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
  && ln -s ../lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx \
  && npm install -g pnpm@10.33.0

RUN curl -fsSLo /tmp/clojure-install.sh https://download.clojure.org/install/linux-install-1.12.4.1618.sh \
  && chmod +x /tmp/clojure-install.sh \
  && /tmp/clojure-install.sh \
  && rm /tmp/clojure-install.sh

COPY build.sh /usr/local/bin/build.sh
RUN chmod 755 /usr/local/bin/build.sh

USER opam
WORKDIR /src

ENTRYPOINT ["/usr/local/bin/build.sh"]
