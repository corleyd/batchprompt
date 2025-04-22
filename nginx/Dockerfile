FROM nginx:alpine

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy SSL certificates (in production, consider using volumes or secrets)
# COPY ./certs/cert.pem /etc/nginx/ssl/cert.pem
# COPY ./certs/key.pem /etc/nginx/ssl/key.pem

EXPOSE 443