# ==========================================
# Stage 1: Build the React Application
# ==========================================
FROM node:20-alpine AS build
WORKDIR /app

# Copy dependency files
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy application source code
COPY . .

# Build the Vite application
RUN npm run build

# ==========================================
# Stage 2: Serve using Nginx
# ==========================================
FROM nginx:alpine

# Copy custom build output to nginx document root
COPY --from=build /app/dist /usr/share/nginx/html

# Expose port 80
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
