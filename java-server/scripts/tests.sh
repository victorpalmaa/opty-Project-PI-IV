#!/usr/bin/env bash
set -e

# Custom ASCII Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check Maven
if ! command -v mvn >/dev/null 2>&1; then
    printf "%bError: Maven is not installed%b\n" "$RED" "$NC"
    exit 1
fi

# Check pom.xml exists
if [ ! -f "pom.xml" ]; then
    printf "%bError: pom.xml not found. Run this from the project root.%b\n" "$RED" "$NC"
    exit 1
fi

# Run tests
printf "%bRunning tests...%b\n" "$GREEN" "$NC"
echo ""

# Run specific tests if provided, otherwise run all tests
if [ -n "$1" ]; then
    printf "%bRunning specific test: %s%b\n" "$YELLOW" "$1" "$NC"
    mvn test -Dtest="$1"
else
    mvn test
fi

# Check result
if [ $? -eq 0 ]; then
    echo ""
    printf "%b✅ All tests passed!%b\n" "$GREEN" "$NC"
    exit 0
else
    echo ""
    printf "%b❌ Some tests failed!%b\n" "$RED" "$NC"
    exit 1
fi
