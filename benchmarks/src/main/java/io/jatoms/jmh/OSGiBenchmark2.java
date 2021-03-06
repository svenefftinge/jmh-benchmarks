package io.jatoms.jmh;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.felix.framework.Felix;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

import de.unia.smds.test.ITest;
import de.unia.smds.test.Test;

@State(Scope.Thread)
@Threads(1)
@Fork(1)
@Warmup(iterations = 10, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 10, timeUnit = TimeUnit.SECONDS)
public class OSGiBenchmark2 {

    Felix felix;
    ComponentFactory<ITest> factoryService;
    Dictionary<String, Object> configSmall;
    Dictionary<String, Object> configLarge;

    @Setup(Level.Trial)
    public void prepare() throws BundleException, FileNotFoundException, InterruptedException, InvalidSyntaxException {
        // Configure Felix to use the framework classloader and delegate the
        // classloading for org.osgi.service.component to this one,
        // as otherwise we get nasty classcastexceptions, as the framework and the
        // bootstrapping code would have different instances of the same class
        Map<String, Object> config = new HashMap<>();
        config.put("org.osgi.framework.bootdelegation", "org.osgi.service.component");
        config.put("org.osgi.framework.bundle.parent", "framework");

        felix = new Felix(config);
        felix.start();

        BundleContext context = felix.getBundleContext();

        // install bundles for the test
        Path scr = Paths.get(
                "/workspace/jmh-benchmarks/benchmarks/libs/org/apache/felix/org.apache.felix.scr/2.1.0/org.apache.felix.scr-2.1.0.jar");

        Path testlib = Paths.get(
                "/workspace/jmh-benchmarks/benchmarks/libs/io/jatoms/test/test.bundle/0.1.0/test.bundle-0.1.0.jar");

        context.installBundle("scr", new FileInputStream(scr.toFile())).start();
        ;
        context.installBundle("testlib", new FileInputStream(testlib.toFile())).start();

        // wait for osgi to start all services we need
        Thread.sleep(1000);

        // the framework bundle has no service reference to ComponentFactory so we
        // search the other bundles for such a reference
        Bundle[] bundles = context.getBundles();
        for (Bundle bundle : bundles) {
            ServiceReference<ComponentFactory> ref = bundle.getBundleContext()
                    .getServiceReference(ComponentFactory.class);
            if (ref != null) {
                factoryService = bundle.getBundleContext().getService(ref);
                break;
            }
        }

        configSmall = new Hashtable<>();
        configSmall.put("test", 1);

        configLarge = new Hashtable<>();
        configLarge.put("Test1", 1);
        configLarge.put("Test2", 4);
        configLarge.put("Test3", "Test3");
        configLarge.put("Test4", "supergeilertext");
        configLarge.put("Test5", 1);
        configLarge.put("Test6", 4);
        configLarge.put("Test7", "Test3");
        configLarge.put("Test8", "supergeilertext");
        configLarge.put("Test9", 1);
        configLarge.put("Test10", 4);
        configLarge.put("Test11", "Test3");
        configLarge.put("Test12", "supergeilertext");
    }

    @TearDown
    public void shutdown() {
        try {
            felix.stop();
        } catch (BundleException e) {
            e.printStackTrace();
        }
    }

    @Benchmark
    public void osgiInstanceCreationNoConfig(Blackhole blackhole) {
        ComponentInstance<ITest> service = factoryService.newInstance(null);
        service.dispose();
    }

    @Benchmark
    public void newInstanceCreationNoConfig(Blackhole blackhole) {
        Test test = new Test();
        blackhole.consume(test);
    }

    @Benchmark
    public void osgiInstanceCreationSmallConfig(Blackhole blackhole) {
        ComponentInstance<ITest> service = factoryService.newInstance(configSmall);
        service.dispose();
    }

    @Benchmark
    public void osgiInstanceCreationLargeConfig(Blackhole blackhole) {
        ComponentInstance<ITest> service = factoryService.newInstance(configLarge);
        service.dispose();
    }
}