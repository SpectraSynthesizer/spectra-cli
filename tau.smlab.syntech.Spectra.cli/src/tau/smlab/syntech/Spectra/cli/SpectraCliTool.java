package tau.smlab.syntech.Spectra.cli;

import java.io.File;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import tau.smlab.syntech.bddgenerator.BDDGenerator;
import tau.smlab.syntech.bddgenerator.BDDGenerator.TraceInfo;
import tau.smlab.syntech.bddgenerator.energy.BDDEnergyReduction;
import tau.smlab.syntech.gameinput.model.GameInput;
import tau.smlab.syntech.gameinputtrans.TranslationException;
import tau.smlab.syntech.gameinputtrans.TranslationProvider;
import tau.smlab.syntech.gameinputtrans.translator.DefaultTranslators;
import tau.smlab.syntech.gameinputtrans.translator.Translator;
import tau.smlab.syntech.gamemodel.GameModel;
import tau.smlab.syntech.gamemodel.PlayerModule.TransFuncType;
import tau.smlab.syntech.games.controller.enumerate.ConcreteControllerConstruction;
import tau.smlab.syntech.games.controller.enumerate.printers.MAAMinimizeAutomatonPrinter;
import tau.smlab.syntech.games.controller.enumerate.printers.SimpleTextPrinter;
import tau.smlab.syntech.games.controller.symbolic.SymbolicController;
import tau.smlab.syntech.games.controller.symbolic.SymbolicControllerConstruction;
import tau.smlab.syntech.games.controller.symbolic.SymbolicControllerJitInfo;
import tau.smlab.syntech.games.controller.symbolic.SymbolicControllerReaderWriter;
import tau.smlab.syntech.games.gr1.GR1Game;
import tau.smlab.syntech.games.gr1.GR1GameExperiments;
import tau.smlab.syntech.games.gr1.GR1GameImplC;
import tau.smlab.syntech.games.gr1.GR1GameMemoryless;
import tau.smlab.syntech.games.gr1.GR1SymbolicControllerConstruction;
import tau.smlab.syntech.games.gr1.jit.SymbolicControllerJitInfoConstruction;
import tau.smlab.syntech.games.rabin.RabinConcreteControllerConstruction;
import tau.smlab.syntech.games.rabin.RabinGame;
import tau.smlab.syntech.jtlv.BDDPackage;
import tau.smlab.syntech.jtlv.Env;
import tau.smlab.syntech.jtlv.ModuleVariableException;
import tau.smlab.syntech.jtlv.BDDPackage.BBDPackageVersion;
import tau.smlab.syntech.spectragameinput.ErrorsInSpectraException;
import tau.smlab.syntech.spectragameinput.SpectraInputProviderNoIDE;
import tau.smlab.syntech.spectragameinput.SpectraTranslationException;
	
public class SpectraCliTool {

	public static void main(String[] args) throws ParseException {

		Options options = new Options();
		options.addOption("i", "input", true, "Spectra input file name");
		options.addOption("o", "output", true, "Ouptut folder");
		options.addOption("s", "synthesize", false, "Synthesize symbolic controller");
		options.addOption(null, "static", false, "Synthesize static symbolic controller");
		options.addOption(null, "jtlv", false, "Use JTLV package instead of CUDD");
		options.addOption(null, "disable-opt", true, "Disable optimizations");
		options.addOption(null, "disable-grouping", false, "Disable reorder with grouping");
		options.addOption("v", "verbose", false, "Verbose logging");
		options.addOption(null, "reorder", false, "Reorder BDD before save for reduced size");
		options.addOption(null, "counter-strategy", false, "Generate counter-strategy for an unrealizable specification");
		options.addOption(null, "counter-strategy-jtlv-format", false, "Generate counter-strategy for an unrealizable specification and print in JTLV format");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		
		if (!cmd.hasOption("i")) {
			System.out.println("Error: No Spectra file name provided");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar spectra-cli.jar", options);
			return;
		}
		String fileName = cmd.getOptionValue("i");
		
		String outputFolderName; 
		if (cmd.hasOption("o")) {
			outputFolderName = cmd.getOptionValue("o");
		} else {
			// Attempt to output the controller in an out folder in the same directory of the spec file
			File f = new File(fileName);
			if (f.getParent() != null) {
				outputFolderName = new File(fileName).getParent() + File.separator + "out";	
			} else {
				outputFolderName = "out";
			}
		}
		
		boolean synthesize = cmd.hasOption("s");
		boolean stat = cmd.hasOption("static");
		boolean optimize = !cmd.hasOption("disable-opt");
		boolean grouping = !cmd.hasOption("disable-grouping");
		boolean jtlv = cmd.hasOption("jtlv");
		boolean verbose = cmd.hasOption("v");
		boolean reorder = cmd.hasOption("reorder");
		boolean counterStrategyJtlvFormat = cmd.hasOption("counter-strategy-jtlv-format");
		boolean counterStrategy = cmd.hasOption("counter-strategy") || counterStrategyJtlvFormat;
		
		
		BDDPackage pkg = jtlv ? BDDPackage.JTLV : BDDPackage.CUDD;
		BDDPackage.BBDPackageVersion version = jtlv ? BBDPackageVersion.DEFAULT : BBDPackageVersion.CUDD_3_0;
		BDDPackage.setCurrPackage(pkg, version);
		Env.enableReorder();
		Env.TRUE().getFactory().autoReorder(BDDFactory.REORDER_SIFT);

		SpectraInputProviderNoIDE sip = new SpectraInputProviderNoIDE();
		GameInput gi;
		try {
			gi = sip.getGameInput(fileName);
		} catch (SpectraTranslationException | ErrorsInSpectraException e) {
			System.out.println("Error: Could not prepare game input from Spectra file. "
					+ "Please verify that the file is a valid Spectra specification.");
			e.printStackTrace();
			return;
		}
		
		if (!gi.getWeightDefs().isEmpty()) {
			if (!cmd.hasOption("energy-bound")) {
				System.out.println("Error: Must specify energy bound for specification");
				return;
			} else {
				int energyBound = Integer.parseInt(cmd.getOptionValue("energy-bound"));
				gi.setEnergyBound(energyBound);
			}
		}
		
		try {
			List<Translator> transList = DefaultTranslators.getDefaultTranslators();
			TranslationProvider.translate(gi, transList);
		} catch (TranslationException e) {
			System.out.println("Error: Could not execute translators on Spectra file. "
					+ "Please verify that the file is a valid Spectra specification.");
			e.printStackTrace();
			return;
		}
		GameModel gameModel = BDDGenerator.generateGameModel(
				gi, TraceInfo.NONE, grouping, 
				optimize ? TransFuncType.DECOMPOSED_FUNC : TransFuncType.SINGLE_FUNC, verbose);
		GR1Game gr1 = getGR1Game(gameModel, gi, pkg, optimize);
		
		if (synthesize) {
			
			if (!gr1.checkRealizability()) {
				System.out.println("Error: Cannot synthesize an unrealizable specification");
				return;
			}
			
			System.out.println("Result: Specification is realizable. Proceeding to synthesis");
			
			BDD minWinCred = Env.TRUE();
			if (gameModel.getWeights() != null) {
				minWinCred = BDDEnergyReduction.getMinWinCred(gameModel, gr1.sysWinningStates());					
			}
			
			if (stat) {
				
				SymbolicControllerConstruction cc = new GR1SymbolicControllerConstruction(gr1.getMem(), gameModel);
				SymbolicController ctrl = cc.calculateSymbolicController();
				
				ctrl.initial().andWith(minWinCred);
				
				try {
					// create symbolic controller if not exists
					// write the actual symbolic controller BDDs and doms
					SymbolicControllerReaderWriter.writeSymbolicController(ctrl, gameModel, outputFolderName, reorder);
				} catch (Exception e) {
					System.out.println("Error: Could not write bdd files");
					e.printStackTrace();
					return;
				}
				
				System.out.println("Result: Successfully synthesized a static controller in output folder");
				
			} else {
				
				SymbolicControllerJitInfoConstruction jitInfoConstruction = new SymbolicControllerJitInfoConstruction(gr1.getMem(), gameModel, minWinCred);
				SymbolicControllerJitInfo jitInfo = jitInfoConstruction.calculateJitSymbollicControllerInfo();
				
				try {
					// write down symbolic controller jit info
					SymbolicControllerReaderWriter.writeJitSymbolicController(jitInfo, gameModel, outputFolderName, reorder);
				} catch (Exception e) {
					System.out.println("Error: Could not write bdd files");
					e.printStackTrace();
					return;
				}
				
				System.out.println("Result: Successfully synthesized a just-in-time controller in output folder");
			}
			
		} else {  // Only check realizability
			
			if (gr1.checkRealizability()) {
				System.out.println("Result: Specification is realizable");
				
				if (counterStrategy) {
					System.out.println("Error: Cannot generate counter-strategy for a realizable specification");
				}
			} else {
				System.out.println("Result: Specification is unrealizable");
				
				if (counterStrategy) {

					RabinGame rabin = new RabinGame(gameModel);
					if (!rabin.checkRealizability()) {
						System.out.println("Error: Cannot generate counter-strategy for a realizable specification");
					}

					if (gameModel.getWeights() != null) {
						try {
							BDDEnergyReduction.updateSysIniTransWithEngConstraintsForCounterStrategy(gameModel, gi.getEnergyBound());
						} catch (ModuleVariableException e) {
							System.out.println("Error: Could not generate concrete counter-strategy");
							e.printStackTrace();
						}
					}

					Env.disableReorder();

					ConcreteControllerConstruction cc = new RabinConcreteControllerConstruction(rabin.getMem(), gameModel);
					try {
						if (counterStrategyJtlvFormat) {
							new SimpleTextPrinter().printController(System.out, cc.calculateConcreteController());
						} else {
							MAAMinimizeAutomatonPrinter.REMOVE_DEAD_STATES = false;
							new MAAMinimizeAutomatonPrinter(gameModel).printController(System.out, cc.calculateConcreteController());
						}
					} catch (Exception e) {
						System.out.println("Error: Could not generate concrete counter-strategy");
						e.printStackTrace();
					}
				}
			}
		}
		
		gameModel.free();
		gr1.free();
	}

	private static GR1Game getGR1Game(GameModel gameModel, GameInput gi, BDDPackage pkg, boolean optimize) {
		
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
}
