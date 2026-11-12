# pinned on purpose with := (not ?=): the shell's JAVA_HOME (e.g. sdkman's "current") may point
# at an older JDK, and running the JDK-25-built jar on it fails with UnsupportedClassVersionError.
# The fallback is for machines that are not this one - a CI runner gets its JDK from setup-java, and
# pinning a path that does not exist there turns every target into "no such file or directory"
# (#333). Where the pinned JDK is present it still wins, which is the whole point of the pin.
# Which JDK that is, .sdkmanrc says - the same one `sdk env` sets the shell to, so that make and
# ./gradlew share a daemon - and nothing here repeats its version (#586)
PINNED_JAVA_HOME := $(HOME)/.sdkman/candidates/java/$(shell sed -n 's/^java=//p' .sdkmanrc)
JAVA_HOME := $(if $(wildcard $(PINNED_JAVA_HOME)),$(PINNED_JAVA_HOME),$(JAVA_HOME))
# the daemon arguments of gradle.properties, for the plugin builds: they are builds of their own
# without a gradle.properties, and with these they share the host's daemon instead of starting one of
# their own (#580)
PLUGIN_DAEMON := "-Dorg.gradle.jvmargs=$(shell sed -n 's/^org.gradle.jvmargs=//p' gradle.properties)" -Dorg.gradle.daemon.idletimeout=$(shell sed -n 's/^org.gradle.daemon.idletimeout=//p' gradle.properties)
JAR := $(shell find build/libs -maxdepth 1 -name '*-all.jar' 2>/dev/null | head -1)
# the published release a vault this build writes has to stay readable in (#402); gradle.properties
# is the one place the number lives
RELEASE_JAR_VERSION := $(shell sed -n 's/^formatCompatibilityRelease=//p' gradle.properties)
ENVELOPE_RELEASE_JAR_VERSION := $(shell sed -n 's/^envelopeMoveRelease=//p' gradle.properties)

PLUGIN_OBFUSCATION_DIR := plugins/obfuscation-plugin
PLUGIN_CHECKSUM_DIR := plugins/checksum-plugin
PLUGIN_CONVERSION_DIR := plugins/conversion-plugin
PLUGIN_CONSOLE_DIR := plugins/console-plugin
PLUGIN_KEYGEN_DIR := plugins/keygen-plugin
PLUGIN_CERTIFICATE_DIR := plugins/certificate-plugin
PLUGIN_PASSWORD_HASH_DIR := plugins/password-hash-plugin
PLUGIN_KEM_DEMO_DIR := plugins/kem-demo-plugin
PLUGIN_MENU_DESIGNER_DIR := plugins/menu-designer-plugin
PLUGIN_PQC_SIGNATURE_DIR := plugins/pqc-signature-plugin
PLUGIN_KEYSTORE_DIR := plugins/keystore-plugin
PLUGIN_FILE_CRYPT_DIR := plugins/file-crypt-plugin
PLUGIN_SECRET_SHARING_DIR := plugins/secret-sharing-plugin
PLUGIN_LETHENON_DIR := plugins/lethenon-plugin
PLUGIN_RELEASE_KEY_DIR := plugins/release-key-plugin
PLUGIN_INSTALL_DIR := $(HOME)/.config/mystic-crypt-ui/plugins
# what every plugin target runs besides packaging its zip: the plugin's unit tests, and for the two
# plugins whose zip is started in a test of its own, that test. make run builds the zips without
# them, because a start does not need fifteen test suites run again (#909)
PLUGIN_TESTS ?= test
PLUGIN_ZIP_TESTS ?= zipTest
SCRIPT_TESTS ?= yes

.PHONY: help verify-installer merge-pr test-fast release-jar release-jar-if-reachable build build-full build-with-plugins bwp run run-isolated start all clean test test-e2e test-e2e-demo \
	clean-build-installer izpack-installer izpack-installer-signed release-gate \
	dependencies dependency-updates jacoco-coverage jacoco-report jar javadoc \
	license-format publish-local spotless-java spotless-misc tag-release \
	version-catalog-format version-catalog-update all-dependencies-jar \
	build-stacktrace build-warning bump-check plugin-obfuscation plugin-checksum plugin-conversion \
	plugin-console plugin-keygen plugin-certificate plugin-password-hash plugin-kem-demo \
	plugin-menu-designer plugin-pqc-signature plugin-keystore plugin-file-crypt plugin-secret-sharing plugin-lethenon plugin-release-key plugins plugins-install \
	install-hooks

# `make` without a target lists the targets: the first rule used to be install-hooks, so a bare
# make set core.hooksPath while README.md said it built and started the application (#911)
.DEFAULT_GOAL := help

# every target, grouped, with the description on its own rule line after ##, so the Makefile is the
# one place a target is described; MakefileTargetsAreDescribedTest fails on a target without one
HELP_GROUPS := start build test plugins quality release
help: ## [help] lists every target with what it does
	@for group in $(HELP_GROUPS); do \
		printf '\n%s\n' "$$group"; \
		sed -n "s/^\([a-zA-Z0-9_-]*\):.*## \[$$group\] \(.*\)$$/\1|\2/p" $(firstword $(MAKEFILE_LIST)) \
			| awk -F'|' '{ printf "  %-26s %s\n", $$1, $$2 }'; \
	done

# point git at the repository's own hooks, so the pre-commit formatting check runs before a commit exists.
# A hook is per working copy: run this once in every fresh clone and every new worktree (#373)
install-hooks: ## [quality] point git at .githooks, so the pre-commit Spotless check runs; once per clone and worktree
	git config core.hooksPath .githooks
	@printf 'hooks installed: core.hooksPath -> %s\n' "$$(git config core.hooksPath)"

# fast build: clean, compile, package the runnable jar - skips tests/spotless/license
build: ## [build] the runnable jar build/libs/*-all.jar, after a clean; no tests, no Spotless
	JAVA_HOME=$(JAVA_HOME) ./gradlew createAllDependendiesJar $(GRADLE_FLAGS)

# full build: package the runnable jar first, then compile, test, spotless and license.
# The packaging task cleans - it has to, because the jar signing globs over build/libs and would
# otherwise sign leftovers from an earlier build - so running it second wiped the test report the
# first invocation had just written, and a green gate could not say what it had measured (#227).
# the release gate. It depends on plugins because `./gradlew build` does not build them and the
# end-to-end suite needs their zips: until #333 the 54 tests that install a plugin SKIPPED here, so
# a release could be cut with every plugin feature unverified and the gate still green. They fail
# now, which is only useful if the gate builds what they need (#333)
build-full: plugins release-jar ## [build] every plugin with its tests, the release jars, the runnable jar, then gradlew build: every test suite and Spotless
	JAVA_HOME=$(JAVA_HOME) ./gradlew createAllDependendiesJar $(GRADLE_FLAGS)
	JAVA_HOME=$(JAVA_HOME) ./gradlew build $(GRADLE_FLAGS)

# host jar plus every internal plugin, built and installed into the app's plugins directory -
# root `build` alone never touches plugins/ (each is its own separate Gradle build), so this is
# the one command that leaves both the jar and the installed plugins current
build-with-plugins: build plugins-install ## [build] build, then plugins-install

# shortcut alias for build-with-plugins
bwp: build-with-plugins ## [build] the same as build-with-plugins

# the development build with its internal plugins, in a profile of its own - the same as
# run-isolated below: it never reads or writes the installed release's ~/.config/mystic-crypt-ui, so
# develop's plugins (Lethenon among them) show up here and stay away from the release (#501)
run: run-isolated ## [start] the same as run-isolated

# the development build and the internal plugins built from this checkout, started in a profile of
# its own: user.home points at ISOLATED_HOME, so its configuration and plugins directory is
# ISOLATED_HOME/.config/mystic-crypt-ui and never the installed release's (#498). A vault is opened
# where it lies; the profile holds settings, the sign-in memory and the plugins
ISOLATED_HOME ?= $(HOME)/mystic-crypt-ui-dev
# The plugins are built without their tests here: a start needs their zips, not fifteen test suites
# run again; make plugins and the gates still run them (#909)
run-isolated: ## [start] the plugin zips without their tests, the runnable jar, then a start in ISOLATED_HOME (default ~/mystic-crypt-ui-dev)
	$(MAKE) plugins PLUGIN_TESTS= PLUGIN_ZIP_TESTS= SCRIPT_TESTS=no GRADLE_FLAGS="$(GRADLE_FLAGS)"
	JAVA_HOME=$(JAVA_HOME) ./gradlew withAllDependendiesJar $(GRADLE_FLAGS)
	JAVA_HOME=$(JAVA_HOME) ./scripts/run-isolated.sh "$(ISOLATED_HOME)"

# starts what is already built - the jar in build/libs and the plugin zips under plugins/*/build - in
# the profile of run-isolated, without building or testing anything, so it takes seconds. After a
# change in the code, make run builds first; without a build to start, the script says so (#909)
start: ## [start] starts the last build in ISOLATED_HOME, nothing is built; refuses when there is no build
	JAVA_HOME=$(JAVA_HOME) ./scripts/run-isolated.sh "$(ISOLATED_HOME)"

# build then run - always launches exactly what was just built, with the standard internal
# plugins built and installed into the app's plugins directory beforehand
all: build-with-plugins run ## [start] build-with-plugins, then run

test: release-jar-if-reachable ## [test] the unit tests, on an Xvfb of their own; fetches the release jars the compatibility tests need when it can
	JAVA_HOME=$(JAVA_HOME) ./gradlew test $(GRADLE_FLAGS)

# the cheap local run: the unit suite only, on a display the build starts itself (#319, #505)
test-fast: release-jar-if-reachable ## [test] the same as test
	JAVA_HOME=$(JAVA_HOME) ./gradlew test $(GRADLE_FLAGS)

# the application jar of the last release, downloaded, checked against its published sha256 and
# cached per version. A failure here fails the target: CI and the release gate must not run the
# compatibility test without it (#402)
release-jar: ## [test] downloads the jars of the last releases the compatibility tests read, and fails without them
	./scripts/fetch-release-jar.sh $(RELEASE_JAR_VERSION) && ./scripts/fetch-release-jar.sh $(ENVELOPE_RELEASE_JAR_VERSION)

# the same for a local run, where no network is not an error: the compatibility test then skips and
# says why (#402)
release-jar-if-reachable: ## [test] the same as release-jar, but a download that fails only skips those tests
	@./scripts/fetch-release-jar.sh $(RELEASE_JAR_VERSION) || echo "==> release jar $(RELEASE_JAR_VERSION) not fetched; VaultOpensInTheLastReleaseTest will skip locally (in CI it fails)"
	@./scripts/fetch-release-jar.sh $(ENVELOPE_RELEASE_JAR_VERSION) || echo "==> release jar $(ENVELOPE_RELEASE_JAR_VERSION) not fetched; TheEnvelopeMoveKeepsTheVaultFormatTest will skip locally (in CI it fails)"

# merges a pull request the way the rules describe, stopping at the first step that fails (#324)
# TARGET defaults to develop; a hotfix pull request names its branch, e.g. TARGET=hotfix/8.5.1 (#395)
merge-pr: ## [release] merges a pull request the way the rules say: make merge-pr PR=<number> [TARGET=<branch>]
	@test -n "$(PR)" || (echo "usage: make merge-pr PR=<number> [TARGET=<branch>]" && false)
	python3 ./scripts/merge_pr.py $(PR) $(or $(TARGET),develop)

# end-to-end UI tests (AssertJ-Swing) - fast mode (default): as fast as possible.
# They run on a display of the build's own, as every test task of every target does: the own-display
# plugin starts an Xvfb, never uses the display of the shell the build is started from (#504, #505),
# and verifies that a window manager is actually answering before a test starts: without one the
# suite does not fail, it hangs (#322, #741).
# Both also build the plugins first, because a missing plugin zip now FAILS its tests instead of
# skipping them (#333) - CI builds them before the suite for the same reason, and a target that
# leaves 54 tests red by construction would only teach people to ignore them
test-e2e: plugins ## [test] every plugin with its tests, then the end-to-end suites e2eTest, e2eLockTest and e2eKdbxTest on an Xvfb of their own
	JAVA_HOME=$(JAVA_HOME) ./gradlew e2eTest e2eLockTest e2eKdbxTest $(GRADLE_FLAGS)

# end-to-end UI tests in demo mode: paced like a real user, watchable on screen. The one target that
# runs on the current display, by setting E2E_USE_CURRENT_DISPLAY=1, which the own-display plugin
# honours - watching is its whole point (#504). Every other way into the tests gets a display of its own
test-e2e-demo: plugins ## [test] the end-to-end suites paced like a user, on the current display, to watch
	E2E_USE_CURRENT_DISPLAY=1 JAVA_HOME=$(JAVA_HOME) ./gradlew e2eTest --rerun e2eLockTest --rerun e2eKdbxTest --rerun -Dmystic.crypt.ui.test.mode=demo $(GRADLE_FLAGS)

clean: ## [build] gradlew clean
	JAVA_HOME=$(JAVA_HOME) ./gradlew clean $(GRADLE_FLAGS)

# --- internal plugins ---

# publish the host to the local Maven cache so the plugins can compile against the current API.
# GRADLE_FLAGS reach it like every plugin target: with -PuseMavenLocal the host has to resolve a
# library candidate from the local repository as well, or it is built against Central's version (#508)
publish-local: ## [plugins] publishes the host to ~/.m2, which every plugin build compiles against; the plugin targets run it first
	JAVA_HOME=$(JAVA_HOME) ./gradlew publishToMavenLocal -x test $(GRADLE_FLAGS)

# build the internal obfuscation plugin zip (needs the host published locally first)
plugin-obfuscation: publish-local ## [plugins] the obfuscation plugin: its tests and zip
	JAVA_HOME=$(JAVA_HOME) ./gradlew $(PLUGIN_DAEMON) -p $(PLUGIN_OBFUSCATION_DIR) $(PLUGIN_TESTS) pluginZip $(GRADLE_FLAGS)
	@echo "==> plugin zip: $$(find $(PLUGIN_OBFUSCATION_DIR)/build/plugin-dist -name '*.zip')"

# build the internal checksum plugin zip (needs the host published locally first)
plugin-checksum: publish-local ## [plugins] the checksum plugin: its tests and zip
	JAVA_HOME=$(JAVA_HOME) ./gradlew $(PLUGIN_DAEMON) -p $(PLUGIN_CHECKSUM_DIR) $(PLUGIN_TESTS) pluginZip $(GRADLE_FLAGS)
	@echo "==> plugin zip: $$(find $(PLUGIN_CHECKSUM_DIR)/build/plugin-dist -name '*.zip')"

# build the internal der-to-pem conversion plugin zip (needs the host published locally first)
plugin-conversion: publish-local ## [plugins] the conversion plugin: its tests and zip
	JAVA_HOME=$(JAVA_HOME) ./gradlew $(PLUGIN_DAEMON) -p $(PLUGIN_CONVERSION_DIR) $(PLUGIN_TESTS) pluginZip $(GRADLE_FLAGS)
	@echo "==> plugin zip: $$(find $(PLUGIN_CONVERSION_DIR)/build/plugin-dist -name '*.zip')"

# build the internal console plugin zip (needs the host published locally first)
plugin-console: publish-local ## [plugins] the console plugin: its tests and zip
	JAVA_HOME=$(JAVA_HOME) ./gradlew $(PLUGIN_DAEMON) -p $(PLUGIN_CONSOLE_DIR) $(PLUGIN_TESTS) pluginZip $(GRADLE_FLAGS)
	@echo "==> plugin zip: $$(find $(PLUGIN_CONSOLE_DIR)/build/plugin-dist -name '*.zip')"

# build the internal keygen plugin zip (needs the host published locally first)
plugin-keygen: publish-local ## [plugins] the keygen plugin: its tests and zip
	JAVA_HOME=$(JAVA_HOME) ./gradlew $(PLUGIN_DAEMON) -p $(PLUGIN_KEYGEN_DIR) $(PLUGIN_TESTS) pluginZip $(GRADLE_FLAGS)
	@echo "==> plugin zip: $$(find $(PLUGIN_KEYGEN_DIR)/build/plugin-dist -name '*.zip')"

# build the internal certificate plugin zip (needs the host published locally first)
plugin-certificate: publish-local ## [plugins] the certificate plugin: its tests and zip
	JAVA_HOME=$(JAVA_HOME) ./gradlew $(PLUGIN_DAEMON) -p $(PLUGIN_CERTIFICATE_DIR) $(PLUGIN_TESTS) pluginZip $(GRADLE_FLAGS)
	@echo "==> plugin zip: $$(find $(PLUGIN_CERTIFICATE_DIR)/build/plugin-dist -name '*.zip')"

# build the internal password-hash plugin zip (needs the host published locally first)
plugin-password-hash: publish-local ## [plugins] the password-hash plugin: its tests and zip
	JAVA_HOME=$(JAVA_HOME) ./gradlew $(PLUGIN_DAEMON) -p $(PLUGIN_PASSWORD_HASH_DIR) $(PLUGIN_TESTS) pluginZip $(GRADLE_FLAGS)
	@echo "==> plugin zip: $$(find $(PLUGIN_PASSWORD_HASH_DIR)/build/plugin-dist -name '*.zip')"

# build the internal ML-KEM / hybrid key-encapsulation demo plugin zip (needs the host published
# locally first)
plugin-kem-demo: publish-local ## [plugins] the kem-demo plugin: its tests and zip
	JAVA_HOME=$(JAVA_HOME) ./gradlew $(PLUGIN_DAEMON) -p $(PLUGIN_KEM_DEMO_DIR) $(PLUGIN_TESTS) pluginZip $(GRADLE_FLAGS)
	@echo "==> plugin zip: $$(find $(PLUGIN_KEM_DEMO_DIR)/build/plugin-dist -name '*.zip')"

# build the internal menu-designer plugin zip (needs the host published locally first)
plugin-menu-designer: publish-local ## [plugins] the menu designer plugin: its tests and zip; development tooling, not in the installer
	JAVA_HOME=$(JAVA_HOME) ./gradlew $(PLUGIN_DAEMON) -p $(PLUGIN_MENU_DESIGNER_DIR) $(PLUGIN_TESTS) pluginZip $(GRADLE_FLAGS)
	@echo "==> plugin zip: $$(find $(PLUGIN_MENU_DESIGNER_DIR)/build/plugin-dist -name '*.zip')"

# build the internal post-quantum signature plugin zip (needs the host published locally first)
plugin-pqc-signature: publish-local ## [plugins] the pqc-signature plugin: its tests and zip
	JAVA_HOME=$(JAVA_HOME) ./gradlew $(PLUGIN_DAEMON) -p $(PLUGIN_PQC_SIGNATURE_DIR) $(PLUGIN_TESTS) pluginZip $(GRADLE_FLAGS)
	@echo "==> plugin zip: $$(find $(PLUGIN_PQC_SIGNATURE_DIR)/build/plugin-dist -name '*.zip')"

# build the internal key store plugin zip (needs the host published locally first)
plugin-keystore: publish-local ## [plugins] the keystore plugin: its tests and zip
	JAVA_HOME=$(JAVA_HOME) ./gradlew $(PLUGIN_DAEMON) -p $(PLUGIN_KEYSTORE_DIR) $(PLUGIN_TESTS) pluginZip $(GRADLE_FLAGS)
	@echo "==> plugin zip: $$(find $(PLUGIN_KEYSTORE_DIR)/build/plugin-dist -name '*.zip')"

# build the internal file encryption plugin zip (needs the host published locally first)
plugin-file-crypt: publish-local ## [plugins] the file-crypt plugin: its tests and zip
	JAVA_HOME=$(JAVA_HOME) ./gradlew $(PLUGIN_DAEMON) -p $(PLUGIN_FILE_CRYPT_DIR) $(PLUGIN_TESTS) pluginZip $(GRADLE_FLAGS)
	@echo "==> plugin zip: $$(find $(PLUGIN_FILE_CRYPT_DIR)/build/plugin-dist -name '*.zip')"

# build the internal secret sharing plugin zip (needs the host published locally first)
plugin-secret-sharing: publish-local ## [plugins] the secret-sharing plugin: its tests and zip
	JAVA_HOME=$(JAVA_HOME) ./gradlew $(PLUGIN_DAEMON) -p $(PLUGIN_SECRET_SHARING_DIR) $(PLUGIN_TESTS) pluginZip $(GRADLE_FLAGS)
	@echo "==> plugin zip: $$(find $(PLUGIN_SECRET_SHARING_DIR)/build/plugin-dist -name '*.zip')"

# build the internal Lethenon chain plugin zip (needs the host published locally first). Unlike
# the others this zip carries a library in lib/: the chain itself, which the host does not ship
plugin-lethenon: publish-local ## [plugins] the Lethenon plugin: its tests, the test that starts it from its zip, the zip, which carries the lethenon library
	JAVA_HOME=$(JAVA_HOME) ./gradlew $(PLUGIN_DAEMON) -p $(PLUGIN_LETHENON_DIR) $(PLUGIN_TESTS) $(PLUGIN_ZIP_TESTS) pluginZip $(GRADLE_FLAGS)
	@echo "==> plugin zip: $$(find $(PLUGIN_LETHENON_DIR)/build/plugin-dist -name '*.zip')"

# build the internal release key plugin zip (needs the host published locally first). It reads the
# GnuPG home through mystic-crypt, which the host ships, so the zip carries no library (#644). Its
# tests run gpg, in temporary homes of their own
plugin-release-key: publish-local ## [plugins] the release key plugin: its tests, which run gpg, the test that starts it from its zip, the zip
	JAVA_HOME=$(JAVA_HOME) ./gradlew $(PLUGIN_DAEMON) -p $(PLUGIN_RELEASE_KEY_DIR) $(PLUGIN_TESTS) $(PLUGIN_ZIP_TESTS) pluginZip $(GRADLE_FLAGS)
	@echo "==> plugin zip: $$(find $(PLUGIN_RELEASE_KEY_DIR)/build/plugin-dist -name '*.zip')"

# the pre-check for ANY bump of crypt-data or mystic-crypt: the host's own suites AND every
# internal plugin. The plugins depend on crypt-data DIRECTLY, so a breaking change there can leave
# the host green and a plugin red - measured on the 13.3 bump (#480), where crypt-data 13.0's
# refusal to write PKCS#1 for a key without a traditional form failed four cases in the keygen
# plugin while the host's 570 unit tests passed. Pass -PuseMavenLocal through when the candidate is
# only published locally:  make bump-check GRADLE_FLAGS=-PuseMavenLocal - the host and every plugin
# build then take the family's libraries from the local repository (gradle/plugin-repositories.gradle,
# #508), which is the point: the check runs before the candidate is on Central
bump-check: ## [plugins] before a crypt-data or mystic-crypt bump: the host unit tests and every plugin
	JAVA_HOME=$(JAVA_HOME) ./gradlew test $(GRADLE_FLAGS)
	$(MAKE) plugins GRADLE_FLAGS="$(GRADLE_FLAGS)"
	@echo "==> bump-check: the host suites and all internal plugins are green"

# build every internal plugin
plugins: plugin-obfuscation plugin-checksum plugin-conversion plugin-console plugin-keygen plugin-certificate plugin-password-hash plugin-kem-demo plugin-menu-designer plugin-pqc-signature plugin-keystore plugin-file-crypt plugin-secret-sharing plugin-lethenon plugin-release-key ## [plugins] every internal plugin with its tests and zip, and the two script test suites
ifeq ($(SCRIPT_TESTS),yes)
	python3 scripts/merge_pr_test.py
	python3 scripts/assemble_changelog_test.py
endif

# build all internal plugins and install them into the app's plugins directory
plugins-install: plugins ## [plugins] plugins, then copies the zips into ~/.config/mystic-crypt-ui/plugins, the real profile
	mkdir -p "$(PLUGIN_INSTALL_DIR)"
	cp $(PLUGIN_OBFUSCATION_DIR)/build/plugin-dist/*.zip "$(PLUGIN_INSTALL_DIR)/"
	cp $(PLUGIN_CHECKSUM_DIR)/build/plugin-dist/*.zip "$(PLUGIN_INSTALL_DIR)/"
	cp $(PLUGIN_CONVERSION_DIR)/build/plugin-dist/*.zip "$(PLUGIN_INSTALL_DIR)/"
	cp $(PLUGIN_CONSOLE_DIR)/build/plugin-dist/*.zip "$(PLUGIN_INSTALL_DIR)/"
	cp $(PLUGIN_KEYGEN_DIR)/build/plugin-dist/*.zip "$(PLUGIN_INSTALL_DIR)/"
	cp $(PLUGIN_CERTIFICATE_DIR)/build/plugin-dist/*.zip "$(PLUGIN_INSTALL_DIR)/"
	cp $(PLUGIN_PASSWORD_HASH_DIR)/build/plugin-dist/*.zip "$(PLUGIN_INSTALL_DIR)/"
	cp $(PLUGIN_KEM_DEMO_DIR)/build/plugin-dist/*.zip "$(PLUGIN_INSTALL_DIR)/"
	cp $(PLUGIN_MENU_DESIGNER_DIR)/build/plugin-dist/*.zip "$(PLUGIN_INSTALL_DIR)/"
	cp $(PLUGIN_PQC_SIGNATURE_DIR)/build/plugin-dist/*.zip "$(PLUGIN_INSTALL_DIR)/"
	cp $(PLUGIN_KEYSTORE_DIR)/build/plugin-dist/*.zip "$(PLUGIN_INSTALL_DIR)/"
	cp $(PLUGIN_FILE_CRYPT_DIR)/build/plugin-dist/*.zip "$(PLUGIN_INSTALL_DIR)/"
	cp $(PLUGIN_SECRET_SHARING_DIR)/build/plugin-dist/*.zip "$(PLUGIN_INSTALL_DIR)/"
	cp $(PLUGIN_LETHENON_DIR)/build/plugin-dist/*.zip "$(PLUGIN_INSTALL_DIR)/"
	cp $(PLUGIN_RELEASE_KEY_DIR)/build/plugin-dist/*.zip "$(PLUGIN_INSTALL_DIR)/"
	@# pf4j loads an already extracted plugin directory in preference to the zip beside it, so a
	@# directory left over from an earlier install keeps serving old code however often the zip is
	@# replaced - the freshly installed zip has to be the only thing left to extract
	@for zip in "$(PLUGIN_INSTALL_DIR)"/*.zip; do \
		dir="$${zip%.zip}"; \
		if [ -d "$$dir" ]; then rm -rf "$$dir"; echo "==> removed stale $$(basename $$dir)"; fi; \
	done
	@echo "==> installed all internal plugins into $(PLUGIN_INSTALL_DIR)"

# --- mirrors Gradle "Run Configurations" panel ---

build-stacktrace: ## [build] gradlew build --stacktrace --warning-mode all
	JAVA_HOME=$(JAVA_HOME) ./gradlew build --stacktrace --warning-mode all $(GRADLE_FLAGS)

build-warning: ## [build] gradlew build --warning-mode all
	JAVA_HOME=$(JAVA_HOME) ./gradlew build --warning-mode all $(GRADLE_FLAGS)

# the installer ships the internal plugin zips (pack "plugins" in src/main/izpack/install.xml)
# except menu-designer, which edits the application's own menu bar and is for development only,
# so they have to be built before izpack packs them - otherwise it fails on the missing files
clean-build-installer: plugins ## [release] plugins, the signed runnable jar, then gradlew build izPackCreateInstaller
	JAVA_HOME=$(JAVA_HOME) $(SIGNING) ./gradlew --no-daemon createAllDependendiesJar -PcreateIzPackInstaller=true $(GRADLE_FLAGS)
	JAVA_HOME=$(JAVA_HOME) ./gradlew build izPackCreateInstaller $(GRADLE_FLAGS)

# The installer packs the SIGNED uber jar (src/main/izpack/install.xml references
# signed/<name>-<version>-all.jar), and only createAllDependendiesJar produces it. Two things were
# undeclared here, and both failed on a missing file nothing in the target was going to create
# (#245): the packaging step itself, and -PcreateIzPackInstaller=true. Signing runs when the
# version is a release version OR that flag is set (gradle/packaging.gradle), so between releases -
# when the version carries -SNAPSHOT, which since #247 is the normal state of develop - the flag is
# what makes the target work at all. A machine without the release keystore cannot build an
# installer either way; the build refuses before izpack does (#579).
#
# The signing command gets the keystore's two passwords from the desktop's keyring, in its own
# environment and no other process's (scripts/with-signing-passwords.sh, #579). It runs without a
# daemon: a daemon started from it would keep that environment for hours after the build.
SIGNING := scripts/with-signing-passwords.sh
izpack-installer: plugins ## [release] plugins, the signed runnable jar with the installer, then its checksums; needs the release keystore
	JAVA_HOME=$(JAVA_HOME) $(SIGNING) ./gradlew --no-daemon createAllDependendiesJar -PcreateIzPackInstaller=true $(GRADLE_FLAGS)
	JAVA_HOME=$(JAVA_HOME) ./gradlew checksumInstaller $(GRADLE_FLAGS)

# checks the installer after its checksums were written, release-workflow.md step 5 (#793): VERSION is the
# version gradle.properties names, PREVIOUS a published release whose signer certificate it must equal, INSTALLER
# another installer file, for the one downloaded from the release
verify-installer: ## [release] checks the installer after its checksums are written: [VERSION=] [PREVIOUS=<release>] [INSTALLER=<file>]
	JAVA_HOME=$(JAVA_HOME) scripts/verify-installer.sh $(or $(VERSION),$(shell sed -n 's/^projectVersion=//p' gradle.properties)) \
		$(if $(PREVIOUS),--previous $(PREVIOUS)) $(if $(INSTALLER),--installer $(INSTALLER))

izpack-installer-signed: plugins ## [release] plugins, then the installer from the signed jar
	JAVA_HOME=$(JAVA_HOME) $(SIGNING) ./gradlew --no-daemon createIzPackInstallerFromSignedJar $(GRADLE_FLAGS)

# The full release gate of release-workflow.md step 5, every phase with this Makefile's JAVA_HOME,
# so that one daemon serves all of them instead of one per JDK, and before each phase the daemons
# stopped once none of them is building (#580). Each phase overwrites build/test-results; copy them
# away between phases when a number is in doubt (lessons-learned.md). The gate ends by checking
# that the results it leaves are those of the forced phase, not an older run's from the cache (#630).
release-gate: ## [release] the full release gate: build-full, the forced test tasks, gradlew build, and a check of the results
	scripts/stop-idle-daemons.sh
	$(MAKE) build-full
	scripts/stop-idle-daemons.sh
	mkdir -p build && date -u +%s > build/release-gate-forced-run-started
	JAVA_HOME=$(JAVA_HOME) ./gradlew test --rerun e2eTest --rerun e2eLockTest --rerun e2eKdbxTest --rerun $(GRADLE_FLAGS)
	scripts/stop-idle-daemons.sh
	JAVA_HOME=$(JAVA_HOME) ./gradlew build $(GRADLE_FLAGS)
	scripts/check-forced-run-results.sh "$$(cat build/release-gate-forced-run-started)" \
		build/test-results/test build/test-results/e2eTest build/test-results/e2eLockTest \
		build/test-results/e2eKdbxTest

dependencies: ## [quality] gradlew dependencies
	JAVA_HOME=$(JAVA_HOME) ./gradlew dependencies $(GRADLE_FLAGS)

dependency-updates: ## [quality] the dependency currency check, gradlew dependencyUpdates
	JAVA_HOME=$(JAVA_HOME) ./gradlew dependencyUpdates $(GRADLE_FLAGS)

jacoco-coverage: ## [test] gradlew jacocoTestCoverageVerification
	JAVA_HOME=$(JAVA_HOME) ./gradlew jacocoTestCoverageVerification $(GRADLE_FLAGS)

jacoco-report: ## [test] the coverage report, gradlew jacocoTestReport
	JAVA_HOME=$(JAVA_HOME) ./gradlew jacocoTestReport $(GRADLE_FLAGS)

jar: ## [build] the thin jar alone, gradlew jar
	JAVA_HOME=$(JAVA_HOME) ./gradlew jar $(GRADLE_FLAGS)

javadoc: ## [build] gradlew javadoc
	JAVA_HOME=$(JAVA_HOME) ./gradlew javadoc $(GRADLE_FLAGS)

license-format: ## [quality] the same as spotless-java: Spotless writes the license headers since #686
	JAVA_HOME=$(JAVA_HOME) ./gradlew spotlessJavaApply $(GRADLE_FLAGS)

spotless-java: ## [quality] formats the Java sources and their license headers, gradlew spotlessJavaApply
	JAVA_HOME=$(JAVA_HOME) ./gradlew spotlessJavaApply $(GRADLE_FLAGS)

spotless-misc: ## [quality] formats the Gradle files, the Markdown and .gitignore, gradlew spotlessMiscApply
	JAVA_HOME=$(JAVA_HOME) ./gradlew spotlessMiscApply $(GRADLE_FLAGS)

tag-release: ## [release] creates the annotated tag RELEASE-<version> locally, gradlew tagRelease; pushing it is a step of its own
	JAVA_HOME=$(JAVA_HOME) ./gradlew tagRelease $(GRADLE_FLAGS)

version-catalog-format: ## [quality] gradlew versionCatalogFormat
	JAVA_HOME=$(JAVA_HOME) ./gradlew versionCatalogFormat $(GRADLE_FLAGS)

version-catalog-update: ## [quality] gradlew versionCatalogUpdate
	JAVA_HOME=$(JAVA_HOME) ./gradlew versionCatalogUpdate $(GRADLE_FLAGS)

all-dependencies-jar: ## [build] the runnable jar alone, without a clean and without signing, gradlew withAllDependendiesJar
	JAVA_HOME=$(JAVA_HOME) ./gradlew withAllDependendiesJar $(GRADLE_FLAGS)
