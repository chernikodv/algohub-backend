package edu.wpi.cs.dss.serverless.benchmark;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        RemoveBenchmarkHandlerTest.class,
        CreateBenchmarkHandlerTest.class,
        LoadBenchmarkByImplementationHandlerTest.class
})
public class BenchmarkTestSuite {
}
