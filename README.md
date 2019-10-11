# Spectra Synthesizer command line interface
This repository contains a command line tool that allows realizability checking and symbolic controller construction independant of the Eclipse environment.

In order to use the Spectra command line tool:

1. Build the jar, or use spectra-cli.jar in the lib folder
2. Run from lib folder `java -jar spectra-cli.jar` with the following arguments:
```
-i /--input		The path to the input spectra file. The only required argument.
-o/--output		The folder where the controller should be created. If not specified, the same folder as the input will be used.
-s/--synthesize		Synthesize a controller or just check for realizability. Default is false.
   --jtlv		Use JTLV Java package for BDD manipulation instead of CUDD C library. Default is false.
   --disable-opt	Disable optimizations. Default is false.
   --disable-grouping	Disable variable grouping. Default is false.
-v/--verbose		Verbose logging. Default is false.
```
