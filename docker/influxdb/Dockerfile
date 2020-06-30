FROM influxdb:1.8.0-alpine

ENV INFLUXDB_CONFIG_PATH=/etc/influxdb/influxdb.conf

COPY docker/influxdb/config/influxdb.conf /etc/influxdb/influxdb.conf
COPY docker/influxdb/init_scripts /docker-entrypoint-initdb.d

