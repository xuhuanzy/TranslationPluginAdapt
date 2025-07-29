package cn.yiiguxing.plugin.translate.extensions;

import com.intellij.lang.documentation.psi.PsiElementDocumentationTarget;
import com.intellij.model.Pointer;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.platform.backend.documentation.DocumentationData;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import com.intellij.platform.backend.documentation.DocumentationTarget;
import com.intellij.lang.Language;


public class ComputeDocumentationAdvice {

//    @Advice.OnMethodEnter
//    public static Language onEnter(@Advice.Argument(0) @NotNull Pointer<? extends DocumentationTarget> pointer) throws Exception {
//        DocumentationTarget dereference = pointer.dereference();
//        if (dereference instanceof PsiElementDocumentationTarget) {
//            Language language = ((PsiElementDocumentationTarget) dereference).getTargetElement().getLanguage();
//            return language;
//        }
//        return null;
//    }


    @Advice.OnMethodExit()
//    public static void onExit( @Advice.Return Object result, @Advice.Enter Language language) {
    public static void onExit(@Advice.Return Object result) {
        System.out.println("onExit");

        if (!(result instanceof DocumentationData documentationData)) {
            return;
        }
        try {
            String currentHtml = documentationData.getHtml();
            Class<?> testClass = Class.forName("cn.yiiguxing.plugin.translate.extensions.CustomDispatcher");
            Field dispatcherField = testClass.getDeclaredField("quickDispatcher");
            dispatcherField.setAccessible(true);
            BiFunction<String, Language, String> dispatcher = (BiFunction<String, Language, String>) dispatcherField.get(null);

            // 通过反射拿到实际存储文档的字段
            Field contentField = documentationData.getClass().getDeclaredField("content");
            contentField.setAccessible(true);
            Object contentData = contentField.get(documentationData);
            Field htmlField = contentData.getClass().getDeclaredField("html");
            htmlField.setAccessible(true);

            // 设置
//            htmlField.set(contentData, dispatcher.apply(currentHtml, language));
            htmlField.set(contentData, dispatcher.apply(currentHtml, null));

        } catch (Exception e) {
//            System.out.println("异常");
            e.printStackTrace();
        }
    }
}