package io.opentracing.contrib.jfr.internal;

import io.opentracing.Scope;
import io.opentracing.Span;

public class BasicScope implements Scope {

	private final JFRScopeManager manager;
	private final Scope parent;
	private final Scope scope;

	public BasicScope(JFRScopeManager manager, Scope scope) {
		this.manager = manager;
		this.parent = manager.active();
		this.scope = scope;
	}

	@Override
	public void close() {
		scope.close();
		manager.setActive(parent);
	}

	@Override
	public Span span() {
		return scope.span();
	}
}
