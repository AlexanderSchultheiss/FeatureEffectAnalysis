package net.ssehub.kernel_haven.fe_analysis;

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
        if (null == System.getProperty("KH.ANT.Test.Execution")) {
            System.err.println(getTestClass().getName() + " skipped because it was called from outside of ANT script.");
        } else {
            super.run(notifier);                        
        }
    }
}
