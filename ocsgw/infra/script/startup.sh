#!/usr/bin/env bash

# This script is used in GCE to cleanup Docker on each reboot

tee /etc/systemd/system/docker-prune.service <<EOF
[Unit]
Description=Prune unused Docker images
[Service]
Type=simple
ExecStart=/usr/bin/docker system prune -a -f --volumes
[Install]
WantedBy=default.target
EOF

tee /etc/systemd/system/docker-prune.timer <<EOF
[Unit]
Description=Prune unused Docker images daily
RefuseManualStart=no
RefuseManualStop=no
[Timer]
Persistent=false
OnBootSec=80
OnCalendar=daily
Unit=docker-prune.service
[Install]
WantedBy=timers.target
EOF

systemctl start docker-prune.timer
systemctl enable docker-prune.timer
