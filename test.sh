#!/bin/sh
set -eu
cd "$(dirname "$0")"
. ./tools/java-env.sh
mkdir -p build/tests
javac -d build/tests src/org/poopware/cacaexplorer/FileOps.java src/org/poopware/cacaexplorer/ArchiveOps.java src/org/poopware/cacaexplorer/ThemeCatalog.java tests/*.java
java -cp build/tests FileOpsTest
java -cp build/tests ArchiveOpsTest
java -cp build/tests org.poopware.cacaexplorer.ThemeCatalogTest

python3 -m unittest discover -s tests -p "test_*.py"
