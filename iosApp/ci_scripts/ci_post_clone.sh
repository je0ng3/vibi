#!/bin/sh
# Xcode Cloud post-clone hook. ci_scripts/ 가 iosApp/ 옆에 있어야 자동 실행.
#
# Xcode Cloud macOS runner 에는 JDK 미설치 — KMP 의 ./gradlew 호출이
# 모두 fail. brew 로 JDK 21 을 설치하고 JAVA_HOME 을 export.
#
# Gradle 9.3.1 (gradle/wrapper/gradle-wrapper.properties) 는 JDK 17+ 요구,
# Kotlin 2.x + AGP 9 호환을 위해 21 LTS 선택.

set -eu

echo "[ci_post_clone] installing openjdk@21 via Homebrew"
brew install --quiet openjdk@21

JDK_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
echo "JAVA_HOME=$JDK_HOME"

# Xcode Cloud 는 ci_*.sh 가 export 한 env 를 후속 빌드 phase 로 전파 — preBuildScripts
# 의 ./gradlew 호출이 이 JAVA_HOME 을 그대로 받음.
export JAVA_HOME="$JDK_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

java -version
