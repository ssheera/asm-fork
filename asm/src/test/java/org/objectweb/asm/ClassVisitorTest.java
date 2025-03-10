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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.test.AsmTest;
import org.objectweb.asm.test.ClassFile;

/**
 * Unit tests for {@link ClassVisitor}. Also tests {@link FieldVisitor}, {@ink MethodVisitor},
 * {@link ModuleVisitor} and {@link AnnotationVisitor}.
 *
 * @author Eric Bruneton
 */
class ClassVisitorTest extends AsmTest {

  @Test
  void testConstructor_validApi() {
    Executable constructor = () -> new ClassVisitor(Opcodes.ASM4) {};

    assertDoesNotThrow(constructor);
  }

  @Test
  void testConstructor_invalidApi() {
    Executable constructor = () -> new ClassVisitor(0) {};

    Exception exception = assertThrows(IllegalArgumentException.class, constructor);
    assertEquals("Unsupported api 0", exception.getMessage());
  }

  @Test
  void testGetDelegate() {
    ClassVisitor delegate = new ClassVisitor(Opcodes.ASM4) {};
    ClassVisitor visitor = new ClassVisitor(Opcodes.ASM4, delegate) {};

    assertSame(delegate, visitor.getDelegate());
  }

  /**
   * Tests that classes are unchanged when transformed with a ClassReader -> class adapter ->
   * ClassWriter chain, where "class adapter" is a ClassVisitor which returns FieldVisitor,
   * MethodVisitor, ModuleVisitor and AnnotationVisitor instances.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testReadAndWrite_emptyVisitor(
      final PrecompiledClass classParameter, final Api apiParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);
    ClassAdapter classAdapter = new ClassAdapter(apiParameter.value(), classWriter);

    Executable transform = () -> classReader.accept(classAdapter, attributes(), 0);

    if (classParameter.isMoreRecentThan(apiParameter)) {
      Exception exception = assertThrows(UnsupportedOperationException.class, transform);
      assertTrue(exception.getMessage().matches(UNSUPPORTED_OPERATION_MESSAGE_PATTERN));
    } else {
      assertDoesNotThrow(transform);
      assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
    }
  }

  /**
   * Tests that a ClassReader -> class adapter -> ClassWriter chain gives the same result with or
   * without the copy pool option, and a class adapter that changes the method exceptions.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testReadAndWrite_copyPool_changeMethodExceptions(
      final PrecompiledClass classParameter, final Api apiParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);
    ClassWriter classWriterWithCopyPool = new ClassWriter(classReader, 0);

    classReader.accept(new ChangeExceptionAdapter(classWriter), attributes(), 0);
    classReader.accept(new ChangeExceptionAdapter(classWriterWithCopyPool), attributes(), 0);

    assertEquals(
        new ClassFile(classWriter.toByteArray()),
        new ClassFile(classWriterWithCopyPool.toByteArray()));
  }

  /**
   * Tests that a ClassReader -> class adapter -> ClassWriter chain gives the same result with or
   * without the copy pool option, and a class adapter that changes the deprecated method flags.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testReadAndWrite_copyPool_changeMethodDeprecatedFlag(
      final PrecompiledClass classParameter, final Api apiParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);
    ClassWriter classWriterWithCopyPool = new ClassWriter(classReader, 0);
    int access = Opcodes.ACC_DEPRECATED;

    classReader.accept(new ChangeAccessAdapter(classWriter, access), attributes(), 0);
    classReader.accept(new ChangeAccessAdapter(classWriterWithCopyPool, access), attributes(), 0);

    assertEquals(
        new ClassFile(classWriter.toByteArray()),
        new ClassFile(classWriterWithCopyPool.toByteArray()));
  }

  /**
   * Tests that a ClassReader -> class adapter -> ClassWriter chain gives the same result with or
   * without the copy pool option, and a class adapter that changes the synthetic method flags.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testReadAndWrite_copyPool_changeMethodSyntheticFlag(
      final PrecompiledClass classParameter, final Api apiParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);
    ClassWriter classWriterWithCopyPool = new ClassWriter(classReader, 0);
    int access = Opcodes.ACC_SYNTHETIC;

    classReader.accept(new ChangeAccessAdapter(classWriter, access), attributes(), 0);
    classReader.accept(new ChangeAccessAdapter(classWriterWithCopyPool, access), attributes(), 0);

    assertEquals(
        new ClassFile(classWriter.toByteArray()),
        new ClassFile(classWriterWithCopyPool.toByteArray()));
  }

  /**
   * Tests that a ClassReader -> class adapter -> ClassWriter chain gives the same result with or
   * without the copy pool option, and a class adapter that changes the class version (and
   * optionally the method synthetic flags / attributes as well).
   */
  @ParameterizedTest
  @CsvSource({"true, true", "true, false", "false, true", "false, false"})
  void testReadAndWrite_copyPool_changeClassVersionAndMethodSyntheticFlag(
      final boolean upgradeVersion, final boolean changeSynthetic) {
    ClassWriter sourceClassWriter = new ClassWriter(0);
    sourceClassWriter.visit(
        upgradeVersion ? Opcodes.V1_4 : Opcodes.V1_5,
        Opcodes.ACC_ABSTRACT,
        "C",
        null,
        "java/lang/Object",
        null);
    sourceClassWriter
        .visitMethod(Opcodes.ACC_ABSTRACT | Opcodes.ACC_SYNTHETIC, "m", "()V", null, null)
        .visitEnd();
    sourceClassWriter.visitEnd();
    ClassReader classReader = new ClassReader(sourceClassWriter.toByteArray());
    ClassWriter classWriter = new ClassWriter(0);
    ClassWriter copyPoolClassWriter = new ClassWriter(classReader, 0);
    int version = upgradeVersion ? Opcodes.V1_5 : Opcodes.V1_4;
    int access = changeSynthetic ? Opcodes.ACC_SYNTHETIC : 0;

    classReader.accept(
        new ChangeVersionAdapter(new ChangeAccessAdapter(classWriter, access), version), 0);
    classReader.accept(
        new ChangeVersionAdapter(new ChangeAccessAdapter(copyPoolClassWriter, access), version), 0);

    assertEquals(
        new ClassFile(classWriter.toByteArray()), new ClassFile(copyPoolClassWriter.toByteArray()));
  }

  /**
   * Tests that a ClassReader -> class adapter -> ClassWriter chain gives the same result with or
   * without the copy pool option, and a class adapter that changes the method descriptors.
   */
  @Test
  void testReadAndWrite_copyPool_changeMethodDescriptor() {
    ClassWriter sourceClassWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    sourceClassWriter.visit(
        Opcodes.V1_7, Opcodes.ACC_ABSTRACT, "C", null, "java/lang/Object", null);
    MethodVisitor methodVisitor =
        sourceClassWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
    methodVisitor.visitCode();
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
    methodVisitor.visitMethodInsn(
        Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    methodVisitor.visitInsn(Opcodes.RETURN);
    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();
    sourceClassWriter.visitEnd();
    ClassReader classReader = new ClassReader(sourceClassWriter.toByteArray());
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    ClassWriter copyPoolClassWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);

    classReader.accept(new AddParameterAdapter(classWriter), 0);
    classReader.accept(new AddParameterAdapter(copyPoolClassWriter), 0);

    assertEquals(
        new ClassFile(classWriter.toByteArray()), new ClassFile(copyPoolClassWriter.toByteArray()));
  }

  /** Test that classes with only visible or only invisible annotations can be read correctly. */
  @ParameterizedTest
  @ValueSource(strings = {"true", "false"})
  void testReadAndWrite_removeAnnotations(final boolean visibilityValue) {
    ClassWriter classWriter = new ClassWriter(0);
    new ClassReader(PrecompiledClass.JDK8_ALL_STRUCTURES.getBytes())
        .accept(new RemoveAnnotationAdapter(classWriter, visibilityValue), 0);
    byte[] classFile = classWriter.toByteArray();
    ClassWriter newClassWriter = new ClassWriter(0);

    new ClassReader(classFile)
        .accept(new RemoveAnnotationAdapter(newClassWriter, visibilityValue), 0);

    assertEquals(new ClassFile(classFile), new ClassFile(newClassWriter.toByteArray()));
  }

  /**
   * Tests that optional module data (ModulePackage, ModuleMainClass, etc) can be removed with a
   * ClassReader -> class adapter -> ClassWriter chain.
   */
  @Test
  void testReadAndWrite_removeOptionalModuleData() {
    byte[] classFile = PrecompiledClass.JDK9_MODULE.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);
    ClassVisitor classVisitor =
        new ClassVisitor(/* latest */ Opcodes.ASM10_EXPERIMENTAL, classWriter) {

          @Override
          public ModuleVisitor visitModule(
              final String name, final int access, final String version) {
            return new ModuleVisitor(api, super.visitModule(name, access, version)) {

              @Override
              public void visitMainClass(final String mainClass) {}

              @Override
              public void visitPackage(final String packaze) {}

              @Override
              public void visitRequire(
                  final String module, final int access, final String version) {
                super.visitRequire(module, access, null);
              }

              @Override
              public void visitExport(
                  final String packaze, final int access, final String... modules) {
                super.visitExport(packaze, access, (String[]) null);
              }

              @Override
              public void visitOpen(
                  final String packaze, final int access, final String... modules) {
                super.visitOpen(packaze, access, (String[]) null);
              }
            };
          }
        };

    classReader.accept(classVisitor, null, 0);

    String classDump = new ClassFile(classWriter.toByteArray()).toString();
    assertFalse(classDump.contains("ModulePackage"));
    assertFalse(classDump.contains("ModuleMainClass"));
  }

  static Attribute[] attributes() {
    return new Attribute[] {new Comment(), new CodeComment()};
  }

  public static class AnnotationAdapter extends AnnotationVisitor {

    AnnotationAdapter(final int api, final AnnotationVisitor annotationVisitor) {
      super(api, annotationVisitor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
      return new AnnotationAdapter(api, super.visitAnnotation(name, descriptor));
    }

    @Override
    public AnnotationVisitor visitArray(final String name) {
      return new AnnotationAdapter(api, super.visitArray(name));
    }
  }

  public static class ClassAdapter extends ClassVisitor {

    ClassAdapter(final int api, final ClassVisitor classVisitor) {
      super(api, classVisitor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
      return new AnnotationAdapter(api, super.visitAnnotation(descriptor, visible));
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
        final int typeRef,
        final TypePath typePath,
        final String descriptor,
        final boolean visible) {
      return new AnnotationAdapter(
          api, super.visitTypeAnnotation(typeRef, typePath, descriptor, visible));
    }

    @Override
    public FieldVisitor visitField(
        final int access,
        final String name,
        final String descriptor,
        final String signature,
        final Object value) {
      return new FieldAdapter(api, super.visitField(access, name, descriptor, signature, value));
    }

    @Override
    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String descriptor,
        final String signature,
        final String[] exceptions) {
      return new MethodAdapter(
          api, super.visitMethod(access, name, descriptor, signature, exceptions));
    }

    @Override
    public ModuleVisitor visitModule(final String name, final int access, final String version) {
      return new ModuleVisitor(api, super.visitModule(name, access, version)) {};
    }
  }

  public static class FieldAdapter extends FieldVisitor {

    FieldAdapter(final int api, final FieldVisitor fieldVisitor) {
      super(api, fieldVisitor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
      return new AnnotationAdapter(api, super.visitAnnotation(descriptor, visible));
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
        final int typeRef,
        final TypePath typePath,
        final String descriptor,
        final boolean visible) {
      return new AnnotationAdapter(
          api, super.visitTypeAnnotation(typeRef, typePath, descriptor, visible));
    }
  }

  public static class MethodAdapter extends MethodVisitor {

    MethodAdapter(final int api, final MethodVisitor methodVisitor) {
      super(api, methodVisitor);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
      return new AnnotationAdapter(api, super.visitAnnotationDefault());
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
      return new AnnotationAdapter(api, super.visitAnnotation(descriptor, visible));
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
        final int typeRef,
        final TypePath typePath,
        final String descriptor,
        final boolean visible) {
      return new AnnotationAdapter(
          api, super.visitTypeAnnotation(typeRef, typePath, descriptor, visible));
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(
        final int parameter, final String descriptor, final boolean visible) {
      return new AnnotationAdapter(
          api, super.visitParameterAnnotation(parameter, descriptor, visible));
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(
        final int typeRef,
        final TypePath typePath,
        final String descriptor,
        final boolean visible) {
      return new AnnotationAdapter(
          api, super.visitInsnAnnotation(typeRef, typePath, descriptor, visible));
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(
        final int typeRef,
        final TypePath typePath,
        final String descriptor,
        final boolean visible) {
      return new AnnotationAdapter(
          api, super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible));
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(
        final int typeRef,
        final TypePath typePath,
        final Label[] start,
        final Label[] end,
        final int[] index,
        final String descriptor,
        final boolean visible) {
      return new AnnotationAdapter(
          api,
          super.visitLocalVariableAnnotation(
              typeRef, typePath, start, end, index, descriptor, visible));
    }
  }

  public static class ChangeExceptionAdapter extends ClassVisitor {

    ChangeExceptionAdapter(final ClassVisitor classVisitor) {
      super(/* latest */ Opcodes.ASM10_EXPERIMENTAL, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String descriptor,
        final String signature,
        final String[] exceptions) {
      if (exceptions != null && exceptions.length > 0) {
        exceptions[0] = "java/lang/Throwable";
      }
      return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
  }

  public static class ChangeVersionAdapter extends ClassVisitor {

    public final int newVersion;

    ChangeVersionAdapter(final ClassVisitor classVisitor, final int newVersion) {
      super(/* latest */ Opcodes.ASM10_EXPERIMENTAL, classVisitor);
      this.newVersion = newVersion;
    }

    @Override
    public void visit(
        final int version,
        final int access,
        final String name,
        final String signature,
        final String superName,
        final String[] interfaces) {
      super.visit(newVersion, access, name, signature, superName, interfaces);
    }
  }

  public static class ChangeAccessAdapter extends ClassVisitor {

    public final int accessFlags;

    ChangeAccessAdapter(final ClassVisitor classVisitor, final int accessFlags) {
      super(/* latest */ Opcodes.ASM10_EXPERIMENTAL, classVisitor);
      this.accessFlags = accessFlags;
    }

    @Override
    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String descriptor,
        final String signature,
        final String[] exceptions) {
      return super.visitMethod(access ^ accessFlags, name, descriptor, signature, exceptions);
    }
  }

  /** A class visitor which removes either all visible or all invisible [type] annotations. */
  public static class RemoveAnnotationAdapter extends ClassVisitor {

    public final boolean visibilityValue;

    RemoveAnnotationAdapter(final ClassVisitor classVisitor, final boolean visibilityValue) {
      super(/* latest */ Opcodes.ASM10_EXPERIMENTAL, classVisitor);
      this.visibilityValue = visibilityValue;
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
      if (visible == visibilityValue) {
        return null;
      }
      return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
        final int typeRef,
        final TypePath typePath,
        final String descriptor,
        final boolean visible) {
      if (visible == visibilityValue) {
        return null;
      }
      return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public FieldVisitor visitField(
        final int access,
        final String name,
        final String descriptor,
        final String signature,
        final Object value) {
      return new FieldVisitor(api, super.visitField(access, name, descriptor, signature, value)) {

        @Override
        public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
          if (visible == visibilityValue) {
            return null;
          }
          return super.visitAnnotation(descriptor, visible);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(
            final int typeRef,
            final TypePath typePath,
            final String descriptor,
            final boolean visible) {
          if (visible == visibilityValue) {
            return null;
          }
          return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        }
      };
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
        public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
          if (visible == visibilityValue) {
            return null;
          }
          return super.visitAnnotation(descriptor, visible);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(
            final int typeRef,
            final TypePath typePath,
            final String descriptor,
            final boolean visible) {
          if (visible == visibilityValue) {
            return null;
          }
          return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(
            final int parameter, final String descriptor, final boolean visible) {
          if (visible == visibilityValue) {
            return null;
          }
          return super.visitParameterAnnotation(parameter, descriptor, visible);
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(
            final int typeRef,
            final TypePath typePath,
            final String descriptor,
            final boolean visible) {
          if (visible == visibilityValue) {
            return null;
          }
          return super.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(
            final int typeRef,
            final TypePath typePath,
            final String descriptor,
            final boolean visible) {
          if (visible == visibilityValue) {
            return null;
          }
          return super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(
            final int typeRef,
            final TypePath typePath,
            final Label[] start,
            final Label[] end,
            final int[] index,
            final String descriptor,
            final boolean visible) {
          if (visible == visibilityValue) {
            return null;
          }
          return super.visitLocalVariableAnnotation(
              typeRef, typePath, start, end, index, descriptor, visible);
        }
      };
    }
  }

  /** A class visitor which adds a parameter to the declared method descriptors. */
  public static class AddParameterAdapter extends ClassVisitor {

    public AddParameterAdapter(final ClassVisitor classVisitor) {
      super(/* latest */ Opcodes.ASM10_EXPERIMENTAL, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String descriptor,
        final String signature,
        final String[] exceptions) {
      List<Type> argumentTypes = new ArrayList<>(Arrays.asList(Type.getArgumentTypes(descriptor)));
      argumentTypes.add(Type.INT_TYPE);
      Type returnType = Type.getReturnType(descriptor);
      return super.visitMethod(
          access,
          name,
          Type.getMethodDescriptor(returnType, argumentTypes.toArray(new Type[0])),
          signature,
          exceptions);
    }
  }
}
