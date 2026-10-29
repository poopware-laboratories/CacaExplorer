# Sourced by build.sh and test.sh; prefer an explicitly configured JDK.
if [ -n "${CACA_JAVA_HOME:-}" ]; then
    JAVA_HOME="$CACA_JAVA_HOME"
elif [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ]; then
    JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
fi
if [ -n "${JAVA_HOME:-}" ]; then export JAVA_HOME; PATH="$JAVA_HOME/bin:$PATH"; export PATH; fi
