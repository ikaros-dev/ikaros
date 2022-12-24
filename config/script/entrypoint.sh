#!/bin/sh
chown -R "${PUID}":"${PGID}" "${WORKDIR_DIR}"
chown "${PUID}":"${PGID}" "${CONFIG_DIR}"
chown "${PUID}":"${PGID}" "${IMPORT_DIR}"
umask "${UMASK}"
exec su-exec "${PUID}":"${PGID}" java ${JAVA_OPTS} -jar ${WORKDIR_DIR}/app.jar