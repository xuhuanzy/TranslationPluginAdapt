package cn.yiiguxing.plugin.translate.extensions;

import cn.yiiguxing.plugin.translate.activity.BaseStartupActivity;
import cn.yiiguxing.plugin.translate.documentation.TranslateType;
import cn.yiiguxing.plugin.translate.provider.TranslatedDocumentationProvider;
import com.intellij.openapi.project.Project;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.RandomString;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import com.intellij.lang.Language;

public class NewTranslateManager extends BaseStartupActivity {

    public NewTranslateManager() {
        super(true, false);
    }


    @Override
    protected Object onRunActivity(@NotNull Project project, @NotNull Continuation<? super Unit> $completion) {
        try {
            Instrumentation instrumentation = ByteBuddyAgent.install();
            // 需要注入的类加载器
            ClassLoader pathLoader = Class.forName("com.intellij.platform.backend.documentation.impl.ImplKt").getClassLoader();
            NewTranslateManager.injectDispatcher(instrumentation, pathLoader);
            NewTranslateManager.commonAdvice(pathLoader);
//            NewTranslateManager.riderAdvice(instrumentation, pathLoader);


        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    // 注入调度器
    public static void injectDispatcher(Instrumentation instrumentation, ClassLoader pathLoader) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        NewTranslateManager.injectClass(instrumentation, pathLoader, CustomDispatcher.class);
        Class<?> dispatcherClass = Class.forName("cn.yiiguxing.plugin.translate.extensions.CustomDispatcher", true, pathLoader);
        // 注册翻译入口
        BiFunction<String, Language, String> translateDispatch = (html, language) -> TranslatedDocumentationProvider.Companion.translateNew(html, language, null);
        Field dispatcherClassField = dispatcherClass.getDeclaredField("quickDispatcher");
        dispatcherClassField.setAccessible(true);
        dispatcherClassField.set(null, translateDispatch);
    }

    // 通用文档生成
    public static void commonAdvice(ClassLoader pathLoader) throws ClassNotFoundException {
        Class<?> implKtClass = Class.forName("com.intellij.platform.backend.documentation.impl.ImplKt");
        new ByteBuddy()
                .rebase(implKtClass)
                .visit(
                        Advice.to(ComputeDocumentationAdvice.class)
                                .on(ElementMatchers.named("computeDocumentation"))
                )
                .make()
                .load(implKtClass.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());

    }



    // Rider 特有的
    public static void riderAdvice(Instrumentation instrumentation,ClassLoader pathLoader) {
        Class<?> companionClass = null;
        try {
            companionClass = Class.forName("com.jetbrains.rider.completion.summaryInfo.SummaryInfoViewItem$Companion");
        } catch (ClassNotFoundException e) {
            return;
        }
        // 注册特有的翻译接口
        Class<?> dispatcherClass = null;
        try {
            dispatcherClass = Class.forName("cn.yiiguxing.plugin.translate.extensions.CustomDispatcher", true, pathLoader);
            Function<String, String> translateDispatch = (html) -> TranslatedDocumentationProvider.Companion.translateNew(html, null, TranslateType.RiderSummaryItem);
            Field dispatcherField = dispatcherClass.getDeclaredField("riderSummaryDispatcher");
            dispatcherField.setAccessible(true);
            dispatcherField.set(null, translateDispatch);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        // 拦截
        new ByteBuddy()
                .rebase(companionClass)
                .visit(
                        Advice.to(RiderSummaryInfoAdvice.class)
                                .on(ElementMatchers.named("getSignatureOrTypeSummaryHtml"))
                )
                .make()
                .load(pathLoader, ClassReloadingStrategy.fromInstalledAgent());
    }



    // 注入类
    public static void injectClass(Instrumentation instrumentation, ClassLoader classLoader, Class calssname) throws ClassNotFoundException {
        // 创建一个临时目录来存放类文件
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "bytebuddy-temp-" + RandomString.make());
        if (!tempDir.mkdir()) {
            throw new IllegalStateException("Failed to create temporary directory");
        }
        // 注入
        ClassInjector injector = ClassInjector.UsingInstrumentation.of(tempDir, ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, instrumentation);
        injector.inject(Collections.singletonMap(
                new TypeDescription.ForLoadedType(Class.forName(calssname.getName())),
                ClassFileLocator.ForClassLoader.read(calssname)
        ));
//        classLoader.loadClass(calssname.getName());
    }

}
