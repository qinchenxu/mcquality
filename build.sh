#!/usr/bin/env zsh
set -e

export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$HOME/gradle/gradle-8.13/bin:$PATH"

echo "Java: $(java -version 2>&1 | head -1)"
echo "Gradle: $(gradle -v 2>/dev/null | grep '^Gradle')"
echo ""

cd "$(dirname "$0")"
if [ "$#" -eq 0 ]; then
	gradle build
else
	gradle "$@"
fi
