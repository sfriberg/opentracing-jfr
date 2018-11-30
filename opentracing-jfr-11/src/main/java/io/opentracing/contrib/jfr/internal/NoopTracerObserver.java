package io.opentracing.contrib.jfr.internal;

import io.opentracing.contrib.api.SpanData;
import io.opentracing.contrib.api.SpanObserver;
import io.opentracing.contrib.api.TracerObserver;

public final class NoopTracerObserver implements TracerObserver {

	public static final TracerObserver INSTANCE = new NoopTracerObserver();

	private NoopTracerObserver() {
	}

	@Override
	public SpanObserver onStart(SpanData sd) {
		return NoopSpanObserver.INSTANCE;
	}
}
