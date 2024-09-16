package cn.yiiguxing.plugin.translate.extensions;

import cn.yiiguxing.plugin.translate.activity.BaseStartupActivity;
import cn.yiiguxing.plugin.translate.documentation.TranslateType;
import com.intellij.openapi.project.Project;
import com.intellij.platform.backend.documentation.DocumentationTarget;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
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

public class NewTranslateManager extends BaseStartupActivity {

    public NewTranslateManager() {
        super(true, false);
    }


    @Override
    protected Object onRunActivity(@NotNull Project project, @NotNull Continuation<? super Unit> $completion) {
        try {
            Instrumentation inst = ByteBuddyAgent.install();
            injectTools.setInst(inst);
            ClassLoader pathLoader = Thread.currentThread().getContextClassLoader();

            NewTranslateManager.injectDispatcher();

            NewTranslateManager.commonIntercept(pathLoader, inst);
            NewTranslateManager.riderIntercept(pathLoader);


        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 注入调度器
     */
    public static void injectDispatcher() throws Exception {
        // 打印字节流以复制
//         byte[] classBytes = injectTools.getClassBytes("cn.yiiguxing.plugin.translate.extensions.TranslateDispatcher");
//         System.out.println(Arrays.toString(classBytes));
        byte[] classBytes = new byte[]{
                -54, -2, -70, -66, 0, 0, 0, 61, 0, 19, 10, 0, 2, 0, 3, 7, 0, 4, 12, 0, 5, 0, 6, 1, 0, 16, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 79, 98, 106, 101, 99, 116, 1, 0, 6, 60, 105, 110, 105, 116, 62, 1, 0, 3, 40, 41, 86, 7, 0, 8, 1, 0, 60, 99, 110, 47, 121, 105, 105, 103, 117, 120, 105, 110, 103, 47, 112, 108, 117, 103, 105, 110, 47, 116, 114, 97, 110, 115, 108, 97, 116, 101, 47, 101, 120, 116, 101, 110, 115, 105, 111, 110, 115, 47, 84, 114, 97, 110, 115, 108, 97, 116, 101, 68, 105, 115, 112, 97, 116, 99, 104, 101, 114, 1, 0, 10, 100, 105, 115, 112, 97, 116, 99, 104, 101, 114, 1, 0, 62, 76, 99, 110, 47, 121, 105, 105, 103, 117, 120, 105, 110, 103, 47, 112, 108, 117, 103, 105, 110, 47, 116, 114, 97, 110, 115, 108, 97, 116, 101, 47, 101, 120, 116, 101, 110, 115, 105, 111, 110, 115, 47, 84, 114, 97, 110, 115, 108, 97, 116, 101, 68, 105, 115, 112, 97, 116, 99, 104, 101, 114, 59, 1, 0, 4, 67, 111, 100, 101, 1, 0, 15, 76, 105, 110, 101, 78, 117, 109, 98, 101, 114, 84, 97, 98, 108, 101, 1, 0, 18, 76, 111, 99, 97, 108, 86, 97, 114, 105, 97, 98, 108, 101, 84, 97, 98, 108, 101, 1, 0, 4, 116, 104, 105, 115, 1, 0, 9, 116, 114, 97, 110, 115, 108, 97, 116, 101, 1, 0, 57, 40, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 83, 116, 114, 105, 110, 103, 59, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 83, 116, 114, 105, 110, 103, 59, 73, 41, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 83, 116, 114, 105, 110, 103, 59, 1, 0, 10, 83, 111, 117, 114, 99, 101, 70, 105, 108, 101, 1, 0, 24, 84, 114, 97, 110, 115, 108, 97, 116, 101, 68, 105, 115, 112, 97, 116, 99, 104, 101, 114, 46, 106, 97, 118, 97, 4, 33, 0, 7, 0, 2, 0, 0, 0, 1, 0, 9, 0, 9, 0, 10, 0, 0, 0, 2, 0, 1, 0, 5, 0, 6, 0, 1, 0, 11, 0, 0, 0, 47, 0, 1, 0, 1, 0, 0, 0, 5, 42, -73, 0, 1, -79, 0, 0, 0, 2, 0, 12, 0, 0, 0, 6, 0, 1, 0, 0, 0, 4, 0, 13, 0, 0, 0, 12, 0, 1, 0, 0, 0, 5, 0, 14, 0, 10, 0, 0, 4, 1, 0, 15, 0, 16, 0, 0, 0, 1, 0, 17, 0, 0, 0, 2, 0, 18
        };

        // 构建类型描述和字节码的映射
        Map<TypeDescription, byte[]> classDefinitions = new HashMap<>();
        // 构造要注入的字节码映射
        TypeDescription typeDescription = new TypeDescription.Latent(
                "cn.yiiguxing.plugin.translate.extensions.TranslateDispatcher",
                // 类的修饰符可以根据需要调整
                Modifier.PUBLIC,
                null
        );
        classDefinitions.put(typeDescription, classBytes);
        injectTools.injectBOOT(classDefinitions);

        // 注入后设置值
        Class<?> dispatcherClass = Class.forName("cn.yiiguxing.plugin.translate.extensions.TranslateDispatcher");
        Field dispatcherClassField = dispatcherClass.getDeclaredField("dispatcher");
        dispatcherClassField.setAccessible(true);

        Class<?> dynamicDispatcher = new ByteBuddy()
                .subclass(dispatcherClass) // 继承抽象类
                .method(ElementMatchers.named("translate")) // 匹配 translate 方法
                .intercept(MethodDelegation.to(TranslateDispatcherImpl.class)) // 使用拦截器实现方法
                .make()
                // 要加载到插件的类加载器中
                .load(NewTranslateManager.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        // 设置实例
        Object dispatcherInstance = dynamicDispatcher.getDeclaredConstructor().newInstance();
        dispatcherClassField.set(null, dispatcherInstance);
    }


    public static void commonIntercept(ClassLoader pathLoader, Instrumentation inst) throws Exception {
        injectTools.injectSystem(DocumentationTargetInterceptor.DocumentImplName.class);
        injectTools.injectSystem(TranslateType.class);

        injectTools.injectSystem(DocumentationTargetInterceptor.class);


        new AgentBuilder.Default()
                .type(ElementMatchers.isSubTypeOf(DocumentationTarget.class))
                .transform((builder, typeDescription, classLoader, module, domain) ->
                        builder.method(ElementMatchers.named("computeDocumentation"))
                                .intercept(MethodDelegation.to(DocumentationTargetInterceptor.class))
                )
                .with(ClassFileLocator.ForClassLoader.of(pathLoader))
                .installOn(inst);

//        injectTools.injectSystem(ComputeDocumentationInterceptor.class);
//        // 动态修改目标类
//        new ByteBuddy()
//                .rebase(Class.forName("com.intellij.platform.backend.documentation.impl.ImplKt$computeDocumentation$2"))
//                .method(ElementMatchers.named("invokeSuspend"))
//                .intercept(MethodDelegation.to(ComputeDocumentationInterceptor.class))
//                .make()
//                .load(pathLoader, ClassReloadingStrategy.fromInstalledAgent());

    }


    // Rider 特有的
    public static void riderIntercept(ClassLoader pathLoader) {
        try {
            Class<?> companionClass = Class.forName("com.jetbrains.rider.completion.summaryInfo.SummaryInfoViewItem$Companion");
            injectTools.injectSystem(RiderSummaryInfoInterceptor.class);
            // 拦截

            new ByteBuddy()
                    .rebase(companionClass)
                    .visit(
                            Advice.to(RiderSummaryInfoAdvice.class)
                                    .on(ElementMatchers.named("getSignatureOrTypeSummaryHtml"))
                    )
                    .visit(
                            Advice.to(RiderSummaryInfoAdvice.class)
                                    .on(ElementMatchers.named("B"))
                    )
                    .make()
                    .load(pathLoader, ClassReloadingStrategy.fromInstalledAgent());

        } catch (Exception e) {
//            System.out.println("riderIntercept 失败  " + e.getMessage());
//            e.printStackTrace(System.err);
        }

// 正式环境没有权限
//            new ByteBuddy()
//                    .rebase(companionClass)
//                    .method(ElementMatchers.named("getSignatureOrTypeSummaryHtml"))
//                    .intercept(MethodDelegation.to(RiderSummaryInfoInterceptor.class))
//                    .make()
//                    .load(pathLoader, ClassReloadingStrategy.fromInstalledAgent());

    }


    /**
     * 注入类
     */
    public static class injectTools {
        static File tempDir;
        static Instrumentation inst;

        // 初始化时操作
        static {
            try {
                tempDir = new File(System.getProperty("java.io.tmpdir"), "bytebuddy-temp-" + RandomString.make());
                if (!tempDir.mkdir()) {
                    throw new IllegalStateException("Failed to create temporary directory");
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize static block", e);
            }
        }

        static void setInst(Instrumentation inst) {
            injectTools.inst = inst;
        }

        /**
         * 注入到系统类引导器中, 这里是注入到`PathLoader`. 会先在当前类加载器加载指定类.
         *
         * @param targetClass 目标类
         */
        static void injectSystem(Class<?> targetClass) {
            ClassInjector classInjector = ClassInjector.UsingInstrumentation.of(tempDir, ClassInjector.UsingInstrumentation.Target.SYSTEM, inst);
            injectClass(classInjector, targetClass);
        }

        /**
         * 注入到`引导类加载器`中
         *
         * @param types 类型信息
         */
        static void injectBOOT(Map<TypeDescription, byte[]> types) {
            ClassInjector injector = ClassInjector.UsingInstrumentation.of(tempDir, ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, inst);
            injector.inject(types);
        }

        private static void injectClass(ClassInjector injector, Class<?> targetClass) {
            injector.inject(Collections.singletonMap(
                    new TypeDescription.ForLoadedType(targetClass),
                    ClassFileLocator.ForClassLoader.read(targetClass)
            ));
        }

        /**
         * 获得当前插件内指定类的字节流(不会加载类)
         *
         * @param className 全名
         */
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

}
