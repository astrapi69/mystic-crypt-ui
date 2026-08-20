JAVA_HOME ?= /home/astrapi69/.sdkman/candidates/java/21.0.6-tem
JAR := $(shell find build/libs -maxdepth 1 -name '*-all.jar' 2>/dev/null | head -1)

.PHONY: build build-full run all clean test

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
