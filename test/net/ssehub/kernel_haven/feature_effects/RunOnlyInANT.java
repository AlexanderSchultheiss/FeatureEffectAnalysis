package net.ssehub.kernel_haven.feature_effects;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * May be used to specify that a certain test class runs only when called from <b>ANT</b>.
 * Usage: <tt>@RunWith(value = RunOnlyInANT.class)</tt>
 * @author El-Sharkawy
 *
 */
public class RunOnlyInANT extends BlockJUnit4ClassRunner {
    
    /**
     * Creates a BlockJUnit4ClassRunner to run {@code clazz}.
     * @param clazz The test class.
     * @throws InitializationError if the test class is malformed.
     */
    public RunOnlyInANT(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    public void run(RunNotifier notifier) {
        if (null == System.getProperty("ant.project.name") && null == System.getProperty("ant.version")) {
            System.err.println(this.getTestClass().getName() + " skipped because of wrong OS used.");            
        } else {
            super.run(notifier);                        
        }
    }
}
