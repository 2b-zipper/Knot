package app.zipper.knot.mapping;

import static app.zipper.knot.mapping.Finder.sig;

import app.zipper.knot.LineVersion;
import java.lang.reflect.Modifier;
import java.util.List;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

final class MessageRules {

  static final Mappings.Section[] SECTIONS = {
    MessageRules::muteMessage,
    MessageRules::readReceipt,
    MessageRules::userAgent,
    MessageRules::messageRow,
    MessageRules::incomingMessage,
    MessageRules::operations
  };

  private MessageRules() {}

  private static void muteMessage(Finder f, LineVersion.Config c) {
    c.muteMessage.labFeatureClass = f.classUsing("popup_chat_folder.png");
    c.muteMessage.methodIsFeatureEnabled =
        f.method(
            c.muteMessage.labFeatureClass,
            MethodMatcher.create().addInvoke("Ljava/lang/Boolean;->booleanValue()Z"));
    c.muteMessage.silentMessageFeatureClass =
        f.classUsing("https://contact-cc.line.me/labs/detailId/13533");

    c.muteMessage.sendModeEnumClass =
        f.classUsing(
            ClassMatcher.create().addField(FieldMatcher.create().name("isMuteMessageEnabled")),
            "LAB",
            "PREMIUM");
    c.muteMessage.sendModeClass =
        f.cls(
            ClassMatcher.create()
                .addMethod(sig(c.muteMessage.sendModeEnumClass))
                .methodCount(2)
                .fieldCount(1));
    c.muteMessage.methodSendMode =
        f.method(
            c.muteMessage.sendModeClass,
            MethodMatcher.create().addUsingField(FieldMatcher.create().name("PREMIUM")));
  }

  private static void readReceipt(Finder f, LineVersion.Config c) {
    // The chat list controller; mark-as-read from its long-press menu runs through it
    c.readReceipt.longPressReadClass =
        Finder.packageOf(
            f.cls(
                ClassMatcher.create()
                    .usingEqStrings("ChatListLoadingStatLogger", "prefetchMention")));

    c.readReceipt.methodReact =
        f.methodOf(c.readReceipt.reactClientClass, null, f.classUsing("ReactRequest(reqSeq:"));
    c.readReceipt.reactRequestMessageIdField =
        f.fieldOf(
            f.paramType(c.readReceipt.reactClientClass, c.readReceipt.methodReact, 0), "long");
    c.readReceipt.reactSuccessResultClass = f.classUsing("Result(result=");

    c.readReceipt.readReceiptManagerClass =
        f.classAmong(
            Finder.constructs(
                f.methodData(
                    c.notification.accessServiceClass,
                    MethodMatcher.create().name("onStartCommand"))),
            ClassMatcher.create()
                .addField(
                    FieldMatcher.create()
                        .type("jp.naver.line.android.thrift.client.TalkServiceClient"))
                .addMethod(
                    MethodMatcher.create().name("<init>").paramTypes("android.content.Context")));
    String manager = c.readReceipt.readReceiptManagerClass;
    c.readReceipt.methodExecuteReadReceiptAsync =
        f.method(
            manager,
            sig(null, "java.lang.String").addNoneOf(MethodMatcher.create().returnType("long")));
    c.readReceipt.methodReadAll = f.methodOf(manager, "java.util.List");
    c.readReceipt.methodResolveReadTarget = f.methodOf(manager, "long", "java.lang.String");
    c.readReceipt.methodSendReadReceipt =
        f.method(
            manager,
            MethodMatcher.create().addUsingField(FieldMatcher.create().name("PROCESSING")));
  }

  private static void userAgent(Finder f, LineVersion.Config c) {
    c.unsend.appInfoProviderClass = f.classUsing("LA/UK");
    String provider = c.unsend.appInfoProviderClass;
    c.unsend.methodGetFullUserAgentWithContext =
        f.method(provider, MethodMatcher.create().usingEqStrings("	Android OS	"));
    c.unsend.methodGetSimpleUserAgentWithContext =
        f.method(provider, MethodMatcher.create().usingEqStrings("LA/UK"));
    c.unsend.methodGetFullUserAgent =
        f.method(
            provider,
            MethodMatcher.create()
                .addInvoke(
                    MethodMatcher.create()
                        .declaredClass(provider)
                        .name(c.unsend.methodGetFullUserAgentWithContext)));
    c.unsend.methodGetSimpleUserAgent =
        f.method(
            provider,
            MethodMatcher.create()
                .addInvoke(
                    MethodMatcher.create()
                        .declaredClass(provider)
                        .name(c.unsend.methodGetSimpleUserAgentWithContext)));
  }

  private static void messageRow(Finder f, LineVersion.Config c) {
    c.unsend.carouselAdapterClass =
        f.classUsingId(
            MethodMatcher.create().name("onCreateViewHolder"),
            "id",
            "chat_ui_row_carousel_image_check_box");

    c.unsend.chatMessageViewHolderClass =
        f.classUsing("Total message count should not less than 10");
    String holder = c.unsend.chatMessageViewHolderClass;
    c.unsend.methodGetItemView =
        f.method(
            holder,
            sig("android.view.View")
                .addUsingField(
                    FieldMatcher.create()
                        .declaredClass(c.androidx.viewHolderClass)
                        .type("android.view.View")));
    c.unsend.methodBind =
        f.methodOf(holder, "boolean", c.message.selectionProviderClass, null, null);
    String row = f.paramType(holder, c.unsend.methodBind, c.unsend.methodBindIndex);
    c.unsend.methodGetCommonData =
        f.methodOf(row, f.classUsing("ChatMessageCommonViewData(chatId="));
    String common = f.returnType(row, c.unsend.methodGetCommonData);
    c.unsend.chatMessageServerIdLongField = f.propertyField(common, "serverMessageId");
    c.unsend.chatMessageIdField = f.propertyField(common, "validServerMessageId");

    c.unsend.chatServiceConfigClass = f.classUsing(", isMuteMessageGuideFloatingChipEnabled=");
    String config = c.unsend.chatServiceConfigClass;
    c.unsend.methodUnsendLimit =
        f.getter(config, f.propertyField(config, "unsendMessageTimelimit"));
    c.unsend.methodUnsendPremiumLimit =
        f.getter(config, f.propertyField(config, "unsendMessagePremiumTimelimit"));
  }

  private static void incomingMessage(Finder f, LineVersion.Config c) {
    c.unsend.incomingMessageTypeEnumClass =
        f.clsIn(
            Finder.packageOf(f.classUsing("OLD_STICON_MAPPING_LAST_UPDATED_TIME_MILLIS")),
            ClassMatcher.create()
                .addField(FieldMatcher.create().name("UNSENT_NO_MARK"))
                .addField(FieldMatcher.create().name("typeId")));
    String incoming = Finder.outer(c.unsend.incomingMessageTypeEnumClass);
    c.unsend.incomingMessageTypeField = f.fieldOf(incoming, c.unsend.incomingMessageTypeEnumClass);
    // The thrift factory writes the message id right after the type
    List<String> written =
        Finder.writes(
            f.methodData(
                incoming,
                MethodMatcher.create()
                    .modifiers(Modifier.STATIC)
                    .returnType(incoming)
                    .addUsingField(FieldMatcher.create().name("E2EE_UNDECRYPTED"))),
            incoming);
    c.unsend.incomingMessageIdField = Finder.after(written, c.unsend.incomingMessageTypeField);
    c.unsend.incomingMessageParamsField = f.fieldOf(incoming, c.message.processorMetadataClass);
    c.unsend.incomingMessageParamsMapField =
        f.fieldOf(
            f.fieldType(incoming, c.unsend.incomingMessageParamsField), "java.util.LinkedHashMap");

    c.unsend.messageConverterClass =
        f.clsIn(
            Finder.packageOf(c.message.dataManagerClass),
            ClassMatcher.create()
                .addMethod(
                    MethodMatcher.create()
                        .name("<init>")
                        .paramTypes(
                            "android.content.Context",
                            "android.database.sqlite.SQLiteDatabase",
                            null,
                            null,
                            null)));
    c.unsend.methodConvertMessage =
        f.method(
            c.unsend.messageConverterClass,
            MethodMatcher.create()
                .addUsingField(FieldMatcher.create().name("KEY_SILENTLY_UNSENT")));
  }

  // Since 26.17.0 R8 moves a handler's operation type out of its constructor into the registry
  private static void operations(Finder f, LineVersion.Config c) {
    c.unsend.handlerSuccessResultClass = f.classUsing("Success(shouldNotifyListener=");
    String base = Finder.outer(Finder.outer(c.unsend.handlerSuccessResultClass));
    String handlers = Finder.packageOf(c.message.processorClass);
    // NOTIFIED_JOIN_CHAT also buffers TALK_OPERATION reads but holds a dependency
    c.unsend.notifiedReadMessageHandlerClass =
        f.clsIn(
            handlers,
            ClassMatcher.create()
                .superClass(base)
                .fieldCount(0)
                .addMethod(
                    MethodMatcher.create()
                        .addUsingField(FieldMatcher.create().name("TALK_OPERATION"))));
    String readHandler = c.unsend.notifiedReadMessageHandlerClass;
    c.unsend.notifiedSendReactionHandlerClass =
        Finder.outer(
            f.classAnnotatedIn(
                handlers, "jp.naver.line.android.talkop.processor.impl.NOTIFIED_SEND_REACTION"));
    // NOTIFIED_DESTROY_MESSAGE is the default argument; DESTROY_MESSAGE subclasses it
    c.unsend.unsendDestroyHandlerClass =
        f.clsIn(
            handlers,
            ClassMatcher.create()
                .superClass(base)
                .addMethod(
                    MethodMatcher.create()
                        .name("<init>")
                        .addUsingField(FieldMatcher.create().name("NOTIFIED_DESTROY_MESSAGE"))));
    c.unsend.destroyMessageHandlerClass =
        f.clsIn(handlers, ClassMatcher.create().superClass(c.unsend.unsendDestroyHandlerClass));
    c.unsend.methodDestroyHandler =
        f.method(c.unsend.unsendDestroyHandlerClass, MethodMatcher.create().usingEqStrings(""));

    c.unsend.methodReadBuffer =
        f.method(
            readHandler,
            MethodMatcher.create().addUsingField(FieldMatcher.create().name("TALK_OPERATION")));
    String operation = f.paramType(readHandler, c.unsend.methodReadBuffer, 1);
    c.unsend.operationCreatedTimeField = f.thriftField(operation, "createdTime");
    c.unsend.operationTypeField = f.thriftField(operation, "type");
    c.unsend.operationParam1Field = f.thriftField(operation, "param1");
    c.unsend.operationParam2Field = f.thriftField(operation, "param2");
    c.unsend.operationParam3Field = f.thriftField(operation, "param3");
    c.unsend.methodOperationTypeValueOf =
        f.method(
            f.fieldType(operation, c.unsend.operationTypeField),
            MethodMatcher.create()
                .addUsingField(
                    FieldMatcher.create().name("CHANNEL_PAAK_AUTHENTICATION_REQUESTED")));
  }
}
