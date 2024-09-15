package cn.yiiguxing.plugin.translate.extensions;

import com.intellij.lang.Language;
import com.intellij.platform.backend.documentation.DocumentationData;
import com.intellij.psi.tree.IElementType;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class ComputeDocumentationInterceptor {
    static Class<?> DispatcherClass;
    static Method TranslateMethod;
    static Object TranslateObj;

    static Field DocumentationContentField;
    static Field HtmlField;

    @RuntimeType
    public static Object intercept(@SuperCall Callable<Object> zuper,
                                   @Argument(0) Object arg,
                                   @This Object self
    ) throws Exception {
        Object result = zuper.call();  // 调用原始方法

        if (!(result instanceof DocumentationData documentationData)) {
            return result;
        }
        Language language = getLanguage(self);

        try {
            String currentHtml = documentationData.getHtml();

            if (DispatcherClass == null) {
                DispatcherClass = Class.forName("cn.yiiguxing.plugin.translate.extensions.TranslateDispatcher");
                Field dispatcherField = DispatcherClass.getDeclaredField("dispatcher");
                dispatcherField.setAccessible(true);
                TranslateObj = dispatcherField.get(null);
                TranslateMethod = DispatcherClass.getMethod("translate", String.class);
                TranslateMethod.setAccessible(true);
            }
            if (DocumentationContentField == null) {
                DocumentationContentField = documentationData.getClass().getDeclaredField("content");
                DocumentationContentField.setAccessible(true);
                HtmlField = DocumentationContentField.get(documentationData).getClass().getDeclaredField("html");
                HtmlField.setAccessible(true);
            }


            // 设置
            HtmlField.set(
                    DocumentationContentField.get(documentationData),
                    TranslateMethod.invoke(TranslateObj, currentHtml)
            );

        } catch (Exception ignored) {
        }

        return result;
    }

    private static Language getLanguage(Object self) {
        try {
            Field targetPointerField = self.getClass().getDeclaredField("$targetPointer");
            targetPointerField.setAccessible(true);
            Object pointer = targetPointerField.get(self);

            Field targetPointerField2 = pointer.getClass().getDeclaredField("targetPointer");
            targetPointerField2.setAccessible(true);
            Object targetPointer = targetPointerField2.get(pointer);

            Field myElementField = targetPointer.getClass().getDeclaredField("myElement");
            myElementField.setAccessible(true);
            SoftReference<?> myElement = (SoftReference<?>) myElementField.get(targetPointer);

            Object referent = myElement.get();
            Method myElementTypeMethod = referent.getClass().getMethod("getElementType");
            myElementTypeMethod.setAccessible(true);
            IElementType myElementType = (IElementType) myElementTypeMethod.invoke(referent);
            return myElementType.getLanguage();
        } catch (Exception e) {
            return null;
        }

    }
}
