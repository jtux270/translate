package org.ovirt.engine.ui.uicommonweb.action;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.ui.uicommonweb.action.UiAction.ActionFlowState;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;

@RunWith(MockitoJUnitRunner.class)
public class UiVdcMultipleActionTest extends AsyncUiActionTest<IFrontendMultipleActionAsyncCallback> {

    @Test
    public void runNextWithFirstActionFailureNoWaitForResultTest() {
        runNextActionFlowWithFirstActionFailureTestCommon(false);
    }

    @Test
    public void runNextWithFirstActionFailureWaitForResultTest() {
        runNextActionFlowWithFirstActionFailureTestCommon(true);
    }

    private void runNextActionFlowWithFirstActionFailureTestCommon(boolean waitFoResult) {
        UiAction action1 = createAction(waitFoResult, true);
        UiAction action2 = createAction(waitFoResult, true);
        action1.then(action2);
        action1.runAction();

        verifyRunActionAndExecuteCallbacksRandomly(false, action1.getActionFlowState(), 1, waitFoResult);
        verifyRunActionAndExecuteCallbacksRandomly(true, action1.getActionFlowState(), 2, 1, waitFoResult);

        assertFinishedWithErrors(new ArrayList<UiAction>(Arrays.asList(action1, action2)), 1);
    }

    @Test
    public void runMixedActionFlowWithFailureOnDependentAction() {
        // action1.and(action2).next(action3).and(action4)
        // action1 and action 2 has failure -> action3 and action4 won't be executed

        List<UiAction> actions = runActionFlow(ActionType.parallel, ActionType.next, ActionType.parallel);
        ActionFlowState flowState = actions.get(0).getActionFlowState();

        verifyRunActionAndExecuteCallbacksRandomly(false, flowState, 2);
        verifyRunActionAndExecuteCallbacksRandomly(true, flowState, 4, 2);

        assertFinishedWithErrors(actions.subList(0, 2), 2);
    }

    @Test
    public void dontRunNextInCaseOfErrorNoWaitForResultTest() {
        dontRunNextInCaseOfErrorCommon(false);
    }

    @Test
    public void dontRunNextInCaseOfErrorWaitForResultTest() {
        dontRunNextInCaseOfErrorCommon(true);
    }

    private void dontRunNextInCaseOfErrorCommon(boolean waitForResult) {
        UiAction action1 = createAction(waitForResult, false);
        UiAction action2 = createAction();
        action1.then(action2);
        action1.runAction();

        ActionFlowState flowState = action1.getActionFlowState();

        verifyRunActionAndExecuteCallbacksRandomly(false, flowState, 1, waitForResult);
        verifyRunAction(1, waitForResult);

        assertFinishedWithErrors(Collections.singletonList(action1), 1);
    }

    @Test
    public void noParametersTest() {
        UiAction action = createAction(true);
        action.runAction();
        verifyRunAction(0);

        assertFinishedWithNoErrors(Collections.singletonList(action));
    }

    @Override
    protected UiVdcMultipleAction createAction() {
        return createAction(false);
    }

    private UiVdcMultipleAction createAction(boolean emptyParams) {
        return new UiVdcMultipleAction(ACTION_TYPE, emptyParams ? new ArrayList<VdcActionParametersBase>()
                : Collections.singletonList(new VdcActionParametersBase()), model);
    }

    private UiVdcMultipleAction createAction(boolean waitForResult, boolean runNextInCaseOfError) {
        return new UiVdcMultipleAction(ACTION_TYPE,
                Collections.singletonList(new VdcActionParametersBase()),
                model,
                waitForResult,
                runNextInCaseOfError);
    }

    @Override
    protected void verifyRunActionAndExecuteCallbacksRandomly(boolean success,
            ActionFlowState flowState,
            int exepectedNumOfRunActionExecutions, int numOfCallbacksFromTheEndToExecute) {
        verifyRunActionAndExecuteCallbacksRandomly(success,
                flowState,
                exepectedNumOfRunActionExecutions,
                numOfCallbacksFromTheEndToExecute,
                false);
    }

    private void verifyRunActionAndExecuteCallbacksRandomly(boolean success,
            ActionFlowState flowState,
            int exepectedNumOfRunActionExecutions, int numOfCallbacksFromTheEndToExecute, boolean waitForResult) {
        List<IFrontendMultipleActionAsyncCallback> callbacks =
                verifyRunAction(exepectedNumOfRunActionExecutions, waitForResult);
        executeCallbacks(success,
                flowState,
                callbacks.subList(callbacks.size() - numOfCallbacksFromTheEndToExecute, callbacks.size()),
                waitForResult);
    }

    private void verifyRunActionAndExecuteCallbacksRandomly(boolean success,
            ActionFlowState flowState,
            int exepectedNumOfRunActionExecutions, boolean waitForResult) {
        verifyRunActionAndExecuteCallbacksRandomly(success,
                flowState,
                exepectedNumOfRunActionExecutions,
                exepectedNumOfRunActionExecutions,
                waitForResult);
    }

    private void executeCallbacks(boolean success,
            ActionFlowState flowState,
            List<IFrontendMultipleActionAsyncCallback> callbacks, boolean waitForResult) {
        Collections.shuffle(callbacks);

        for (IFrontendMultipleActionAsyncCallback callback : callbacks) {
            assertNotAllDone(flowState);
            VdcReturnValueBase result = new VdcReturnValueBase();
            result.setCanDoAction(waitForResult ? true : success);
            result.setSucceeded(waitForResult ? success : false);
            callback.executed(new FrontendMultipleActionAsyncResult(ACTION_TYPE,
                    null,
                    Collections.singletonList(result)));
        }
    }

    @Override
    protected void executeCallbacks(boolean success,
            ActionFlowState flowState,
            List<IFrontendMultipleActionAsyncCallback> callbacks) {
        executeCallbacks(success, flowState, callbacks, false);
    }

    private List<IFrontendMultipleActionAsyncCallback> verifyRunAction(int exepectedNumOfRunActionExecutions,
            boolean waitForResult) {
        verify(frontend, times(exepectedNumOfRunActionExecutions)).runMultipleAction(eq(ACTION_TYPE),
                anyListOf(VdcActionParametersBase.class),
                callbackCaptor.capture(),
                eq(false),
                eq(waitForResult));

        List<IFrontendMultipleActionAsyncCallback> callbacks = callbackCaptor.getAllValues();
        return callbacks;
    }

    @Override
    protected List<IFrontendMultipleActionAsyncCallback> verifyRunAction(int exepectedNumOfRunActionExecutions) {
        return verifyRunAction(exepectedNumOfRunActionExecutions, false);
    }
}
