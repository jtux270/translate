package org.ovirt.engine.core.utils.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.ovirt.engine.core.utils.log.Logged.LogLevel;

/**
 * Tests for the {@link LoggedUtils} class.
 */
public class LoggedUtilsTest {

    /* --- Types used for testing purposes --- */

    /**
     * Interface used for DRY testing of {@link LoggedUtils#log}.
     */
    private static interface LogSetup {
        public void setup(Log mock, String message, Object args);

        public void verifyCall(Log mock, String message, Object args);
    }

    @Logged(executionLevel = LogLevel.OFF, errorLevel = LogLevel.OFF)
    public class LoggedClass {
        // Intentionally empty - a stub class for testing
    }

    public class LoggedSubclass extends LoggedClass {
        // Intentionally empty - a stub class for testing
    }

    @Logged(executionLevel = LogLevel.DEBUG, errorLevel = LogLevel.WARN,
            parametersLevel = LogLevel.FATAL, returnLevel = LogLevel.FATAL)
    public class LoggedOverridingSubclass extends LoggedClass {
        // Intentionally empty - a stub class for testing
    }

    @Logged(parametersLevel = LogLevel.DEBUG)
    public class LoggedOverridingSubclassNoParameters extends LoggedClass {
        // Intentionally empty - a stub class for testing
    }

    @Logged(returnLevel = LogLevel.DEBUG)
    public class LoggedOverridingSubclassNoReturn extends LoggedClass {
        // Intentionally empty - a stub class for testing
    }

    /* --- Tests for the method "getObjectId" --- */

    @Test
    public void testGetObjectIdFromNull() throws Exception {
        assertEquals("0", LoggedUtils.getObjectId(null));
    }

    @Test
    public void testGetObjectIdUniqueForDifferentObjects() throws Exception {
        assertTrue(!LoggedUtils.getObjectId(new Object()).equals(LoggedUtils.getObjectId(new Object())));
    }

    @Test
    public void testGetObjectIdConsistentForSameObject() throws Exception {
        Object obj = new Object();
        assertEquals(LoggedUtils.getObjectId(obj), LoggedUtils.getObjectId(obj));
    }

    /* --- Tests for the method "getAnnotation" --- */

    @Test
    public void testGetAnnotationFromNull() throws Exception {
        assertNull(LoggedUtils.getAnnotation(null));
    }

    @Test
    public void testGetAnnotationFromClass() throws Exception {
        Logged logged = LoggedUtils.getAnnotation(new LoggedClass());

        assertEquals(LogLevel.OFF, logged.executionLevel());
        assertEquals(LogLevel.OFF, logged.errorLevel());
        assertEquals(LogLevel.INFO, logged.parametersLevel());
        assertEquals(LogLevel.INFO, logged.returnLevel());
    }

    @Test
    public void testGetAnnotationFromSubclass() throws Exception {
        assertSame(LoggedUtils.getAnnotation(new LoggedClass()), LoggedUtils.getAnnotation(new LoggedSubclass()));
    }

    @Test
    public void testGetAnnotationFromOverridingSubclass() throws Exception {
        Logged logged = LoggedUtils.getAnnotation(new LoggedOverridingSubclass());

        assertEquals(LogLevel.DEBUG, logged.executionLevel());
        assertEquals(LogLevel.WARN, logged.errorLevel());
        assertEquals(LogLevel.FATAL, logged.parametersLevel());
        assertEquals(LogLevel.FATAL, logged.returnLevel());
    }

    /* --- Tests for the method "determineMessage" --- */

    @Test
    public void testDetermineMessageReturnsObjectForParameterExpansion() throws Exception {
        Object obj = new Object();
        Log log = mock(Log.class);
        when(log.isDebugEnabled()).thenReturn(true);

        assertSame(obj,
                LoggedUtils.determineMessage(log, LoggedOverridingSubclass.class.getAnnotation(Logged.class), obj));
    }

    @Test
    public void testDetermineMessageReturnsClassNameForNoParameterExpansion() throws Exception {
        Log log = mock(Log.class);

        assertEquals("LoggedUtils.determineMessage shouldn't return parameter expansion for a disabled log level.",
                Object.class.getName(),
                LoggedUtils.determineMessage(log,
                        LoggedOverridingSubclassNoParameters.class.getAnnotation(Logged.class), new Object()));
        assertEquals("LoggedUtils.determineMessage shouldn't return parameter expansion when diabled completely.",
                Object.class.getName(),
                LoggedUtils.determineMessage(log, LoggedClass.class.getAnnotation(Logged.class), new Object()));
    }

    /* --- Tests for the method "log" --- */

    @Test
    public void testLogOffDoesntLog() {
        Log log = mock(Log.class);

        LoggedUtils.log(log, LogLevel.OFF, "{0}", new Object());
        verifyZeroInteractions(log);
    }

    @Test
    public void testLogTrace() throws Exception {
        helpTestLog(LogLevel.TRACE, new LogSetup() {
            @Override
            public void setup(Log mock, String message, Object args) {
                when(mock.isTraceEnabled()).thenReturn(true);
            }

            @Override
            public void verifyCall(Log mock, String message, Object args) {
                verify(mock).traceFormat(message, args);
            }
        });
    }

    @Test
    public void testLogDebug() throws Exception {
        helpTestLog(LogLevel.DEBUG, new LogSetup() {
            @Override
            public void setup(Log mock, String message, Object args) {
                when(mock.isDebugEnabled()).thenReturn(true);
            }

            @Override
            public void verifyCall(Log mock, String message, Object args) {
                verify(mock).debugFormat(message, args);
            }
        });
    }

    @Test
    public void testLogInfo() throws Exception {
        helpTestLog(LogLevel.INFO, new LogSetup() {
            @Override
            public void setup(Log mock, String message, Object args) {
                when(mock.isInfoEnabled()).thenReturn(true);
            }

            @Override
            public void verifyCall(Log mock, String message, Object args) {
                verify(mock).infoFormat(message, args);
            }
        });
    }

    @Test
    public void testLogWarn() throws Exception {
        helpTestLog(LogLevel.WARN, new LogSetup() {
            @Override
            public void setup(Log mock, String message, Object args) {
                when(mock.isWarnEnabled()).thenReturn(true);
            }

            @Override
            public void verifyCall(Log mock, String message, Object args) {
                verify(mock).warnFormat(message, args);
            }
        });
    }

    @Test
    public void testLogError() throws Exception {
        helpTestLog(LogLevel.ERROR, new LogSetup() {
            @Override
            public void setup(Log mock, String message, Object args) {
                when(mock.isErrorEnabled()).thenReturn(true);
            }

            @Override
            public void verifyCall(Log mock, String message, Object args) {
                verify(mock).errorFormat(message, args);
            }
        });
    }

    @Test
    public void testLogFatal() throws Exception {
        helpTestLog(LogLevel.FATAL, new LogSetup() {
            @Override
            public void setup(Log mock, String message, Object args) {
                when(mock.isFatalEnabled()).thenReturn(true);
            }

            @Override
            public void verifyCall(Log mock, String message, Object args) {
                verify(mock).fatalFormat(message, args);
            }
        });
    }

    /* --- Tests for the method "logEntry" --- */

    @Test
    public void testLogEntryDoesntLogWhenNoAnnotation() throws Exception {
        Log log = mock(Log.class);
        LoggedUtils.logEntry(log, "", new Object());
        verifyZeroInteractions(log);
    }

    @Test
    public void testLogEntryDoesntLogWhenLogLevelInactive() throws Exception {
        Log log = mock(Log.class);
        LoggedUtils.logEntry(log, "", new LoggedOverridingSubclass());
        verifyNoLogging(log);
    }

    @Test
    public void testLogEntryLogsWhenLogLevelActive() throws Exception {
        String id = "";
        Log log = mock(Log.class);
        when(log.isDebugEnabled()).thenReturn(true);

        LoggedUtils.logEntry(log, id, new LoggedOverridingSubclass());
        verify(log).debugFormat(eq(LoggedUtils.ENTRY_LOG), anyObject(), eq(id));
    }

    /* --- Tests for the method "logReturn" --- */

    @Test
    public void testLogReturnDoesntLogWhenNoAnnotation() throws Exception {
        Log log = mock(Log.class);
        LoggedUtils.logReturn(log, "", new Object(), new Object());
        verifyZeroInteractions(log);
    }

    @Test
    public void testLogReturnDoesntLogWhenLogLevelInactive() throws Exception {
        Log log = mock(Log.class);
        LoggedUtils.logReturn(log, "", new LoggedOverridingSubclass(), new Object());
        verifyNoLogging(log);
    }

    @Test
    public void testLogReturnLogsWhenLogLevelActiveAndNoExpandReturn() throws Exception {
        String id = "";
        Log log = mock(Log.class);
        when(log.isInfoEnabled()).thenReturn(true);

        LoggedUtils.logReturn(log, id, new LoggedOverridingSubclassNoReturn(), new Object());
        verify(log).infoFormat(eq(LoggedUtils.EXIT_LOG_VOID), anyObject(), eq(id));
    }

    @Test
    public void testLogReturnLogsWhenLogLevelActiveAndExpandReturn() throws Exception {
        String id = "";
        Log log = mock(Log.class);
        when(log.isDebugEnabled()).thenReturn(true);

        LoggedUtils.logReturn(log, id, new LoggedOverridingSubclass(), new Object());
        verify(log).debugFormat(eq(LoggedUtils.EXIT_LOG_RETURN_VALUE), anyObject(), anyObject(), eq(id));
    }

    @Test
    public void testLogReturnLogsWhenLogLevelActiveAndExpandReturnButNullReturn() throws Exception {
        String id = "";
        Log log = mock(Log.class);
        when(log.isDebugEnabled()).thenReturn(true);

        LoggedUtils.logReturn(log, id, new LoggedOverridingSubclass(), null);
        verify(log).debugFormat(eq(LoggedUtils.EXIT_LOG_VOID), anyObject(), eq(id));
    }

    /* --- Tests for the method "logError" --- */

    @Test
    public void testLogErrorDoesntLogWhenNoAnnotation() throws Exception {
        Log log = mock(Log.class);
        LoggedUtils.logError(log, "", new Object(), new Exception());
        verifyZeroInteractions(log);
    }

    @Test
    public void testLogErrorDoesntLogWhenLogLevelInactive() throws Exception {
        Log log = mock(Log.class);
        LoggedUtils.logError(log, "", new LoggedOverridingSubclass(), new Exception());
        verifyNoLogging(log);
    }

    @Test
    public void testLogErrorLogsWhenLogLevelActive() throws Exception {
        String id = "";
        Log log = mock(Log.class);

        // when the call to determine whether to log the parameters or not.
        when(log.isDebugEnabled()).thenReturn(true);
        when(log.isWarnEnabled()).thenReturn(true);

        LoggedUtils.logError(log, id, new LoggedOverridingSubclass(), new Exception());
        verify(log).warnFormat(eq(LoggedUtils.ERROR_LOG), anyObject(), anyObject(), eq(id), anyObject());
    }

    /* --- Helper methods --- */

    /**
     * Verifies that no logging was done on the given log mock.
     */
    private static void verifyNoLogging(Log logMock) {
        verify(logMock, never()).traceFormat(any(String.class), any());
        verify(logMock, never()).debugFormat(any(String.class), any());
        verify(logMock, never()).infoFormat(any(String.class), any());
        verify(logMock, never()).warnFormat(any(String.class), any());
        verify(logMock, never()).errorFormat(any(String.class), any());
        verify(logMock, never()).fatalFormat(any(String.class), any());
    }

    /**
     * Helper method to test the {@link LoggedUtils#log} method functionality.
     * @param logLevel Log level to test.
     * @param logSetup Setup the mocks using an anonymous inner class of this interface.
     */
    private static void helpTestLog(LogLevel logLevel, LogSetup logSetup) {
        Log log = mock(Log.class);
        String message = "{0}";
        Object args = new Object();
        logSetup.setup(log, message, args);

        LoggedUtils.log(log, logLevel, message, args);
        logSetup.verifyCall(log, message, args);
    }
}
