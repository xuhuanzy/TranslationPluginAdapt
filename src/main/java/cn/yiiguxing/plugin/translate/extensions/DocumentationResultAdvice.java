package cn.yiiguxing.plugin.translate.extensions;

import com.intellij.platform.backend.documentation.DocumentationData;
import com.intellij.platform.backend.documentation.DocumentationResult;
import net.bytebuddy.asm.Advice;


public class DocumentationResultAdvice {
    @Advice.OnMethodExit
    public static void onExit(@Advice.Return Object result) {
        System.out.println("onExit DocumentationResultAdvice");

    }
}
