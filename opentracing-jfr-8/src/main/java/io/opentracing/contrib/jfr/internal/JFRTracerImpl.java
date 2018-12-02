package io.opentracing.contrib.jfr.internal;

import com.oracle.jrockit.jfr.FlightRecorder;
import com.oracle.jrockit.jfr.Producer;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.opentracing.contrib.jfr.internal.JFRFactory.startJFRSpan;

public class JFRTracerImpl implements Tracer {

	private static final Logger LOG = LoggerFactory.getLogger(JFRTracerImpl.class);

	private static volatile Producer producer;

	private final Tracer tracer;
	private final JFRScopeManager scopeManager;
	private FlightRecorder jfr;

	public JFRTracerImpl(Tracer tracer) {
		this.tracer = tracer;
		this.scopeManager = new JFRScopeManager(tracer);
	}

	@Override
	public ScopeManager scopeManager() {
		return scopeManager;
	}

	@Override
	public Span activeSpan() {
		return tracer.activeSpan();
	}

	@Override
	public SpanBuilder buildSpan(String operationName) {
		SpanBuilder spanBuilder = tracer.buildSpan(operationName);
		return new JFRSpanBuilder(operationName, spanBuilder);
	}

	@Override
	public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
		tracer.inject(spanContext, format, carrier);
	}

	@Override
	public <C> SpanContext extract(Format<C> format, C carrier) {
		return tracer.extract(format, carrier);
	}

	private class JFRSpanBuilder implements SpanBuilder {

		private final SpanBuilder spanBuilder;
		private final String operationName;

		JFRSpanBuilder(String operationName, SpanBuilder spanBuilder) {
			this.operationName = operationName;
			this.spanBuilder = spanBuilder;
		}

		@Override
		public SpanBuilder asChildOf(SpanContext parent) {
			return spanBuilder.asChildOf(parent);
		}

		@Override
		public SpanBuilder asChildOf(Span parent) {
			return spanBuilder.asChildOf(parent);
		}

		@Override
		public SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
			return spanBuilder.addReference(referenceType, referencedContext);
		}

		@Override
		public SpanBuilder ignoreActiveSpan() {
			return spanBuilder.ignoreActiveSpan();
		}

		@Override
		public SpanBuilder withTag(String key, String value) {
			return spanBuilder.withTag(key, value);
		}

		@Override
		public SpanBuilder withTag(String key, boolean value) {
			return spanBuilder.withTag(key, value);
		}

		@Override
		public SpanBuilder withTag(String key, Number value) {
			return spanBuilder.withTag(key, value);
		}

		@Override
		public SpanBuilder withStartTimestamp(long microseconds) {
			return spanBuilder.withStartTimestamp(microseconds);
		}

		@Override
		public Scope startActive(boolean finishSpanOnClose) {
			return scopeManager.activate(start(), finishSpanOnClose);
		}

		@Override
		@Deprecated
		public Span startManual() {
			Span span = spanBuilder.startManual();
			return startJFRSpan(tracer, span, operationName);
		}

		@Override
		public Span start() {
			Span span = spanBuilder.start();
			return startJFRSpan(tracer, span, operationName);
		}
	}
}
