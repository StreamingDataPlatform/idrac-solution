FROM grafana/grafana:6.7.3

COPY docker/grafana/config/customdashboards.yaml /usr/share/grafana/conf/provisioning/dashboards/customdashboards.yaml
COPY docker/grafana/config/customdashboards.yaml /etc/grafana/provisioning/dashboards/
COPY docker/grafana/grafanaJson/ /var/lib/grafana/dashboards/
