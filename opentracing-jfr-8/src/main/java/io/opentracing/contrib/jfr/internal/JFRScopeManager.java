package io.opentracing.contrib.jfr.internal;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.Tracer;

import static io.opentracing.contrib.jfr.internal.JFRFactory.startJFRScope;
import static io.opentracing.contrib.jfr.internal.JFRFactory.startJFRSpan;

public class JFRScopeManager implements ScopeManager {

	final ThreadLocal<Scope> activeScope = new ThreadLocal<>();
	private final Tracer tracer;

	JFRScopeManager(Tracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public Scope activate(Span span, boolean finishSpanOnClose) {
		Scope scope;
		if (span instanceof JFRSpan) {
			scope = startJFRScope(tracer, this, tracer.scopeManager().activate(((JFRSpan) span).unwrap(), finishSpanOnClose), ((JFRSpan) span).getName(), span, finishSpanOnClose);
		} else {
			String operationName = "unknown";
			scope = startJFRScope(tracer, this, tracer.scopeManager().activate(span, finishSpanOnClose), operationName, startJFRSpan(tracer, span, operationName), finishSpanOnClose);
		}
		activeScope.set(scope);
		return scope;
	}

	@Override
	public Scope active() {
		return activeScope.get();
	}
}
