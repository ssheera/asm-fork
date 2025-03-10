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

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * A benchmark to measure the performance of ASM when reading and writing Java 8 classes (containing
 * stack map frames and invokedynamic instructions) with no intermediate transformation.
 *
 * @author Eric Bruneton
 */
@Fork(1)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 30, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
public class AdapterBenchmarkJava8 extends AbstractBenchmark {

  public Adapter asm5dot0;
  public Adapter asm6dot0;
  public Adapter asm7dot0;
  public Adapter asm8dot0;
  public Adapter asm9dot0;
  public Adapter asmCurrent;

  public AdapterBenchmarkJava8() {
    super("org.objectweb.asm.benchmarks.AsmAdapter");
  }

  /**
   * Prepares the benchmark by creating an {@link Adapter} for each library to be tested, and by
   * loading some test data (i.e. some classes to "adapt").
   *
   * @throws Exception if an error occurs.
   */
  @Setup
  public void prepare() throws Exception {
    asm5dot0 = (Adapter) new AsmBenchmarkFactory(AsmVersion.V5_0).newAsmBenchmark();
    asm6dot0 = (Adapter) new AsmBenchmarkFactory(AsmVersion.V6_0).newAsmBenchmark();
    asm7dot0 = (Adapter) new AsmBenchmarkFactory(AsmVersion.V7_0).newAsmBenchmark();
    asm8dot0 = (Adapter) new AsmBenchmarkFactory(AsmVersion.V8_0).newAsmBenchmark();
    asm9dot0 = (Adapter) new AsmBenchmarkFactory(AsmVersion.V9_0).newAsmBenchmark();
    asmCurrent = (Adapter) new AsmBenchmarkFactory(AsmVersion.V_CURRENT).newAsmBenchmark();

    // Check that the correct versions of ASM have been loaded.
    if (!asm5dot0.getVersion().equals("ASM5")
        || !asm6dot0.getVersion().equals("ASM6")
        || !asm7dot0.getVersion().equals("ASM7")
        || !asm8dot0.getVersion().equals("ASM8")
        || !asm9dot0.getVersion().equals("ASM9")
        || !asmCurrent.getVersion().equals("ASM9")) {
      throw new IllegalStateException();
    }

    prepareClasses();
  }

  @Benchmark
  public void getClassInfo_asm5_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm5dot0.getClassInfo(classFile));
    }
  }

  @Benchmark
  public void getClassInfo_asm6_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm6dot0.getClassInfo(classFile));
    }
  }

  @Benchmark
  public void getClassInfo_asm7_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm7dot0.getClassInfo(classFile));
    }
  }

  @Benchmark
  public void getClassInfo_asm8_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm8dot0.getClassInfo(classFile));
    }
  }

  @Benchmark
  public void getClassInfo_asm9_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm9dot0.getClassInfo(classFile));
    }
  }

  @Benchmark
  public void getClassInfo_asmCurrent(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asmCurrent.getClassInfo(classFile));
    }
  }

  @Benchmark
  public void read_asm5_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm5dot0.read(classFile));
    }
  }

  @Benchmark
  public void read_asm6_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm6dot0.read(classFile));
    }
  }

  @Benchmark
  public void read_asm7_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm7dot0.read(classFile));
    }
  }

  @Benchmark
  public void read_asm8_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm8dot0.read(classFile));
    }
  }

  @Benchmark
  public void read_asm9_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm9dot0.read(classFile));
    }
  }

  @Benchmark
  public void read_asmCurrent(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asmCurrent.read(classFile));
    }
  }

  @Benchmark
  public void readAndWrite_asm5_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm5dot0.readAndWrite(classFile, /* computeMaxs= */ false));
    }
  }

  @Benchmark
  public void readAndWrite_asm6_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm6dot0.readAndWrite(classFile, /* computeMaxs= */ false));
    }
  }

  @Benchmark
  public void readAndWrite_asm7_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm7dot0.readAndWrite(classFile, /* computeMaxs= */ false));
    }
  }

  @Benchmark
  public void readAndWrite_asm8_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm8dot0.readAndWrite(classFile, /* computeMaxs= */ false));
    }
  }

  @Benchmark
  public void readAndWrite_asm9_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm9dot0.readAndWrite(classFile, /* computeMaxs= */ false));
    }
  }

  @Benchmark
  public void readAndWrite_asmCurrent(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asmCurrent.readAndWrite(classFile, /* computeMaxs= */ false));
    }
  }

  @Benchmark
  public void readAndWriteWithCopyPool_asm5_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm5dot0.readAndWriteWithCopyPool(classFile));
    }
  }

  @Benchmark
  public void readAndWriteWithCopyPool_asm6_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm6dot0.readAndWriteWithCopyPool(classFile));
    }
  }

  @Benchmark
  public void readAndWriteWithCopyPool_asm7_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm7dot0.readAndWriteWithCopyPool(classFile));
    }
  }

  @Benchmark
  public void readAndWriteWithCopyPool_asm8_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm8dot0.readAndWriteWithCopyPool(classFile));
    }
  }

  @Benchmark
  public void readAndWriteWithCopyPool_asm9_0(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asm9dot0.readAndWriteWithCopyPool(classFile));
    }
  }

  @Benchmark
  public void readAndWriteWithCopyPool_asmCurrent(final Blackhole blackhole) {
    for (byte[] classFile : java8classFiles) {
      blackhole.consume(asmCurrent.readAndWriteWithCopyPool(classFile));
    }
  }
}
