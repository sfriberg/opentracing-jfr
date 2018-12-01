package io.opentracing.contrib.jfr.internal;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import jdk.jfr.FlightRecorder;

import static java.util.Objects.isNull;

public class JFRTracerImpl implements Tracer {

	private final Tracer tracer;
	private final JFRScopeManager scopeManager;
	private FlightRecorder jfr;

	public JFRTracerImpl(Tracer tracer) {
		this.tracer = tracer;
		this.scopeManager = new JFRScopeManager(tracer.scopeManager());
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

	private class JFRScopeManager implements ScopeManager {

		final ThreadLocal<JFRScope> activeScope = new ThreadLocal<>();

		private final ScopeManager scopeManager;

		JFRScopeManager(ScopeManager scopeManager) {
			this.scopeManager = scopeManager;
		}

		@Override
		public Scope activate(Span span, boolean finishSpanOnClose) {
			JFRScope scope;
			if (span instanceof JFRSpan) {
				scope = new JFRScope(this, scopeManager.activate(((JFRSpan) span).unwrap(), finishSpanOnClose), (JFRSpan) span, finishSpanOnClose);
			} else {
				JFRSpan jfrSpan = new JFRSpan(tracer, span, "unknown");
				jfrSpan.begin();
				scope = new JFRScope(this, scopeManager.activate(span, finishSpanOnClose), jfrSpan, finishSpanOnClose);
			}
			activeScope.set(scope);
			return scope;
		}

		@Override
		public Scope active() {
			return activeScope.get();
		}
	}

	private class JFRScope implements Scope {

		private final Scope scope;
		private final JFRSpan jfrSpan;
		private final boolean finishSpanOnClose;
		private final JFRScope parent;
		private final JFRScopeManager manager;

		JFRScope(JFRScopeManager manager, Scope scope, JFRSpan span, boolean finishSpanOnClose) {
			this.scope = scope;
			this.jfrSpan = span;
			this.finishSpanOnClose = finishSpanOnClose;
			this.parent = manager.activeScope.get();
			this.manager = manager;
		}

		@Override
		public void close() {
			scope.close();
			manager.activeScope.set(parent);
			if (finishSpanOnClose) {
				jfrSpan.finishJFR();
			}
		}

		@Override
		public Span span() {
			return jfrSpan;
		}
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
			return startJFR(span);
		}

		@Override
		public Span start() {
			Span span = spanBuilder.start();
			return startJFR(span);
		}

		private Span startJFR(Span span) {
			if (FlightRecorder.isAvailable() && FlightRecorder.isInitialized()) {

				// Avoid synchronization
				if (isNull(jfr)) {
					jfr = FlightRecorder.getFlightRecorder();
				}

				if (!jfr.getRecordings().isEmpty()) {
					var event = new JFRSpan(tracer, span, operationName);
					event.begin();
					return event;
				}
			}
			return span;
		}
	}
}
