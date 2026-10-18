package de.simplepvparena.support;

import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.message.Messages;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

public final class LanguageExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        UseLanguage language = AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), UseLanguage.class)
                .or(() -> AnnotationSupport.findAnnotation(context.getRequiredTestClass(), UseLanguage.class))
                .orElseThrow(() -> new IllegalStateException("the test names no language"));
        Messages.use(MessageCatalogLoader.shippedCatalog(language.value()));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Messages.reset();
    }
}
