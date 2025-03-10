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
package org.objectweb.asm.commons;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.test.AsmTest;
import org.objectweb.asm.test.ClassFile;

/**
 * Unit tests for {@link CodeSizeEvaluator}.
 *
 * @author Eric Bruneton
 */
class CodeSizeEvaluatorTest extends AsmTest {

  /**
   * Tests that the size estimations of CodeSizeEvaluator are correct, and that classes are
   * unchanged with a ClassReader->CodeSizeEvaluator->ClassWriter transform.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testAllMethods_precompiledClass(
      final PrecompiledClass classParameter, final Api apiParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);
    ArrayList<CodeSizeEvaluation> evaluations = new ArrayList<>();
    ClassVisitor codeSizesEvaluator =
        new CodeSizesEvaluator(apiParameter.value(), classWriter, evaluations);

    Executable accept = () -> classReader.accept(codeSizesEvaluator, attributes(), 0);

    if (classParameter.isMoreRecentThan(apiParameter)) {
      Exception exception = assertThrows(UnsupportedOperationException.class, accept);
      assertTrue(exception.getMessage().matches(UNSUPPORTED_OPERATION_MESSAGE_PATTERN));
    } else {
      assertDoesNotThrow(accept);
      for (CodeSizeEvaluation evaluation : evaluations) {
        assertTrue(evaluation.actualSize >= evaluation.minSize);
        assertTrue(evaluation.actualSize <= evaluation.maxSize);
      }
      assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
    }
  }

  public static Attribute[] attributes() {
    return new Attribute[] {new Comment(), new CodeComment()};
  }

  static class CodeSizeEvaluation {

    final int minSize;
    final int maxSize;
    final int actualSize;

    CodeSizeEvaluation(final int minSize, final int maxSize, final int actualSize) {
      this.minSize = minSize;
      this.maxSize = maxSize;
      this.actualSize = actualSize;
    }
  }

  static class CodeSizesEvaluator extends ClassVisitor {

    public final ArrayList<CodeSizeEvaluation> evaluations;

    CodeSizesEvaluator(
        final int api,
        final ClassWriter classWriter,
        final ArrayList<CodeSizeEvaluation> evaluations) {
      super(api, classWriter);
      this.evaluations = evaluations;
    }

    @Override
    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String descriptor,
        final String signature,
        final String[] exceptions) {
      MethodVisitor methodVisitor =
          super.visitMethod(access, name, descriptor, signature, exceptions);
      return new CodeSizeEvaluator(api, methodVisitor) {

        @Override
        public void visitLdcInsn(final Object value) {
          if (value instanceof Boolean
              || value instanceof Byte
              || value instanceof Short
              || value instanceof Character
              || value instanceof Integer
              || value instanceof Long
              || value instanceof Double
              || value instanceof Float
              || value instanceof String
              || value instanceof Type
              || value instanceof Handle
              || value instanceof ConstantDynamic) {
            super.visitLdcInsn(value);
          } else {
            // If this happens, add support for the new type in
            // CodeSizeEvaluator.visitLdcInsn(), if needed.
            throw new IllegalArgumentException("Unsupported type of value: " + value);
          }
        }

        @Override
        public void visitMaxs(final int maxStack, final int maxLocals) {
          Label end = new Label();
          visitLabel(end);
          super.visitMaxs(maxStack, maxLocals);

          evaluations.add(new CodeSizeEvaluation(getMinSize(), getMaxSize(), end.getOffset()));
        }
      };
    }
  }
}
