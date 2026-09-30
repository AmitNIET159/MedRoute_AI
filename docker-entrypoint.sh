#!/bin/bash
# =====================================================
# MedRoute AI â€” Docker Entrypoint
# Dynamically sets Tomcat port from PORT env var
# (Required by Render, Railway, and other PaaS providers)
# =====================================================

set -e

# Use PORT env var if set (Render provides this), default 8080
PORT="${PORT:-8080}"

echo "============================================="
echo "  MedRoute AI â€” Starting on port ${PORT}"
echo "============================================="

# Disable Tomcat's shutdown port (8005) so Render's health checks don't hit it
sed -i 's/port="8005"/port="-1"/' /usr/local/tomcat/conf/server.xml

# Replace Tomcat's default 8080 connector port with the PORT env var
sed -i "s/port=\"8080\"/port=\"${PORT}\"/" /usr/local/tomcat/conf/server.xml

# Start Tomcat in foreground
catalina.sh run
