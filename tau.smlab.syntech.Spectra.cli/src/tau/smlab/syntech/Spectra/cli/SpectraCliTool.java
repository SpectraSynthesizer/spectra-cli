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
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import net.sf.javabdd.BDD;
import tau.smlab.syntech.bddgenerator.BDDEnergyReduction;
import tau.smlab.syntech.bddgenerator.BDDGenerator;
import tau.smlab.syntech.bddgenerator.BDDGenerator.TraceInfo;
import tau.smlab.syntech.gameinput.model.GameInput;
import tau.smlab.syntech.gameinputtrans.TranslationException;
import tau.smlab.syntech.gameinputtrans.TranslationProvider;
import tau.smlab.syntech.gameinputtrans.translator.DefaultTranslators;
import tau.smlab.syntech.gameinputtrans.translator.Translator;
import tau.smlab.syntech.gamemodel.GameModel;
import tau.smlab.syntech.gamemodel.PlayerModule.TransFuncType;
import tau.smlab.syntech.games.controller.symbolic.SymbolicController;
import tau.smlab.syntech.games.controller.symbolic.SymbolicControllerConstruction;
import tau.smlab.syntech.games.controller.symbolic.SymbolicControllerReaderWriter;
import tau.smlab.syntech.games.gr1.GR1Game;
import tau.smlab.syntech.games.gr1.GR1GameExistentialMemoryless;
import tau.smlab.syntech.games.gr1.GR1GameExperiments;
import tau.smlab.syntech.games.gr1.GR1GameImplC;
import tau.smlab.syntech.games.gr1.GR1GameMemoryless;
import tau.smlab.syntech.games.gr1.GR1SymbolicControllerConstruction;
import tau.smlab.syntech.jtlv.BDDPackage;
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
		options.addOption(null, "jtlv", false, "Use JTLV package instead of CUDD");
		options.addOption(null, "disable-opt", true, "Disable optimizations");
		options.addOption(null, "disable-grouping", false, "Disable reorder with grouping");
		options.addOption("v", "verbose", false, "Verbose logging");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		
		if (!cmd.hasOption("i")) {
			System.out.println("Error: no Spectra file name provided");
			return;
		}
		String fileName = cmd.getOptionValue("i");
		
		String outputFolderName; 
		if (cmd.hasOption("o")) {
			outputFolderName = cmd.getOptionValue("o");
		} else {
			outputFolderName = new File(fileName).getParent() + File.separator + "out";
		}
		
		boolean synthesize = cmd.hasOption("synthesize");
		boolean optimize = !cmd.hasOption("disable-opt");
		boolean grouping = !cmd.hasOption("disable-grouping");
		boolean jtlv = cmd.hasOption("jtlv");
		boolean verbose = cmd.hasOption("v");
		BDDPackage pkg = jtlv ? BDDPackage.JTLV : BDDPackage.CUDD;
		BDDPackage.BBDPackageVersion version = jtlv ? BBDPackageVersion.DEFAULT : BBDPackageVersion.CUDD_3_0;
		BDDPackage.setCurrPackage(pkg, version);

		SpectraInputProviderNoIDE sip = new SpectraInputProviderNoIDE();
		GameInput gi;
		try {
			gi = sip.getGameInput(fileName);
		} catch (SpectraTranslationException | ErrorsInSpectraException e) {
			System.out.println("Error: could not prepare game input from Spectra file. "
					+ "Please verify that the file is a valid Spectra specification.");
			e.printStackTrace();
			return;
		}
		
		try {
			List<Translator> transList = DefaultTranslators.getDefaultTranslators();
			TranslationProvider.translate(gi, transList);
		} catch (TranslationException e) {
			System.out.println("Error: could not execute translators on Spectra file. "
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
				System.out.println("Error: Trying to synthesize an unrealizable specification");
				return;
			}
			
			System.out.println("Result: Specification is realizable. Proceeding to synthesis");
			
			SymbolicControllerConstruction cc = new GR1SymbolicControllerConstruction(gr1.getMem(), gameModel);
			SymbolicController ctrl = cc.calculateSymbolicController();
			
			if (gameModel.getWeights() != null) {
				BDD minWinCred = BDDEnergyReduction.getMinWinCred(gameModel, gr1.sysWinningStates());					
				ctrl.initial().andWith(minWinCred);
			}
			
			try {
				// create symbolic controller if not exists
				File outFolder = new File(outputFolderName);
				
				// write the actual symbolic controller BDDs and doms
				SymbolicControllerReaderWriter.writeSymbolicController(ctrl, gameModel, outFolder.getAbsolutePath());
			} catch (Exception e) {
				System.out.println("Error: could not write bdd files");
				e.printStackTrace();
				return;
			}
			
			System.out.println("Result: Successfully synthesized a controller in /out folder");
			
		} else {  // Only check realizability
			
			if (gr1.checkRealizability()) {
				System.out.println("Result: Specification is realizable");
			} else {
				System.out.println("Result: Specification is unrealizable");
			}
		}
		
		gameModel.free();
		gr1.free();
	}

	private static GR1Game getGR1Game(GameModel gameModel, GameInput gi, BDDPackage pkg, boolean optimize) {
		
		GR1Game gr1;
		
		if (gameModel.getSys().existentialGarNum() > 0) {
			gr1 = new GR1GameExistentialMemoryless(gameModel);
		} else {
			if (BDDPackage.CUDD.equals(pkg)) {
				gr1 = new GR1GameImplC(gameModel);
			} else if (optimize) {
				gr1 = new GR1GameExperiments(gameModel);
			} else {
				gr1 = new GR1GameMemoryless(gameModel);
			}
		}
		
		return gr1;
	}
}
