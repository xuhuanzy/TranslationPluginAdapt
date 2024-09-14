package cn.yiiguxing.plugin.translate.extensions;

import cn.yiiguxing.plugin.translate.activity.BaseStartupActivity;
import cn.yiiguxing.plugin.translate.provider.TranslatedDocumentationProvider;
import com.intellij.openapi.project.Project;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class NewTranslateManager extends BaseStartupActivity {

    public NewTranslateManager() {
        super(true, false);
    }

    @Override
    protected Object onRunActivity(@NotNull Project project, @NotNull Continuation<? super Unit> $completion) {
        try {
            // 加载目标类
            Class<?> clazz = Class.forName("com.xuhuanzy.TranslateExtension.DocumentModify");

            // 获取 addAction 方法
            Method addActionMethod = clazz.getDeclaredMethod(
                    "addAction", Class.forName("com.xuhuanzy.TranslateExtension.DocumentModify$Action")
            );

            // 创建动态代理来实现 Action 接口
            Object actionProxy = Proxy.newProxyInstance(
                    clazz.getClassLoader(),
                    new Class[]{Class.forName("com.xuhuanzy.TranslateExtension.DocumentModify$Action")},
                    (proxy, method, args1) -> {
                        // 将 args1[0] 强制转换为 String
                        String html = (String) args1[0];
                        // 调用方法 (例如，你可以对 html 做一些处理)
                        return TranslatedDocumentationProvider.Companion.translateNew(html, null) ;
                    }
            );

            // 调用 addAction 方法
            addActionMethod.invoke(null, actionProxy);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        return null;
    }


}
