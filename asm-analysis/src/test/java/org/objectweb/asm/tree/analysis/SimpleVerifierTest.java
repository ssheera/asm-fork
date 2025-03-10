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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Unit tests for {@link SimpleVerifier}.
 *
 * @author Eric Bruneton
 */
class SimpleVerifierTest {

  @Test
  void testConstructor() {
    assertDoesNotThrow(() -> new SimpleVerifier());
    assertThrows(IllegalStateException.class, () -> new SimpleVerifier() {});
  }

  @ParameterizedTest
  @CsvSource({
    "java/lang/String, java/lang/Number, java/lang/Object",
    "java/lang/Integer, java/lang/Number, java/lang/Number",
    "java/lang/Float, java/lang/Integer, java/lang/Number",
    "java/lang/Long, java/util/List, java/lang/Object",
    "java/util/Map, java/util/List, java/lang/Object"
  })
  void testMerge_objectTypes(
      final String internalName1, final String internalName2, final String expectedInternalName) {
    BasicValue value1 = new BasicValue(Type.getObjectType(internalName1));
    BasicValue value2 = new BasicValue(Type.getObjectType(internalName2));
    SimpleVerifier verifier = new SimpleVerifier();

    BasicValue merge1 = verifier.merge(value1, value2);
    BasicValue merge2 = verifier.merge(value2, value1);

    BasicValue expectedValue = new BasicValue(Type.getObjectType(expectedInternalName));
    assertEquals(expectedValue, merge1);
    assertEquals(expectedValue, merge2);
  }

  @Test
  void testIsAssignableFrom_subclassWithInterfaces() {
    Type baseType = Type.getObjectType("C");
    Type superType = Type.getObjectType("D");
    Type interfaceType = Type.getObjectType("I");
    SimpleVerifier simpleVerifier =
        new SimpleVerifier(
            /* latest */ Opcodes.ASM10_EXPERIMENTAL,
            baseType,
            superType,
            List.of(interfaceType),
            false) {

          @Override
          public boolean isAssignableFrom(final Type type1, final Type type2) {
            return super.isAssignableFrom(type1, type2);
          }

          @Override
          public Class<?> getClass(final Type type) {
            // Return dummy classes, to make sure isAssignable in test() does not rely on them.
            if (type == baseType) {
              return int.class;
            }
            if (type == superType) {
              return float.class;
            }
            if (type == interfaceType) {
              return double.class;
            }
            return super.getClass(type);
          }
        };

    assertTrue(simpleVerifier.isAssignableFrom(baseType, baseType));
    assertTrue(simpleVerifier.isAssignableFrom(superType, baseType));
    assertTrue(simpleVerifier.isAssignableFrom(interfaceType, baseType));
  }

  @Test
  void testIsAssignableFrom_interface() {
    Type baseType = Type.getObjectType("C");
    Type interfaceType = Type.getObjectType("I");
    SimpleVerifier simpleVerifier =
        new SimpleVerifier(
            /* latest */ Opcodes.ASM10_EXPERIMENTAL, interfaceType, null, null, true) {

          @Override
          public Type getSuperClass(final Type type) {
            return Type.getObjectType("java/lang/Object");
          }
        };

    assertTrue(simpleVerifier.isAssignableFrom(interfaceType, baseType));
    assertTrue(simpleVerifier.isAssignableFrom(interfaceType, Type.getObjectType("[I")));
    assertFalse(simpleVerifier.isAssignableFrom(interfaceType, Type.INT_TYPE));
  }
}
