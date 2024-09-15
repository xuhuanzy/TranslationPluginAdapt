package cn.yiiguxing.plugin.translate.extensions;


import cn.yiiguxing.plugin.translate.provider.TranslatedDocumentationProvider;

public class TranslateDispatcherImpl{
    public static String translate(String html) {
        return TranslatedDocumentationProvider.Companion.translateNew(html, null, null);
    }
}
