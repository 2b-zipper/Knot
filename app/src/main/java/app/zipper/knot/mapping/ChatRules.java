package app.zipper.knot.mapping;

import static app.zipper.knot.mapping.Finder.sig;

import app.zipper.knot.LineVersion;
import java.lang.reflect.Modifier;
import java.util.List;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

final class ChatRules {

  static final Mappings.Section[] SECTIONS = {
    ChatRules::search,
    ChatRules::searchQuery,
    ChatRules::searchResults,
    ChatRules::editSelectAll,
    ChatRules::messageMenu,
    ChatRules::plusMenu,
    ChatRules::chatListMoreMenu,
    ChatRules::misc
  };

  private ChatRules() {}

  private static void search(Finder f, LineVersion.Config c) {
    c.chat.searchBoxEditTextField = f.fieldOf(c.chat.searchBoxViewClass, "android.widget.EditText");
    c.chat.searchBoxIconField = f.fieldOf(c.chat.searchBoxViewClass, "android.widget.ImageView");

    c.chat.searchHeaderHelperClass = f.classUsing("postKeywordChangedEvent(Ljava/lang/String;)V");
    String header = c.chat.searchHeaderHelperClass;
    c.chat.searchHeaderShowMethod =
        f.method(header, MethodMatcher.create().addUsingField(FieldMatcher.create().name("TRUE")));
    c.chat.searchHeaderViewField = f.fieldOf(header, "android.view.View");
    c.chat.searchHeaderEventBusField = f.fieldOf(header, c.eventBus.busClass);
    c.chat.searchHeaderControllerField =
        f.field(
            header,
            FieldMatcher.create()
                .type(
                    ClassMatcher.create()
                        .modifiers(Modifier.INTERFACE)
                        .addMethod(MethodMatcher.create().name("isVisible"))
                        .addMethod(sig("android.view.View"))));
    c.chat.searchControllerSearchBoxMethod =
        f.methodOf(f.fieldType(header, c.chat.searchHeaderControllerField), "android.view.View");

    c.chat.searchKeywordEventClass = f.classUsing("SearchInChatKeywordChangedEvent(keyword=");
    c.chat.searchKeywordEventKeywordField =
        f.fieldOf(c.chat.searchKeywordEventClass, "java.lang.String");
    c.chat.searchKeywordTypeClass =
        f.clsIn(
            Finder.packageOf(f.classUsing("MainChatMemberSearchRequestParam(entireMemberList=")),
            ClassMatcher.create()
                .addField(FieldMatcher.create().name("minimumAcceptableKeywordLength")));
    c.chat.searchKeywordTypeMethod =
        f.methodOf(c.chat.searchKeywordTypeClass, "boolean", "java.lang.String");

    c.chat.searchPresenterClass = f.classUsing("'Loading' cannot be selected.");
    c.chat.searchPresenterKeywordSubjectField =
        f.fieldReadIn(c.chat.searchPresenterClass, c.chat.searchPresenterKeywordChangedMethod);
    c.chat.searchKeywordSubjectValueMethod =
        f.methodOf(
            f.fieldType(c.chat.searchPresenterClass, c.chat.searchPresenterKeywordSubjectField),
            "java.lang.Object");
  }

  private static void searchQuery(Finder f, LineVersion.Config c) {
    c.chat.searchFtsInChatQueryClass =
        f.classUsing(
            "\n        SELECT fts_message.rowid, fts_message.formatted_message, message_chat_relation.chat_id\n        FROM fts_message\n        CROSS JOIN message_chat_relation\n            ON fts_message.rowid = message_chat_relation.message_id\n        WHERE fts_message.formatted_message MATCH ?\n            AND message_chat_relation.chat_id = ?\n        ORDER BY fts_message.rowid DESC\n        LIMIT ?\n        ");
    String query = c.chat.searchFtsInChatQueryClass;
    c.chat.searchFtsQueryField = f.ctorField(query, 0, "java.lang.String");
    c.chat.searchFtsChatIdField = f.ctorField(query, 1, "java.lang.String");
    c.chat.searchFtsLimitField = f.fieldOf(query, "int");

    MethodData invoke = f.methodData(query, MethodMatcher.create().name("invoke"));
    for (MethodData call : Finder.invokes(invoke)) {
      List<String> p = call.getParamTypeNames();
      if (p.size() == 1 && p.get(0).equals("java.lang.String")) {
        if (c.chat.searchFtsPrepareMethod.isEmpty()) c.chat.searchFtsPrepareMethod = call.getName();
      } else if (p.size() == 2 && p.get(0).equals("int") && p.get(1).equals("java.lang.String")) {
        c.chat.searchFtsBindTextMethod = call.getName();
      } else if (p.isEmpty() && call.getReturnTypeName().equals("boolean")) {
        c.chat.searchFtsStepMethod = call.getName();
      }
    }
  }

  private static void searchResults(Finder f, LineVersion.Config c) {
    c.chat.searchResultClass = f.classUsing("MessageSearchInChatResult(chatId=");
    c.chat.searchResultCountField = f.fieldOf(c.chat.searchResultClass, "int");
    c.chat.searchResultWrapperClass = f.classUsing("SearchInChatResult(chatId=");
    c.chat.searchResultWrapperResultOptionalField =
        f.propertyField(c.chat.searchResultWrapperClass, "messageSearchResult");

    c.chat.searchResultHeaderItemClass = f.classUsing("Title(titleText=");
    c.chat.searchResultHeaderTitleField =
        f.fieldOf(c.chat.searchResultHeaderItemClass, "java.lang.String");
    c.chat.searchResultHeaderCountField = f.fieldOf(c.chat.searchResultHeaderItemClass, "int");

    // "+" between U+202D and U+202C
    String ltr = "‭+‬";
    c.chat.searchResultTitleViewHolderClass = f.classUsing(ltr);
    String title = c.chat.searchResultTitleViewHolderClass;
    c.chat.searchResultTitleBindMethod =
        f.method(title, MethodMatcher.create().usingEqStrings(ltr));
    c.chat.searchResultTitleBindingField = f.fieldReadIn(title, c.chat.searchResultTitleBindMethod);
    c.chat.searchResultTitleTextViewField =
        f.fieldOf(
            f.fieldType(title, c.chat.searchResultTitleBindingField), "android.widget.TextView");
  }

  private static void editSelectAll(Finder f, LineVersion.Config c) {
    String state = c.message.selectionStateClass;
    c.chatEditSelectAll.methodGetSelectedIds = f.methodOf(state, "java.util.Set");
    c.chatEditSelectAll.methodToggleItem = f.methodOf(state, "void", (String) null);
    String item = f.paramType(state, c.chatEditSelectAll.methodToggleItem, 0);
    c.chatEditSelectAll.methodIsItemSelected = f.method(state, sig("boolean", item));
    c.chatEditSelectAll.methodGetSelectionState =
        f.methodOf(c.message.selectionProviderClass, state);
    // Of the two (int) -> item getters, the one falling back to a placeholder item
    c.chatEditSelectAll.methodGetItem =
        f.methodNameAnywhere(
            MethodMatcher.create()
                .declaredClass(ClassMatcher.create().addInterface(c.message.selectionProviderClass))
                .returnType(item)
                .paramTypes("int")
                .addUsingField(
                    FieldMatcher.create().declaredClass(item + "$", StringMatchType.StartsWith)));
  }

  private static void messageMenu(Finder f, LineVersion.Config c) {
    c.messageEditHistory.editRequestClass = f.classUsing(", editedChatHistoryParameters=");
    c.messageEditHistory.editRequestIdField =
        f.propertyField(c.messageEditHistory.editRequestClass, "serverMessageId");
    c.messageEditHistory.editRequestTextField =
        f.propertyField(c.messageEditHistory.editRequestClass, "editedMessageContent");

    c.messageEditHistory.menuItemEnumClass =
        f.clsIn(
            Finder.packageOf(f.classUsing("ContextMenuMessageViewData(messageViewType=")),
            ClassMatcher.create()
                .superClass("java.lang.Enum")
                .addField(FieldMatcher.create().name("UNSEND_SQUARE_MESSAGE"))
                .addField(FieldMatcher.create().name("PROCESS_TEXT"))
                .addNoneOf(
                    ClassMatcher.create().addField(FieldMatcher.create().name("buttonAction"))));
    c.messageEditHistory.menuPresentationEnumClass =
        f.classUsing(
            ClassMatcher.create().addField(FieldMatcher.create().name("buttonAction")),
            "UNSEND_SQUARE_MESSAGE");
    String presentation = c.messageEditHistory.menuPresentationEnumClass;
    c.messageEditHistory.methodMenuIcon =
        f.method(
            presentation,
            MethodMatcher.create()
                .paramCount(0)
                .addUsingField(FieldMatcher.create().name("contextIconRes")));
    c.messageEditHistory.methodMenuLabel =
        f.method(
            presentation,
            sig("java.lang.String", "android.content.Context")
                .addUsingField(FieldMatcher.create().name("contextButtonTextRes")));

    String action = f.fieldType(presentation, "buttonAction");
    c.messageEditHistory.methodMenuActionAccessor = f.method(presentation, sig(action));
    c.messageEditHistory.menuActionLambdaClass =
        f.cls(
            ClassMatcher.create()
                .superClass(action)
                .addField(
                    FieldMatcher.create()
                        .type(
                            ClassMatcher.create()
                                .addInterface(f.interfaceOf(c.kotlin.function0Class))
                                .addMethod(
                                    sig(
                                        "java.lang.Object",
                                        "java.lang.Object",
                                        "java.lang.Object",
                                        "java.lang.Object")))));

    String params = f.classUsing("ContextMenuDialogParams(messageViewType=");
    c.messageEditHistory.menuListBuilderClass =
        f.clsIn(
            Finder.packageOf(presentation),
            ClassMatcher.create()
                .addMethod(
                    sig("java.util.List", null, null, params)
                        .addUsingField(FieldMatcher.create().name("DELETE"))));
    c.messageEditHistory.menuListMethod =
        f.method(
            c.messageEditHistory.menuListBuilderClass, sig("java.util.List", null, null, params));

    String context =
        f.paramType(
            c.messageEditHistory.menuListBuilderClass, c.messageEditHistory.menuListMethod, 2);
    c.messageEditHistory.menuContextMessageField = f.propertyField(context, "messageViewData");
    String message = f.fieldType(context, c.messageEditHistory.menuContextMessageField);
    c.messageEditHistory.menuMessageDataField =
        f.propertyField(message, "chatMessageCommonViewData");
    String data = f.fieldType(message, c.messageEditHistory.menuMessageDataField);
    c.messageEditHistory.menuMessageIdField = f.propertyField(data, "serverMessageId");
    c.messageEditHistory.menuEditedFlagField = f.propertyField(data, "isEditedMessage");
  }

  private static void plusMenu(Finder f, LineVersion.Config c) {
    c.plusMenu.plusMenuCallbackClass = c.kotlin.function0Class;
    c.plusMenu.plusMenuComponentClass =
        f.classUsingIdIn(
            Finder.packageOf(f.classUsing("MARK_ALL_AS_READ")),
            "drawable",
            "chat_tab_ui_header_plusmenu_addchat");
    c.plusMenu.methodAddMenuItem =
        f.method(
            c.plusMenu.plusMenuComponentClass,
            MethodMatcher.create().usingEqStrings("invalid weight; must be greater than zero"));
    // The composable's parameter order changes between versions; the count does not
    c.plusMenu.methodCreateMenu =
        f.method(
            c.plusMenu.plusMenuComponentClass,
            MethodMatcher.create().returnType("void").modifiers(Modifier.STATIC).paramCount(6));
  }

  private static void chatListMoreMenu(Finder f, LineVersion.Config c) {
    c.chatListMoreMenu.clickListenerClass =
        f.clsIn(
            Finder.packageOf(f.classUsing("CreateChatButtonStatus(visibility=")),
            ClassMatcher.create()
                .addInterface("android.widget.AdapterView$OnItemClickListener")
                .addMethod(
                    MethodMatcher.create().name("<init>").paramTypes(null, "java.util.List")));
    c.chatListMoreMenu.fieldListView =
        f.fieldOf(c.chatListMoreMenu.popupListViewClass, "android.widget.ListView");
    c.chatListMoreMenu.methodAddItem =
        f.methodOf(
            c.chatListMoreMenu.popupListViewClass, "void", "java.lang.String", "int", "boolean");
    c.chatListMoreMenu.popupListAdapterClass =
        f.clsIn(
            "jp.naver.line.android.common.view.listview",
            ClassMatcher.create()
                .superClass("android.widget.BaseAdapter")
                .addMethod(
                    MethodMatcher.create().addUsingField(FieldMatcher.create().name("second"))));
    c.chatListMoreMenu.fieldPopupItems =
        f.fieldOf(c.chatListMoreMenu.popupListAdapterClass, "java.util.ArrayList");
  }

  private static void misc(Finder f, LineVersion.Config c) {
    String message = f.classUsing("Message(createdTimeMillis=");
    c.chatTimestamp.displayTimeInterface = f.interfaceOf(message);
    c.chatTimestamp.methodCreatedMillis =
        f.getter(message, f.propertyField(message, "createdTimeMillis"));

    // R8 keeps the setters of View subclasses
    c.keywordAssist.toggleViewClass =
        f.cls(
            ClassMatcher.create()
                .superClass("android.widget.FrameLayout")
                .addMethod(MethodMatcher.create().name("setOnPopupAction")));
  }
}
