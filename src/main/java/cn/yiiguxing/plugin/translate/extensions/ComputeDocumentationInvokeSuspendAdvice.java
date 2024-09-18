package cn.yiiguxing.plugin.translate.extensions;

import cn.yiiguxing.plugin.translate.documentation.TranslateType;
import com.intellij.lang.Language;
import com.intellij.platform.backend.documentation.DocumentationData;
import net.bytebuddy.asm.Advice;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

public class ComputeDocumentationInvokeSuspendAdvice {

    public static Class<?> DispatcherClass;
    public static Method TranslateMethod;
    public static Object TranslateObj;

    public static Field DocumentationContentField;
    public static Field HtmlField;

    @Advice.OnMethodExit()
    public static void onExit(
            @Advice.Return Object result,
            @Advice.This(optional = true) Object self // 获取被增强的对象实例
    ) {

        // 不是`DocumentationData`类型时为协程信号
        if (!(result instanceof DocumentationData data)) {
            return;
        }

        String html = data.getHtml();
        // 如果已经处理过了会有标记
        if (html.lastIndexOf("XuhuanzyTranslateCompleted") != -1) {
            return;
        }
        try {
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
        } catch (Exception e) {
            System.out.println("ComputeDocumentationInvokeSuspendAdvice translate error");
            e.printStackTrace(System.err);
        }

    }

    public static @NotNull String getLanguage(Object self) {
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