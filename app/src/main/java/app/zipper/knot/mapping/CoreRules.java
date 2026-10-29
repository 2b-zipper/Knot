package app.zipper.knot.mapping;

import static app.zipper.knot.mapping.Finder.sig;

import app.zipper.knot.LineVersion;
import java.lang.reflect.Modifier;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

final class CoreRules {

  static final String CHAT_ACTIVITY =
      "jp.naver.line.android.activity.chathistory.ChatHistoryActivity";

  static final Mappings.Section[] SECTIONS = {
    CoreRules::eventBus,
    CoreRules::services,
    CoreRules::chatActivity,
    CoreRules::message,
    CoreRules::thrift
  };

  private CoreRules() {}

  private static void eventBus(Finder f, LineVersion.Config c) {
    MethodData post =
        f.methodDataIn(
            "com.linecorp.rxeventbus",
            sig("void", "java.lang.Object")
                .addUsingField(FieldMatcher.create().name("STICKY_TYPES")));
    c.eventBus.busClass = f.interfaceOf(Finder.declaring(post));
    c.eventBus.methodPost = Finder.name(post);
    c.eventBus.providerField =
        f.field(c.eventBus.busClass, FieldMatcher.create().modifiers(Modifier.STATIC));
  }

  private static void services(Finder f, LineVersion.Config c) {
    c.services.keyClass =
        f.clsIn(
            Finder.packageOf(f.classUsing("Service implementation is not found.")),
            ClassMatcher.create()
                .modifiers(Modifier.ABSTRACT)
                .addField(
                    FieldMatcher.create()
                        .type("java.util.concurrent.atomic.AtomicReferenceFieldUpdater"))
                .addMethod(sig("java.lang.Object", "android.content.Context")));
    String key = c.services.keyClass;
    c.services.locatorClass =
        f.clsIn(
            Finder.packageOf(key),
            ClassMatcher.create()
                .addMethod(
                    sig("java.lang.Object", "android.content.Context", key)
                        .modifiers(Modifier.STATIC)));
    c.services.methodLocate =
        f.staticMethodOf(
            c.services.locatorClass, "java.lang.Object", "android.content.Context", key);
  }

  private static void chatActivity(Finder f, LineVersion.Config c) {
    c.chatActivity.chatIdField =
        f.field(
            CHAT_ACTIVITY,
            FieldMatcher.create()
                .type(
                    ClassMatcher.create()
                        .className("com.linecorp.line.chat.request.", StringMatchType.StartsWith)));
    c.chatActivity.methodGetChatId =
        f.methodOf(f.fieldType(CHAT_ACTIVITY, c.chatActivity.chatIdField), "java.lang.String");
  }

  private static void message(Finder f, LineVersion.Config c) {
    c.message.processorClass =
        Finder.outer(
            f.classAnnotatedIn(
                Finder.packageOf(f.classUsing("Decrypted(decryptedMessage=")),
                "jp.naver.line.android.talkop.processor.impl.RECEIVE_MESSAGE$receiveMessage$1"));
    c.message.processorMetadataClass = f.classUsing("AB", "NT");
    c.message.dataManagerClass = f.classUsing("before_registering_e2ee_public_key");

    c.message.selectionStateClass =
        f.cls(
            ClassMatcher.create()
                .modifiers(Modifier.INTERFACE)
                .addMethod(sig("java.util.Set"))
                .addMethod(sig("boolean", "java.util.Collection"))
                .addMethod(sig("boolean", "long")));
    String state = c.message.selectionStateClass;
    c.message.selectionProviderClass =
        f.clsIn(
            Finder.packageOf(state),
            ClassMatcher.create()
                .modifiers(Modifier.INTERFACE)
                .addMethod(sig(state))
                .addMethod(MethodMatcher.create().name("getCount")));
  }

  private static void thrift(Finder f, LineVersion.Config c) {
    String outOfSequence = "%s failed: out of sequence response: expected %d but got %d";
    c.thrift.protocolClass = f.classUsing(outOfSequence);
    c.thrift.methodReadMessageBegin =
        f.method(c.thrift.protocolClass, MethodMatcher.create().usingEqStrings(outOfSequence));
    c.thrift.messageClass = f.paramType(c.thrift.protocolClass, c.thrift.methodReadMessageBegin, 1);
    c.thrift.methodWriteMessageBegin =
        f.method(
            c.thrift.protocolClass,
            sig("void", "java.lang.String", c.thrift.messageClass)
                .addInvoke(MethodMatcher.create().name("write")));
    c.thrift.v1 =
        f.methodOf(
            c.thrift.talkServiceClientImplClass,
            "void",
            "int",
            "java.lang.String",
            "java.lang.String");
  }
}
