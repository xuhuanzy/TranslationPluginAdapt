package cn.yiiguxing.plugin.translate.extensions;

import java.util.function.BiFunction;

import com.intellij.lang.Language;

public class CustomDispatcher {
    // 快速文档的调度器
    public static BiFunction<String, Language, String> quickDispatcher;
    // rider的`SummaryInfo`建议列表调度器
    public static BiFunction<String, Language, String> riderSummaryDispatcher;
}
