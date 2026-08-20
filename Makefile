JAVA_HOME ?= /home/astrapi69/.sdkman/candidates/java/21.0.6-tem
JAR := $(shell find build/libs -maxdepth 1 -name '*-all.jar' 2>/dev/null | head -1)

.PHONY: build build-full run all clean test \
	bootRun clean-build-installer izpack-installer izpack-installer-signed \
	dependencies dependency-updates jacoco-coverage jacoco-report jar javadoc \
	license-format publish spotless-java spotless-misc tag-release \
	version-catalog-format version-catalog-update all-dependencies-jar \
	build-stacktrace build-warning

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
	"$(JAVA_HOME)/bin/java" -jar "$$jar"

# build then run - always launches exactly what was just built
all: build run

test:
	JAVA_HOME=$(JAVA_HOME) ./gradlew test

clean:
	JAVA_HOME=$(JAVA_HOME) ./gradlew clean

# --- mirrors Gradle "Run Configurations" panel ---

bootRun:
	JAVA_HOME=$(JAVA_HOME) ./gradlew bootRun

build-stacktrace:
	JAVA_HOME=$(JAVA_HOME) ./gradlew build --stacktrace --warning-mode all

build-warning:
	JAVA_HOME=$(JAVA_HOME) ./gradlew build --warning-mode all

clean-build-installer:
	JAVA_HOME=$(JAVA_HOME) ./gradlew clean build izPackCreateInstaller

izpack-installer:
	JAVA_HOME=$(JAVA_HOME) ./gradlew izPackCreateInstaller

izpack-installer-signed:
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
