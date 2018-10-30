FROM nginx:1.15-alpine
MAINTAINER CSI csi@telenordigital.com

# Copy configuration
COPY config/nginx.conf /etc/nginx/nginx.conf

# Copy certificates
COPY certs/nginx.crt /etc/nginx/ssl.crt
COPY certs/nginx.key /etc/nginx/ssl.key

# Expose DIAMETER (3868) port
EXPOSE 3868

CMD ["nginx"]