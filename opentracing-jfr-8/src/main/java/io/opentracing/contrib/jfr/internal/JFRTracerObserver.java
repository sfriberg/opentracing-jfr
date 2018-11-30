package io.opentracing.contrib.jfr.internal;

import com.oracle.jrockit.jfr.FlightRecorder;
import com.oracle.jrockit.jfr.InvalidEventDefinitionException;
import com.oracle.jrockit.jfr.InvalidValueException;
import com.oracle.jrockit.jfr.Producer;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.contrib.api.SpanData;
import io.opentracing.contrib.api.SpanObserver;
import io.opentracing.contrib.api.TracerObserver;
import io.opentracing.contrib.jfr.OpenTracingJFR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;

@SuppressWarnings("deprecation")
public class JFRTracerObserver implements TracerObserver {

	private final static Logger LOG = LoggerFactory.getLogger(OpenTracingJFR.class);

	private static boolean initialized;

	public JFRTracerObserver() {
	}

	@Override
	public SpanObserver onStart(SpanData sd) {
		if (FlightRecorder.isActive()) {
			if (!initialized) {
				synchronized (JFRTracerObserver.class) {
					if (!initialized) {
						try {
							Producer p = new Producer("OpenTracing", "OpenTracing JFR Events", "http://opentracing.io/");
							p.addEvent(JFRSpanObserver.class);
							p.register();
							initialized = true;
						} catch (URISyntaxException | InvalidValueException | InvalidEventDefinitionException ex) {
							LOG.error("Unable to register JFR producer.", ex);
						}
					}
				}
			}

			// Some recording is running
			SpanContext context = ((Span) sd).context();
			JFRSpanObserver event = new JFRSpanObserver(context.toTraceId(), context.toSpanId(), sd.getOperationName());
			event.begin();
			return event;
		}

		return NoopSpanObserver.INSTANCE;
	}
}
