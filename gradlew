#!/bin/sh
# Gradle wrapper script for Android builds

WRAPPER_JAR="${GRADLE_USER_HOME}/wrapper/dists/gradle-8.7-bin/*/gradle-8.7/lib/gradle-launcher-*.jar"
exec java -Xmx64m -Xms64m -classpath "$WRAPPER_JAR" org.gradle.launcher.GradleMain "$@"