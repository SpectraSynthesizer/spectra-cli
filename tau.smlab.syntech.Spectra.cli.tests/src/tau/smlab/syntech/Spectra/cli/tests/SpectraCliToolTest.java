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
		
		assertTrue(outContent.toString().contains("Error: could not execute translators on Spectra file"));
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
					"-s"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Successfully synthesized a controller in /out folder"));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/controller.init.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/out/controller.trans.bdd")));
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
		
		assertTrue(outContent.toString().contains("Error: Trying to synthesize an unrealizable specification"));
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
		
		assertTrue(outContent.toString().contains("Error: no Spectra file name provided"));
	}
	
	@Test
	void testOutputFileProvided() throws IOException {
		
		try {
			SpectraCliTool.main(new String[] {
					"-i",
					"models/Realizable.spectra",
					"--synthesize",
					"-o",
					"models/custom-folder"});
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		assertTrue(outContent.toString().contains("Result: Successfully synthesized a controller in /out folder"));
		assertTrue(Files.deleteIfExists(Paths.get("models/custom-folder/controller.init.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/custom-folder/controller.trans.bdd")));
		assertTrue(Files.deleteIfExists(Paths.get("models/custom-folder/vars.doms")));
		assertTrue(Files.deleteIfExists(Paths.get("models/custom-folder")));
	}
}
