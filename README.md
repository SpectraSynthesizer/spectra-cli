# Spectra Synthesizer command line interface
This repository contains a command line tool that allows realizability checking and symbolic controller construction of Spectra specifications independently from the Eclipse environment. The tool, as well as the Spectra itself, was tested on recent Windows and Linux environments, and will not work on OS X.

However, it is recommended to use Spectra tools and features inside the Eclipse environment. Spectra language relies on the Xtext framework, which is integrated into the IDE and is loaded once at startup along with all the Eclipse libraries. When using the command line tool, Xtext libraries will have to load on every call, which may affect performance. Furthermore, Spectra and its advanced add-ons benefit from the formatting and line marking features of the IDE.

## Usage 
In order to use the Spectra command line tool:

1. Build the jar, or use spectra-cli.jar in the lib folder
2. Run the jar from a folder containing cudd.dll (Windows), or libcudd.so (Linux). On Linux you may need to update `LD_LIBRARY_PATH` with the following command:
```
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/path/to/libcudd.so
```
3. You can apply the following arguments:
```
-i/--input		The path to the input spectra file. The only required argument.
-o/--output		The folder where the controller should be created. If not specified, the same folder as the input will be used.
-s/--synthesize		Synthesize a controller or just check for realizability. Default is false.
   --jit                Synthesize a just-in-time controller
   --jtlv		Use JTLV Java package for BDD manipulation instead of CUDD C library. Default is false.
   --disable-opt	Disable optimizations. Default is false.
   --disable-grouping	Disable variable grouping. Default is false.
-v/--verbose		Verbose logging. Default is false.
```
