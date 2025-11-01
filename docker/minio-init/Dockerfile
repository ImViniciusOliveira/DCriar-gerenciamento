# Minimal init image that installs the MinIO client (mc) and runs a small Python
# init script to wait for MinIO, create the bucket and apply the policy.
# This is used only during local development to avoid depending on host scripts.

FROM alpine:3.18

ENV MC_VERSION="RELEASE.2024-01-01T00-00-00Z"

RUN apk add --no-cache ca-certificates curl python3 py3-pip bash \
    && curl -fsSL "https://dl.min.io/client/mc/release/linux-amd64/mc" -o /usr/bin/mc \
    && chmod +x /usr/bin/mc \
    && adduser -D -h /home/init init || true

WORKDIR /home/init
COPY init.py /home/init/init.py
RUN chmod +x /home/init/init.py

USER init
ENTRYPOINT ["/usr/bin/python3", "/home/init/init.py"]

