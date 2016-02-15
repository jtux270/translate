package org.ovirt.engine.ui.frontend.communication;

import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.queries.VdcQueryType;

/**
 * This class represents a single operation, which is either a query or an action.
 * @param <T> The type of operation, either {@code VdcQueryType} or {@code VdcActionType}.
 * @param <P> The parameter type for the operation.
 */
public class VdcOperation<T, P> {

    /**
     * The actual operation, can be either {@code VdcQueryType} or {@code VdcActionType}.
     */
    private final T operationType;

    /**
     * The parameter of the operation.
     */
    private final P parameter;

    /**
     * The callback to call when the operation is finished.
     */
    private final VdcOperationCallback<?, ?> operationCallback;

    /**
     * The source object if we used a copy constructor to build this object.
     */
    private final VdcOperation<T, P> source;

    /**
     * Determines if the operation is public or not.
     */
    private final boolean isPublic;

    /**
     * If {@code true}, this operation represents an action. If {@code false}, this operation represents a query.
     */
    private final boolean isAction;

    /**
     * If {@code true}, this operation was part of a list of operations before being split into a single operation.
     */
    private final boolean isFromList;

    /**
     * Private constructor that initializes the final members.
     * @param operation The operation.
     * @param param The parameter for the operation.
     * @param callback The callback to call when the operation is finished.
     * @param sourceOperation If we cloned an operation this is the source it came from.
     * @param isPublicOperation Determines if this operation should be public or not.
     * @param fromList Shows if the operation came from a list of operations, before being split.
     */
    private VdcOperation(final T operation, final P param, final VdcOperationCallback<?, ?> callback,
            final VdcOperation<T, P> sourceOperation, final boolean isPublicOperation, final boolean fromList) {
        if (operation instanceof VdcActionType) {
            this.isAction = true;
        } else if (operation instanceof VdcQueryType) {
            this.isAction = false;
        } else {
            throw new IllegalArgumentException(
                    "Operation type must be either VdcActionType or VdcQueryType"); //$NON-NLS-1$
        }

        this.operationType = operation;
        this.parameter = param;
        this.operationCallback = callback;
        this.source = sourceOperation;
        this.isPublic = isPublicOperation;
        this.isFromList = fromList;
    }

    /**
     * Copy constructor that allows for a different callback.
     * @param sourceOperation The source {@code VdcOperation} object.
     * @param callback The new callback method.
     */
    public VdcOperation(final VdcOperation<T, P> sourceOperation, final VdcOperationCallback<?, ?> callback) {
        this(sourceOperation.getOperation(), sourceOperation.getParameter(), callback, sourceOperation,
                sourceOperation.isPublic(), sourceOperation.isFromList);
    }

    /**
     * Constructor.
     * @param operation The operation to set.
     * @param operationParameter The parameter for the operation.
     * @param callback The callback to call when the operation is finished.
     */
    public VdcOperation(final T operation, final P operationParameter, final VdcOperationCallback<?, ?> callback) {
        this(operation, operationParameter, callback, null, false, false);
    }

    /**
     * Constructor.
     * @param operation The operation to set.
     * @param operationParameter The parameter for the operation.
     * @param fromList Is the operation originally from a list.
     * @param callback The callback to call when the operation is finished.
     */
    public VdcOperation(final T operation, final P operationParameter, final boolean fromList,
            final VdcOperationCallback<?, ?> callback) {
        this(operation, operationParameter, callback, null, false, fromList);
    }

    /**
     * Constructor.
     * @param operation The operation to set.
     * @param operationParameter The parameter for the operation.
     * @param isPublicOperation determines if an operation is public or not. Only applicable for queries, not actions.
     * @param callback The callback to call when the operation is finished.
     */
    public VdcOperation(final T operation, final P operationParameter, final boolean isPublicOperation,
            final boolean fromList, final VdcOperationCallback<?, ?> callback) {
        this(operation, operationParameter, callback, null, isPublicOperation, fromList);
    }

    /**
     * Getter.
     * @return The action associated with the operation
     */
    public T getOperation() {
        return operationType;
    }

    /**
     * Getter.
     * @return The action parameter of the operation
     */
    public P getParameter() {
        return parameter;
    }

    /**
     * Getter.
     * @return The callback to call when the operation completes.
     */
    @SuppressWarnings("rawtypes")
    public VdcOperationCallback getCallback() {
        return operationCallback;
    }

    /**
     * Check if duplicates of this are allowed. If the operation wraps an action
     * duplicates are allowed.
     * @return True if duplicates are allowed, false otherwise.
     */
    public boolean allowDuplicates() {
        return isAction();
    }

    /**
     * Returns the number of times this operation has been copied using the copy constructor.
     * If this is the original returns 1.
     * @return The copy count.
     */
    public int getCopyCount() {
        int result = 1;
        if (source != null) {
            result += source.getCopyCount();
        }
        return result;
    }

    /**
     * Get source object if it exists.
     * @return The source object.
     */
    public VdcOperation<T, P> getSource() {
        return source;
    }

    /**
     * Returns true if the operation does not require the user to be logged in.
     * @return {@code true} if the operation is public, {@code false} otherwise.
     */
    public boolean isPublic() {
        return isPublic;
    }

    /**
     * @return {@code true} if this operation represents an action, {@code false} if this operation represents a query.
     */
    public boolean isAction() {
        return isAction;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((operationType == null) ? 0 : operationType.hashCode());
        result = prime * result + ((parameter == null) ? 0 : parameter.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (getClass() != other.getClass()) {
            return false;
        }
        boolean result = false;
        @SuppressWarnings("unchecked")
        VdcOperation<T, P> otherOperation = (VdcOperation<T, P>) other;
        result = operationType.equals(otherOperation.getOperation())
                    && parameter.equals(otherOperation.getParameter());
        return result;
    }

    public boolean isFromList() {
        return isFromList;
    }

}
