#!/usr/bin/env bash
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

# Ensure all scripts have execute permissions
chmod +x run-server.sh run-client.sh run-db.sh run-cli.sh 2>/dev/null || true

# Free port 8080 if previously used
lsof -ti :8080 | xargs kill -9 2>/dev/null || true

# Open all 3 terminal windows via AppleScript
osascript -e '
tell application "Terminal"
    activate
    set w1 to do script "cd \"'"$PROJECT_ROOT"'\" && ./run-server.sh"
    set custom title of w1 to "1. Throttlix Gateway Server"
    delay 1
    set w2 to do script "cd \"'"$PROJECT_ROOT"'\" && ./run-client.sh"
    set custom title of w2 to "2. Throttlix API Client"
    delay 1
    set w3 to do script "cd \"'"$PROJECT_ROOT"'\" && ./run-db.sh"
    set custom title of w3 to "3. MySQL Database Console"
end tell'

# Open Frontend Web UI in default browser
open "http://localhost:5173/" 2>/dev/null || true

echo "All 3 terminal windows and the Web Dashboard have been launched!"
