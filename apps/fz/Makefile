# JDK 17: AGP 8.9 rejects newer JDKs. The AVD lives under XDG_CONFIG_HOME, which the emulator ignores.
export JAVA_HOME ?= /opt/homebrew/opt/openjdk@17
export ANDROID_HOME ?= $(HOME)/Library/Android/sdk
export ANDROID_AVD_HOME ?= $(HOME)/.config/.android/avd
EMULATOR := $(ANDROID_HOME)/emulator/emulator
AVD ?= fz_pixel7
SERIAL ?= emulator-5554

.PHONY: build test lint fmt install home emu

build:
	./gradlew assembleDebug

test:
	./gradlew testDebugUnitTest

lint:
	./gradlew ktlintCheck lintDebug

fmt:
	./gradlew ktlintFormat

# adb -s, not installDebug: Gradle installs on every connected device
install: build
	adb -s $(SERIAL) install -r app/build/outputs/apk/debug/app-debug.apk

home: install
	adb -s $(SERIAL) shell cmd package set-home-activity io.lilfeelz.fz/.MainActivity
	adb -s $(SERIAL) shell input keyevent KEYCODE_HOME

emu:
	$(EMULATOR) -avd $(AVD) -no-snapshot-save -no-boot-anim &
