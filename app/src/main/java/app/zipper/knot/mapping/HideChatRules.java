package app.zipper.knot.mapping;

import static app.zipper.knot.mapping.Finder.sig;

import app.zipper.knot.LineVersion;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

final class HideChatRules {

  static final Mappings.Section[] SECTIONS = {
    HideChatRules::chatList,
    HideChatRules::dialog,
    HideChatRules::chatTab,
    HideChatRules::hidePopup,
    HideChatRules::chatData
  };

  private HideChatRules() {}

  private static void chatList(Finder f, LineVersion.Config c) {
    c.permanentHideChat.chatListRowBinderClass =
        f.clsIn(
            Finder.packageOf(f.classUsing("Unknown checked argument: ")),
            ClassMatcher.create()
                .addMethod(sig("void", null, c.androidx.viewHolderClass, "boolean", null)));
    c.permanentHideChat.methodChatListRowBind =
        f.method(
            c.permanentHideChat.chatListRowBinderClass,
            MethodMatcher.create().addUsingField(FieldMatcher.create().name("APPLY_THEME")));

    c.permanentHideChat.chatListMenuActionClass = c.kotlin.function0Class;
    c.permanentHideChat.chatListMenuItemClass = f.classUsing("LineDialogItem(labelResourceId=");
    c.permanentHideChat.chatListHideItemClass = f.classUsing("ArchiveDialogItemCreator(item=");
    String creator = c.permanentHideChat.chatListHideItemClass;
    c.permanentHideChat.chatListHideItemField = f.propertyField(creator, "item");
    c.permanentHideChat.chatListHideListenerField = f.propertyField(creator, "itemClickListener");
    c.permanentHideChat.methodChatListMenuItem =
        f.methodOf(creator, c.permanentHideChat.chatListMenuItemClass);

    String item = f.fieldType(creator, c.permanentHideChat.chatListHideItemField);
    c.permanentHideChat.methodChatListItemInfo = f.methodOf(item, null);
    c.permanentHideChat.chatListItemChatIdField =
        f.propertyField(f.returnType(item, c.permanentHideChat.methodChatListItemInfo), "chatId");
    c.permanentHideChat.chatListHideConfirmClass =
        f.clsIn(
            Finder.packageOf(
                f.classUsing(
                    "ShowNetworkRequestErrorDialogEvent(showNetworkRequestErrorDialogRequestFlow=")),
            ClassMatcher.create()
                .modifiers(Modifier.INTERFACE)
                .addMethod(sig("void", "android.content.Context", item)));
    c.permanentHideChat.methodConfirmHide =
        f.methodOf(
            c.permanentHideChat.chatListHideConfirmClass, "void", "android.content.Context", null);

    c.permanentHideChat.chatPreviewItemClass = f.classUsing("ChatPreviewItemData(chatId=");
    String preview = c.permanentHideChat.chatPreviewItemClass;
    c.permanentHideChat.chatPreviewChatIdField = f.fieldOf(preview, "java.lang.String");
    c.permanentHideChat.chatMenuHideItemClass =
        Finder.declaring(
            f.methodDataAnywhere(
                MethodMatcher.create()
                    .returnType("int")
                    .paramTypes(preview)
                    .usingNumbers(f.id("string", "chatlist_label_hide"))));
    c.permanentHideChat.methodChatMenuItemLabel =
        f.methodOf(c.permanentHideChat.chatMenuHideItemClass, "int", preview);
  }

  private static void dialog(Finder f, LineVersion.Config c) {
    c.permanentHideChat.dialogBuilderClass =
        f.classUsingIdIn(
            Finder.packageOf(c.permanentHideChat.chatListMenuItemClass),
            "dimen",
            "common_dialog_title_bottom_margin");
    String builder = c.permanentHideChat.dialogBuilderClass;

    // create() shows the title before the message
    List<String> shown =
        Finder.reads(
            f.methodData(
                builder,
                MethodMatcher.create().usingNumbers(f.id("id", "common_dialog_background"))),
            builder);
    int last = -1;
    for (MethodData setter : f.declaredMethods(builder, sig("void", "java.lang.String"))) {
      List<String> field = Finder.writes(setter, builder);
      int at = field.size() == 1 ? shown.indexOf(field.get(0)) : -1;
      if (at > last) {
        last = at;
        c.permanentHideChat.methodDialogMessage = setter.getName();
      }
    }

    c.permanentHideChat.methodDialogPositive =
        f.methodOf(
            builder, "void", "java.lang.String", "android.content.DialogInterface$OnClickListener");
    List<String> positive =
        Finder.writes(
            f.methodData(
                builder, MethodMatcher.create().name(c.permanentHideChat.methodDialogPositive)),
            builder);
    List<String> negative = new ArrayList<>();
    for (MethodData m :
        f.declaredMethods(
            builder, sig("void", "int", "android.content.DialogInterface$OnClickListener"))) {
      if (Collections.disjoint(Finder.writes(m, builder), positive)) negative.add(m.getName());
    }
    c.permanentHideChat.methodDialogNegative = negative.size() == 1 ? negative.get(0) : "";

    c.permanentHideChat.methodDialogShow =
        f.getter(builder, f.field(builder, FieldMatcher.create().type("android.content.Context")));
  }

  private static void chatTab(Finder f, LineVersion.Config c) {
    c.permanentHideChat.chatTabItemClass =
        Finder.outer(
            f.classAnnotatedIn(
                Finder.packageOf(
                    f.classUsing(
                        "onClick(Lcom/linecorp/line/chat/tab/ui/subtab/chat/ActivityModuleSlot;)V")),
                "com.linecorp.line.chat.tab.ui.subtab.chat.ChatListKt$ChatList$1$1"));
    c.permanentHideChat.chatTabRowDataClass =
        f.classUsing("ComposeChatListChatCommonViewData(chatId=");
    String rows = Finder.packageOf(c.permanentHideChat.chatTabRowDataClass);
    c.permanentHideChat.chatTabRowClass =
        f.clsIn(
            rows,
            ClassMatcher.create()
                .modifiers(Modifier.INTERFACE)
                .addMethod(MethodMatcher.create().name("getKey")));
    c.permanentHideChat.methodChatTabItem =
        f.methodTaking(c.permanentHideChat.chatTabItemClass, c.permanentHideChat.chatTabRowClass);
    // The row layout composable's Compose group key; nothing else in it is named
    MethodData rowLayout = f.methodDataIn(rows, MethodMatcher.create().usingNumbers(696642897));
    c.permanentHideChat.chatTabRowLayoutClass = Finder.declaring(rowLayout);
    c.permanentHideChat.methodChatTabRowLayout = Finder.name(rowLayout);

    c.permanentHideChat.updateViewEventClass = f.classUsing("HIDDEN_CHAT_LIST");
    c.permanentHideChat.chatTabBadgeEventClass =
        f.clsIn(
            Finder.packageOf(c.permanentHideChat.updateViewEventClass),
            ClassMatcher.create()
                .fieldCount(1)
                .methodCount(1)
                .addMethod(MethodMatcher.create().name("<clinit>")));
    c.permanentHideChat.chatTabBadgeEventField =
        f.fieldOf(
            c.permanentHideChat.chatTabBadgeEventClass, c.permanentHideChat.chatTabBadgeEventClass);

    // Modifier.alpha is graphicsLayer(alpha = ...) with the default-argument mask 520187
    MethodData alpha =
        f.methodDataAnywhere(
            MethodMatcher.create()
                .modifiers(Modifier.STATIC)
                .paramTypes(null, "float")
                .usingNumbers(520187));
    c.permanentHideChat.alphaModifierClass = Finder.declaring(alpha);
    c.permanentHideChat.methodAlphaModifier = Finder.name(alpha);

    // TextButton(onClick, modifier, content), by its Compose group key
    c.permanentHideChat.textButtonClass =
        Finder.declaring(
            f.methodDataAnywhere(
                sig(
                        "void",
                        c.kotlin.function0Class,
                        null,
                        c.kotlin.function2Class,
                        c.compose.composerClass,
                        "int")
                    .modifiers(Modifier.STATIC)
                    .usingNumbers(-2074112678)));
    c.permanentHideChat.methodTextButton =
        f.method(
            c.permanentHideChat.textButtonClass,
            MethodMatcher.create().addInvoke("Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;"));

    c.permanentHideChat.stringResourceClass =
        f.clsIn(
            Finder.packageOf(
                f.classUsing(
                    "Only VectorDrawables and rasterized asset types are supported ex. PNG, JPG, WEBP")),
            ClassMatcher.create()
                .addMethod(
                    sig(
                        "java.lang.String",
                        "int",
                        "int",
                        "java.lang.Object[]",
                        c.compose.composerClass)));
    c.permanentHideChat.methodStringResource =
        f.staticMethodOf(c.permanentHideChat.stringResourceClass, "java.lang.String", null, null);
  }

  private static void hidePopup(Finder f, LineVersion.Config c) {
    c.permanentHideChat.hidePopupRequestClass =
        f.classUsing("ChatSubTabHideConfirmationPopupRequest(mid=");
    String request = c.permanentHideChat.hidePopupRequestClass;
    c.permanentHideChat.hidePopupChatIdField = f.fieldOf(request, "java.lang.String");
    c.permanentHideChat.hidePopupMessageClass =
        f.clsIn(
            Finder.packageOf(request),
            ClassMatcher.create()
                .addInterface(c.kotlin.function2Class)
                .addMethod(MethodMatcher.create().name("<init>").paramTypes(request)));

    String popups =
        Finder.packageOf(f.classUsing("ChatSubTabDeleteAndReportConfirmationPopupRequest(item="));
    MethodData popup =
        f.creatorIn(
            popups, f.classUsingIdIn(popups, "string", "chatlist_hideconfirmdialog_message"));
    c.permanentHideChat.hidePopupClass = Finder.declaring(popup);
    c.permanentHideChat.methodHidePopup = Finder.name(popup);

    // By a label's Compose group key (R8 may inline the labels); the confirm label comes first
    c.permanentHideChat.hidePopupLabelsClass =
        f.clsIn(
            popups,
            ClassMatcher.create()
                .addMethod(MethodMatcher.create().name("<clinit>").usingNumbers(1236328262)));
    String labels = c.permanentHideChat.hidePopupLabelsClass;
    c.permanentHideChat.hidePopupConfirmLabelField =
        Finder.at(Finder.writes(f.initializer(labels), labels), 0);
  }

  private static void chatData(Finder f, LineVersion.Config c) {
    c.permanentHideChat.chatDaoClass =
        f.classUsing("\n            ORDER BY c.last_created_time DESC\n        ");
    c.permanentHideChat.methodUpdateChatOnMessage =
        f.method(
            c.permanentHideChat.chatDaoClass,
            MethodMatcher.create().usingEqStrings("\n            SET\n                "));
    c.permanentHideChat.methodUpdateArchived =
        f.methodOf(
            c.message.dataManagerClass, "java.lang.Object", null, null, "boolean", "boolean");

    c.permanentHideChat.chatOpenLoaderClass =
        f.classUsing("This function should be used by base chat only.");
    c.permanentHideChat.methodChatOpenLoad =
        f.staticMethodOf(
            c.permanentHideChat.chatOpenLoaderClass,
            "void",
            "android.content.Context",
            null,
            null,
            c.eventBus.busClass,
            null,
            null);
    // The chat screen's loader checks only whether the chat is archived
    String archived =
        Finder.at(
            Finder.calls(
                f.methodData(
                    c.permanentHideChat.chatOpenLoaderClass,
                    MethodMatcher.create().name(c.permanentHideChat.methodChatOpenLoad)),
                c.permanentHideChat.chatDataClass),
            0);
    c.permanentHideChat.methodChatDataArchived =
        f.method(c.permanentHideChat.chatDataClass, sig("boolean").name(archived));

    c.permanentHideChat.squareChatColumnClass =
        f.classUsing("NEW_MEMBER_NOTIFICATION_SETTING_STATE");
    c.permanentHideChat.squareChatDaoClass =
        f.classUsing(
            "\n                    WHERE c.group_mid=?\n                    AND c.is_joined=1\n                    AND c.chat_type=");
    String dao = c.permanentHideChat.squareChatDaoClass;
    c.permanentHideChat.methodGetSquareChat =
        f.method(dao, MethodMatcher.create().usingEqStrings(" WHERE c.chat_mid=?"));
    c.permanentHideChat.methodUpdateSquareChat = f.methodOf(dao, "int", null, null);
    String squareChat = f.paramType(dao, c.permanentHideChat.methodUpdateSquareChat, 0);
    c.permanentHideChat.squareChatIdField = f.propertyField(squareChat, "chatId");
    c.permanentHideChat.squareChatArchiveTypeField = f.propertyField(squareChat, "archivedType");
    c.permanentHideChat.squareArchiveTypeClass =
        f.fieldType(squareChat, c.permanentHideChat.squareChatArchiveTypeField);
    c.permanentHideChat.methodSetSquareArchiveType =
        f.methodOf(dao, "void", "java.lang.String", c.permanentHideChat.squareArchiveTypeClass);
  }
}
