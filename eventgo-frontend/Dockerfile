# Development image
FROM node:20-alpine

WORKDIR /app

# Copy package files for installation
COPY package*.json ./

RUN npm install

# Copy all remaining files into the image
COPY . .

# Expose the port Next.js runs on
EXPOSE 3000

# Start Next.js in development mode
CMD ["npm", "run", "dev"]