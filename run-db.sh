#!/usr/bin/env bash
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

echo "================================================================================"
echo "                   THROTTLIX — MYSQL DATABASE CONSOLE                          "
echo "================================================================================"
echo "Connecting to MySQL database 'throttlix' as user 'root'."
echo "Please enter your password at the prompt below:"
echo "================================================================================"
echo ""

if command -v mysql >/dev/null 2>&1; then
    mysql -u root -p throttlix
else
    /opt/homebrew/opt/mysql/bin/mysql -u root -p throttlix
fi
