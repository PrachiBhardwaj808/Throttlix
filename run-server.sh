#!/usr/bin/env bash
set -e
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT/backend"

if [ -d "/opt/homebrew/Cellar/openjdk@25/25.0.3/libexec/openjdk.jdk/Contents/Home" ]; then
    export JAVA_HOME="/opt/homebrew/Cellar/openjdk@25/25.0.3/libexec/openjdk.jdk/Contents/Home"
fi

# Prompt interactively for the DB password if not already set in environment
if [ -z "$DB_PASSWORD" ]; then
    read -rsp "Enter MySQL password for user 'root' (press Enter if empty): " USER_PASS
    echo ""
    export DB_PASSWORD="$USER_PASS"
fi

mvn exec:java -Dexec.args="server" -q
