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
            // 反射拿到由插件注册的静态字段, 该字段是一个函数, 该函数能拿到插件的上下文.
            Class<?> implKtClass = Class.forName("com.intellij.platform.backend.documentation.impl.ImplKt");
            Field translateManagerActionField = implKtClass.getDeclaredField("_cn_yiiguxing_plugin_translate_xuhuanzy_reflect_action");
            Function<String, String> translateFunctionInstance =  (Function<String, String>)translateManagerActionField.get(null);

            // 通过反射拿到实际存储文档的字段
            Field contentField = documentationData.getClass().getDeclaredField("content");
            contentField.setAccessible(true);
            Object contentData = contentField.get(documentationData);
            Field htmlField = contentData.getClass().getDeclaredField("html");
            htmlField.setAccessible(true);

            // 设置为经过处理的文档
            htmlField.set(contentData, translateFunctionInstance.apply(currentHtml));

        } catch (Exception e) {
        }
    }
}