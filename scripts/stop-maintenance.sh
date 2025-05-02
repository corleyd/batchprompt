#!/bin/bash
sudo rm /etc/nginx/maintenance_mode
sudo systemctl restart nginx
echo "Maintenance mode disabled. Nginx restarted."