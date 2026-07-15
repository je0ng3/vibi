#!/bin/sh
# Xcode Cloud pre-xcodebuild hook. archive 직전에 실행된다.
#
# 빌드 번호(CFBundleVersion)를 Xcode Cloud 가 주는 $CI_BUILD_NUMBER 로 덮어쓴다.
# CI_BUILD_NUMBER 는 워크플로 전체에서 단조증가하는 고유 정수라, 매 업로드가 유일한
# 빌드 번호를 갖게 돼 ASC 의 "bundle version already exists / must be higher" 거절을 막는다.
# (project.yml·Info.plist 에 박힌 정적 "1" 을 그대로 올리면 두 번째 푸시부터 충돌 → 재빌드 지옥.)
#
# CI 에서만 동작 — 로컬 빌드는 CI_BUILD_NUMBER 가 없어 project.yml 의 값을 그대로 쓴다.
# xcodegen 은 CI 에서 재생성하지 않고(커밋된 xcodeproj/Info.plist 사용), 이 훅이 archive
# 전에 돌므로 patch 가 산출물에 반영된다. 마케팅 버전(train)은 여기서 안 건드린다 — 출시로
# train 이 닫히면 CFBundleShortVersionString 을 사람이 올려야 한다(빌드 번호로는 못 푼다).

set -eu

if [ -z "${CI_BUILD_NUMBER:-}" ]; then
  echo "[ci_pre_xcodebuild] CI_BUILD_NUMBER unset — skip build-number override"
  exit 0
fi

INFO_PLIST="${CI_PRIMARY_REPOSITORY_PATH:-$PWD}/iosApp/iosApp/Info.plist"

if [ ! -f "$INFO_PLIST" ]; then
  echo "[ci_pre_xcodebuild] ERROR: Info.plist not found at $INFO_PLIST" >&2
  exit 1
fi

plutil -replace CFBundleVersion -string "$CI_BUILD_NUMBER" "$INFO_PLIST"
echo "[ci_pre_xcodebuild] CFBundleVersion set to $CI_BUILD_NUMBER in $INFO_PLIST"
