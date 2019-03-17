# jMAFrunner

**Command-line tool extending functionality of jMAF (http://www.cs.put.poznan.pl/jblaszczynski/Site/jRS.html)**. Employs java Rough Sets library (http://www.cs.put.poznan.pl/mszelag/Software/software.html). jRS library is included as a separate JAR, compiled with Java 11.

### `Configuration of text files in the project`:
UTF-8 encoding\
LF line endings

### `Building with gradle` (necessary before first calculations; requires Java 11):
**gradlew fatJar**

### `Launching calculations (requires Java 11)`:
cd ./scripts\
jMAFrunner learning-data-file-path union-type consistency-level rule-type compatibility-mode?, e.g.:\
?> jMAFrunner.bat "..\data\windsor.isf" standard 1.0 certain\
?> jMAFrunner.bat "..\data\windsor.isf" monotonic 1.0 certain jmaf\
?> jMAFrunner.bat "..\data\windsor.isf" standard 0.9 possible jrs
