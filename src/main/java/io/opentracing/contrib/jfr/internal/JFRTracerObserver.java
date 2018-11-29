package io.opentracing.contrib.jfr.internal;

import io.opentracing.Span;
import io.opentracing.contrib.api.SpanData;
import io.opentracing.contrib.api.SpanObserver;
import io.opentracing.contrib.api.TracerObserver;
import jdk.jfr.FlightRecorder;

import static java.util.Objects.isNull;

public class JFRTracerObserver implements TracerObserver {

	private FlightRecorder jfr;

	@Override
	public SpanObserver onStart(SpanData sd) {
		if (FlightRecorder.isAvailable() && FlightRecorder.isInitialized()) {

			// Avoid synchronization
			if (isNull(jfr)) {
				jfr = FlightRecorder.getFlightRecorder();
			}

			if (!jfr.getRecordings().isEmpty()) {
				// Some recording is running
				var context = ((Span) sd).context();
				var event = new JFRSpanObserver(context.toTraceId(), context.toSpanId(), sd.getOperationName());
				event.begin();
				return event;
			}
		}

		return NoopSpanObserver.INSTANCE;
	}
}
