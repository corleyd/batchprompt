#!/bin/bash
sudo touch /etc/nginx/maintenance_mode
sudo systemctl restart nginx
echo "Maintenance mode enabled. Nginx restarted."
