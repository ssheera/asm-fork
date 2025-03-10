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
package org.objectweb.asm.benchmarks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.cojen.classfile.ClassFile;
import org.cojen.classfile.CodeBuilder;
import org.cojen.classfile.MethodInfo;
import org.cojen.classfile.Modifiers;
import org.cojen.classfile.TypeDesc;

/**
 * A "Hello World!" class generator using the Cojen library.
 *
 * @author Eric Bruneton
 */
public class CojenGenerator extends Generator {

  public static final TypeDesc PRINT_STREAM = TypeDesc.forClass("java.io.PrintStream");

  @Override
  public byte[] generateClass() {
    ClassFile classFile = new ClassFile("HelloWorld");

    classFile.setSourceFile("HelloWorld.java");

    classFile.addDefaultConstructor();

    TypeDesc[] params = new TypeDesc[] {TypeDesc.STRING.toArrayType()};
    MethodInfo methodInfo = classFile.addMethod(Modifiers.PUBLIC_STATIC, "main", null, params);
    CodeBuilder codeBuilder = new CodeBuilder(methodInfo);
    codeBuilder.loadStaticField("java.lang.System", "out", PRINT_STREAM);
    codeBuilder.loadConstant("Hello world!");
    codeBuilder.invokeVirtual(PRINT_STREAM, "println", null, new TypeDesc[] {TypeDesc.STRING});
    codeBuilder.returnVoid();

    try {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      classFile.writeTo(byteArrayOutputStream);
      return byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Class generation failed", e);
    }
  }
}
