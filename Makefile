export JAVA_HOME ?= /opt/homebrew/opt/openjdk@17
export ANDROID_HOME ?= $(HOME)/Library/Android/sdk
APP ?= fz
SERIAL ?=
AVD ?= fz_pixel7
export ANDROID_AVD_HOME ?= $(HOME)/.config/.android/avd

.PHONY: check check-contract build test lint fmt install home emu
check: check-contract test build lint
check-contract:
	python3 -m unittest discover -s scripts -p 'test_*.py'
build:
	./gradlew --no-daemon :apps:$(APP):assembleDebug
test:
	./gradlew --no-daemon :apps:$(APP):testDebugUnitTest
lint:
	./gradlew --no-daemon ktlintCheck :apps:$(APP):ktlintCheck :apps:$(APP):lintDebug
fmt:
	./gradlew --no-daemon ktlintFormat :apps:$(APP):ktlintFormat

install: build
	@test -n "$(SERIAL)" || { echo 'set SERIAL explicitly' >&2; exit 1; }
	adb -s $(SERIAL) install -r apps/$(APP)/build/outputs/apk/debug/$(APP)-debug.apk
home:
	@test "$(APP)" = fz || { echo 'home applies only to fz' >&2; exit 1; }
	$(MAKE) install APP=fz SERIAL=$(SERIAL)
	adb -s $(SERIAL) shell cmd package set-home-activity io.lilfeelz.fz/.MainActivity
	adb -s $(SERIAL) shell input keyevent KEYCODE_HOME
emu:
	$(ANDROID_HOME)/emulator/emulator -avd $(AVD) -no-snapshot-save -no-boot-anim &
