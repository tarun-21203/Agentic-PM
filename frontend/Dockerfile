FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

# `deploy.sh` runs `npm run build` first, so `dist/` already exists here.
COPY dist/ /usr/share/nginx/html/

