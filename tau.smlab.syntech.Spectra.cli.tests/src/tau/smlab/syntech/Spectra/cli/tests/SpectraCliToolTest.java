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
		
		assertTrue(outContent.toString().contains("Error: Must specify energy bound for specification"));
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
	void testSynthesizeCreatesFilesWithCudd() throws IOException {
		
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/Realizable.spectra",
					"-s",
					"--static"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Successfully synthesized a static controller in output folder"));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/controller.init.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/controller.trans.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/vars.doms")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out")));
		
	}
	
	@Test
	void testJitSynthesizeCreatesFilesWithCudd() throws IOException {
		
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/Realizable.spectra",
					"-s"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Successfully synthesized a just-in-time controller in output folder"));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/fixpoints.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/trans.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/justice.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/sizes")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/vars.doms")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out")));
		
	}
	
	@Test
	void testJitSynthesizeCreatesFilesWithCuddSameFolder() throws IOException {
		
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"Realizable.spectra",
					"-s"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Successfully synthesized a just-in-time controller in output folder"));
		assertTrue(Files.deleteIfExists(Paths.get("out/fixpoints.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("out/trans.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("out/justice.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("out/sizes")));
		assertTrue(Files.deleteIfExists(Paths.get("out/vars.doms")));
		assertTrue(Files.deleteIfExists(Paths.get("out")));
		
	}
	
	@Test
	void testSynthesizeCreatesFilesWithReorderBeforeSave() throws IOException {
		
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/Realizable.spectra",
					"-s",
					"-reorder"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Successfully synthesized a just-in-time controller in output folder"));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/fixpoints.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/trans.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/justice.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/sizes")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/vars.doms")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out")));
		
	}
	
	@Test
	void testUnrealizableTryToSynthesizeWithCudd() {
		
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/Unrealizable.spectra",
					"-s"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Error: Cannot synthesize an unrealizable specification"));
	}
	
	@Test
	void testUnrealizableCounterStrategyJtlv() {
		
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/Unrealizable.spectra",
					"-counter-strategy",
					"-jtlv"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("With no successors"));
	}
	
	@Test
	void testUnrealizableCounterStrategyMAA() {
		
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/Unrealizable.spectra",
					"-counter-strategy"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("INI -> DEAD"));
	}
	
	@Test
	void testRealizableCounterStrategy() {
		
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/Realizable.spectra",
					"-counter-strategy"});
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
		
		assertTrue(outContent.toString().contains("Error: No Spectra file name provided"));
	}
	
	@Test
	void testOutputFileProvided() throws IOException {
		
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/Realizable.spectra",
					"--synthesize",
					"--static",
					"-o",
					"models/custom-folder"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Successfully synthesized a static controller in output folder"));
		assertTrue(Files.deleteIfExists(Paths.get("models/custom-folder/controller.init.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/custom-folder/controller.trans.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/custom-folder/vars.doms")));
		assertTrue(Files.deleteIfExists(Paths.get("models/custom-folder")));
	}
}
