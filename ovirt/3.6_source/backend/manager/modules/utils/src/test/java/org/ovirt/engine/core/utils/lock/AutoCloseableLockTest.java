package org.ovirt.engine.core.utils.lock;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.locks.Lock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AutoCloseableLockTest {

    @Mock
    Lock lock;

    @Test
    public void testLocking() {
        try (AutoCloseableLock l = new AutoCloseableLock(lock)) {
            // Do nothing with the lock..
        }

        verify(lock).unlock();
    }

    @Test
    public void testExceptionInsideCodeBlock() {
        try {
            try (AutoCloseableLock l = new AutoCloseableLock(lock)) {
                throw new RuntimeException();
            }
        } catch (RuntimeException e) {
            verify(lock).unlock();
            return;
        }
    }

    @Test
    public void testExceptionWhileLocking() {
        doThrow(new RuntimeException()).when(lock).lock();
        try (AutoCloseableLock l = new AutoCloseableLock(lock)) {
            fail("Lock wasn't acquired, this code shouldn't happen.");
        } catch (RuntimeException e) {
            // No worry, we're good..
        }

        verify(lock, never()).unlock();
    }
}
