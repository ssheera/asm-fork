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
package org.objectweb.asm;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.test.AsmTest;
import org.objectweb.asm.test.ClassFile;

/**
 * Unit tests for {@link ClassWriter}.
 *
 * @author Eric Bruneton
 */
class ClassWriterTest extends AsmTest {

  /**
   * Tests that the non-static fields of ClassWriter are the expected ones. This test is designed to
   * fail each time new fields are added to ClassWriter, and serves as a reminder to update the
   * field reset logic in {@link ClassWriter#replaceAsmInstructions(byte[], boolean)}, if needed,
   * each time a new field is added.
   */
  @Test
  void testInstanceFields() {
    Set<String> actualFields =
        Arrays.stream(ClassWriter.class.getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .map(Field::getName)
            .collect(toSet());

    Set<String> expectedFields =
        Set.of(
            "flags",
            "version",
            "symbolTable",
            "accessFlags",
            "thisClass",
            "superClass",
            "interfaceCount",
            "interfaces",
            "firstField",
            "lastField",
            "firstMethod",
            "lastMethod",
            "numberOfInnerClasses",
            "innerClasses",
            "enclosingClassIndex",
            "enclosingMethodIndex",
            "signatureIndex",
            "sourceFileIndex",
            "debugExtension",
            "lastRuntimeVisibleAnnotation",
            "lastRuntimeInvisibleAnnotation",
            "lastRuntimeVisibleTypeAnnotation",
            "lastRuntimeInvisibleTypeAnnotation",
            "moduleWriter",
            "nestHostClassIndex",
            "numberOfNestMemberClasses",
            "nestMemberClasses",
            "numberOfPermittedSubclasses",
            "permittedSubclasses",
            "firstRecordComponent",
            "lastRecordComponent",
            "firstAttribute",
            "compute");
    // IMPORTANT: if this fails, update the string list AND update the logic that resets the
    // ClassWriter fields in ClassWriter.toByteArray(), if needed (this logic is used to do a
    // ClassReader->ClassWriter round trip to remove the ASM specific instructions due to large
    // forward jumps).
    assertEquals(expectedFields, actualFields);
  }

  /**
   * Checks that all the ClassVisitor methods are overridden by ClassWriter and are final.
   * ClassWriter does not take an api version as constructor argument. Therefore, backward
   * compatibility of user subclasses overriding some visit methods of ClassWriter would not be
   * possible to ensure. To prevent this, the ClassWriter visit methods must be final.
   */
  @Test
  void testVisitMethods_final() {
    ArrayList<Method> publicClassVisitorMethods = new ArrayList<>();
    for (Method classVisitorMethod : ClassVisitor.class.getDeclaredMethods()) {
      int modifiers = classVisitorMethod.getModifiers();
      if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
        publicClassVisitorMethods.add(classVisitorMethod);
      }
    }

    for (Method classVisitorMethod : publicClassVisitorMethods) {
      try {
        Method classWriterMethod =
            ClassWriter.class.getMethod(
                classVisitorMethod.getName(), classVisitorMethod.getParameterTypes());
        if (!classWriterMethod.getName().equals("getDelegate")) {
          assertTrue(
              Modifier.isFinal(classWriterMethod.getModifiers()), classWriterMethod + " is final");
        }
      } catch (NoSuchMethodException e) {
        fail("ClassWriter must override " + classVisitorMethod);
      }
    }
  }

  @Test
  void testNewConst() {
    ClassWriter classWriter = newEmptyClassWriter();

    classWriter.newConst(Boolean.FALSE);
    classWriter.newConst(Byte.valueOf((byte) 1));
    classWriter.newConst(Character.valueOf('2'));
    classWriter.newConst(Short.valueOf((short) 3));

    String constantPoolDump = getConstantPoolDump(classWriter);
    assertTrue(constantPoolDump.contains("constant_pool: 0"));
    assertTrue(constantPoolDump.contains("constant_pool: 1"));
    assertTrue(constantPoolDump.contains("constant_pool: 50"));
    assertTrue(constantPoolDump.contains("constant_pool: 3"));
  }

  @Test
  void testNewConst_illegalArgument() {
    ClassWriter classWriter = newEmptyClassWriter();

    Executable newConst = () -> classWriter.newConst(new Object());

    Exception exception = assertThrows(IllegalArgumentException.class, newConst);
    assertTrue(exception.getMessage().matches("value java\\.lang\\.Object@.*"));
  }

  @Test
  void testNewUtf8() {
    ClassWriter classWriter = newEmptyClassWriter();

    classWriter.newUTF8("A");

    assertTrue(getConstantPoolDump(classWriter).contains("constant_pool: A"));
  }

  @Test
  void testNewClass() {
    ClassWriter classWriter = newEmptyClassWriter();

    classWriter.newClass("A");

    assertTrue(getConstantPoolDump(classWriter).contains("constant_pool: ConstantClassInfo A"));
  }

  @Test
  void testNewMethodType() {
    ClassWriter classWriter = newEmptyClassWriter();

    classWriter.newMethodType("()V");

    assertTrue(
        getConstantPoolDump(classWriter).contains("constant_pool: ConstantMethodTypeInfo ()V"));
  }

  @Test
  void testNewModule() {
    ClassWriter classWriter = newEmptyClassWriter();

    classWriter.newModule("A");

    assertTrue(getConstantPoolDump(classWriter).contains("constant_pool: ConstantModuleInfo A"));
  }

  @Test
  void testNewPackage() {
    ClassWriter classWriter = newEmptyClassWriter();

    classWriter.newPackage("A");

    assertTrue(getConstantPoolDump(classWriter).contains("constant_pool: ConstantPackageInfo A"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void testDeprecatedNewHandle() {
    ClassWriter classWriter = newEmptyClassWriter();

    classWriter.newHandle(Opcodes.H_GETFIELD, "A", "h", "I");

    assertTrue(
        getConstantPoolDump(classWriter)
            .contains("constant_pool: ConstantMethodHandleInfo 1.ConstantFieldRefInfo A.hI"));
  }

  @Test
  void testNewHandle() {
    ClassWriter classWriter = newEmptyClassWriter();

    classWriter.newHandle(Opcodes.H_GETFIELD, "A", "h", "I", false);

    assertTrue(
        getConstantPoolDump(classWriter)
            .contains("constant_pool: ConstantMethodHandleInfo 1.ConstantFieldRefInfo A.hI"));
  }

  @Test
  void testNewHandleIsInterface() {
    ClassWriter classWriter = newEmptyClassWriter();

    int index1 = classWriter.newHandle(Opcodes.H_INVOKEVIRTUAL, "A", "m", "()V", false);
    int index2 = classWriter.newHandle(Opcodes.H_INVOKEVIRTUAL, "A", "m", "()V", true);

    assertNotEquals(index1, index2);
  }

  @Test
  void testNewConstantDynamic() {
    ClassWriter classWriter = newEmptyClassWriter();

    classWriter.newConstantDynamic(
        "m", "Ljava/lang/String;", new Handle(Opcodes.H_INVOKESTATIC, "A", "m", "()V", false));

    String constantPoolDump = getConstantPoolDump(classWriter);
    assertTrue(
        constantPoolDump.contains("constant_pool: ConstantDynamicInfo 0.mLjava/lang/String;"));
    assertTrue(
        constantPoolDump.contains(
            "constant_pool: ConstantMethodHandleInfo 6.ConstantMethodRefInfo A.m()V"));
    assertTrue(constantPoolDump.contains("constant_pool: BootstrapMethods"));
  }

  @Test
  void testNewInvokeDynamic() {
    ClassWriter classWriter = newEmptyClassWriter();

    classWriter.newInvokeDynamic("m", "()V", new Handle(Opcodes.H_GETFIELD, "A", "h", "I", false));

    String constantPoolDump = getConstantPoolDump(classWriter);
    assertTrue(constantPoolDump.contains("ConstantInvokeDynamicInfo 0.m()V"));
    assertTrue(
        constantPoolDump.contains(
            "constant_pool: ConstantMethodHandleInfo 1.ConstantFieldRefInfo A.hI"));
    assertTrue(constantPoolDump.contains("constant_pool: BootstrapMethods"));
  }

  @Test
  void testNewField() {
    ClassWriter classWriter = newEmptyClassWriter();

    classWriter.newField("A", "f", "I");

    assertTrue(
        getConstantPoolDump(classWriter).contains("constant_pool: ConstantFieldRefInfo A.fI"));
  }

  @Test
  void testNewMethod() {
    ClassWriter classWriter = newEmptyClassWriter();

    classWriter.newMethod("A", "m", "()V", false);

    assertTrue(
        getConstantPoolDump(classWriter).contains("constant_pool: ConstantMethodRefInfo A.m()V"));
  }

  @Test
  void testNewNameType() {
    ClassWriter classWriter = newEmptyClassWriter();

    classWriter.newNameType("m", "()V");

    assertTrue(
        getConstantPoolDump(classWriter).contains("constant_pool: ConstantNameAndTypeInfo m()V"));
  }

  @ParameterizedTest
  @ValueSource(ints = {65535, 65536})
  void testToByteArray_constantPoolSizeTooLarge(final int constantPoolCount) {
    ClassWriter classWriter = newEmptyClassWriter();
    int initConstantPoolCount = 5;
    for (int i = 0; i < constantPoolCount - initConstantPoolCount; ++i) {
      classWriter.newConst(Integer.valueOf(i));
    }

    Executable toByteArray = () -> classWriter.toByteArray();

    if (constantPoolCount > 65535) {
      ClassTooLargeException exception = assertThrows(ClassTooLargeException.class, toByteArray);
      assertEquals("C", exception.getClassName());
      assertEquals(constantPoolCount, exception.getConstantPoolCount());
      assertEquals("Class too large: C", exception.getMessage());
    } else {
      assertDoesNotThrow(toByteArray);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {65535, 65536})
  void testToByteArray_methodCodeSizeTooLarge(final int methodCodeSize) {
    ClassWriter classWriter = newEmptyClassWriter();
    String methodName = "m";
    String descriptor = "()V";
    MethodVisitor methodVisitor =
        classWriter.visitMethod(Opcodes.ACC_STATIC, methodName, descriptor, null, null);
    methodVisitor.visitCode();
    for (int i = 0; i < methodCodeSize - 1; ++i) {
      methodVisitor.visitInsn(Opcodes.NOP);
    }
    methodVisitor.visitInsn(Opcodes.RETURN);
    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();

    Executable toByteArray = () -> classWriter.toByteArray();

    if (methodCodeSize > 65535) {
      MethodTooLargeException exception = assertThrows(MethodTooLargeException.class, toByteArray);
      assertEquals(methodName, exception.getMethodName());
      assertEquals("C", exception.getClassName());
      assertEquals(descriptor, exception.getDescriptor());
      assertEquals(methodCodeSize, exception.getCodeSize());
      assertEquals("Method too large: C.m ()V", exception.getMessage());
    } else {
      assertDoesNotThrow(toByteArray);
    }
  }

  @Test
  void testToByteArray_largeSourceDebugExtension() {
    ClassWriter classWriter = newEmptyClassWriter();
    classWriter.visitSource("Test.java", new String(new char[100000]));

    classWriter.toByteArray();

    assertTrue(getDump(classWriter).contains("attribute_name_index: SourceDebugExtension"));
  }

  /**
   * Tests that the COMPUTE_MAXS option works correctly on classes with very large or deeply nested
   * subroutines (#307600, #311642).
   *
   * @throws IOException if the input class file can't be read.
   */
  @ParameterizedTest
  @ValueSource(strings = {"Issue307600.class", "Issue311642.class"})
  void testToByteArray_computeMaxs_largeSubroutines(final String classFileName) throws IOException {
    ClassReader classReader =
        new ClassReader(Files.newInputStream(Paths.get("src/test/resources/" + classFileName)));
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    classReader.accept(classWriter, attributes(), 0);

    Executable toByteArray = () -> classWriter.toByteArray();

    assertDoesNotThrow(toByteArray);
  }

  @Test
  void testToByteArray_computeFrames_mergeLongOrDouble() {
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    classWriter.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, "A", null, "java/lang/Object", null);
    // Generate a default constructor, so that we can instantiate the class.
    MethodVisitor methodVisitor =
        classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
    methodVisitor.visitCode();
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
    methodVisitor.visitMethodInsn(
        Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    methodVisitor.visitInsn(Opcodes.RETURN);
    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();
    // A method with a long local variable using slots 0 and 1, with an int stored in slot 1 in a
    // branch. At the end of the method, the stack map frame should contain 'TOP' for slot 0,
    // otherwise the class instantiation fails with a verification error.
    methodVisitor = classWriter.visitMethod(Opcodes.ACC_STATIC, "m", "(J)V", null, null);
    methodVisitor.visitCode();
    methodVisitor.visitInsn(Opcodes.ICONST_0);
    Label label = new Label();
    methodVisitor.visitJumpInsn(Opcodes.IFNE, label);
    methodVisitor.visitInsn(Opcodes.ICONST_0);
    methodVisitor.visitVarInsn(Opcodes.ISTORE, 1);
    methodVisitor.visitLabel(label);
    methodVisitor.visitInsn(Opcodes.RETURN);
    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();
    classWriter.visitEnd();

    byte[] classFile = classWriter.toByteArray();

    assertDoesNotThrow(() -> new ClassFile(classFile).newInstance());
  }

  @Test
  void testToByteArray_computeFrames_highDimensionArrays() {
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    classWriter.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, "A", null, "java/lang/Object", null);
    MethodVisitor methodVisitor =
        classWriter.visitMethod(
            Opcodes.ACC_STATIC,
            "m",
            "(I[[[[[[[[Ljava/lang/Integer;[[[[[[[[Ljava/lang/Long;)Ljava/lang/Object;",
            null,
            null);
    methodVisitor.visitCode();
    methodVisitor.visitVarInsn(Opcodes.ILOAD, 0);
    Label thenLabel = new Label();
    Label endIfLabel = new Label();
    methodVisitor.visitJumpInsn(Opcodes.IFNE, thenLabel);
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
    methodVisitor.visitJumpInsn(Opcodes.GOTO, endIfLabel);
    methodVisitor.visitLabel(thenLabel);
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
    // At this point the stack can contain either an 8 dimensions Integer array or an 8 dimensions
    // Long array. The merged type computed with the COMPUTE_FRAMES option should therefore be an
    // 8 dimensions Number array.
    methodVisitor.visitLabel(endIfLabel);
    methodVisitor.visitInsn(Opcodes.ARETURN);
    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();
    classWriter.visitEnd();

    byte[] classFile = classWriter.toByteArray();

    // Check that the merged frame type is correctly computed.
    assertTrue(new ClassFile(classFile).toString().contains("[[[[[[[[Ljava/lang/Number;"));
  }

  @Test
  void testToByteArray_manyFramesWithForwardLabelReferences() {
    ClassWriter classWriter = new ClassWriter(0);
    classWriter.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, "A", null, "java/lang/Object", null);
    MethodVisitor constructor =
        classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
    constructor.visitCode();
    constructor.visitVarInsn(Opcodes.ALOAD, 0);
    constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    constructor.visitInsn(Opcodes.RETURN);
    constructor.visitMaxs(1, 1);
    constructor.visitEnd();
    MethodVisitor methodVisitor =
        classWriter.visitMethod(Opcodes.ACC_STATIC, "m", "()V", null, null);
    methodVisitor.visitCode();
    Label label0 = new Label();
    methodVisitor.visitJumpInsn(Opcodes.GOTO, label0);
    Label label1 = new Label();
    methodVisitor.visitLabel(label1);
    Label[] newLabels = new Label[24];
    for (int i = 0; i < newLabels.length; ++i) {
      newLabels[i] = new Label();
    }
    methodVisitor.visitFrame(Opcodes.F_NEW, newLabels.length, newLabels, 0, null);
    for (int i = 0; i < newLabels.length; ++i) {
      methodVisitor.visitVarInsn(Opcodes.ALOAD, i);
      methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "A", "<init>", "()V", false);
    }
    Label label2 = new Label();
    methodVisitor.visitJumpInsn(Opcodes.GOTO, label2);
    methodVisitor.visitLabel(label0);
    Object[] topTypes = new Object[newLabels.length];
    for (int i = 0; i < topTypes.length; ++i) {
      topTypes[i] = Opcodes.TOP;
    }
    methodVisitor.visitFrame(Opcodes.F_NEW, topTypes.length, topTypes, 0, null);
    for (int i = 0; i < newLabels.length; ++i) {
      methodVisitor.visitLabel(newLabels[i]);
      methodVisitor.visitTypeInsn(Opcodes.NEW, "A");
      methodVisitor.visitVarInsn(Opcodes.ASTORE, i);
    }
    methodVisitor.visitJumpInsn(Opcodes.GOTO, label1);
    methodVisitor.visitLabel(label2);
    String[] newTypes = new String[newLabels.length];
    for (int i = 0; i < newTypes.length; ++i) {
      newTypes[i] = "A";
    }
    methodVisitor.visitFrame(Opcodes.F_NEW, newTypes.length, newTypes, 0, null);
    methodVisitor.visitInsn(Opcodes.RETURN);
    methodVisitor.visitMaxs(1, newLabels.length);
    methodVisitor.visitEnd();
    classWriter.visitEnd();

    byte[] classFile = classWriter.toByteArray();

    assertDoesNotThrow(() -> new ClassFile(classFile).newInstance());
  }

  @Test
  void testGetCommonSuperClass() {
    ClassWriter classWriter = new ClassWriter(0);

    assertEquals(
        "java/lang/Object",
        classWriter.getCommonSuperClass("java/lang/Object", "java/lang/Integer"));
    assertEquals(
        "java/lang/Object",
        classWriter.getCommonSuperClass("java/lang/Integer", "java/lang/Object"));
    assertEquals(
        "java/lang/Object",
        classWriter.getCommonSuperClass("java/lang/Integer", "java/lang/Runnable"));
    assertEquals(
        "java/lang/Object",
        classWriter.getCommonSuperClass("java/lang/Runnable", "java/lang/Integer"));
    assertEquals(
        "java/lang/Throwable",
        classWriter.getCommonSuperClass(
            "java/lang/IndexOutOfBoundsException", "java/lang/AssertionError"));
    Exception exception =
        assertThrows(
            TypeNotPresentException.class,
            () -> classWriter.getCommonSuperClass("-", "java/lang/Object"));
    assertEquals("Type - not present", exception.getMessage());
    exception =
        assertThrows(
            TypeNotPresentException.class,
            () -> classWriter.getCommonSuperClass("java/lang/Object", "-"));
    assertEquals("Type - not present", exception.getMessage());
  }

  /** Tests that a ClassReader -> ClassWriter transform leaves classes unchanged. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testReadAndWrite(final PrecompiledClass classParameter, final Api apiParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);

    classReader.accept(classWriter, attributes(), 0);

    assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
  }

  /**
   * Tests that a ClassReader -> ClassWriter transform with the SKIP_CODE option produces a valid
   * class.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testReadAndWrite_skipCode(final PrecompiledClass classParameter, final Api apiParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);

    classReader.accept(classWriter, attributes(), ClassReader.SKIP_CODE);

    assertFalse(classWriter.hasFlags(ClassWriter.COMPUTE_MAXS));
    assertFalse(classWriter.hasFlags(ClassWriter.COMPUTE_FRAMES));
    assertTrue(
        new ClassFile(classWriter.toByteArray())
            .toString()
            .contains(classParameter.getInternalName()));
  }

  /**
   * Tests that a ClassReader -> ClassWriter transform with the copy pool option leaves classes
   * unchanged.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testReadAndWrite_copyPool(final PrecompiledClass classParameter, final Api apiParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(classReader, 0);

    classReader.accept(classWriter, attributes(), 0);

    assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
  }

  /**
   * Tests that a ClassReader -> ClassWriter transform with the EXPAND_FRAMES option leaves classes
   * unchanged.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testReadAndWrite_expandFrames(
      final PrecompiledClass classParameter, final Api apiParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);

    classReader.accept(classWriter, attributes(), ClassReader.EXPAND_FRAMES);

    assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
  }

  /**
   * Tests that a ClassReader -> ClassWriter transform with the COMPUTE_MAXS option leaves classes
   * unchanged. This is not true in general (the valid max stack and max locals for a given method
   * are not unique), but this should be the case with our precompiled classes (except
   * jdk3.SubOptimalMaxStackAndLocals, which has non optimal max values on purpose).
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testReadAndWrite_computeMaxs(final PrecompiledClass classParameter, final Api apiParameter) {
    assumeTrue(classParameter != PrecompiledClass.JDK3_SUB_OPTIMAL_MAX_STACK_AND_LOCALS);
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

    classReader.accept(classWriter, attributes(), 0);

    assertTrue(classWriter.hasFlags(ClassWriter.COMPUTE_MAXS));
    assertFalse(classWriter.hasFlags(ClassWriter.COMPUTE_FRAMES));
    assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
  }

  /**
   * Tests that classes going through a ClassReader -> ClassWriter transform with the COMPUTE_MAXS
   * option can be loaded and pass bytecode verification.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testReadAndWrite_computeMaxs_newInstance(
      final PrecompiledClass classParameter, final Api apiParameter) throws Exception {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    classReader.accept(classWriter, attributes(), 0);

    Executable newInstance = () -> new ClassFile(classWriter.toByteArray()).newInstance();

    if (classParameter.isNotCompatibleWithCurrentJdk()) {
      assertThrows(UnsupportedClassVersionError.class, newInstance);
    } else {
      assertDoesNotThrow(newInstance);
    }
  }

  /**
   * Tests that classes going through a ClassReader -> ClassWriter transform with the COMPUTE_FRAMES
   * option can be loaded and pass bytecode verification.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testReadAndWrite_computeFrames(
      final PrecompiledClass classParameter, final Api apiParameter) {
    assumeFalse(hasJsrOrRetInstructions(classParameter));
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    classReader.accept(classWriter, attributes(), 0);

    byte[] newClassFile = classWriter.toByteArray();

    assertFalse(classWriter.hasFlags(ClassWriter.COMPUTE_MAXS));
    assertTrue(classWriter.hasFlags(ClassWriter.COMPUTE_FRAMES));
    // The computed stack map frames should be equal to the original ones, if any (classes before
    // JDK8 don't have ones). This is not true in general (the valid frames for a given method are
    // not unique), but this should be the case with our precompiled classes (except
    // jdk3.SubOptimalMaxStackAndLocals, which has non optimal max values on purpose).
    if (classParameter.isMoreRecentThan(Api.ASM4)
        && classParameter != PrecompiledClass.JDK3_SUB_OPTIMAL_MAX_STACK_AND_LOCALS) {
      assertEquals(new ClassFile(classFile), new ClassFile(newClassFile));
    }
    Executable newInstance = () -> new ClassFile(newClassFile).newInstance();
    if (classParameter.isNotCompatibleWithCurrentJdk()) {
      assertThrows(UnsupportedClassVersionError.class, newInstance);
    } else {
      assertDoesNotThrow(newInstance);
    }
  }

  /**
   * Tests that classes going through a ClassReader -> ClassWriter transform with the COMPUTE_FRAMES
   * option can be loaded and pass bytecode verification.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testReadAndWrite_computeFrames_jsrInstructions(
      final PrecompiledClass classParameter, final Api apiParameter) {
    assumeTrue(hasJsrOrRetInstructions(classParameter));
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

    Executable accept = () -> classReader.accept(classWriter, attributes(), 0);

    Exception exception = assertThrows(IllegalArgumentException.class, accept);
    assertEquals("JSR/RET are not supported with computeFrames option", exception.getMessage());
  }

  /**
   * Tests that classes going through a ClassReader -> ClassWriter transform with the SKIP_FRAMES
   * and COMPUTE_FRAMES options can be loaded and pass bytecode verification.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testReadAndWrite_skipAndComputeFrames(
      final PrecompiledClass classParameter, final Api apiParameter) {
    assumeFalse(hasJsrOrRetInstructions(classParameter));
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    classReader.accept(classWriter, attributes(), ClassReader.SKIP_FRAMES);

    byte[] newClassFile = classWriter.toByteArray();

    // The computed stack map frames should be equal to the original ones, if any (classes before
    // JDK8 don't have ones). This is not true in general (the valid frames for a given method are
    // not unique), but this should be the case with our precompiled classes (except
    // jdk3.SubOptimalMaxStackAndLocals, which has non optimal max values on purpose).
    if (classParameter.isMoreRecentThan(Api.ASM4)
        && classParameter != PrecompiledClass.JDK3_SUB_OPTIMAL_MAX_STACK_AND_LOCALS) {
      assertEquals(new ClassFile(classFile), new ClassFile(newClassFile));
    }
    Executable newInstance = () -> new ClassFile(newClassFile).newInstance();
    if (classParameter.isNotCompatibleWithCurrentJdk()) {
      assertThrows(UnsupportedClassVersionError.class, newInstance);
    } else {
      assertDoesNotThrow(newInstance);
    }
  }

  /**
   * Tests that classes with dead code going through a ClassWriter with the COMPUTE_FRAMES option
   * can be loaded and pass bytecode verification.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testReadAndWrite_computeFramesAndDeadCode(
      final PrecompiledClass classParameter, final Api apiParameter) {
    assumeFalse(
        hasJsrOrRetInstructions(classParameter) || classParameter.isMoreRecentThan(apiParameter));
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    ClassVisitor classVisitor = new DeadCodeInserter(apiParameter.value(), classWriter);
    classReader.accept(classVisitor, attributes(), ClassReader.SKIP_FRAMES);

    byte[] newClassFile = classWriter.toByteArray();

    Executable newInstance = () -> new ClassFile(newClassFile).newInstance();
    if (classParameter.isNotCompatibleWithCurrentJdk()) {
      assertThrows(UnsupportedClassVersionError.class, newInstance);
    } else {
      assertDoesNotThrow(newInstance);
    }
  }

  /**
   * Tests that classes with large methods (more than 32k) going through a ClassWriter with no
   * option can be loaded and pass bytecode verification. Also tests that frames are not recomputed
   * from stratch during this process (by making sure that {@link ClassWriter#getCommonSuperClass}
   * is not called).
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testReadAndWrite_largeMethod(final PrecompiledClass classParameter, final Api apiParameter) {
    byte[] classFile = classParameter.getBytes();
    assumeFalse(
        classFile.length > Short.MAX_VALUE || classParameter.isMoreRecentThan(apiParameter));
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriterWithoutGetCommonSuperClass();
    ForwardJumpNopInserter forwardJumpNopInserter =
        new ForwardJumpNopInserter(apiParameter.value(), classWriter);
    classReader.accept(forwardJumpNopInserter, attributes(), 0);
    if (!forwardJumpNopInserter.transformed) {
      classWriter = new ClassWriterWithoutGetCommonSuperClass();
      classReader.accept(
          new WideForwardJumpInserter(apiParameter.value(), classWriter), attributes(), 0);
    }

    byte[] transformedClass = classWriter.toByteArray();

    Executable newInstance = () -> new ClassFile(transformedClass).newInstance();
    if (classParameter.isNotCompatibleWithCurrentJdk()) {
      assertThrows(UnsupportedClassVersionError.class, newInstance);
    } else {
      assertDoesNotThrow(newInstance);
    }
    // The transformed class should have the same structure as the original one (#317792).
    ClassWriter originalClassWithoutCode = new ClassWriter(0);
    classReader.accept(originalClassWithoutCode, ClassReader.SKIP_CODE);
    ClassWriter transformedClassWithoutCode = new ClassWriter(0);
    new ClassReader(transformedClass).accept(transformedClassWithoutCode, ClassReader.SKIP_CODE);
    assertEquals(
        new ClassFile(originalClassWithoutCode.toByteArray()),
        new ClassFile(transformedClassWithoutCode.toByteArray()));
  }

  public static boolean hasJsrOrRetInstructions(final PrecompiledClass classParameter) {
    return classParameter == PrecompiledClass.JDK3_ALL_INSTRUCTIONS
        || classParameter == PrecompiledClass.JDK3_LARGE_METHOD;
  }

  public static ClassWriter newEmptyClassWriter() {
    ClassWriter classWriter = new ClassWriter(0);
    classWriter.visit(Opcodes.V1_1, Opcodes.ACC_PUBLIC, "C", null, "java/lang/Object", null);
    return classWriter;
  }

  public static String getConstantPoolDump(final ClassWriter classWriter) {
    return new ClassFile(classWriter.toByteArray()).getConstantPoolDump();
  }

  public static String getDump(final ClassWriter classWriter) {
    return new ClassFile(classWriter.toByteArray()).toString();
  }

  public static Attribute[] attributes() {
    return new Attribute[] {new Comment(), new CodeComment()};
  }

  public static class DeadCodeInserter extends ClassVisitor {

    public String className;

    DeadCodeInserter(final int api, final ClassVisitor classVisitor) {
      super(api, classVisitor);
    }

    @Override
    public void visit(
        final int version,
        final int access,
        final String name,
        final String signature,
        final String superName,
        final String[] interfaces) {
      className = name;
      // Set V1_7 version to prevent fallback to old verifier.
      super.visit(
          (version & 0xFFFF) < Opcodes.V1_7 ? Opcodes.V1_7 : version,
          access,
          name,
          signature,
          superName,
          interfaces);
    }

    @Override
    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String descriptor,
        final String signature,
        final String[] exceptions) {
      int seed = (className + "." + name + descriptor).hashCode();
      return new MethodDeadCodeInserter(
          api, seed, super.visitMethod(access, name, descriptor, signature, exceptions));
    }
  }

  public static class MethodDeadCodeInserter extends MethodVisitor implements Opcodes {

    public Random random;
    public boolean inserted;

    MethodDeadCodeInserter(final int api, final int seed, final MethodVisitor methodVisitor) {
      super(api, methodVisitor);
      random = new Random(seed);
    }

    @Override
    public void visitInsn(final int opcode) {
      super.visitInsn(opcode);
      maybeInsertDeadCode();
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
      super.visitIntInsn(opcode, operand);
      maybeInsertDeadCode();
    }

    @Override
    public void visitVarInsn(final int opcode, final int varIndex) {
      super.visitVarInsn(opcode, varIndex);
      maybeInsertDeadCode();
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
      super.visitTypeInsn(opcode, type);
      maybeInsertDeadCode();
    }

    @Override
    public void visitFieldInsn(
        final int opcode, final String owner, final String name, final String descriptor) {
      super.visitFieldInsn(opcode, owner, name, descriptor);
      maybeInsertDeadCode();
    }

    @Override
    public void visitMethodInsn(
        final int opcode,
        final String owner,
        final String name,
        final String descriptor,
        final boolean isInterface) {
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
      maybeInsertDeadCode();
    }

    @Override
    public void visitInvokeDynamicInsn(
        final String name,
        final String descriptor,
        final Handle bootstrapMethodHandle,
        final Object... bootstrapMethodArguments) {
      super.visitInvokeDynamicInsn(
          name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
      maybeInsertDeadCode();
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
      super.visitJumpInsn(opcode, label);
      maybeInsertDeadCode();
    }

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
        maybeInsertDeadCode();
      } else {
        // If this happens, add support for the new type in
        // MethodWriter.visitLdcInsn(), if needed.
        throw new IllegalArgumentException("Unsupported type of value: " + value);
      }
    }

    @Override
    public void visitIincInsn(final int varIndex, final int increment) {
      super.visitIincInsn(varIndex, increment);
      maybeInsertDeadCode();
    }

    @Override
    public void visitTableSwitchInsn(
        final int min, final int max, final Label dflt, final Label... labels) {
      super.visitTableSwitchInsn(min, max, dflt, labels);
      maybeInsertDeadCode();
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
      super.visitLookupSwitchInsn(dflt, keys, labels);
      maybeInsertDeadCode();
    }

    @Override
    public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
      super.visitMultiANewArrayInsn(descriptor, numDimensions);
      maybeInsertDeadCode();
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
      if (!inserted) {
        insertDeadCode();
      }
      super.visitMaxs(maxStack, maxLocals);
    }

    public void maybeInsertDeadCode() {
      // Inserts dead code once every 50 instructions in average.
      if (!inserted && random.nextFloat() < 1.0 / 50.0) {
        insertDeadCode();
      }
    }

    public void insertDeadCode() {
      Label end = new Label();
      visitJumpInsn(Opcodes.GOTO, end);
      visitLdcInsn("DEAD CODE");
      visitLabel(end);
      inserted = true;
    }
  }

  /** Inserts NOP instructions after the first forward jump found, to get a wide jump. */
  public static class ForwardJumpNopInserter extends ClassVisitor {

    boolean transformed;

    ForwardJumpNopInserter(final int api, final ClassVisitor classVisitor) {
      super(api, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String descriptor,
        final String signature,
        final String[] exceptions) {
      return new MethodVisitor(
          api, super.visitMethod(access, name, descriptor, signature, exceptions)) {
        public final HashSet<Label> labels = new HashSet<>();

        @Override
        public void visitLabel(final Label label) {
          super.visitLabel(label);
          labels.add(label);
        }

        @Override
        public void visitJumpInsn(final int opcode, final Label label) {
          if (!transformed && labels.contains(label)) {
            transformed = true;
            for (int i = 0; i <= Short.MAX_VALUE; ++i) {
              visitInsn(Opcodes.NOP);
            }
          }
          super.visitJumpInsn(opcode, label);
        }
      };
    }
  }

  /** Inserts a wide forward jump in the first non-abstract method that is found. */
  public static class WideForwardJumpInserter extends ClassVisitor {

    public boolean needFrames;
    public boolean transformed;

    WideForwardJumpInserter(final int api, final ClassVisitor classVisitor) {
      super(api, classVisitor);
    }

    @Override
    public void visit(
        final int version,
        final int access,
        final String name,
        final String signature,
        final String superName,
        final String[] interfaces) {
      needFrames = (version & 0xFFFF) >= Opcodes.V1_7;
      super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String descriptor,
        final String signature,
        final String[] exceptions) {
      return new MethodVisitor(
          api, super.visitMethod(access, name, descriptor, signature, exceptions)) {

        @Override
        public void visitCode() {
          super.visitCode();
          if (!transformed) {
            Label startLabel = new Label();
            visitJumpInsn(Opcodes.GOTO, startLabel);
            if (needFrames) {
              visitLabel(new Label());
              visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
            for (int i = 0; i <= Short.MAX_VALUE; ++i) {
              visitInsn(Opcodes.NOP);
            }
            visitLabel(startLabel);
            if (needFrames) {
              visitFrame(Opcodes.F_SAME, 0, null, 0, null);
              visitInsn(Opcodes.NOP);
            }
            transformed = true;
          }
        }
      };
    }
  }

  /**
   * A ClassWriter whose {@link ClassWriter#getCommonSuperClass} method always throws an exception.
   */
  public static class ClassWriterWithoutGetCommonSuperClass extends ClassWriter {

    public ClassWriterWithoutGetCommonSuperClass() {
      super(0);
    }

    @Override
    public String getCommonSuperClass(final String type1, final String type2) {
      throw new UnsupportedOperationException();
    }
  }
}
