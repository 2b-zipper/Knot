package app.zipper.knot.mapping;

import static app.zipper.knot.mapping.Finder.sig;

import app.zipper.knot.LineVersion;
import java.lang.reflect.Modifier;
import org.luckypray.dexkit.query.enums.UsingType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

final class LibraryRules {

  static final Mappings.Section[] SECTIONS = {
    LibraryRules::kotlin, LibraryRules::compose, LibraryRules::androidx, LibraryRules::room
  };

  private LibraryRules() {}

  private static void kotlin(Finder f, LineVersion.Config c) {
    // Matched by shape: R8 renames invoke in some FunctionN (Function3's is k)
    c.kotlin.function0Class =
        f.cls(
            ClassMatcher.create()
                .modifiers(Modifier.INTERFACE)
                .methodCount(1)
                .addMethod(sig("java.lang.Object"))
                .addInterface(
                    ClassMatcher.create()
                        .modifiers(Modifier.INTERFACE)
                        .methodCount(0)
                        .interfaceCount(0)));
    String functions = Finder.packageOf(c.kotlin.function0Class);
    String marker = f.interfaceOf(c.kotlin.function0Class);
    c.kotlin.function1Class =
        f.clsIn(
            functions,
            ClassMatcher.create()
                .modifiers(Modifier.INTERFACE)
                .methodCount(1)
                .addMethod(sig("java.lang.Object", "java.lang.Object"))
                .addInterface(marker));
    c.kotlin.function2Class =
        f.clsIn(
            functions,
            ClassMatcher.create()
                .modifiers(Modifier.INTERFACE)
                .methodCount(1)
                .addMethod(sig("java.lang.Object", "java.lang.Object", "java.lang.Object"))
                .addInterface(marker));

    c.kotlin.unitClass =
        f.clsIn(
            Finder.packageOf(f.classUsing("Version components are out of range: ")),
            ClassMatcher.create().usingEqStrings("kotlin.Unit").methodCount(3).fieldCount(1));
    c.kotlin.fieldUnitInstance = f.fieldOf(c.kotlin.unitClass, c.kotlin.unitClass);

    // ContinuationImpl kept its property names
    String suspendBase = f.classUsing("create(Continuation) has not been overridden");
    c.kotlin.continuationImplClass =
        f.clsIn(
            Finder.packageOf(suspendBase),
            ClassMatcher.create()
                .superClass(suspendBase)
                .addField(FieldMatcher.create().name("_context")));

    // R8 narrows MutableStateFlow fields to this class
    c.kotlin.stateFlowImplClass =
        f.classUsing("MutableStateFlow.resetReplayCache is not supported");
  }

  private static void compose(Finder f, LineVersion.Config c) {
    c.compose.layoutCoordinatesClass =
        f.classUsing("localPositionOf is not implemented on this LayoutCoordinates");
    c.compose.methodCoordinatesSize = f.methodOf(c.compose.layoutCoordinatesClass, "long");

    // NodeCoordinator.localToWindow calls localToRoot, which checks isAttached
    String attached = "LayoutCoordinate operations are only valid when isAttached is true";
    String coordinator =
        f.clsIn(
            "androidx.compose.ui.node",
            ClassMatcher.create()
                .addInterface(c.compose.layoutCoordinatesClass)
                .usingEqStrings(attached));
    c.compose.methodLocalToWindow =
        f.method(
            coordinator,
            sig("long", "long")
                .addNoneOf(MethodMatcher.create().usingEqStrings(attached))
                .addInvoke(
                    MethodMatcher.create()
                        .declaredClass(coordinator)
                        .returnType("long")
                        .paramTypes("long")
                        .usingEqStrings(attached)));

    // OnGloballyPositionedNode, its element and Modifier.onGloballyPositioned share a package
    String layout = Finder.packageOf(c.compose.layoutCoordinatesClass);
    String node =
        f.clsIn(
            layout,
            ClassMatcher.create()
                .addField(FieldMatcher.create().type(c.kotlin.function1Class))
                .addInterface(
                    ClassMatcher.create()
                        .modifiers(Modifier.INTERFACE)
                        .methodCount(1)
                        .addMethod(sig("void", coordinator))));
    String element =
        f.classAmong(
            f.classesIn(
                layout,
                ClassMatcher.create()
                    .addMethod(
                        MethodMatcher.create().name("<init>").paramTypes(c.kotlin.function1Class))),
            ClassMatcher.create()
                .addMethod(
                    MethodMatcher.create()
                        .addUsingField(
                            FieldMatcher.create().declaredClass(node), UsingType.Write)));
    MethodData positioned =
        f.methodDataIn(
            layout,
            MethodMatcher.create()
                .modifiers(Modifier.STATIC)
                .addInvoke(MethodMatcher.create().declaredClass(element).name("<init>")));
    c.compose.onGloballyPositionedClass = Finder.declaring(positioned);
    c.compose.methodOnGloballyPositioned = Finder.name(positioned);

    // ComposerImpl extends an abstract base that implements Composer
    c.compose.composerImplClass =
        f.classUsing("A call to createNode(), emitNode() or useNode() expected");
    c.compose.composerClass = f.interfaceOf(f.superOf(c.compose.composerImplClass));
    String scope =
        f.returnTypeWhere(
            c.compose.composerImplClass,
            ClassMatcher.create().addMethod(MethodMatcher.create().name("invalidate")));
    // endRestartGroup() is the only no-argument method that writes to the scope it returns
    c.compose.methodEndRestartGroup =
        f.method(
            c.compose.composerImplClass,
            MethodMatcher.create()
                .paramCount(0)
                .returnType(scope)
                .addUsingField(FieldMatcher.create().declaredClass(scope), UsingType.Write));

    // clickable$default(modifier, enabled, onClickLabel, role, source, onClick, mask)
    c.compose.clickableClass =
        f.clsIn(
            Finder.packageOf(
                f.classUsing(", animationMode=Immediately, delayMillis=0, initialDelayMillis=")),
            ClassMatcher.create()
                .addMethod(
                    sig(
                        null,
                        null,
                        "boolean",
                        "java.lang.String",
                        null,
                        null,
                        c.kotlin.function0Class,
                        "int")));
    // clickable(modifier, source, indication, enabled, role, onClick)
    c.compose.methodClickable =
        f.staticMethodOf(
            c.compose.clickableClass,
            null,
            null,
            null,
            null,
            "boolean",
            null,
            c.kotlin.function0Class);
    // combinedClickable$default(modifier, source, enabled, role, onLongClick, onClick, mask)
    c.compose.methodCombinedClickable =
        f.staticMethodOf(
            c.compose.clickableClass,
            null,
            null,
            null,
            "boolean",
            null,
            c.kotlin.function0Class,
            c.kotlin.function0Class,
            "int");
  }

  private static void androidx(Finder f, LineVersion.Config c) {
    c.androidx.lifecycleEventClass = f.classUsing(" has no target state");
    c.androidx.lifecycleOwnerClass =
        f.clsIn(
            "androidx.lifecycle",
            ClassMatcher.create()
                .modifiers(Modifier.INTERFACE)
                .methodCount(1)
                .addMethod(MethodMatcher.create().name("getLifecycle").paramCount(0)));

    c.androidx.viewHolderClass =
        f.classUsing(". ViewHolders should be fully detached before resetting.");
    c.androidx.viewHolderViewField = f.fieldOf(c.androidx.viewHolderClass, "android.view.View");
  }

  private static void room(Finder f, LineVersion.Config c) {
    c.room.migrationUtilClass =
        f.clsIn(
            Finder.packageOf(f.classUsing("SELECT * FROM sqlite_master WHERE `name` = '")),
            ClassMatcher.create()
                .addMethod(sig("java.util.List", null, "int", "int"))
                .methodCount(2)
                .fieldCount(0));
    c.room.methodIsMigrationRequired =
        f.staticMethodOf(c.room.migrationUtilClass, "boolean", null, "int", "int");
  }
}
