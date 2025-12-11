package org.friesoft.porturl.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

public class NativeBinaryHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Keycloak & Resteasy Core
        hints.reflection().registerType(TypeReference.of("org.keycloak.admin.client.spi.ResteasyClientClassicProvider"),
                hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        hints.reflection().registerType(TypeReference.of("org.keycloak.admin.client.JacksonProvider"),
                hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        hints.reflection().registerType(TypeReference.of("org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl"),
                hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
        hints.reflection().registerType(TypeReference.of("org.jboss.resteasy.client.jaxrs.internal.proxy.ProxyBuilderImpl"),
                hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));

        // Resteasy Core (jaxrs)
        registerResteasyLogging(hints, "org.jboss.resteasy.resteasy_jaxrs.i18n.Messages");
        registerResteasyLogging(hints, "org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages");

        // Resteasy Client
        registerResteasyLogging(hints, "org.jboss.resteasy.client.jaxrs.i18n.Messages");
        registerResteasyLogging(hints, "org.jboss.resteasy.client.jaxrs.i18n.LogMessages");

        // Resteasy JAXB Provider
        registerResteasyLogging(hints, "org.jboss.resteasy.plugins.providers.jaxb.i18n.Messages");
        registerResteasyLogging(hints, "org.jboss.resteasy.plugins.providers.jaxb.i18n.LogMessages");

        // Resteasy Multipart Provider (Commonly used)
        registerResteasyLogging(hints, "org.jboss.resteasy.plugins.providers.multipart.i18n.Messages");
        registerResteasyLogging(hints, "org.jboss.resteasy.plugins.providers.multipart.i18n.LogMessages");
    }

    private void registerResteasyLogging(RuntimeHints hints, String baseClassName) {
        // Register the interface
        hints.reflection().registerType(TypeReference.of(baseClassName),
                hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));

        // Register the implementation (usually _$bundle or _$logger)
        // We register both possible suffixes just in case, or we could distinguish based on name.
        // Messages -> _$bundle, LogMessages -> _$logger
        if (baseClassName.endsWith("Messages") && !baseClassName.endsWith("LogMessages")) {
             hints.reflection().registerType(TypeReference.of(baseClassName + "_$bundle"),
                hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.ACCESS_PUBLIC_FIELDS));
        } else if (baseClassName.endsWith("LogMessages")) {
             hints.reflection().registerType(TypeReference.of(baseClassName + "_$logger"),
                hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.ACCESS_PUBLIC_FIELDS));
        }
    }
}
