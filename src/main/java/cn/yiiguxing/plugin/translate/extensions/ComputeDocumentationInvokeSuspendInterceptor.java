package cn.yiiguxing.plugin.translate.extensions;

import cn.yiiguxing.plugin.translate.documentation.TranslateType;
import com.intellij.lang.Language;
import com.intellij.platform.backend.documentation.DocumentationData;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Callable;

public class ComputeDocumentationInvokeSuspendInterceptor {

//    static Class<?> DispatcherClass;
//    static Method TranslateMethod;
//    static Object TranslateObj;
//
//    static Field DocumentationContentField;
//    static Field HtmlField;

    @RuntimeType
    public static Object intercept(
            @AllArguments Object[] args,
            @SuperCall Callable<Object> zuper,
            @This Object self
    ) throws Exception {
        Object result = zuper.call();  // 调用原始方法, 会先经过另一个拦截方法, 可能会对文档进行翻译
        // 不是`DocumentationData`类型时为协程信号
        if (!(result instanceof DocumentationData data)) {
            return result;
        }
        String html = data.getHtml();
        // 如果已经处理过了会有标记
        if (html.lastIndexOf("XuhuanzyTranslateCompleted") != -1) {
            return result;
        }
        Class<?> DispatcherClass = DocumentationTargetInterceptor.DispatcherClass;
        Method TranslateMethod = DocumentationTargetInterceptor.TranslateMethod;
        Object TranslateObj = DocumentationTargetInterceptor.TranslateObj;

        Field DocumentationContentField = DocumentationTargetInterceptor.DocumentationContentField;
        Field HtmlField = DocumentationTargetInterceptor.HtmlField;
        //noinspection DuplicatedCode
        if (DispatcherClass == null) {
            DispatcherClass = Class.forName("cn.yiiguxing.plugin.translate.extensions.TranslateDispatcher");
            Field dispatcherField = DispatcherClass.getDeclaredField("dispatcher");
            dispatcherField.setAccessible(true);
            TranslateObj = dispatcherField.get(null);
            // 改了后记得匹配
            TranslateMethod = DispatcherClass.getMethod("translate", String.class, String.class, int.class);
            TranslateMethod.setAccessible(true);
        }

        if (DocumentationContentField == null) {
            DocumentationContentField = data.getClass().getDeclaredField("content");
            DocumentationContentField.setAccessible(true);
            HtmlField = DocumentationContentField.get(data).getClass().getDeclaredField("html");
            HtmlField.setAccessible(true);
        }

        // 设置
        HtmlField.set(
                DocumentationContentField.get(data),
                TranslateMethod.invoke(TranslateObj,
                        html,
                        getLanguage(self),
                        // 不往引导类注入就需要转换为 int
                        TranslateType.QuickDocumentation.ordinal()
                )
        );
        return result;
    }

    private static @NotNull String getLanguage(Object self) {
        try {
            Field $targetPointerField = self.getClass().getDeclaredField("$targetPointer");
            $targetPointerField.setAccessible(true);
            Object $targetPointer = $targetPointerField.get(self);
            String $targetPointerClassName = $targetPointer.getClass().getName();
            // 根据Pointer类名直接默认为对应的语言, 有可能是错的, 但没验证
            if ($targetPointerClassName.equals("com.intellij.lang.documentation.psi.PsiElementDocumentationTarget$PsiElementDocumentationTargetPointer")) {
                return Objects.requireNonNull(Language.findLanguageByID("kotlin")).getID();
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}