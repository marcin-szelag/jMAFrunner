@ECHO OFF
REM in the following two lines, it is possible to explicitly set the path to JAVA's JDK or JRE (version 11+); in such case, remove the leading REM
REM SET JAVA_HOME=c:\Program Files\Java\jdk-11.0.1
REM SET PATH=%JAVA_HOME%\bin;%PATH%

java -jar ../build/libs/jMAFrunner-standalone-0.1.0.jar %1 %2 %3 %4 %5