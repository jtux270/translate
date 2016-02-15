package org.ovirt.engine.ui.uicommonweb.action;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SyncUiActionTest extends UiActionBaseTest {

    @Test
    public void runSingleActionTest() {
        UiAction action = createAction();

        action.runAction();
        assertFinishedWithNoErrors(Collections.singletonList(action));
    }

    @Test
    public void runParallelActionFlowTest() {
        List<UiAction> actions = runActionFlow(ActionType.parallel);
        assertFinishedWithNoErrors(actions);

    }

    @Test
    public void runNextActionFlowTest() {
        List<UiAction> actions = runActionFlow(ActionType.next);
        assertFinishedWithNoErrors(actions);
    }

    @Test
    public void runMixedActionFlowTest() {
        List<UiAction> actions = runActionFlow(ActionType.next, ActionType.parallel);
        assertFinishedWithNoErrors(actions);
    }

    @Test
    public void runActionWithFlowStateTest() {
        final UiAction action2 = createAction();
        final UiAction action1 = createAction(new Executer() {

            @Override
            public void onActionExecuted(UiAction action) {
                action2.runParallelAction(action.getActionFlowState());

            }
        });

        action1.runAction();
        assertSame(action1.getActionFlowState(), action2.getActionFlowState());
        assertFinishedWithNoErrors(new ArrayList<UiAction>(Arrays.asList(action1, action2)));
    }

    @Test
    public void finalActionTest() {
        final UiAction action1 = createAction(new Executer() {

            @Override
            public void onActionExecuted(UiAction action) {
                assertNotAllDone(action.getActionFlowState());
            }
        });

        final UiAction action2 = createAction(new Executer() {

            @Override
            public void onActionExecuted(UiAction action) {
                assertNotAllDone(action.getActionFlowState());
            }
        });

        final SimpleAction finalAction = createFinalAction(new ArrayList<UiAction>(Arrays.asList(action1, action2)));

        action1.and(action2).onAllExecutionsFinish(finalAction);
        action1.runAction();
    }

    @Test
    public void shouldExecuteTest() {
        UiAction action = new SyncUiAction(model, "test") { //$NON-NLS-1$
                    @Override
                    protected void onActionExecute() {
                        fail("the action shouln't be executed"); //$NON-NLS-1$
                    }

                    @Override
                    protected boolean shouldExecute() {
                        return false;
                    }
                };

        final SimpleAction finalAction = createFinalAction(Collections.singletonList(action));

        action.onAllExecutionsFinish(finalAction);
        action.runAction();
    }

    @Test
    public void progressWasAlreadyStartedTest() {
        when(model.getProgress()).thenReturn(progressModel);
        UiAction action = createAction();

        action.runAction();
        assertFinishedWithNoErrors(Collections.singletonList(action), false);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void duplicateFinalAction() {
        UiAction action1 = createAction();
        UiAction action2 = createAction();

        SimpleAction finalAction = new SimpleAction() {

            @Override
            public void execute() {
                fail();
            }
        };
        action1.onAllExecutionsFinish(finalAction);
        action2.onAllExecutionsFinish(finalAction);

        action1.then(action2);
        action1.runAction();
    }

    private SimpleAction createFinalAction(final List<UiAction> actions) {
        return new SimpleAction() {

            @Override
            public void execute() {
                assertAllDone(actions);
            }
        };
    }

    protected UiAction createAction() {
        return createAction(null);
    }

    protected UiAction createAction(final Executer executer) {
        UiAction action = new SyncUiAction(model, "test") { //$NON-NLS-1$
                    @Override
                    protected void onActionExecute() {
                        if (executer != null) {
                            executer.onActionExecuted(this);
                        }
                    }
                };

        return action;
    }

    protected static interface Executer {
        void onActionExecuted(UiAction action);
    }
}
