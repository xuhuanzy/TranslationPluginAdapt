package cn.yiiguxing.plugin.translate.extensions;

import cn.yiiguxing.plugin.translate.activity.BaseStartupActivity;
import cn.yiiguxing.plugin.translate.provider.TranslatedDocumentationProvider;
import com.intellij.openapi.project.Project;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class NewTranslateManager extends BaseStartupActivity {

    public NewTranslateManager() {
        super(true, false);
    }

    @Override
    protected Object onRunActivity(@NotNull Project project, @NotNull Continuation<? super Unit> $completion) {
        try {
            ByteBuddyAgent.install();
            Class<?> implKtClass = Class.forName("com.intellij.platform.backend.documentation.impl.ImplKt");

            DynamicType.Loaded<?> bb = new ByteBuddy()
                    .redefine(implKtClass)
                    .visit(Advice.to(ComputeDocumentationAdvice.class,
                                    new ClassFileLocator.Compound(
                                            ClassFileLocator.ForClassLoader.of(implKtClass.getClassLoader()),
                                            ClassFileLocator.ForClassLoader.of(NewTranslateManager.class.getClassLoader()))
                            ).on(ElementMatchers.named("computeDocumentation"))
                    )
                    .defineField("_cn_yiiguxing_plugin_translate_xuhuanzy_reflect_action", Function.class, java.lang.reflect.Modifier.STATIC | java.lang.reflect.Modifier.PUBLIC)
                    .make()
                    .load(implKtClass.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());

            // 注册翻译入口
            Function<String, String> translateFunction = (html) -> {
                return TranslatedDocumentationProvider.Companion.translateNew(html, null);
            };
            // 使用反射将静态字段赋值为该 Function 实例
            Field translateManagerActionField = implKtClass.getDeclaredField("_cn_yiiguxing_plugin_translate_xuhuanzy_reflect_action");
            translateManagerActionField.setAccessible(true);
            translateManagerActionField.set(null, translateFunction);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


}
