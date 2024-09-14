package cn.yiiguxing.plugin.translate.extensions;

import com.intellij.platform.backend.documentation.DocumentationData;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;


public class ComputeDocumentationAdvice {

    @Advice.OnMethodExit()
    public static void onExit(@Advice.Return Object result) {
        try {
            // 如果目标方法返回的结果为 null，直接返回
            if (result == null) {
                return;
            }
            DocumentationData documentationData = (DocumentationData) result;
            String currentHtml = documentationData.getHtml();

            Class<?> testClass = Class.forName("cn.yiiguxing.plugin.translate.extensions.CustomDispatcher");
            Field dispatcherField = testClass.getDeclaredField("dispatcher");
            dispatcherField.setAccessible(true);
            Function<String, String> dispatcher = (Function<String, String>) dispatcherField.get(null);

            // 通过反射拿到实际存储文档的字段
            Field contentField = documentationData.getClass().getDeclaredField("content");
            contentField.setAccessible(true);
            Object contentData = contentField.get(documentationData);
            Field htmlField = contentData.getClass().getDeclaredField("html");
            htmlField.setAccessible(true);

            // 设置
            htmlField.set(contentData,dispatcher.apply(currentHtml));

        } catch (Exception e) {
        }
    }
}