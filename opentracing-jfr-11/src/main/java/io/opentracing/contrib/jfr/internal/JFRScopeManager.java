package io.opentracing.contrib.jfr.internal;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.Tracer;

public class JFRScopeManager implements ScopeManager {

	final ThreadLocal<JFRScope> activeScope = new ThreadLocal<>();
	private final Tracer tracer;

	JFRScopeManager(Tracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public Scope activate(Span span, boolean finishSpanOnClose) {
		JFRScope scope;
		if (span instanceof JFRSpan) {
			scope = new JFRScope(tracer, this, tracer.scopeManager().activate(((JFRSpan) span).unwrap(), finishSpanOnClose), (JFRSpan) span, finishSpanOnClose);
		} else {
			JFRSpan jfrSpan = new JFRSpan(tracer, span, "unknown");
			jfrSpan.begin();
			scope = new JFRScope(tracer, this, tracer.scopeManager().activate(span, finishSpanOnClose), jfrSpan, finishSpanOnClose);
		}
		activeScope.set(scope);
		return scope;
	}

	@Override
	public Scope active() {
		return activeScope.get();
	}

}
