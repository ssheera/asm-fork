package org.objectweb.asm.util;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.test.AsmTest;
import org.objectweb.asm.test.AsmTest.PrecompiledClass;

/**
 * Provides class, field and method signatures for parameterized unit tests.
 *
 * @author Eric Bruneton
 */
public final class SignaturesProviders {

  public static final List<String> CLASS_SIGNATURES = new ArrayList<>();
  public static final List<String> FIELD_SIGNATURES = new ArrayList<>();
  public static final List<String> METHOD_SIGNATURES = new ArrayList<>();

  static {
    AsmTest.allClassesAndLatestApi()
        .map(argument -> (PrecompiledClass) argument.get()[0])
        .filter(precompiledClass -> !precompiledClass.isMoreRecentThan(AsmTest.Api.ASM7))
        .forEach(precompiledClass -> collectSignatures(precompiledClass));
    assertFalse(CLASS_SIGNATURES.isEmpty());
    assertFalse(FIELD_SIGNATURES.isEmpty());
    assertFalse(METHOD_SIGNATURES.isEmpty());
  }

  public SignaturesProviders() {}

  public static void collectSignatures(final PrecompiledClass classParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    classReader.accept(
        new ClassVisitor(/* latest api*/ Opcodes.ASM9) {
          @Override
          public void visit(
              final int version,
              final int access,
              final String name,
              final String signature,
              final String superName,
              final String[] interfaces) {
            if (signature != null) {
              CLASS_SIGNATURES.add(signature);
            }
          }

          @Override
          public FieldVisitor visitField(
              final int access,
              final String name,
              final String descriptor,
              final String signature,
              final Object value) {
            if (signature != null) {
              FIELD_SIGNATURES.add(signature);
            }
            return null;
          }

          @Override
          public MethodVisitor visitMethod(
              final int access,
              final String name,
              final String descriptor,
              final String signature,
              final String[] exceptions) {
            if (signature != null) {
              METHOD_SIGNATURES.add(signature);
            }
            return null;
          }
        },
        0);
  }

  static Stream<String> classSignatures() {
    return CLASS_SIGNATURES.stream();
  }

  static Stream<String> fieldSignatures() {
    return FIELD_SIGNATURES.stream();
  }

  static Stream<String> methodSignatures() {
    return METHOD_SIGNATURES.stream();
  }
}
