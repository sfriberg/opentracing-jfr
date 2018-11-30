package io.opentracing.contrib.jfr.internal;

import io.opentracing.contrib.api.SpanData;
import io.opentracing.contrib.api.SpanObserver;

import java.util.Map;

final class NoopSpanObserver implements SpanObserver {

	static final SpanObserver INSTANCE = new NoopSpanObserver();

	NoopSpanObserver() {
	}

	@Override
	public void onSetOperationName(SpanData spanData, String operationName) {
	}

	@Override
	public void onSetTag(SpanData spanData, String key, Object value) {
	}

	@Override
	public void onSetBaggageItem(SpanData spanData, String key, String value) {
	}

	@Override
	public void onLog(SpanData spanData, long timestampMicroseconds, Map<String, ?> fields) {
	}

	@Override
	public void onLog(SpanData spanData, long timestampMicroseconds, String event) {
	}

	@Override
	public void onFinish(SpanData spanData, long finishMicros) {
	}
}
