package cn.yiiguxing.plugin.translate.extensions;

import cn.yiiguxing.plugin.translate.documentation.TranslateType;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class RiderSummaryInfoInterceptor {
    static Class<?> DispatcherClass;
    static Method TranslateMethod;
    static Object TranslateObj;

    @RuntimeType
    public static String intercept(@SuperCall Callable<Object> zuper) throws Exception {
        String result = (String) zuper.call();  // 调用原始方法
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
            result = (String) TranslateMethod.invoke(TranslateObj,
                    result,
                    "",
                    TranslateType.RiderSummaryItem.ordinal()
            );

        } catch (Exception ignored) {
        }

        return result;
    }
}
