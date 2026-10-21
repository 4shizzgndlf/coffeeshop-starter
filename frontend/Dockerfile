# The frontend image: Nginx, its config and the static files - nothing else.
# No Node.js or build step: the files in src/ are served exactly as they are.
FROM nginx:1.30-alpine

# Remove the image's own welcome and error pages, so only our files are served
RUN rm -rf /usr/share/nginx/html/*

COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY src/ /usr/share/nginx/html/

EXPOSE 80
