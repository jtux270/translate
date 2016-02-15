package org.ovirt.engine.ui.uicommonweb.models.tags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.ovirt.engine.core.common.action.TagsActionParametersBase;
import org.ovirt.engine.core.common.action.TagsOperationParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.Tags;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeModel;
import org.ovirt.engine.ui.uicommonweb.models.common.SelectionTreeNodeModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.EventDefinition;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

@SuppressWarnings("unused")
public class TagListModel extends SearchableListModel
{

    public static final EventDefinition resetRequestedEventDefinition;
    private Event privateResetRequestedEvent;

    public Event getResetRequestedEvent()
    {
        return privateResetRequestedEvent;
    }

    private void setResetRequestedEvent(Event value)
    {
        privateResetRequestedEvent = value;
    }

    private UICommand privateNewCommand;

    public UICommand getNewCommand()
    {
        return privateNewCommand;
    }

    private void setNewCommand(UICommand value)
    {
        privateNewCommand = value;
    }

    private UICommand privateEditCommand;

    @Override
    public UICommand getEditCommand()
    {
        return privateEditCommand;
    }

    private void setEditCommand(UICommand value)
    {
        privateEditCommand = value;
    }

    private UICommand privateRemoveCommand;

    public UICommand getRemoveCommand()
    {
        return privateRemoveCommand;
    }

    private void setRemoveCommand(UICommand value)
    {
        privateRemoveCommand = value;
    }

    private UICommand privateResetCommand;

    public UICommand getResetCommand()
    {
        return privateResetCommand;
    }

    private void setResetCommand(UICommand value)
    {
        privateResetCommand = value;
    }

    @Override
    public TagModel getSelectedItem()
    {
        return (TagModel) super.getSelectedItem();
    }

    public void setSelectedItem(TagModel value)
    {
        super.setSelectedItem(value);
    }

    @Override
    public Collection getItems()
    {
        return items;
    }

    @Override
    public void setItems(Collection value)
    {
        if (items != value)
        {
            itemsChanging(value, items);
            items = value;
            itemsChanged();
            getItemsChangedEvent().raise(this, EventArgs.EMPTY);
            onPropertyChanged(new PropertyChangedEventArgs("Items")); //$NON-NLS-1$
        }
    }

    private ArrayList<SelectionTreeNodeModel> selectionNodeList;

    public ArrayList<SelectionTreeNodeModel> getSelectionNodeList()
    {
        return selectionNodeList;
    }

    public void setSelectionNodeList(ArrayList<SelectionTreeNodeModel> value)
    {
        if (selectionNodeList != value)
        {
            selectionNodeList = value;
            onPropertyChanged(new PropertyChangedEventArgs("SelectionNodeList")); //$NON-NLS-1$
        }
    }

    private Map<Guid, Boolean> attachedTagsToEntities;

    public Map<Guid, Boolean> getAttachedTagsToEntities()
    {
        return attachedTagsToEntities;
    }

    public void setAttachedTagsToEntities(Map<Guid, Boolean> value)
    {
        if (attachedTagsToEntities != value)
        {
            attachedTagsToEntities = value;
            attachedTagsToEntitiesChanged();
            onPropertyChanged(new PropertyChangedEventArgs("AttachedTagsToEntities")); //$NON-NLS-1$
        }
    }

    static
    {
        resetRequestedEventDefinition = new EventDefinition("ResetRequested", SystemTreeModel.class); //$NON-NLS-1$
    }

    public TagListModel()
    {
        setResetRequestedEvent(new Event(resetRequestedEventDefinition));

        setNewCommand(new UICommand("New", this)); //$NON-NLS-1$
        setEditCommand(new UICommand("Edit", this)); //$NON-NLS-1$
        setRemoveCommand(new UICommand("Remove", this)); //$NON-NLS-1$
        setResetCommand(new UICommand("Reset", this)); //$NON-NLS-1$

        setIsTimerDisabled(true);

        getSearchCommand().execute();

        updateActionAvailability();

        // Initialize SelectedItems property with empty collection.
        setSelectedItems(new ArrayList<TagModel>());

        setSelectionNodeList(new ArrayList<SelectionTreeNodeModel>());
    }

    @Override
    protected void syncSearch()
    {
        super.syncSearch();

        AsyncDataProvider.getRootTag(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {

                        TagListModel tagListModel = (TagListModel) target;
                        TagModel rootTag =
                                tagListModel.tagToModel((Tags) returnValue);
                        rootTag.getName().setEntity(ConstantsManager.getInstance().getConstants().rootTag());
                        rootTag.setType(TagModelType.Root);
                        rootTag.setIsChangable(false);
                        tagListModel.setItems(new ArrayList<TagModel>(Arrays.asList(new TagModel[] {rootTag})));

                    }
                }));
    }

    @Override
    protected void itemsChanged()
    {
        super.itemsChanged();

        if (getSelectionNodeList() != null && getSelectionNodeList().isEmpty() && getAttachedTagsToEntities() != null)
        {
            attachedTagsToEntitiesChanged();
        }
    }

    protected void attachedTagsToEntitiesChanged()
    {
        ArrayList<TagModel> tags = (ArrayList<TagModel>) getItems();

        if (tags != null)
        {
            TagModel root = tags.get(0);

            if (getAttachedTagsToEntities() != null)
            {
                recursiveSetSelection(root, getAttachedTagsToEntities());
            }

            if (getSelectionNodeList().isEmpty())
            {
                setSelectionNodeList(new ArrayList<SelectionTreeNodeModel>(Arrays.asList(new SelectionTreeNodeModel[] { createTree(root) })));
            }
        }
    }

    public void recursiveSetSelection(TagModel tagModel, Map<Guid, Boolean> attachedEntities)
    {
        if (attachedEntities.containsKey(tagModel.getId()) && attachedEntities.get(tagModel.getId()))
        {
            tagModel.setSelection(true);
        }
        else
        {
            tagModel.setSelection(false);
        }
        if (tagModel.getChildren() != null)
        {
            for (TagModel subModel : tagModel.getChildren())
            {
                recursiveSetSelection(subModel, attachedEntities);
            }
        }
    }

    public SelectionTreeNodeModel createTree(TagModel tag)
    {
        SelectionTreeNodeModel node = new SelectionTreeNodeModel();
        node.setDescription(tag.getName().getEntity().toString());
        node.setIsSelectedNullable(tag.getSelection());
        node.setIsChangable(tag.getIsChangable());
        node.setIsSelectedNotificationPrevent(true);
        node.setEntity(tag);
        node.getPropertyChangedEvent().addListener(this);

        if (tag.getChildren().isEmpty())
        {
            getSelectionNodeList().add(node);
            return node;
        }

        for (TagModel childTag : tag.getChildren())
        {
            SelectionTreeNodeModel childNode = createTree(childTag);
            childNode.setParent(node);
            node.getChildren().add(childNode);
        }

        return node;
    }

    public TagModel cloneTagModel(TagModel tag)
    {
        ArrayList<TagModel> children = new ArrayList<TagModel>();
        for (TagModel child : tag.getChildren())
        {
            children.add(cloneTagModel(child));
        }

        TagModel model = new TagModel();
        model.setId(tag.getId());
        model.setName(tag.getName());
        model.setDescription(tag.getDescription());
        model.setType(tag.getType());
        model.setSelection(tag.getSelection());
        model.setParentId(tag.getParentId());
        model.setChildren(children);
        model.getSelectionChangedEvent().addListener(this);

        for (TagModel child : children) {
            child.setParent(model);
        }

        return model;
    }

    public TagModel tagToModel(Tags tag)
    {
        EntityModel tempVar = new EntityModel();
        tempVar.setEntity(tag.gettag_name());
        EntityModel name = tempVar;
        EntityModel tempVar2 = new EntityModel();
        tempVar2.setEntity(tag.getdescription());
        EntityModel description = tempVar2;

        ArrayList<TagModel> children = new ArrayList<TagModel>();
        for (Tags a : tag.getChildren())
        {
            children.add(tagToModel(a));
        }

        TagModel model = new TagModel();
        model.setId(tag.gettag_id());
        model.setName(name);
        model.setDescription(description);
        model.setType((tag.getIsReadonly() == null ? false : tag.getIsReadonly()) ? TagModelType.ReadOnly
                : TagModelType.Regular);
        model.setSelection(false);
        model.setParentId(tag.getparent_id() == null ? Guid.Empty : tag.getparent_id());
        model.setChildren(children);

        for (TagModel child : children) {
            child.setParent(model);
        }

        model.getSelectionChangedEvent().addListener(this);

        return model;
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args)
    {
        super.eventRaised(ev, sender, args);

        if (ev.matchesDefinition(TagModel.selectionChangedEventDefinition))
        {
            onTagSelectionChanged(sender, args);
        }
    }

    @Override
    protected void entityPropertyChanged(Object sender, PropertyChangedEventArgs e)
    {
        super.entityPropertyChanged(sender, e);
        if (e.propertyName.equals("IsSelectedNullable")) //$NON-NLS-1$
        {
            SelectionTreeNodeModel selectionTreeNodeModel = (SelectionTreeNodeModel) sender;
            TagModel tagModel = (TagModel) selectionTreeNodeModel.getEntity();

            tagModel.setSelection(selectionTreeNodeModel.getIsSelectedNullable());
            onTagSelectionChanged(tagModel, e);
        }
    }

    private void onTagSelectionChanged(Object sender, EventArgs e)
    {
        TagModel model = (TagModel) sender;

        ArrayList<TagModel> list = new ArrayList<TagModel>();
        if (getSelectedItems() != null)
        {
            for (Object item : getSelectedItems())
            {
                list.add((TagModel) item);
            }
        }

        if ((model.getSelection() == null ? false : model.getSelection()))
        {
            list.add(model);
        }
        else
        {
            list.remove(model);
        }

        setSelectedItems(list);
    }

    private void reset()
    {
        setSelectedItems(new ArrayList<TagModel>());

        if (getItems() != null)
        {
            for (Object item : getItems())
            {
                resetInternal((TagModel) item);
            }
        }

        // Async tag search will cause tree selection to be cleared
        // Search();

        getResetRequestedEvent().raise(this, EventArgs.EMPTY);
    }

    private void resetInternal(TagModel root)
    {
        root.setSelection(false);
        for (TagModel item : root.getChildren())
        {
            resetInternal(item);
        }
    }

    public void remove()
    {
        if (getWindow() != null)
        {
            return;
        }

        ConfirmationModel model = new ConfirmationModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().removeTagsTitle());
        model.setHelpTag(HelpTag.remove_tag);
        model.setHashName("remove_tag"); //$NON-NLS-1$

        ArrayList<String> items = new ArrayList<String>();
        items.add(getSelectedItem().getName().getEntity());
        model.setItems(items);

        model.setNote(ConstantsManager.getInstance().getConstants().noteRemovingTheTagWillAlsoRemoveAllItsDescendants());

        UICommand tempVar = new UICommand("OnRemove", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    public void onRemove()
    {
        ConfirmationModel model = (ConfirmationModel) getWindow();

        if (model.getProgress() != null)
        {
            return;
        }

        model.startProgress(null);

        Frontend.getInstance().runAction(VdcActionType.RemoveTag, new TagsActionParametersBase(getSelectedItem().getId()),
                new IFrontendActionAsyncCallback() {
                    @Override
                    public void executed(FrontendActionAsyncResult result) {

                        TagListModel tagListModel = (TagListModel) result.getState();
                        VdcReturnValueBase returnVal = result.getReturnValue();
                        boolean success = returnVal != null && returnVal.getSucceeded();
                        if (success)
                        {
                            tagListModel.getSearchCommand().execute();
                        }
                        tagListModel.cancel();
                        tagListModel.stopProgress();

                    }
                }, this);
    }

    public void edit()
    {
        if (getWindow() != null)
        {
            return;
        }

        TagModel model = new TagModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().editTagTitle());
        model.setHelpTag(HelpTag.edit_tag);
        model.setHashName("edit_tag"); //$NON-NLS-1$
        model.setIsNew(false);
        model.getName().setEntity(getSelectedItem().getName().getEntity());
        model.getDescription().setEntity(getSelectedItem().getDescription().getEntity());
        model.setParent(getSelectedItem());
        model.setParentId(getSelectedItem().getParentId());

        UICommand tempVar = new UICommand("OnSave", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    public void newEntity()
    {
        if (getWindow() != null)
        {
            return;
        }

        TagModel model = new TagModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().newTagTitle());
        model.setHelpTag(HelpTag.new_tag);
        model.setHashName("new_tag"); //$NON-NLS-1$
        model.setIsNew(true);

        UICommand tempVar = new UICommand("OnSave", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    public void onSave()
    {
        TagModel model = (TagModel) getWindow();

        if (model.getProgress() != null)
        {
            return;
        }

        if (!model.validate())
        {
            return;
        }

        Tags tempVar =
                new Tags();
        tempVar.settag_id(model.getIsNew() ? Guid.Empty : getSelectedItem().getId());
        tempVar.setparent_id(model.getIsNew() ? getSelectedItem().getId() : getSelectedItem().getParentId());
        tempVar.settag_name(model.getName().getEntity());
        tempVar.setdescription(model.getDescription().getEntity());
        Tags tag = tempVar;

        model.startProgress(null);

        Frontend.getInstance().runAction(model.getIsNew() ? VdcActionType.AddTag : VdcActionType.UpdateTag,
                new TagsOperationParameters(tag),
                new IFrontendActionAsyncCallback() {
                    @Override
                    public void executed(FrontendActionAsyncResult result) {

                        TagListModel localModel = (TagListModel) result.getState();
                        localModel.postOnSave(result.getReturnValue());

                    }
                },
                this);
    }

    public void postOnSave(VdcReturnValueBase returnValue)
    {
        TagModel model = (TagModel) getWindow();

        model.stopProgress();

        if (returnValue != null && returnValue.getSucceeded())
        {
            cancel();
            getSearchCommand().execute();
        }
    }

    public void cancel()
    {
        setWindow(null);
    }

    @Override
    protected void onSelectedItemChanged()
    {
        super.onSelectedItemChanged();
        updateActionAvailability();
    }

    private void updateActionAvailability()
    {
        getNewCommand().setIsExecutionAllowed(getSelectedItem() != null);
        getEditCommand().setIsExecutionAllowed(getSelectedItem() != null
                && getSelectedItem().getType() == TagModelType.Regular);
        getRemoveCommand().setIsExecutionAllowed(getSelectedItem() != null
                && getSelectedItem().getType() == TagModelType.Regular);
    }

    @Override
    public void executeCommand(UICommand command)
    {
        super.executeCommand(command);

        if (command == getResetCommand())
        {
            reset();
        }
        else if (command == getNewCommand())
        {
            newEntity();
        }
        else if (command == getEditCommand())
        {
            edit();
        }
        else if (command == getRemoveCommand())
        {
            remove();
        }
        else if ("Cancel".equals(command.getName())) //$NON-NLS-1$
        {
            cancel();
        }
        else if ("OnSave".equals(command.getName())) //$NON-NLS-1$
        {
            onSave();
        }
        else if ("OnRemove".equals(command.getName())) //$NON-NLS-1$
        {
            onRemove();
        }
    }

    @Override
    protected String getListName() {
        return "TagListModel"; //$NON-NLS-1$
    }
}
