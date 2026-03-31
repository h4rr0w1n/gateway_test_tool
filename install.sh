#!/bin/bash

# AMHS/SWIM Gateway Test Tool Installation Script for Linux
# This script requires root/sudo privileges to install missing packages.

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${CYAN}Starting AMHS/SWIM Gateway Test Tool Installation...${NC}"

# Ensure we're in the project root
cd "$(dirname "$0")" || exit 1

# Helper function to detect OS and install packages
install_package() {
    local pkg_name=$1
    if [ -x "$(command -v apt-get)" ]; then
        sudo apt-get update && sudo apt-get install -y "$pkg_name"
    elif [ -x "$(command -v dnf)" ]; then
        sudo dnf install -y "$pkg_name"
    elif [ -x "$(command -v yum)" ]; then
        sudo yum install -y "$pkg_name"
    elif [ -x "$(command -v zypper)" ]; then
        sudo zypper install -y "$pkg_name"
    else
        echo -e "${RED}Could not detect package manager. Please install $pkg_name manually.${NC}"
        exit 1
    fi
}

# 1. Check and Install JDK (11 or higher)
echo -e "\n${CYAN}Checking for Java Development Kit (JDK)...${NC}"
if type -p java >/dev/null 2>&1; then
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]]; then
    _java="$JAVA_HOME/bin/java"
else
    _java=""
fi

if [[ "$_java" ]]; then
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo -e "${GREEN}Java is already installed (Version $version).${NC}"
else
    echo -e "${YELLOW}Java not found. Installing OpenJDK 11...${NC}"
    # The package name varies by distro, default to openjdk-11-jdk/java-11-openjdk
    if [ -x "$(command -v apt-get)" ]; then
        install_package "openjdk-11-jdk"
    else
        install_package "java-11-openjdk-devel"
    fi
fi

# 2. Check and Install Maven
echo -e "\n${CYAN}Checking for Apache Maven...${NC}"
if type -p mvn >/dev/null 2>&1; then
    mvn_version=$(mvn -version | head -n 1)
    echo -e "${GREEN}Maven is already installed ($mvn_version).${NC}"
else
    echo -e "${YELLOW}Maven not found. Installing Apache Maven...${NC}"
    install_package "maven"
fi

# 3. Setup lib directory
LIB_DIR="lib"
echo -e "\n${CYAN}Setting up dependencies in '${LIB_DIR}'...${NC}"
if [ ! -d "$LIB_DIR" ]; then
    mkdir -p "$LIB_DIR"
    echo -e "${GREEN}Created lib/ directory.${NC}"
else
    echo -e "${GREEN}lib/ directory already exists.${NC}"
fi

# Prepare download command
if type -p wget >/dev/null 2>&1; then
    DOWNLOAD_CMD="wget -qO"
elif type -p curl >/dev/null 2>&1; then
    DOWNLOAD_CMD="curl -sLo"
else
    echo -e "${YELLOW}Neither wget nor curl found. Installing wget...${NC}"
    install_package "wget"
    DOWNLOAD_CMD="wget -qO"
fi

# 4. Download JARs
echo -e "\n${CYAN}Locating and Downloading Solace and Isode JAR files...${NC}"

# 4.1 Solace JCSMP
SOLACE_JAR="$LIB_DIR/sol-jcsmp-10.1.0.jar"
if [ ! -f "$SOLACE_JAR" ]; then
    echo "Downloading Solace JCSMP JAR from Maven Central..."
    if $DOWNLOAD_CMD "$SOLACE_JAR" "https://repo1.maven.org/maven2/com/solacesystems/sol-jcsmp/10.1.0/sol-jcsmp-10.1.0.jar"; then
        echo -e "${GREEN}Successfully downloaded 'sol-jcsmp-10.1.0.jar'.${NC}"
    else
        echo -e "${RED}Failed to download Solace JAR.${NC}"
    fi
else
    echo -e "${GREEN}Solace JAR already exists. Skipping.${NC}"
fi

# 4.2 Isode X.400 SDK
ISODE_X400_JAR="$LIB_DIR/isode-x400-api.jar"
if [ ! -f "$ISODE_X400_JAR" ]; then
    echo "Setting up Isode X.400 API JAR..."
    echo -e "${YELLOW}Warning: Isode X.400 JAR needs to be provided manually (proprietary).${NC}"
    touch "$ISODE_X400_JAR"
    echo -e "${YELLOW}Created placeholder at '$ISODE_X400_JAR'. Please replace with actual JAR.${NC}"
else
    echo -e "${GREEN}Isode X.400 JAR already exists. Skipping.${NC}"
fi

# 4.3 Isode Directory SDK
ISODE_DIR_JAR="$LIB_DIR/isode-directory-api.jar"
if [ ! -f "$ISODE_DIR_JAR" ]; then
    echo "Setting up Isode Directory API JAR..."
    echo -e "${YELLOW}Warning: Isode Directory JAR needs to be provided manually (proprietary).${NC}"
    touch "$ISODE_DIR_JAR"
    echo -e "${YELLOW}Created placeholder at '$ISODE_DIR_JAR'. Please replace with actual JAR.${NC}"
else
    echo -e "${GREEN}Isode Directory JAR already exists. Skipping.${NC}"
fi

# 5. Execute the remaining installation script
echo -e "\n${CYAN}Executing remaining installation script...${NC}"
if type -p mvn >/dev/null 2>&1; then
    echo "Running Maven build..."
    mvn clean install
elif [ -f "./scripts/build.sh" ]; then
    echo -e "${YELLOW}mvn not directly found. Attempting to run ./scripts/build.sh...${NC}"
    bash ./scripts/build.sh
else
    echo -e "${RED}Could not run Maven. Please run 'mvn clean install' manually.${NC}"
fi

echo -e "\n${GREEN}Installation workflow complete.${NC}"
