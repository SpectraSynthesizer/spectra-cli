/*
Copyright (c) since 2015, Tel Aviv University and Software Modeling Lab

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of Tel Aviv University and Software Modeling Lab nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL Tel Aviv University and Software Modeling Lab 
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT 
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
*/

package tau.smlab.syntech.Spectra.cli;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import tau.smlab.syntech.bddgenerator.BDDGenerator;
import tau.smlab.syntech.bddgenerator.BDDGenerator.TraceInfo;
import tau.smlab.syntech.bddgenerator.energy.BDDEnergyReduction;
import tau.smlab.syntech.checks.ddmin.AbstractDdmin;
import tau.smlab.syntech.cores.util.RealizabilityCheck;
import tau.smlab.syntech.gameinput.model.GameInput;
import tau.smlab.syntech.gameinputtrans.TranslationException;
import tau.smlab.syntech.gameinputtrans.TranslationProvider;
import tau.smlab.syntech.gameinputtrans.translator.DefaultTranslators;
import tau.smlab.syntech.gameinputtrans.translator.Translator;
import tau.smlab.syntech.gamemodel.BehaviorInfo;
import tau.smlab.syntech.gamemodel.GameModel;
import tau.smlab.syntech.gamemodel.PlayerModule.TransFuncType;
import tau.smlab.syntech.gamemodel.util.SysTraceInfoBuilder;
import tau.smlab.syntech.gamemodel.util.TraceIdentifier;
import tau.smlab.syntech.games.AbstractGamesException;
import tau.smlab.syntech.games.controller.enumerate.ConcreteControllerConstruction;
import tau.smlab.syntech.games.controller.enumerate.printers.MAAMinimizeAutomatonPrinter;
import tau.smlab.syntech.games.controller.enumerate.printers.SimpleTextPrinter;
import tau.smlab.syntech.games.controller.jits.SymbolicControllerJitInfo;
import tau.smlab.syntech.games.controller.symbolic.SymbolicController;
import tau.smlab.syntech.games.controller.symbolic.SymbolicControllerConstruction;
import tau.smlab.syntech.games.controller.symbolic.SymbolicControllerReaderWriter;
import tau.smlab.syntech.games.gr1.GR1ConceteControllerConstructionSkip;
import tau.smlab.syntech.games.gr1.GR1Game;
import tau.smlab.syntech.games.gr1.GR1GameExperiments;
import tau.smlab.syntech.games.gr1.GR1GameImplC;
import tau.smlab.syntech.games.gr1.GR1GameMemoryless;
import tau.smlab.syntech.games.gr1.GR1SymbolicControllerConstruction;
import tau.smlab.syntech.games.gr1.jit.SymbolicControllerJitInfoConstruction;
import tau.smlab.syntech.games.gr1.wellseparation.WellSeparationChecker;
import tau.smlab.syntech.games.gr1.wellseparation.WellSeparationChecker.EnvSpecPart;
import tau.smlab.syntech.games.gr1.wellseparation.WellSeparationChecker.Positions;
import tau.smlab.syntech.games.gr1.wellseparation.WellSeparationChecker.Systems;
import tau.smlab.syntech.games.rabin.RabinConcreteControllerConstruction;
import tau.smlab.syntech.games.rabin.RabinGame;
import tau.smlab.syntech.games.rabin.RabinGameImplC;
import tau.smlab.syntech.jtlv.BDDPackage;
import tau.smlab.syntech.jtlv.BDDPackage.BBDPackageVersion;
import tau.smlab.syntech.jtlv.Env;
import tau.smlab.syntech.jtlv.ModuleVariableException;
import tau.smlab.syntech.spectragameinput.ErrorsInSpectraException;
import tau.smlab.syntech.spectragameinput.SpectraInputProviderNoIDE;
import tau.smlab.syntech.spectragameinput.SpectraTranslationException;
	
public class SpectraCliTool {
  static Options options = new Options();

  private static Option analysisKind = Option.builder("a").longOpt("analysis").argName("name").hasArg().desc("Analysis to perform on input (" + printAnalyses() + ")").build();
  /** mandatory input file name */
  private static Option input = Option.builder("i").longOpt("input").argName("file").hasArg().desc("Spectra input file name").required().build();
  /** optional output folder */
  private static Option outputFolder = Option.builder("o").longOpt("output").argName("folder").hasArg().desc("Output folder").build();
  private static Option verbose = Option.builder("v").longOpt("verbose").desc("Verbose logging").build();
  private static Option jtlvBDD = Option.builder().longOpt("jtlv").desc("Use JTLV package instead of CUDD").build();
  private static Option jtlvFormat = Option.builder().longOpt("jtlv-format").desc("Print strategies in JTLV format").build();
  private static Option disableOpt = Option.builder().longOpt("disable-opt").desc("Disable optimizations").build();
  private static Option disableGrouping = Option.builder().longOpt("disable-grouping").desc("Disable reorder with grouping").build();
  private static Option reorderSave = Option.builder().longOpt("reorder").desc("Reorder BDD before saving symbolic controllers for reduced size").build();
  private static Option ignoreSys = Option.builder().longOpt("ignore-sys").desc("Ignore system player in well-separation check").build();
  private static Option energy = Option.builder().longOpt("energy-bound").argName("bound").hasArg().desc("Energy bound when using weights and Energy Games").build();
  private static Option printCoreLines = Option.builder().longOpt("print-core-lines").desc("Print lines of cores").build();
  
  private static GameInput gi;
  
  /** 
   * adds options and parses command line arguments
   * 
   * @param args command line arguments 
   */
  private static CommandLine initOptions(String[] args) {    
    // individual analyses to execute
    options.addOption(analysisKind);

    // mandatory input spec 
    options.addOption(input);
    
    // optional configuration options
    options.addOption(outputFolder);
    options.addOption(verbose);
    
    options.addOption(jtlvBDD);
    options.addOption(disableOpt);
    options.addOption(disableGrouping);
    options.addOption(reorderSave);
    options.addOption(ignoreSys);
    options.addOption(energy);
    options.addOption(jtlvFormat);
    options.addOption(printCoreLines);

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println("Error: " + e.getMessage());
      return null;
    }
    return cmd;
  }

  
	public static void main(String[] args) throws ParseException {
	  
	  CommandLine cmd = initOptions(args);
	  
    if (cmd == null) {
      printHelp();
      return;
    }
		
    SpectraAnalyses analysis;
    if (cmd.hasOption(analysisKind)) {
      String analysisName = cmd.getOptionValue(analysisKind);
      analysis = getAnalysis(analysisName);
      if (analysis == null) {
        System.out.println("Error: Invalid analysis \"" + analysisName + "\" specified, expected: " + printAnalyses());
        return;
      }
    } else {
      // default analysis
      analysis = SpectraAnalyses.REALIZABILITY;
    }
    
		String fileName = cmd.getOptionValue(input);
		
		
		String outputFolderName = setOutputFolderName(cmd, fileName, isStaticController(analysis)); 
		
		try {
      prepareGameInput(cmd, fileName);
    } catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
      return;
    }
		
		BDDPackage pkg = setBDDEngine(cmd);
		
		boolean log = cmd.hasOption(verbose);
		boolean optimize = !cmd.hasOption(disableOpt);
		boolean grouping = !cmd.hasOption(disableGrouping);
		
		boolean reorderBeforeSave = cmd.hasOption(reorderSave);
		boolean printJtlvFormat = cmd.hasOption(jtlvFormat);
		
    switch (analysis) {
    case REALIZABILITY:
      checkRealizability(optimize, grouping, log, pkg);
      break;
    case CHECK_WELL_SEP:
      checkWellSeparation(cmd, log, grouping);
      break;
    case STATIC_SYMBOLIC_CONTROLLER:
      synthesizeController(optimize, grouping, outputFolderName, log, pkg, analysis, reorderBeforeSave,
          printJtlvFormat);
      break;
    case SYMBOLIC_CONTROLLER:
      synthesizeController(optimize, grouping, outputFolderName, log, pkg, analysis, reorderBeforeSave,
          printJtlvFormat);
      break;
    case CONCRETE_COUNTER_STRATEGY:
      synthesizeCounterStrategy(optimize, grouping, log, pkg, printJtlvFormat);
      break;
    case CONCRETE_CONTROLLER:
      synthesizeController(optimize, grouping, outputFolderName, log, pkg, analysis, reorderBeforeSave,
          printJtlvFormat);
      break;
    case UNREALIZABLE_CORE:
      boolean coreLines = cmd.hasOption(printCoreLines);
      calculateUnrealizableCore(optimize, grouping, log, pkg, coreLines);
      break;
    case NON_WELL_SEP_CORE:
      calculateNonWellSeparatedCore(cmd, log, grouping);
      break;
    default:
      System.err.println("Error: Analysis \"" + analysis.getDescription() + "\" not implemented");
      break;
    }
	}


  private static void calculateNonWellSeparatedCore(CommandLine cmd, boolean log, boolean grouping) {
    boolean coreLines = cmd.hasOption(printCoreLines);
    Systems systems = cmd.hasOption(ignoreSys)? Systems.NONE : Systems.SPEC;
    WellSeparationChecker checker = new WellSeparationChecker();
    
    // monolithic transition relation as nws check computes reachable states
    GameModel model = BDDGenerator.generateGameModel(gi, TraceInfo.ALL, grouping, TransFuncType.SINGLE_FUNC, log);
    
    List<BehaviorInfo> res = null;    
    try {
      if (log) {
        res = checker.computeCore(model, systems);
      } else {
        PrintStream out = disableSysOut(null);
        res = checker.computeCore(model, systems);
        disableSysOut(out);
      }
    } catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
      return;
    }
    
    if (res.isEmpty()) {
      System.out.println("Result: Specification is well-separated");
      if (coreLines) {
        System.out.println("CORE-LINES: []"); 
      }

    } else {
      // list of trace ids from res
      List<Integer> traceIds = res.stream().map(bi -> bi.traceId).toList();
      System.out.println(
          "Result: Found non-well-separated core with " + traceIds.size() + " elements, at lines "  + TraceIdentifier.formatLines(traceIds) + ".");
      if (coreLines) {
        System.out.println("CORE-LINES: " + toListOfInt(TraceIdentifier.formatLines(traceIds))); 
      }

    }
  }


  private static void calculateUnrealizableCore(boolean optimize, boolean grouping, boolean log, BDDPackage pkg, boolean coreLines) {
    GameModel gameModel = BDDGenerator.generateGameModel(
        gi, TraceInfo.ALL, grouping, 
        optimize ? TransFuncType.DECOMPOSED_FUNC : TransFuncType.SINGLE_FUNC, log);

    RealizabilityCheck.useCUDD = BDDPackage.CUDD.equals(pkg);
    
    // set up a builder to build GameModels with subsets of the guarantees
    final SysTraceInfoBuilder builder = new SysTraceInfoBuilder(gameModel);

    List<Integer> result = new ArrayList<Integer>();
    AbstractDdmin<Integer> ucmin = new AbstractDdmin<Integer>() {
      @Override 
      public boolean check(List<Integer> part) {
        return !RealizabilityCheck.isRealizable(builder.build(part));
      }
    };
    
    if (log) {
      result.addAll(ucmin.minimize(builder.getTraceList()));
    } else {
      PrintStream out = disableSysOut(null);
      result.addAll(ucmin.minimize(builder.getTraceList()));
      disableSysOut(out);
    }
    
    if (result.isEmpty()) {
      System.out.println("Result: Specification is realizable");      
      if (coreLines) {
        System.out.println("CORE-LINES: []"); 
      }
    } else {
      System.out.println(
          "Result: Found unrealizable core with " + result.size() + " elements, at lines "  + TraceIdentifier.formatLines(result) + ".");
      if (coreLines) {
        System.out.println("CORE-LINES: " + toListOfInt(TraceIdentifier.formatLines(result))); 
      }
    }
    
  }


  private static String toListOfInt(String formatLines) {
    return formatLines.replace("< ", "[").replace(" >", "]").replace(" ", ",").replace("untraced,", "");
  }


  private static PrintStream disableSysOut(PrintStream out) {
    if (out == null) {
      PrintStream current = System.out;
      System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
        @Override public void write(int b) {}
    }) {
        @Override public void flush() {}
        @Override public void close() {}
        @Override public void write(int b) {}
        @Override public void write(byte[] b) {}
        @Override public void write(byte[] buf, int off, int len) {}
        @Override public void print(boolean b) {}
        @Override public void print(char c) {}
        @Override public void print(int i) {}
        @Override public void print(long l) {}
        @Override public void print(float f) {}
        @Override public void print(double d) {}
        @Override public void print(char[] s) {}
        @Override public void print(String s) {}
        @Override public void print(Object obj) {}
        @Override public void println() {}
        @Override public void println(boolean x) {}
        @Override public void println(char x) {}
        @Override public void println(int x) {}
        @Override public void println(long x) {}
        @Override public void println(float x) {}
        @Override public void println(double x) {}
        @Override public void println(char[] x) {}
        @Override public void println(String x) {}
        @Override public void println(Object x) {}
        @Override public java.io.PrintStream printf(String format, Object... args) { return this; }
        @Override public java.io.PrintStream printf(java.util.Locale l, String format, Object... args) { return this; }
        @Override public java.io.PrintStream format(String format, Object... args) { return this; }
        @Override public java.io.PrintStream format(java.util.Locale l, String format, Object... args) { return this; }
        @Override public java.io.PrintStream append(CharSequence csq) { return this; }
        @Override public java.io.PrintStream append(CharSequence csq, int start, int end) { return this; }
        @Override public java.io.PrintStream append(char c) { return this; }
    });
      return current;
    } else {
      System.setOut(out);
      return out;
    }    
  }


  private static void synthesizeCounterStrategy(boolean optimize, boolean grouping, boolean log, BDDPackage pkg,
      boolean printJtlvFormat) {
    // create game model
    GameModel gameModel = BDDGenerator.generateGameModel(gi, TraceInfo.NONE, grouping, TransFuncType.SINGLE_FUNC, log);
    
    RabinGame rabin = getRabinGame(gameModel, pkg, optimize);
    // make sure all initial states are computed
    RabinGame.STOP_WHEN_WIN_FROM_SOME_INITIALS = false;

    if (!rabin.checkRealizability()) {
      System.out.println("Error: Cannot generate counter-strategy for a realizable specification");
      return;
    }
    System.out.println("Result: Specification is unrealizable. Proceeding to synthesis of counter-strategy.");

    if (gameModel.getWeights() != null) {
      try {
        BDDEnergyReduction.updateSysIniTransWithEngConstraintsForCounterStrategy(gameModel, gi.getEnergyBound());
      } catch (ModuleVariableException e) {
        System.out.println("Error: While updating energy values in initial states. Could not generate concrete counter-strategy");
        e.printStackTrace();
      }
    }

    Env.disableReorder();

    // generate concrete counter-strategy and print it
    ConcreteControllerConstruction cc = new RabinConcreteControllerConstruction(rabin.getMem(), gameModel);
    try {
      if (printJtlvFormat) {
        new SimpleTextPrinter().printController(System.out, cc.calculateConcreteController());
      } else {
        MAAMinimizeAutomatonPrinter.REMOVE_DEAD_STATES = false;
        new MAAMinimizeAutomatonPrinter(gameModel).printController(System.out, cc.calculateConcreteController());
      }
    } catch (Exception e) {
      System.out.println("Error: Could not generate concrete counter-strategy due to: " + e.getMessage());
    }
    
  }


  private static void synthesizeController(boolean optimize, boolean grouping, String outputFolderName,
      boolean log, BDDPackage pkg, SpectraAnalyses controllerKind, boolean reorderBeforeSave, boolean printJtlvFormat) {
    
    TransFuncType transFuncType = optimize ? TransFuncType.DECOMPOSED_FUNC : TransFuncType.SINGLE_FUNC;
    // construction of concrete controller always uses single trans() function
    transFuncType = SpectraAnalyses.CONCRETE_CONTROLLER.equals(controllerKind) ? TransFuncType.SINGLE_FUNC : transFuncType;
    // static symbolic controller is monolithic and also needs complete trans() function
    transFuncType = SpectraAnalyses.STATIC_SYMBOLIC_CONTROLLER.equals(controllerKind) ? TransFuncType.SINGLE_FUNC : transFuncType;
    
    // create game model
    GameModel gameModel = BDDGenerator.generateGameModel(gi, TraceInfo.NONE, grouping, transFuncType, log);
    
    // check realizability
    GR1Game gr1 = getGR1Game(gameModel, pkg, optimize);
    boolean realizable = gr1.checkRealizability();
    if (!realizable) {
      System.out.println("Error: Cannot synthesize an unrealizable specification");
    } else {
      System.out.println("Result: Specification is realizable. Proceeding to synthesis.");
      BDD minWinCred = Env.TRUE();
      if (gameModel.getWeights() != null) {
        minWinCred = BDDEnergyReduction.getMinWinCred(gameModel, gr1.sysWinningStates());
      }

      if (SpectraAnalyses.STATIC_SYMBOLIC_CONTROLLER.equals(controllerKind)) {

        SymbolicControllerConstruction cc = new GR1SymbolicControllerConstruction(gr1.getMem(), gameModel);
        SymbolicController ctrl = cc.calculateSymbolicController();

        ctrl.initial().andWith(minWinCred);

        try {
          // create symbolic controller if not exists
          // write the actual symbolic controller BDDs and doms
          SymbolicControllerReaderWriter.writeSymbolicController(ctrl, gameModel, outputFolderName, gi.getName(),
              reorderBeforeSave);
        } catch (Exception e) {
          System.out.println("Error: Could not write bdd files");
          e.printStackTrace();
          return;
        }

        System.out.println("Result: Successfully synthesized a static controller in output folder");

      } else if (SpectraAnalyses.SYMBOLIC_CONTROLLER.equals(controllerKind)) {

        SymbolicControllerJitInfoConstruction jitInfoConstruction = new SymbolicControllerJitInfoConstruction(
            gr1.getMem(), gameModel, minWinCred);
        SymbolicControllerJitInfo jitInfo = jitInfoConstruction.calculateJitSymbollicControllerInfo();

        try {
          // write down symbolic controller jit info
          SymbolicControllerReaderWriter.writeJitSymbolicController(jitInfo, gameModel, outputFolderName, gi.getName(),
              reorderBeforeSave);
        } catch (Exception e) {
          System.out.println("Error: Could not write bdd files");
          e.printStackTrace();
          return;
        }

        System.out.println("Result: Successfully synthesized a just-in-time controller in output folder");
      } else if (SpectraAnalyses.CONCRETE_CONTROLLER.equals(controllerKind)) {
        ConcreteControllerConstruction cc = new GR1ConceteControllerConstructionSkip(gr1.getMem(), gameModel);
        try {
          if (printJtlvFormat) {
            new SimpleTextPrinter().printController(System.out, cc.calculateConcreteController());
          } else {
            MAAMinimizeAutomatonPrinter.REMOVE_DEAD_STATES = true;
            new MAAMinimizeAutomatonPrinter(gameModel).printController(System.out, cc.calculateConcreteController());
          }
          
        } catch (AbstractGamesException e) {
          System.out.println("Error: Could not generate concrete controller due to: " + e.getMessage());
          return;
        }
      }

    }
    gameModel.free();
    gr1.free();
  }


  private static void checkWellSeparation(CommandLine cmd, boolean log, boolean grouping) {
    Systems systems = cmd.hasOption(ignoreSys)? Systems.NONE : Systems.SPEC;
    WellSeparationChecker checker = new WellSeparationChecker();
    
    GameModel model = BDDGenerator.generateGameModel(gi, TraceInfo.ALL, grouping, TransFuncType.SINGLE_FUNC, log);
    boolean hasGF = (model.getEnv().justiceNum() != 0);
    
    List<String> res = null;
    try {
      res = checker.diagnose(model, systems);
    } catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
      return;
    }
    if (res != null) {
      if (res.isEmpty()) {
        System.out.println("Result: Specification is well-separated");
      } else {
        System.out.println("Result: Specification is non-well-separated");
        for (String info : res) {
          // filter out justice reasons when the only justice is GF TRUE added by
          // algorithm
          if (!info.contains(EnvSpecPart.JUSTICE.toString()) || hasGF) {
            String message = "  The system can force the environment to violate ";
            if (info.contains(EnvSpecPart.INI.toString())) {
              message += "some initial assumption ";
            } else if (info.contains(EnvSpecPart.SAFETY.toString())) {
              message += "some safety assumption (G ...) ";
            } else {
              message += "some justice assumption (GF ...) ";
            }
            if (info.contains(Positions.ALL.toString())) {
              message += " from all initial states.";
            } else {
              message += " from some reachable states.";
            }
            System.out.println(message);
          }
        }
      }
    }
  }


  private static void checkRealizability(boolean optimize, boolean grouping, boolean log, BDDPackage pkg) {
    GameModel gameModel = BDDGenerator.generateGameModel(
        gi, TraceInfo.NONE, grouping, 
        optimize ? TransFuncType.DECOMPOSED_FUNC : TransFuncType.SINGLE_FUNC, log);
    GR1Game gr1 = getGR1Game(gameModel, pkg, optimize);
    boolean realizable = gr1.checkRealizability();
    System.out.println("Result: Specification is " + (realizable ? "realizable" : "unrealizable"));    
    gameModel.free();
    gr1.free();    
  }


  private static void prepareGameInput(CommandLine cmd, String fileName) throws Exception  {
    SpectraInputProviderNoIDE sip = new SpectraInputProviderNoIDE();
		try {
			gi = sip.getGameInput(fileName);
		} catch (SpectraTranslationException | ErrorsInSpectraException e) {
			System.out.println("Error: Could not prepare game input from Spectra file");
			throw e;
		}
		
		if (!gi.getWeightDefs().isEmpty()) {
			if (!cmd.hasOption("energy-bound")) {
				throw new RuntimeException("Energy bound not specified");
			} else {
				int energyBound = Integer.parseInt(cmd.getOptionValue(energy));
				gi.setEnergyBound(energyBound);
			}
		}
		
		try {
			List<Translator> transList = DefaultTranslators.getDefaultTranslators();
			TranslationProvider.translate(gi, transList);
		} catch (TranslationException e) {
			System.out.println("Error: Could not execute translators on Spectra file.");
			throw e;
		}
  }


  private static BDDPackage setBDDEngine(CommandLine cmd) {
    boolean jtlv = cmd.hasOption(jtlvBDD);
		BDDPackage pkg = jtlv ? BDDPackage.JTLV : BDDPackage.CUDD;
		BDDPackage.BBDPackageVersion version = jtlv ? BBDPackageVersion.DEFAULT : BBDPackageVersion.CUDD_3_0;
		BDDPackage.setCurrPackage(pkg, version);
		Env.enableReorder();
		Env.TRUE().getFactory().autoReorder(BDDFactory.REORDER_SIFT);
    return pkg;
  }


	private static SpectraAnalyses getAnalysis(String optionValue) {
    for (SpectraAnalyses a : SpectraAnalyses.values()) {
      if (a.getDescription().equals(optionValue)) {
        return a;
      }
    }
    return null;
  }


  private static boolean isStaticController(SpectraAnalyses analysis) {
	  return SpectraAnalyses.STATIC_SYMBOLIC_CONTROLLER.equals(analysis);
  }


  /**
	 * computes the output folder name based on the -o option or the default value if not provided. 
	 * 
	 * @param cmd
	 * @param fileName
	 * @param staticController
	 * @return
	 */
  private static String setOutputFolderName(CommandLine cmd, String fileName, boolean staticController) {
    String outputFolderName;
    if (cmd.hasOption(outputFolder)) {
      outputFolderName = cmd.getOptionValue(outputFolder);
    } else {
      // Attempt to output the controller in an out folder in the same directory of the spec file
      File f = new File(fileName);
      if (f.getParent() != null) {
        outputFolderName = new File(fileName).getParent() + File.separator + "out"; 
      } else {
        outputFolderName = "out";
      }
    }

    // append static or jit folder based on controller type
    if (staticController) {
      outputFolderName += File.separator + "static";
    } else {
      outputFolderName += File.separator + "jit";
    }
    
    return outputFolderName;
  }

  /**
   * prints the available analyses
   * 
   * @return
   */
  private static String printAnalyses() {
    StringBuilder sb = new StringBuilder();
    for (SpectraAnalyses a : SpectraAnalyses.values()) {
      sb.append(a.getDescription()).append(", ");
    }
    // remove last comma
    if (sb.length() > 0) {
      sb.delete(sb.length() - 2, sb.length());
    }
    return sb.toString();
  }

  /**
   * prints command line help message
   */
  private static void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.setOptionComparator(null);
    formatter.printHelp("java -jar spectra-cli.jar ", options, true);
  }

	private static GR1Game getGR1Game(GameModel gameModel, BDDPackage pkg, boolean optimize) {
		
		GR1Game gr1;
		
		if (BDDPackage.CUDD.equals(pkg)) {
			gr1 = new GR1GameImplC(gameModel);
		} else if (optimize) {
			gr1 = new GR1GameExperiments(gameModel);
		} else {
			gr1 = new GR1GameMemoryless(gameModel);
		}
		
		return gr1;
  }

  private static RabinGame getRabinGame(GameModel gameModel, BDDPackage pkg, boolean optimize) {

    RabinGame rabin;

    if (BDDPackage.CUDD.equals(pkg)) {
      rabin = new RabinGameImplC(gameModel);
    } else if (optimize) {
      System.out.println("Warning: Rabin game only supports optimizations when used with CUDD");
      rabin = new RabinGame(gameModel);
    } else {
      rabin = new RabinGame(gameModel);
    }

    return rabin;
  }
}
