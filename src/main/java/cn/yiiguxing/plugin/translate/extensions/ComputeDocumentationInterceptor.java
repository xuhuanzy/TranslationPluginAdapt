package cn.yiiguxing.plugin.translate.extensions;

import com.intellij.lang.Language;
import com.intellij.platform.backend.documentation.DocumentationData;
import com.intellij.platform.backend.documentation.DocumentationResult;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;


public class ComputeDocumentationInterceptor {
    @RuntimeType
    public static Object intercept(
            @AllArguments Object[] args,
            @SuperCall Callable<Object> zuper
    ) throws Exception {
        Object result = zuper.call();  // 调用原始方法
        if (!(result instanceof DocumentationData data)) {
            System.out.println("ComputeDocumentationInterceptor");
            return result;
        }

        System.out.println("正常");
        return data;
    }
}