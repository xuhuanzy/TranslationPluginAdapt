package cn.yiiguxing.plugin.translate.extensions;

import com.intellij.platform.backend.documentation.DocumentationData;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Field;
import java.util.function.Function;

public class RiderSummaryInfoAdvice {
    @Advice.OnMethodExit()
    public static void onExit(@Advice.Return(readOnly = false) String originHtml) {
        System.out.println("RiderSummaryInfoAdvice onExit");
        Function<String, String> dispatcher = null;
        try {
            Class<?> testClass = Class.forName("cn.yiiguxing.plugin.translate.extensions.CustomDispatcher");
            Field dispatcherField = testClass.getDeclaredField("riderSummaryDispatcher");
            dispatcherField.setAccessible(true);
            dispatcher = (Function<String, String>) dispatcherField.get(null);
        } catch (Exception e) {
            return;
        }
//        dispatcher.apply(originHtml);

    }
}
