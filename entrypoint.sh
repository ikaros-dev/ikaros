#!/bin/sh

chown -R "${PUID}":"${PGID}" "${WORKDIR_DIR}" "${CONFIG_DIR}"

umask "${UMASK}"

exec su-exec "${PUID}":"${PGID}" java "${JAVA_OPTS}" -jar ${WORKDIR_DIR}/app.jar
