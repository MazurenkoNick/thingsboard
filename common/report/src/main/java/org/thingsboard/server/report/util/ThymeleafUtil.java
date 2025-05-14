package org.thingsboard.server.report.util;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.thymeleaf.templatemode.TemplateMode.HTML;

public class ThymeleafUtil {
    private static final TemplateEngine classEngine;
    private static final TemplateEngine stringEngine;

    static {
        classEngine = new TemplateEngine();
        ClassLoaderTemplateResolver classResolver = new ClassLoaderTemplateResolver();
        classResolver.setPrefix("/");
        classResolver.setSuffix(".html");
        classResolver.setTemplateMode(HTML);
        classResolver.setOrder(1);
        classResolver.setCharacterEncoding(UTF_8);
        classEngine.setTemplateResolver(classResolver);

        stringEngine = new TemplateEngine();
        StringTemplateResolver stringResolver = new StringTemplateResolver();
        stringResolver.setTemplateMode("HTML");
        stringResolver.setResolvablePatterns(Set.of("*"));  // Resolve everything
        stringResolver.setCacheable(false);
        stringResolver.setOrder(2);
        stringEngine.setTemplateResolver(stringResolver);
    }

    public static String render(String templateHtml, Map<String, Object> variables) {
        Context context = new Context(Locale.getDefault(), variables);
        return classEngine.process(templateHtml, context);
    }

    public static String renderFromString(String html, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);

        return stringEngine.process(html, context);
    }
}
