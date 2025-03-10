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
package org.objectweb.asm.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;

/**
 * Unit tests for {@link TraceSignatureVisitor}.
 *
 * @author Eugene Kuleshov
 */
class TraceSignatureVisitorTest {

  public static final String[][] CLASS_SIGNATURES = {
    {
      "false",
      "<E extends java.lang.Enum<E>> implements java.lang.Comparable<E>, java.io.Serializable",
      "<E:Ljava/lang/Enum<TE;>;>Ljava/lang/Object;Ljava/lang/Comparable<TE;>;Ljava/io/Serializable;"
    },
    {
      "true",
      "<D extends java.lang.reflect.GenericDeclaration> extends java.lang.reflect.Type",
      "<D::Ljava/lang/reflect/GenericDeclaration;>Ljava/lang/Object;Ljava/lang/reflect/Type;"
    },
    {
      "false",
      "<K, V> extends java.util.AbstractMap<K, V> implements java.util.concurrent.ConcurrentMap<K, V>, java.io.Serializable",
      "<K:Ljava/lang/Object;V:Ljava/lang/Object;>Ljava/util/AbstractMap<TK;TV;>;Ljava/util/concurrent/ConcurrentMap<TK;TV;>;Ljava/io/Serializable;"
    },
    {
      "false",
      "<K extends java.lang.Enum<K>, V> extends java.util.AbstractMap<K, V> implements java.io.Serializable, java.lang.Cloneable",
      "<K:Ljava/lang/Enum<TK;>;V:Ljava/lang/Object;>Ljava/util/AbstractMap<TK;TV;>;Ljava/io/Serializable;Ljava/lang/Cloneable;"
    },
    {"false", "<T, R extends T>", "<T:Ljava/lang/Object;R:TT;>Ljava/lang/Object;"}
  };

  public static final String[][] FIELD_SIGNATURES = {
    {"T[]", "[TT;"},
    {"AA<byte[][]>", "LAA<[[B>;"},
    {"java.lang.Class<?>", "Ljava/lang/Class<*>;"},
    {"java.lang.reflect.Constructor<T>", "Ljava/lang/reflect/Constructor<TT;>;"},
    {"java.util.Hashtable<?, ?>", "Ljava/util/Hashtable<**>;"},
    {
      "java.util.concurrent.atomic.AtomicReferenceFieldUpdater<java.io.BufferedInputStream, byte[]>",
      "Ljava/util/concurrent/atomic/AtomicReferenceFieldUpdater<Ljava/io/BufferedInputStream;[B>;"
    },
    {
      "AA<java.util.Map<java.lang.String, java.lang.String>[][]>",
      "LAA<[[Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;>;"
    },
    {
      "java.util.Hashtable<java.lang.Object, java.lang.String>",
      "Ljava/util/Hashtable<Ljava/lang/Object;Ljava/lang/String;>;"
    }
  };

  public static final String[][] METHOD_SIGNATURES = {
    {"void()E, F", "()V^TE;^TF;"},
    {"void(A<E>.B)", "(LA<TE;>.B;)V"},
    {"void(A<E>.B<F>)", "(LA<TE;>.B<TF;>;)V"},
    {"void(boolean, byte, char, short, int, float, long, double)", "(ZBCSIFJD)V"},
    {
      "java.lang.Class<? extends E><E extends java.lang.Class>()",
      "<E:Ljava/lang/Class;>()Ljava/lang/Class<+TE;>;"
    },
    {
      "java.lang.Class<? super E><E extends java.lang.Class>()",
      "<E:Ljava/lang/Class;>()Ljava/lang/Class<-TE;>;"
    },
    {
      "void(java.lang.String, java.lang.Class<?>, java.lang.reflect.Method[], java.lang.reflect.Method, java.lang.reflect.Method)",
      "(Ljava/lang/String;Ljava/lang/Class<*>;[Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)V"
    },
    {
      "java.util.Map<java.lang.Object, java.lang.String>(java.lang.Object, java.util.Map<java.lang.Object, java.lang.String>)",
      "(Ljava/lang/Object;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;)Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;"
    },
    {
      "java.util.Map<java.lang.Object, java.lang.String><T>(java.lang.Object, java.util.Map<java.lang.Object, java.lang.String>, T)",
      "<T:Ljava/lang/Object;>(Ljava/lang/Object;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;TT;)Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;"
    },
    {
      "java.util.Map<java.lang.Object, java.lang.String><E, T extends java.lang.Comparable<E>>(java.lang.Object, java.util.Map<java.lang.Object, java.lang.String>, T)",
      "<E:Ljava/lang/Object;T::Ljava/lang/Comparable<TE;>;>(Ljava/lang/Object;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;TT;)Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;"
    }
  };

  public static Stream<Arguments> classSignatures() {
    return Arrays.stream(CLASS_SIGNATURES).map(values -> Arguments.of((Object[]) values));
  }

  public static Stream<Arguments> fieldSignatures() {
    return Arrays.stream(FIELD_SIGNATURES).map(values -> Arguments.of((Object[]) values));
  }

  public static Stream<Arguments> methodSignatures() {
    return Arrays.stream(METHOD_SIGNATURES).map(values -> Arguments.of((Object[]) values));
  }

  @Test
  void testVisitBaseType_invalidSignature() {
    TraceSignatureVisitor traceSignatureVisitor = new TraceSignatureVisitor(0);

    Executable visitBaseType = () -> traceSignatureVisitor.visitBaseType('-');

    assertThrows(IllegalArgumentException.class, visitBaseType);
  }

  @ParameterizedTest
  @MethodSource("classSignatures")
  void testVisitMethods_classSignature(
      final boolean isInterface, final String declaration, final String signature) {
    SignatureReader signatureReader = new SignatureReader(signature);
    TraceSignatureVisitor traceSignatureVisitor =
        new TraceSignatureVisitor(isInterface ? Opcodes.ACC_INTERFACE : 0);

    signatureReader.accept(traceSignatureVisitor);

    assertEquals(declaration, traceSignatureVisitor.getDeclaration());
  }

  @ParameterizedTest
  @MethodSource("fieldSignatures")
  void testVisitMethods_fieldSignature(final String declaration, final String signature) {
    SignatureReader signatureReader = new SignatureReader(signature);
    TraceSignatureVisitor traceSignatureVisitor = new TraceSignatureVisitor(0);

    signatureReader.acceptType(traceSignatureVisitor);

    assertEquals(declaration, traceSignatureVisitor.getDeclaration());
  }

  @ParameterizedTest
  @MethodSource("methodSignatures")
  void testVisitMethods_methodSignature(final String declaration, final String signature) {
    SignatureReader signatureReader = new SignatureReader(signature);
    TraceSignatureVisitor traceSignatureVisitor = new TraceSignatureVisitor(0);

    signatureReader.accept(traceSignatureVisitor);

    String fullMethodDeclaration =
        traceSignatureVisitor.getReturnType()
            + traceSignatureVisitor.getDeclaration()
            + (traceSignatureVisitor.getExceptions() != null
                ? traceSignatureVisitor.getExceptions()
                : "");
    assertEquals(declaration, fullMethodDeclaration);
  }
}
