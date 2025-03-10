// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.objectweb.asm.tree.analysis;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.test.AsmTest;
import org.objectweb.asm.test.AsmTest.PrecompiledClass;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Unit tests for {@link Analyzer}, when used with a {@link BasicInterpreter}.
 *
 * @author Eric Bruneton
 */
class AnalyzerWithBasicInterpreterTest extends AsmTest {

  public static final String CLASS_NAME = "C";

  @Test
  void testConstructor() {
    assertDoesNotThrow(() -> new BasicInterpreter());
    assertThrows(IllegalStateException.class, () -> new BasicInterpreter() {});
  }

  @Test
  void testAnalyze_invalidNewArray() {
    MethodNode methodNode =
        new MethodNodeBuilder().iconst_0().intInsn(Opcodes.NEWARRAY, -1).vreturn().build();

    Executable analyze =
        () -> new Analyzer<BasicValue>(new BasicInterpreter()).analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Invalid array type"));
  }

  /**
   * Tests that the precompiled classes can be successfully analyzed with a BasicInterpreter, and
   * that Analyzer can be subclassed to use custom frames.
   *
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  void testAnalyze_basicInterpreter(final PrecompiledClass classParameter, final Api apiParameter)
      throws AnalyzerException {
    ClassNode classNode = new ClassNode();
    new ClassReader(classParameter.getBytes()).accept(classNode, 0);
    Analyzer<BasicValue> analyzer =
        new Analyzer<BasicValue>(new BasicInterpreter()) {
          @Override
          public Frame<BasicValue> newFrame(final int numLocals, final int numStack) {
            return new CustomFrame(numLocals, numStack);
          }

          @Override
          public Frame<BasicValue> newFrame(final Frame<? extends BasicValue> src) {
            return new CustomFrame(src);
          }
        };

    ArrayList<Frame<BasicValue>[]> methodFrames = new ArrayList<>();
    for (MethodNode methodNode : classNode.methods) {
      methodFrames.add(analyzer.analyze(classNode.name, methodNode));
    }

    for (Frame<BasicValue>[] frames : methodFrames) {
      for (Frame<BasicValue> frame : frames) {
        assertTrue(frame == null || frame instanceof CustomFrame);
      }
    }
  }

  /**
   * Tests that the precompiled classes can be successfully analyzed with a BasicInterpreter, even
   * if the method node's max locals and max stack size are not set.
   *
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  void testAnalyzeAndComputeMaxs_basicInterpreter(
      final PrecompiledClass classParameter, final Api apiParameter) throws AnalyzerException {
    ClassNode classNode = new ClassNode();
    new ClassReader(classParameter.getBytes()).accept(classNode, 0);
    ArrayList<MethodMaxs> methodMaxs = new ArrayList<>();
    for (MethodNode methodNode : classNode.methods) {
      methodMaxs.add(new MethodMaxs(methodNode.maxStack, methodNode.maxLocals));
      methodNode.maxLocals = 0;
      methodNode.maxStack = 0;
    }
    Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(new BasicInterpreter());

    ArrayList<MethodMaxs> analyzedMethodMaxs = new ArrayList<>();
    for (MethodNode methodNode : classNode.methods) {
      analyzer.analyzeAndComputeMaxs(classNode.name, methodNode);
      analyzedMethodMaxs.add(new MethodMaxs(methodNode.maxStack, methodNode.maxLocals));
    }

    // jdk3.SubOptimalMaxStackAndLocals has non optimal max stack and max local values on purpose.
    assumeTrue(classParameter != PrecompiledClass.JDK3_SUB_OPTIMAL_MAX_STACK_AND_LOCALS);
    for (int i = 0; i < analyzedMethodMaxs.size(); ++i) {
      assertTrue(analyzedMethodMaxs.get(i).maxLocals >= methodMaxs.get(i).maxLocals);
      assertTrue(analyzedMethodMaxs.get(i).maxStack >= methodMaxs.get(i).maxStack);
    }
  }

  /**
   * Tests that analyzeAndComputeMaxs computes the smallest possible maxLocals for static methods.
   *
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @Test
  void testAnalyzeAndComputeMaxs_staticMethod() throws AnalyzerException {
    MethodNode methodNode =
        new MethodNodeBuilder("(I)V", /* maxStack= */ 0, /* maxLocals= */ 0).vreturn().build();
    methodNode.access |= Opcodes.ACC_STATIC;
    Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(new BasicInterpreter());

    analyzer.analyzeAndComputeMaxs("C", methodNode);

    assertEquals(1, methodNode.maxLocals);
    assertEquals(0, methodNode.maxStack);
  }

  /**
   * Tests that the analyzer does not loop infinitely, even if the {@link Interpreter#merge} method
   * does not follow its required contract (namely that if the merge result is equal to the first
   * argument, the first argument should be returned - see #316326).
   *
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @Test
  void testAnalyze_badInterpreter() {
    ClassNode classNode = new ClassNode();
    new ClassReader(PrecompiledClass.JDK8_ALL_FRAMES.getBytes()).accept(classNode, 0);
    Analyzer<BasicValue> analyzer =
        new Analyzer<BasicValue>(
            new BasicInterpreter(/* latest */ Opcodes.ASM10_EXPERIMENTAL) {
              @Override
              public BasicValue merge(final BasicValue value1, final BasicValue value2) {
                return new BasicValue(super.merge(value1, value2).getType());
              }
            });

    ArrayList<Executable> analyses = new ArrayList<>();
    for (MethodNode methodNode : classNode.methods) {
      analyses.add(() -> analyzer.analyze(CLASS_NAME, methodNode));
    }

    for (Executable analysis : analyses) {
      assertTimeoutPreemptively(ofSeconds(1), analysis);
    }
  }

  /**
   * Tests that stack map frames are correctly merged when a JSR instruction can be reached from two
   * different control flow paths, with different local variable types (#316204).
   *
   * @throws IOException if the test class can't be loaded.
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @Test
  void testAnalyze_mergeWithJsrReachableFromTwoDifferentPaths()
      throws IOException, AnalyzerException {
    ClassReader classReader =
        new ClassReader(Files.newInputStream(Paths.get("src/test/resources/Issue316204.class")));
    ClassNode classNode = new ClassNode();
    classReader.accept(classNode, 0);
    Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());

    analyzer.analyze(classNode.name, getMethod(classNode, "basicStopBundles"));

    assertEquals("RIR..... ", analyzer.getFrames()[104].toString());
  }

  public static MethodNode getMethod(final ClassNode classNode, final String name) {
    for (MethodNode methodNode : classNode.methods) {
      if (methodNode.name.equals(name)) {
        return methodNode;
      }
    }
    return null;
  }

  public static class CustomFrame extends Frame<BasicValue> {

    CustomFrame(final int numLocals, final int numStack) {
      super(numLocals, numStack);
    }

    CustomFrame(final Frame<? extends BasicValue> frame) {
      super(frame);
    }

    @Override
    public Frame<BasicValue> init(final Frame<? extends BasicValue> frame) {
      assertTrue(frame instanceof CustomFrame);
      return super.init(frame);
    }
  }

  public static class MethodMaxs {

    public final int maxStack;
    public final int maxLocals;

    public MethodMaxs(final int maxStack, final int maxLocals) {
      this.maxStack = maxStack;
      this.maxLocals = maxLocals;
    }
  }
}
