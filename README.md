# jMAFrunner

**Command-line tool extending functionality of jMAF (http://www.cs.put.poznan.pl/jblaszczynski/Site/jRS.html)**. Employs java Rough Sets library (http://www.cs.put.poznan.pl/mszelag/Software/software.html). jRS library is included as a separate JAR, compiled with Java 11.

### `Configuration of text files in the project`:
UTF-8 encoding\
LF line endings

### `Building with gradle` (necessary before first calculations; requires Java 11 JDK):
**gradlew fatJar**

### `Launching calculations (requires Java 11 JRE)`:
cd ./scripts\
jMAFrunner learning-data-file-path union-type consistency-level rule-type compatibility-mode?, e.g.:

?> jMAFrunner.bat "..\data\windsor.isf" standard 1.0 certain\
?> jMAFrunner.bat "..\data\windsor.isf" monotonic 0.9 certain jmaf\
?> jMAFrunner.bat "..\data\windsor.isf" standard 1.0 possible jrs

**Parameters**:
- learning-data-file-path - relative (with respect to *scripts* directory) or full path to ISF file with data used to induce decision rules
- union-type - type of unions of decision classes; allowed values:
  - monotonic
  - standard
- consistency-level - consistency threshold used to induce decision rules; should be inside interval [0.0, 1.0]; value 1.0 is the most restrictive one and corresponds to DRSA; value lower than 1.0 corresponds to VC-DRSA; the lower the threshold, the less restrictive
- rule-type - type of induced decision rules; allowed values:
  - certain
  - possible
- compatibility-mode - this is an optional parameter; allowed values:
  - jmaf
  - jrs

Default value of the last parameter is *jmaf*. If learning data contain missing attribute values, *jrs* compatibility mode is chosen automatically, so as to handle the learning data properly (jMAF does not handle data with missing values, jRS does).

Induced decision rules are written in the directory of the input ISF file. Name of the saved \*.rules file is chosen automatically and corresponds to the name of ISF file and chosen parameters, e.g., if program is run in this way:

?> jMAFrunner.bat "..\data\windsor.isf" standard 1.0 certain

then it will save file ..\data\windsor_standard_1.0_certain.rules.
