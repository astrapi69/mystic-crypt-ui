# pinned on purpose with := (not ?=): the shell's JAVA_HOME (e.g. sdkman's "current") may point
# at an older JDK, and running the JDK-25-built jar on it fails with UnsupportedClassVersionError
JAVA_HOME := /home/astrapi69/.sdkman/candidates/java/25-tem
JAR := $(shell find build/libs -maxdepth 1 -name '*-all.jar' 2>/dev/null | head -1)

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
PLUGIN_INSTALL_DIR := $(HOME)/.config/mystic-crypt-ui/plugins

.PHONY: build build-full run all clean test test-e2e test-e2e-demo \
	bootRun clean-build-installer izpack-installer izpack-installer-signed \
	dependencies dependency-updates jacoco-coverage jacoco-report jar javadoc \
	license-format publish publish-local spotless-java spotless-misc tag-release \
	version-catalog-format version-catalog-update all-dependencies-jar \
	build-stacktrace build-warning plugin-obfuscation plugin-checksum plugin-conversion \
	plugin-console plugin-keygen plugin-certificate plugin-password-hash plugin-kem-demo \
	plugin-menu-designer plugin-pqc-signature plugin-keystore plugin-file-crypt plugin-secret-sharing plugins plugins-install

# fast build: clean, compile, package the runnable jar - skips tests/spotless/license
build:
	JAVA_HOME=$(JAVA_HOME) ./gradlew createAllDependendiesJar

# full build: clean, compile, test, spotless, license, then package the runnable jar
build-full:
	JAVA_HOME=$(JAVA_HOME) ./gradlew clean build
	JAVA_HOME=$(JAVA_HOME) ./gradlew createAllDependendiesJar

# run the most recently built jar - fails loudly if none exists yet
run:
	@jar=$$(find build/libs -maxdepth 1 -name '*-all.jar' -print -quit); \
	if [ -z "$$jar" ]; then \
		echo "No *-all.jar in build/libs - run 'make build' first." >&2; \
		exit 1; \
	fi; \
	echo "==> Starting $$jar"; \
	"$(JAVA_HOME)/bin/java" --sun-misc-unsafe-memory-access=allow -jar "$$jar"

# build then run - always launches exactly what was just built, with the standard internal
# plugins built and installed into the app's plugins directory beforehand
all: build plugins-install run

test:
	JAVA_HOME=$(JAVA_HOME) ./gradlew test

# end-to-end UI tests (AssertJ-Swing) - fast mode (default): as fast as possible
test-e2e:
	JAVA_HOME=$(JAVA_HOME) ./gradlew test --tests "io.github.astrapi69.mystic.crypt.ui.*" --rerun

# end-to-end UI tests in demo mode: paced like a real user, watchable on screen
test-e2e-demo:
	JAVA_HOME=$(JAVA_HOME) ./gradlew test --tests "io.github.astrapi69.mystic.crypt.ui.*" --rerun -Dmystic.crypt.ui.test.mode=demo

clean:
	JAVA_HOME=$(JAVA_HOME) ./gradlew clean

# --- internal plugins ---

# publish the host to the local Maven cache so the plugins can compile against the current API
publish-local:
	JAVA_HOME=$(JAVA_HOME) ./gradlew publishToMavenLocal -x test

# build the internal obfuscation plugin zip (needs the host published locally first)
plugin-obfuscation: publish-local
	JAVA_HOME=$(JAVA_HOME) ./gradlew -p $(PLUGIN_OBFUSCATION_DIR) test pluginZip
	@echo "==> plugin zip: $$(find $(PLUGIN_OBFUSCATION_DIR)/build/plugin-dist -name '*.zip')"

# build the internal checksum plugin zip (needs the host published locally first)
plugin-checksum: publish-local
	JAVA_HOME=$(JAVA_HOME) ./gradlew -p $(PLUGIN_CHECKSUM_DIR) test pluginZip
	@echo "==> plugin zip: $$(find $(PLUGIN_CHECKSUM_DIR)/build/plugin-dist -name '*.zip')"

# build the internal der-to-pem conversion plugin zip (needs the host published locally first)
plugin-conversion: publish-local
	JAVA_HOME=$(JAVA_HOME) ./gradlew -p $(PLUGIN_CONVERSION_DIR) test pluginZip
	@echo "==> plugin zip: $$(find $(PLUGIN_CONVERSION_DIR)/build/plugin-dist -name '*.zip')"

# build the internal console plugin zip (needs the host published locally first)
plugin-console: publish-local
	JAVA_HOME=$(JAVA_HOME) ./gradlew -p $(PLUGIN_CONSOLE_DIR) test pluginZip
	@echo "==> plugin zip: $$(find $(PLUGIN_CONSOLE_DIR)/build/plugin-dist -name '*.zip')"

# build the internal keygen plugin zip (needs the host published locally first)
plugin-keygen: publish-local
	JAVA_HOME=$(JAVA_HOME) ./gradlew -p $(PLUGIN_KEYGEN_DIR) test pluginZip
	@echo "==> plugin zip: $$(find $(PLUGIN_KEYGEN_DIR)/build/plugin-dist -name '*.zip')"

# build the internal certificate plugin zip (needs the host published locally first)
plugin-certificate: publish-local
	JAVA_HOME=$(JAVA_HOME) ./gradlew -p $(PLUGIN_CERTIFICATE_DIR) test pluginZip
	@echo "==> plugin zip: $$(find $(PLUGIN_CERTIFICATE_DIR)/build/plugin-dist -name '*.zip')"

# build the internal password-hash plugin zip (needs the host published locally first)
plugin-password-hash: publish-local
	JAVA_HOME=$(JAVA_HOME) ./gradlew -p $(PLUGIN_PASSWORD_HASH_DIR) test pluginZip
	@echo "==> plugin zip: $$(find $(PLUGIN_PASSWORD_HASH_DIR)/build/plugin-dist -name '*.zip')"

# build the internal ML-KEM / hybrid key-encapsulation demo plugin zip (needs the host published
# locally first)
plugin-kem-demo: publish-local
	JAVA_HOME=$(JAVA_HOME) ./gradlew -p $(PLUGIN_KEM_DEMO_DIR) test pluginZip
	@echo "==> plugin zip: $$(find $(PLUGIN_KEM_DEMO_DIR)/build/plugin-dist -name '*.zip')"

# build the internal menu-designer plugin zip (needs the host published locally first)
plugin-menu-designer: publish-local
	JAVA_HOME=$(JAVA_HOME) ./gradlew -p $(PLUGIN_MENU_DESIGNER_DIR) test pluginZip
	@echo "==> plugin zip: $$(find $(PLUGIN_MENU_DESIGNER_DIR)/build/plugin-dist -name '*.zip')"

# build the internal post-quantum signature plugin zip (needs the host published locally first)
plugin-pqc-signature: publish-local
	JAVA_HOME=$(JAVA_HOME) ./gradlew -p $(PLUGIN_PQC_SIGNATURE_DIR) test pluginZip
	@echo "==> plugin zip: $$(find $(PLUGIN_PQC_SIGNATURE_DIR)/build/plugin-dist -name '*.zip')"

# build the internal key store plugin zip (needs the host published locally first)
plugin-keystore: publish-local
	JAVA_HOME=$(JAVA_HOME) ./gradlew -p $(PLUGIN_KEYSTORE_DIR) test pluginZip
	@echo "==> plugin zip: $$(find $(PLUGIN_KEYSTORE_DIR)/build/plugin-dist -name '*.zip')"

# build the internal file encryption plugin zip (needs the host published locally first)
plugin-file-crypt: publish-local
	JAVA_HOME=$(JAVA_HOME) ./gradlew -p $(PLUGIN_FILE_CRYPT_DIR) test pluginZip
	@echo "==> plugin zip: $$(find $(PLUGIN_FILE_CRYPT_DIR)/build/plugin-dist -name '*.zip')"

# build the internal secret sharing plugin zip (needs the host published locally first)
plugin-secret-sharing: publish-local
	JAVA_HOME=$(JAVA_HOME) ./gradlew -p $(PLUGIN_SECRET_SHARING_DIR) test pluginZip
	@echo "==> plugin zip: $$(find $(PLUGIN_SECRET_SHARING_DIR)/build/plugin-dist -name '*.zip')"

# build every internal plugin
plugins: plugin-obfuscation plugin-checksum plugin-conversion plugin-console plugin-keygen plugin-certificate plugin-password-hash plugin-kem-demo plugin-menu-designer plugin-pqc-signature plugin-keystore plugin-file-crypt plugin-secret-sharing

# build all internal plugins and install them into the app's plugins directory
plugins-install: plugins
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
	@echo "==> installed all internal plugins into $(PLUGIN_INSTALL_DIR)"

# --- mirrors Gradle "Run Configurations" panel ---

bootRun: plugins-install
	JAVA_HOME=$(JAVA_HOME) ./gradlew bootRun

build-stacktrace:
	JAVA_HOME=$(JAVA_HOME) ./gradlew build --stacktrace --warning-mode all

build-warning:
	JAVA_HOME=$(JAVA_HOME) ./gradlew build --warning-mode all

# the installer ships the internal plugin zips (pack "plugins" in src/main/izpack/install.xml),
# so they have to be built before izpack packs them - otherwise it fails on the missing files
clean-build-installer: plugins
	JAVA_HOME=$(JAVA_HOME) ./gradlew clean build izPackCreateInstaller

izpack-installer: plugins
	JAVA_HOME=$(JAVA_HOME) ./gradlew izPackCreateInstaller

izpack-installer-signed: plugins
	JAVA_HOME=$(JAVA_HOME) ./gradlew createIzPackInstallerFromSignedJar

dependencies:
	JAVA_HOME=$(JAVA_HOME) ./gradlew dependencies

dependency-updates:
	JAVA_HOME=$(JAVA_HOME) ./gradlew dependencyUpdates

jacoco-coverage:
	JAVA_HOME=$(JAVA_HOME) ./gradlew jacocoTestCoverageVerification

jacoco-report:
	JAVA_HOME=$(JAVA_HOME) ./gradlew jacocoTestReport

jar:
	JAVA_HOME=$(JAVA_HOME) ./gradlew jar

javadoc:
	JAVA_HOME=$(JAVA_HOME) ./gradlew javadoc

license-format:
	JAVA_HOME=$(JAVA_HOME) ./gradlew licenseFormat

publish:
	JAVA_HOME=$(JAVA_HOME) ./gradlew publish

spotless-java:
	JAVA_HOME=$(JAVA_HOME) ./gradlew spotlessJavaApply

spotless-misc:
	JAVA_HOME=$(JAVA_HOME) ./gradlew spotlessMiscApply

tag-release:
	JAVA_HOME=$(JAVA_HOME) ./gradlew tagRelease

version-catalog-format:
	JAVA_HOME=$(JAVA_HOME) ./gradlew versionCatalogFormat

version-catalog-update:
	JAVA_HOME=$(JAVA_HOME) ./gradlew versionCatalogUpdate

all-dependencies-jar:
	JAVA_HOME=$(JAVA_HOME) ./gradlew withAllDependendiesJar
