# Build stage
FROM node:20-alpine

# Set working directory
WORKDIR /app

# Install pnpm
RUN npm install -g pnpm

# Copy package files
COPY package.json pnpm-lock.yaml ./

# Install dependencies
RUN pnpm install --no-frozen-lockfile

# Copy source code
COPY . .

# Build argument for API base URL
ARG VITE_API_BASE_URL=http://localhost:8080/api
ENV NEXT_PUBLIC_API_BASE_URL=$VITE_API_BASE_URL

# Build the Next.js application
RUN pnpm run build

# Expose port
EXPOSE 3000

# Start the Next.js application
CMD ["pnpm", "start"]
