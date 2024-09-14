package cn.yiiguxing.plugin.translate.extensions;

import cn.yiiguxing.plugin.translate.activity.BaseStartupActivity;
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
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NewTranslateManager extends BaseStartupActivity {

    public NewTranslateManager() {
        super(true, false);
    }


    @Override
    protected Object onRunActivity(@NotNull Project project, @NotNull Continuation<? super Unit> $completion) {
        try {
            Instrumentation instrumentation = ByteBuddyAgent.install();

            Class<?> implKtClass = Class.forName("com.intellij.platform.backend.documentation.impl.ImplKt");

            // 注入调度类
            ClassLoader pathLoader = implKtClass.getClassLoader();
            NewTranslateManager.injectClass(instrumentation, pathLoader, CustomDispatcher.class);
            Class<?> dispatcherClass = Class.forName("cn.yiiguxing.plugin.translate.extensions.CustomDispatcher", true, pathLoader);
            // 注册翻译入口
            Function<String, String> translateDispatch = (html) -> TranslatedDocumentationProvider.Companion.translateNew(html, null);
            Field dispatcherClassField = dispatcherClass.getDeclaredField("dispatcher");
            dispatcherClassField.setAccessible(true);
            dispatcherClassField.set(null, translateDispatch);

            // 拦截
            new ByteBuddy()
                    .rebase(implKtClass)
                    .visit(Advice.to(ComputeDocumentationAdvice.class,
                                    new ClassFileLocator.Compound(
                                            ClassFileLocator.ForClassLoader.of(implKtClass.getClassLoader()),
                                            ClassFileLocator.ForClassLoader.of(NewTranslateManager.class.getClassLoader()))
                            ).on(ElementMatchers.named("computeDocumentation"))
                    )
                    .make()
                    .load(implKtClass.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    // 从 JAR 文件中提取所有的 .class 文件，并返回类名和字节码的映射
    public static Map<TypeDescription, byte[]> extractClassesFromJar(String jarFilePath) throws IOException {
        Map<TypeDescription, byte[]> classMap = new HashMap<>();
        JarFile jarFile = new JarFile(new File(jarFilePath));

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            // 仅处理 .class 文件
            if (entry.getName().endsWith(".class")) {
                String className = entry.getName().replace("/", ".").replace(".class", "");

                // 读取类文件字节码
                try (InputStream classStream = jarFile.getInputStream(entry)) {
                    byte[] classBytes = classStream.readAllBytes();

                    // 将类的字节码添加到 Map 中
                    TypeDescription typeDescription = new TypeDescription.Latent(className, 0, null);
                    classMap.put(typeDescription, classBytes);
                }
            }
        }
        return classMap;
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
        classLoader.loadClass(calssname.getName());
    }

}
