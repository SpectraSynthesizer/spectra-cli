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

package tau.smlab.syntech.Spectra.cli.tests;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import tau.smlab.syntech.Spectra.cli.SpectraAnalyses;
import tau.smlab.syntech.Spectra.cli.SpectraCliTool;

@TestInstance(Lifecycle.PER_CLASS)
public class SpectraCliToolTest {
	
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	private final PrintStream originalOut = System.out;
	private final PrintStream originalErr = System.err;
	
	@BeforeAll
	public void setUpStreams() {
	    System.setOut(new PrintStream(outContent));
	    System.setErr(new PrintStream(errContent));
	}

	@AfterAll
	public void restoreStreams() {
	    System.setOut(originalOut);
	    System.setErr(originalErr);
	}

	@Test
	void testRealizableSpecWithCudd() {
	
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/Realizable.spectra"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Specification is realizable"));
	}
	
	 @Test
	  void testTmp() {
	  
	    try {
	      SpectraCliTool.main(new String[] {
	          "-i",
	          "models/tmp.spectra"});
	    } catch (ParseException e) {
	      e.printStackTrace(originalOut);
	      throw new RuntimeException(e);
	    }
	    assertTrue(outContent.toString().contains("Error: XtextSyntaxDiagnostic: null:1 no viable alternative at input 'env'"));
	  }
	
	@Test
	void testRealizableSpecWithCuddVerbose() {
	
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/Realizable.spectra",
					"-v"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Env domains:"));
		assertTrue(outContent.toString().contains("Sys domains:"));
		assertTrue(outContent.toString().contains("Spec Stats"));
		assertTrue(outContent.toString().contains("Result: Specification is realizable"));
	}
	
	@Test
	void testNonTranslatableSpecWithCudd() {
	
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/NotTranslatable.spectra"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Error: Could not execute translators on Spectra file"));
	}
	
	@Test
	void testWeightsWithoutEnergyBound() {
	
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/RealWeights.spectra"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Error: Energy bound not specified"));
	}
	
	@Test
	void testWeightsWithEnergyBound() {
	
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/RealWeights.spectra",
					"-energy-bound",
					"5"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Specification is realizable"));
	}
	
	@Test
	void testUnealizableSpecWithCudd() {
		
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/Unrealizable.spectra"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Specification is unrealizable"));
	}
	
	@Test
	void testNonWellSeparatedSpec() {
		
		try {
			SpectraCliTool.main(new String[] {
			    "-a",
			    SpectraAnalyses.CHECK_WELL_SEP.getDescription(),
					"-i",
					"models/NonWellSep.spectra"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Specification is non-well-separated"));
	}
	
	@Test
	void testNonWellSeparatedSpecIgnoreSystem() {
		
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/NonWellSep.spectra",
					"-well-separation-ignore-sys"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Specification is non-well-separated"));
	}
	
	@Test
	void testWellSeparatedSpec() {
		
		try {
			SpectraCliTool.main(new String[] {
          "-a",
          SpectraAnalyses.CHECK_WELL_SEP.getDescription(),
					"-i",
					"models/Realizable.spectra"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Specification is well-separated"));
	}
	
	@Test
	void testSynthesizeCreatesFilesWithCudd() throws IOException {
		
		try {
			SpectraCliTool.main(new String[] {
          "-a",
          SpectraAnalyses.STATIC_SYMBOLIC_CONTROLLER.getDescription(),
					"-i",
					"models/Realizable.spectra"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Successfully synthesized a static controller in output folder"));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/static/DiningPhilo5.controller.init.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/static/DiningPhilo5.controller.trans.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/static/DiningPhilo5.vars.doms")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/static")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out")));
		
	}
	
	@Test
	void testJitSynthesizeCreatesFilesWithCudd() throws IOException {
		
		try {
			SpectraCliTool.main(new String[] {
			    "-a",
          SpectraAnalyses.SYMBOLIC_CONTROLLER.getDescription(),
					"-i",
					"models/Realizable.spectra"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Successfully synthesized a just-in-time controller in output folder"));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/jit/DiningPhilo5.fixpoints.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/jit/DiningPhilo5.trans.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/jit/DiningPhilo5.justice.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/jit/DiningPhilo5.sizes")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/jit/DiningPhilo5.vars.doms")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/jit")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out")));
	}
	
	@Test
	void testJitSynthesizeCreatesFilesWithCuddSameFolder() throws IOException {
		
		try {
			SpectraCliTool.main(new String[] {
          "-a",
          SpectraAnalyses.SYMBOLIC_CONTROLLER.getDescription(),			    
					"-i",
					"Realizable.spectra"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Successfully synthesized a just-in-time controller in output folder"));
		assertTrue(Files.deleteIfExists(Paths.get("out/jit/DiningPhilo5.fixpoints.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("out/jit/DiningPhilo5.trans.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("out/jit/DiningPhilo5.justice.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("out/jit/DiningPhilo5.sizes")));
		assertTrue(Files.deleteIfExists(Paths.get("out/jit/DiningPhilo5.vars.doms")));
		assertTrue(Files.deleteIfExists(Paths.get("out/jit")));
		assertTrue(Files.deleteIfExists(Paths.get("out")));
		
	}
	
	@Test
	void testSynthesizeCreatesFilesWithReorderBeforeSave() throws IOException {
		
		try {
			SpectraCliTool.main(new String[] {
          "-a",
          SpectraAnalyses.SYMBOLIC_CONTROLLER.getDescription(),			    
					"-i",
					"models/Realizable.spectra",
					"-reorder"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Successfully synthesized a just-in-time controller in output folder"));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/jit/DiningPhilo5.fixpoints.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/jit/DiningPhilo5.trans.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/jit/DiningPhilo5.justice.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/jit/DiningPhilo5.sizes")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/jit/DiningPhilo5.vars.doms")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/jit")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out")));
	}
	
	@Test
	void testUnrealizableTryToSynthesizeWithCudd() {
		
		try {
			SpectraCliTool.main(new String[] {
			    "-a",
	        SpectraAnalyses.SYMBOLIC_CONTROLLER.getDescription(),
					"-i",
					"models/Unrealizable.spectra"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Error: Cannot synthesize an unrealizable specification"));
	}
	
	@Test
	void testUnrealizableCounterStrategyJtlv() {
		
		try {
			SpectraCliTool.main(new String[] {
          "-a",
          SpectraAnalyses.CONCRETE_COUNTER_STRATEGY.getDescription(),
					"-i",
					"models/Unrealizable.spectra",
					"-jtlv-format"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("With no successors"));
	}
	
	@Test
	void testUnrealizableCounterStrategyMAA() {
		
		try {
			SpectraCliTool.main(new String[] {
	         "-a",
	          SpectraAnalyses.CONCRETE_COUNTER_STRATEGY.getDescription(),
					"-i",
					"models/Unrealizable.spectra",
					});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("INI -> DEAD"));
	}
	
	@Test
	void testRealizableCounterStrategy() {
		
		try {
			SpectraCliTool.main(new String[] {
          "-a",
          SpectraAnalyses.CONCRETE_COUNTER_STRATEGY.getDescription(),
					"-i",
					"models/Realizable.spectra"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Error: Cannot generate counter-strategy for a realizable specification"));
	}
	
	@Test
	void testRealizableSpecWithJtlv() {
		
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/Realizable.spectra",
					"--jtlv"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Specification is realizable"));
	}
	
	@Test
	void testRealizableSpecNoOptAndGrouping() {
		
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/Realizable.spectra",
					"--disable-opt",
					"--disable-grouping"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Specification is realizable"));
	}
	
	@Test
	void testFileNotProvided() {
		
		try {
			SpectraCliTool.main(new String[] {});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Error: "));
	}
	
	@Test
	void testOutputFileProvided() throws IOException {
		
		try {
			SpectraCliTool.main(new String[] {
          "-a",
          SpectraAnalyses.STATIC_SYMBOLIC_CONTROLLER.getDescription(),
					"-i",
					"models/Realizable.spectra",
					"-o",
					"models/custom-folder"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Successfully synthesized a static controller in output folder"));
		assertTrue(Files.deleteIfExists(Paths.get("models/custom-folder/static/DiningPhilo5.controller.init.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/custom-folder/static/DiningPhilo5.controller.trans.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/custom-folder/static/DiningPhilo5.vars.doms")));
		assertTrue(Files.deleteIfExists(Paths.get("models/custom-folder/static")));
		assertTrue(Files.deleteIfExists(Paths.get("models/custom-folder")));
	}
	
	 @Test
	  void testUnrealCoreRealizable() {
	    
	    try {
	      SpectraCliTool.main(new String[] {
	          "-a",
	          SpectraAnalyses.UNREALIZABLE_CORE.getDescription(),
	          "-i",
	          "models/Realizable.spectra"});
	    } catch (ParseException e) {
	      e.printStackTrace();
	    }
	    
	    assertTrue(outContent.toString().contains("Result: Specification is realizable"));
	  }
	 
   @Test
   void testUnrealCore() {
     
     try {
       SpectraCliTool.main(new String[] {
           "-a",
           SpectraAnalyses.UNREALIZABLE_CORE.getDescription(),
           "-i",
           "models/Unrealizable.spectra"});
     } catch (ParseException e) {
       e.printStackTrace();
     }
     
     assertTrue(outContent.toString().contains("Result: Found unrealizable core with"));
   }
   
   @Test
   void testUnrealCoreLines() {
     
     try {
       SpectraCliTool.main(new String[] {
           "-a",
           SpectraAnalyses.UNREALIZABLE_CORE.getDescription(),
           "-i",
           "models/Unrealizable.spectra",
           "--print-core-lines",
           });
     } catch (ParseException e) {
       e.printStackTrace();
     }
     
     assertTrue(outContent.toString().contains("CORE-LINES: [15,27,31]"));
   }

   @Test
   void testNonWellSeparatedCore() {
     
     try {
       SpectraCliTool.main(new String[] {
           "-a",
           SpectraAnalyses.NON_WELL_SEP_CORE.getDescription(),
           "-i",
           "models/NonWellSep.spectra"});
     } catch (ParseException e) {
       e.printStackTrace();
     }
     
     assertTrue(outContent.toString().contains("Result: Found non-well-separated core with"));
   }
   
   @Test
   void testNonWellSeparatedCoreWellSep() {
     
     try {
       SpectraCliTool.main(new String[] {
           "-a",
           SpectraAnalyses.NON_WELL_SEP_CORE.getDescription(),
           "-i",
           "models/Realizable.spectra"});
     } catch (ParseException e) {
       e.printStackTrace();
     }
     
     assertTrue(outContent.toString().contains("Result: Specification is well-separated"));
   }
   
   @Test
   void testNonWellSeparatedCoreWellSepLines() {
     
     try {
       SpectraCliTool.main(new String[] {
           "-a",
           SpectraAnalyses.NON_WELL_SEP_CORE.getDescription(),
           "--print-core-lines",
           "-i",
           "models/Realizable.spectra"});
     } catch (ParseException e) {
       e.printStackTrace();
     }
     
     assertTrue(outContent.toString().contains("CORE-LINES: []"));
   }
   
   @Test
   void testNonWellSeparatedCoreLines() {
     
     try {
       SpectraCliTool.main(new String[] {
           "-a",
           SpectraAnalyses.NON_WELL_SEP_CORE.getDescription(),
           "--print-core-lines",
           "-i",
           "models/NonWellSep.spectra"});
     } catch (ParseException e) {
       e.printStackTrace();
     }
     
     assertTrue(outContent.toString().contains("CORE-LINES: [12,13]"));
   }
}
