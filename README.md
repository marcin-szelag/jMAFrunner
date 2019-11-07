# jMAFrunner

**Command-line tool extending functionality of jMAF ([http://www.cs.put.poznan.pl/jblaszczynski/Site/jRS.html](http://www.cs.put.poznan.pl/jblaszczynski/Site/jRS.html))**. Employs java Rough Sets library ([http://www.cs.put.poznan.pl/mszelag/Software/software.html](http://www.cs.put.poznan.pl/mszelag/Software/software.html)). jRS library is included as a separate JAR, compiled with Java 11.

jMAF, due to relying on an obsolete version of jRS library, can load data with missing values, but:
- for a learning data set with missing values, it can't calculate approximations of unions of ordered decision classes,
- for a learning data set with missing values, it can't induce decision rules,
- it can't classify objects from a test set, if they contain missing values.

The main idea for jMAFrunner is to use current version of jRS library to perform the first two steps (i.e., calculation of approximations and induction of rules) in batch mode.
As a result, one gets an &ast;.apx file with calculated approximations and &ast;.rules file with induced rules. The last file can be then read directly in jMAF. This way, the rules obtained in batch mode can be analyzed directly in jMAF, and possibly later used also for classification of test set objects, provided that these objects do not contain missing values.

## `Configuration of text files in the project`:
UTF-8 encoding<br/>
LF line endings

## `Building with gradle` (necessary before first calculations; requires Java 11 JDK (or higher)):
**gradlew fatJar**

## `Launching calculations` (requires Java 11 JRE (or higher)):
cd ./scripts<br/>
jMAFrunner learning-data-file-path union-type consistency-level rule-type compatibility-mode?, e.g.:

?> jMAFrunner.bat "../data/windsor.isf" standard 1.0 certain<br/>
?> jMAFrunner.bat "../data/windsor.isf" monotonic 0.9 certain jmaf<br/>
?> jMAFrunner.bat "../data/windsor.isf" standard 1.0 possible jrs

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

Default value of the last parameter is *jmaf*. If learning data contain missing attribute values, *jrs* compatibility mode is chosen automatically, so as to handle the learning data set properly (jMAF cannot process data with missing values).

jMAFrunner (after jRS) supports only one adaptation of the Dominance-based Rough Set Approach (DRSA) to handle missing values - DRSA-mv<sub>2</sub>. More details can be found in [this conference paper](http://www.cs.put.poznan.pl/mszelag/Research/missingValuesIJCRS2017.pdf).

Induced approximations and decision rules are written in the directory of the input ISF file. Name of the saved \*.apx and \*.rules files are chosen automatically and correspond to the name of the ISF file and chosen parameters, e.g., if the program is run in this way:

?> jMAFrunner.bat "../data/windsor.isf" standard 1.0 certain

then it will save two files: ../data/windsor\_standard\_1.0.apx and ../data/windsor\_standard\_1.0_certain.rules.
