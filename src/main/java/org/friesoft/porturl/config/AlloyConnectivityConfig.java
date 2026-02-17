package org.friesoft.porturl.config;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.friesoft.porturl.service.AlloyHealthService;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "porturl.otel.enabled", havingValue = "true")
public class AlloyConnectivityConfig {

    @Component
    public static class AlloyExporterWrapperPostProcessor implements BeanPostProcessor {

        private final AlloyHealthService healthService;

        public AlloyExporterWrapperPostProcessor(@Lazy AlloyHealthService healthService) {
            this.healthService = healthService;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (bean instanceof SpanExporter && !(bean instanceof HealthCheckingSpanExporter)) {
                return new HealthCheckingSpanExporter((SpanExporter) bean, healthService);
            }
            if (bean instanceof LogRecordExporter && !(bean instanceof HealthCheckingLogRecordExporter)) {
                return new HealthCheckingLogRecordExporter((LogRecordExporter) bean, healthService);
            }
            if (bean instanceof MetricExporter && !(bean instanceof HealthCheckingMetricExporter)) {
                return new HealthCheckingMetricExporter((MetricExporter) bean, healthService);
            }
            return bean;
        }
    }

    private static class HealthCheckingSpanExporter implements SpanExporter {
        private final SpanExporter delegate;
        private final AlloyHealthService healthService;

        HealthCheckingSpanExporter(SpanExporter delegate, AlloyHealthService healthService) {
            this.delegate = delegate;
            this.healthService = healthService;
        }

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            if (healthService.isUp()) {
                return delegate.export(spans);
            }
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return delegate.flush();
        }

        @Override
        public CompletableResultCode shutdown() {
            return delegate.shutdown();
        }
    }

    private static class HealthCheckingLogRecordExporter implements LogRecordExporter {
        private final LogRecordExporter delegate;
        private final AlloyHealthService healthService;

        HealthCheckingLogRecordExporter(LogRecordExporter delegate, AlloyHealthService healthService) {
            this.delegate = delegate;
            this.healthService = healthService;
        }

        @Override
        public CompletableResultCode export(Collection<LogRecordData> logs) {
            if (healthService.isUp()) {
                return delegate.export(logs);
            }
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return delegate.flush();
        }

        @Override
        public CompletableResultCode shutdown() {
            return delegate.shutdown();
        }
    }

    private static class HealthCheckingMetricExporter implements MetricExporter {
        private final MetricExporter delegate;
        private final AlloyHealthService healthService;

        HealthCheckingMetricExporter(MetricExporter delegate, AlloyHealthService healthService) {
            this.delegate = delegate;
            this.healthService = healthService;
        }

        @Override
        public CompletableResultCode export(Collection<MetricData> metrics) {
            if (healthService.isUp()) {
                return delegate.export(metrics);
            }
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return delegate.flush();
        }

        @Override
        public CompletableResultCode shutdown() {
            return delegate.shutdown();
        }

        @Override
        public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
            return delegate.getAggregationTemporality(instrumentType);
        }
    }
}
