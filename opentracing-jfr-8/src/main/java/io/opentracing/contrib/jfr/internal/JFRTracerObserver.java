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

import static java.util.Objects.isNull;

@SuppressWarnings("deprecation")
public class JFRTracerObserver implements TracerObserver {

	private final static Logger LOG = LoggerFactory.getLogger(OpenTracingJFR.class);

	private static volatile Producer producer;

	public JFRTracerObserver() {
	}

	@Override
	public SpanObserver onStart(SpanData sd) {
		if (FlightRecorder.isActive()) {
			if (isNull(producer)) {
				synchronized (JFRTracerObserver.class) {
					if (isNull(producer)) {
						try {
							Producer p = new Producer("OpenTracing", "OpenTracing JFR Events", "http://opentracing.io/");
							p.addEvent(JFRSpanObserver.class);
							p.register();
							producer = p;
						} catch (URISyntaxException | InvalidValueException | InvalidEventDefinitionException ex) {
							LOG.error("Unable to register JFR producer.", ex);
						}
					}
				}
			}

			// Some recording is running
			SpanContext context = ((Span) sd).context();
			JFRSpanObserver event = new JFRSpanObserver(context.toTraceId(), context.toSpanId(), sd.getOperationName());
			event.start();
			return event;
		}

		return NoopSpanObserver.INSTANCE;
	}
}
