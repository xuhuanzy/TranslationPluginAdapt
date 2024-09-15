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
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.RandomString;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

            // 创建一个临时目录来存放类文件
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "bytebuddy-temp-" + RandomString.make());
            if (!tempDir.mkdir()) {
                throw new IllegalStateException("Failed to create temporary directory");
            }
            // 引导类注入器
            ClassInjector injector = ClassInjector.UsingInstrumentation.of(tempDir, ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, instrumentation);
            // 需要注入的类加载器
            ClassLoader pathLoader = Class.forName("com.intellij.platform.backend.documentation.impl.ImplKt").getClassLoader();
            NewTranslateManager.injectDispatcherNew(injector, instrumentation);

//            NewTranslateManager.injectDispatcher(instrumentation, pathLoader);
//            NewTranslateManager.commonAdvice(pathLoader);
            NewTranslateManager.commonIntercept(injector, pathLoader, instrumentation);
//            NewTranslateManager.riderAdvice(instrumentation, pathLoader);


        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // 注入调度器
    public static void injectDispatcher(ClassInjector injector, ClassLoader pathLoader) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        NewTranslateManager.injectClass(injector, CustomDispatcher.class);
        Class<?> dispatcherClass = Class.forName("cn.yiiguxing.plugin.translate.extensions.CustomDispatcher", true, pathLoader);
        // 注册翻译入口
        BiFunction<String, Language, String> translateDispatch = (html, language) -> TranslatedDocumentationProvider.Companion.translateNew(html, language, null);

        Field dispatcherClassField = dispatcherClass.getDeclaredField("quickDispatcher");
        dispatcherClassField.setAccessible(true);
        dispatcherClassField.set(null, translateDispatch);
    }

    public static void injectDispatcherNew(ClassInjector injector, Instrumentation instrumentation) throws Exception {
//        byte[] classBytes = NewTranslateManager.getClassBytes("cn.yiiguxing.plugin.translate.extensions.TranslateDispatcher");
        // 打印为可复制的字节流
        byte[] classBytes = new byte[]{
                -54, -2, -70, -66, 0, 0, 0, 61, 0, 19, 10, 0, 2, 0, 3, 7, 0, 4, 12, 0, 5, 0, 6, 1, 0, 16, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 79, 98, 106, 101, 99, 116, 1, 0, 6, 60, 105, 110, 105, 116, 62, 1, 0, 3, 40, 41, 86, 7, 0, 8, 1, 0, 60, 99, 110, 47, 121, 105, 105, 103, 117, 120, 105, 110, 103, 47, 112, 108, 117, 103, 105, 110, 47, 116, 114, 97, 110, 115, 108, 97, 116, 101, 47, 101, 120, 116, 101, 110, 115, 105, 111, 110, 115, 47, 84, 114, 97, 110, 115, 108, 97, 116, 101, 68, 105, 115, 112, 97, 116, 99, 104, 101, 114, 1, 0, 10, 100, 105, 115, 112, 97, 116, 99, 104, 101, 114, 1, 0, 62, 76, 99, 110, 47, 121, 105, 105, 103, 117, 120, 105, 110, 103, 47, 112, 108, 117, 103, 105, 110, 47, 116, 114, 97, 110, 115, 108, 97, 116, 101, 47, 101, 120, 116, 101, 110, 115, 105, 111, 110, 115, 47, 84, 114, 97, 110, 115, 108, 97, 116, 101, 68, 105, 115, 112, 97, 116, 99, 104, 101, 114, 59, 1, 0, 4, 67, 111, 100, 101, 1, 0, 15, 76, 105, 110, 101, 78, 117, 109, 98, 101, 114, 84, 97, 98, 108, 101, 1, 0, 18, 76, 111, 99, 97, 108, 86, 97, 114, 105, 97, 98, 108, 101, 84, 97, 98, 108, 101, 1, 0, 4, 116, 104, 105, 115, 1, 0, 9, 116, 114, 97, 110, 115, 108, 97, 116, 101, 1, 0, 38, 40, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 83, 116, 114, 105, 110, 103, 59, 41, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 83, 116, 114, 105, 110, 103, 59, 1, 0, 10, 83, 111, 117, 114, 99, 101, 70, 105, 108, 101, 1, 0, 24, 84, 114, 97, 110, 115, 108, 97, 116, 101, 68, 105, 115, 112, 97, 116, 99, 104, 101, 114, 46, 106, 97, 118, 97, 4, 33, 0, 7, 0, 2, 0, 0, 0, 1, 0, 9, 0, 9, 0, 10, 0, 0, 0, 2, 0, 1, 0, 5, 0, 6, 0, 1, 0, 11, 0, 0, 0, 47, 0, 1, 0, 1, 0, 0, 0, 5, 42, -73, 0, 1, -79, 0, 0, 0, 2, 0, 12, 0, 0, 0, 6, 0, 1, 0, 0, 0, 5, 0, 13, 0, 0, 0, 12, 0, 1, 0, 0, 0, 5, 0, 14, 0, 10, 0, 0, 4, 1, 0, 15, 0, 16, 0, 0, 0, 1, 0, 17, 0, 0, 0, 2, 0, 18
        };
        // 构建类型描述和字节码的映射
        Map<TypeDescription, byte[]> classDefinitions = new HashMap<>();
        // 构造要注入的字节码映射
        TypeDescription typeDescription = new TypeDescription.Latent(
                "cn.yiiguxing.plugin.translate.extensions.TranslateDispatcher",
                // 类的修饰符可以根据需要调整
                java.lang.reflect.Modifier.PUBLIC,
                null
        );

        classDefinitions.put(typeDescription, classBytes);
        injector.inject(classDefinitions);

        Class<?> dispatcherClass = Class.forName("cn.yiiguxing.plugin.translate.extensions.TranslateDispatcher");
        Field dispatcherClassField = dispatcherClass.getDeclaredField("dispatcher");
        dispatcherClassField.setAccessible(true);

        // 获取 TranslateDispatcher 类的构造方法
        Constructor<?> constructor = dispatcherClass.getDeclaredConstructor();
        constructor.setAccessible(true);

        Class<?> dynamicDispatcher = new ByteBuddy()
                .subclass(dispatcherClass) // 继承抽象类
                .method(ElementMatchers.named("translate")) // 匹配 translate 方法
                .intercept(MethodDelegation.to(TranslateDispatcherImpl.class)) // 使用拦截器实现方法
                .make()
                .load(NewTranslateManager.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        // 创建实例
        Object dispatcherInstance = dynamicDispatcher.getDeclaredConstructor().newInstance();
        // 设置
        dispatcherClassField.set(null, dispatcherInstance);


    }


    // 通用文档生成
    public static void commonAdvice(ClassLoader pathLoader) throws Exception {
        Class<?> targetClass = Class.forName("com.intellij.platform.backend.documentation.impl.ImplKt");
        new ByteBuddy()
                .rebase(targetClass)
                .visit(
                        Advice.to(ComputeDocumentationAdvice.class)
                                .on(ElementMatchers.named("computeDocumentation"))
                )
                .make()
                .load(pathLoader, ClassReloadingStrategy.fromInstalledAgent());

    }

    public static void commonIntercept(ClassInjector injector, ClassLoader pathLoader, Instrumentation instrumentation) throws Exception {
        // 创建一个临时目录来存放类文件
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "bytebuddy-temp-" + RandomString.make());
        if (!tempDir.mkdir()) {
            throw new IllegalStateException("Failed to create temporary directory");
        }
        injector = ClassInjector.UsingInstrumentation.of(tempDir, ClassInjector.UsingInstrumentation.Target.SYSTEM, instrumentation);

        NewTranslateManager.injectClass(injector, ComputeDocumentationInterceptor.class);
        // 动态修改目标类
        new ByteBuddy()
                .rebase(Class.forName("com.intellij.platform.backend.documentation.impl.ImplKt$computeDocumentation$2"))
                .method(ElementMatchers.named("invokeSuspend"))
                .intercept(MethodDelegation.to(ComputeDocumentationInterceptor.class))
                .make()
                .load(pathLoader, ClassReloadingStrategy.fromInstalledAgent());

    }


    // Rider 特有的
    public static void riderAdvice(Instrumentation instrumentation, ClassLoader pathLoader) {
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


    // 注入类到引导类
    public static void injectClass(ClassInjector injector, Class calssname) throws ClassNotFoundException {
        injector.inject(Collections.singletonMap(
                new TypeDescription.ForLoadedType(Class.forName(calssname.getName())),
                ClassFileLocator.ForClassLoader.read(calssname)
        ));
    }

    public static byte[] getClassBytes(String className) throws IOException {
        String classFilePath = className.replace('.', '/') + ".class";
        ClassLoader classLoader = NewTranslateManager.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(classFilePath)) {
            if (inputStream == null) {
                throw new IOException("Class file not found for: " + classFilePath);
            }
            // 将输入流转换为字节数组
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

}
