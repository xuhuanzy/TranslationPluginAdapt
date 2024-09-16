package cn.yiiguxing.plugin.translate.extensions;

import cn.yiiguxing.plugin.translate.documentation.TranslateType;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class RiderSummaryInfoAdvice {
    @Advice.OnMethodExit()
    public static void onExit(@Advice.Return(readOnly = false) String originHtml) {
        try {
            Class<?> DispatcherClass = Class.forName("cn.yiiguxing.plugin.translate.extensions.TranslateDispatcher");
            Field dispatcherField = DispatcherClass.getDeclaredField("dispatcher");
            dispatcherField.setAccessible(true);
            Object TranslateObj  = dispatcherField.get(null);
            // 改了后记得匹配
            Method TranslateMethod = DispatcherClass.getMethod("translate", String.class, String.class, int.class);
            TranslateMethod.setAccessible(true);
            originHtml = (String) TranslateMethod.invoke(TranslateObj,
                    originHtml,
                    "",
                    TranslateType.RiderSummaryItem.ordinal()
            );
        } catch (Exception ignored) {
        }

    }
}
