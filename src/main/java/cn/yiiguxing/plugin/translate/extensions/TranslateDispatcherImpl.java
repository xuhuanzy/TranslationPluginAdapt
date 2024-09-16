package cn.yiiguxing.plugin.translate.extensions;


import cn.yiiguxing.plugin.translate.documentation.TranslateType;
import cn.yiiguxing.plugin.translate.provider.TranslatedDocumentationProvider;
import com.intellij.lang.Language;

public class TranslateDispatcherImpl {
    public static String translate(String html, String language, int type) {
        return TranslatedDocumentationProvider.Companion.translateNew(
                html,
                Language.findLanguageByID(language),
                TranslateType.getEntries().get(type)
        );
    }
}
