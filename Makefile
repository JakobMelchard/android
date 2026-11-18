export JAVA_HOME ?= /opt/homebrew/opt/openjdk@17
export ANDROID_HOME ?= $(HOME)/Library/Android/sdk
SERIAL ?=
AVD ?= pull_pixel7
export ANDROID_AVD_HOME ?= $(HOME)/.config/.android/avd

.PHONY: check check-contract build test lint fmt install emu
check: check-contract test build lint
check-contract:
	python3 -m unittest discover -s scripts -p 'test_*.py'
build:
	./gradlew --no-daemon assembleDebug
test:
	./gradlew --no-daemon testDebugUnitTest
lint:
	./gradlew --no-daemon ktlintCheck lintDebug
fmt:
	./gradlew --no-daemon ktlintFormat

install: build
	@test -n "$(SERIAL)" || { echo 'set SERIAL explicitly' >&2; exit 1; }
	adb -s $(SERIAL) install -r build/outputs/apk/debug/pull-debug.apk
emu:
	$(ANDROID_HOME)/emulator/emulator -avd $(AVD) -no-snapshot-save -no-boot-anim &
