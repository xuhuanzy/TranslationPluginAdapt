package cn.yiiguxing.plugin.translate.extensions;

import cn.yiiguxing.plugin.translate.documentation.TranslateType;
import com.intellij.platform.backend.documentation.DocumentationContentData;
import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DocumentationContentDataInterceptor {
    static Class<?> DispatcherClass;
    static Method TranslateMethod;
    static Object TranslateObj;

    static Field HtmlField;

    @RuntimeType
    public static void intercept(
            @This DocumentationContentData targetObj
    ) {
        try {
            // 搜索是否存在 </XuhuanzyTranslate>, 存在说明被处理过, 该标签会在翻译后添加
            String html = targetObj.getHtml();
            if (html.lastIndexOf("XuhuanzyTranslateCompleted") != -1) {
                return;
            }
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
            if (HtmlField == null) {
                HtmlField = targetObj.getClass().getDeclaredField("html");
                HtmlField.setAccessible(true);
            }

            // 设置
            HtmlField.set(
                    targetObj,
                    TranslateMethod.invoke(TranslateObj,
                            html,
                            targetObj.component4() != null ? targetObj.component4().getLanguage().getID() : "",
                            // 不往引导类注入就需要转换为 int
                            TranslateType.QuickDocumentation.ordinal()
                    )
            );

        } catch (Exception ignored) {
        }
    }
}
