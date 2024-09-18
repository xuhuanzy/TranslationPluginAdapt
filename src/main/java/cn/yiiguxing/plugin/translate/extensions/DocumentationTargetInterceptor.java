package cn.yiiguxing.plugin.translate.extensions;

import cn.yiiguxing.plugin.translate.documentation.TranslateType;
import com.intellij.lang.Language;
import com.intellij.platform.backend.documentation.AsyncDocumentation;
import com.intellij.platform.backend.documentation.DocumentationData;
import com.intellij.platform.backend.documentation.DocumentationResult;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.keyFMap.KeyFMap;
import kotlin.coroutines.Continuation;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import javax.xml.parsers.DocumentBuilder;

public class DocumentationTargetInterceptor {
    public static Class<?> DispatcherClass;
    public static Method TranslateMethod;
    public static Object TranslateObj;

    static Field DocumentationContentField;
    static Field HtmlField;

    @RuntimeType
    public static DocumentationResult intercept(@SuperCall Callable<Object> zuper,
                                                @This Object self
    ) throws Exception {
        DocumentationResult result = (DocumentationResult) zuper.call();  // 调用原始方法
        if (!(result instanceof DocumentationData data)) {
            return result;
        }

        Language language = getLanguage(self);
        try {
            String currentHtml = data.getHtml();

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
                            currentHtml,
                            language != null ? language.getID() : "",
                            // 不往引导类注入就需要转换为 int
                            TranslateType.QuickDocumentation.ordinal()
                    )
            );

        } catch (Exception ignored) {
        }
        return result;

    }

    enum DocumentImplName {

        /**
         * 这个似乎是标准的?
         */
        PsiElement("com.intellij.lang.documentation.psi.PsiElementDocumentationTarget"),
        Clang("com.jetbrains.cidr.lang.daemon.clang.clangd.documentation.ClangDocumentationTarget"),
        Rider("com.jetbrains.rdclient.quickDoc.FrontendDocumentationTarget");


        private final String message;  // 定义枚举的字符串字段

        // 构造函数
        DocumentImplName(String message) {
            this.message = message;
        }

        // 获取字符串值的方法
        public String getMessage() {
            return message;
        }
    }

    private static Language getLanguage(Object self) {
        String className = self.getClass().getName();

        try {
            if (className.equals(DocumentImplName.PsiElement.message)) {
                Field sourceElementField = self.getClass().getDeclaredField("sourceElement");
                sourceElementField.setAccessible(true);
                PsiElement element = (PsiElement) sourceElementField.get(self);
                return element.getLanguage();
            } else if (className.equals(DocumentImplName.Rider.message)) {
                Field fileField = self.getClass().getDeclaredField("file");
                fileField.setAccessible(true);
                PsiFile file = (PsiFile) fileField.get(self);
                return file.getLanguage();
            } else if (className.equals(DocumentImplName.Clang.message)) {
                Method getSourceElementMethod = self.getClass().getMethod("getSourceElement");
                getSourceElementMethod.setAccessible(true);
                PsiElement element = (PsiElement) getSourceElementMethod.invoke(self);
                return element.getLanguage();
            }

        } catch (Exception ignored) {
        }
        return null;
    }
}

