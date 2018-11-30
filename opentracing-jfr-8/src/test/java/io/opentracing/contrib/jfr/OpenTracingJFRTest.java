package io.opentracing.contrib.jfr;

import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import oracle.jrockit.jfr.parser.FLREvent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.opentracing.contrib.jfr.JFRTestUtils.getJfrConfig;
import static io.opentracing.contrib.jfr.JFRTestUtils.startJFR;
import static io.opentracing.contrib.jfr.JFRTestUtils.stopJfr;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OpenTracingJFRTest {

	/**
	 * Test JFR gets the generated span
	 *
	 * @throws java.io.IOException on error
	 */
	@Test
	@SuppressWarnings("deprecation")
	public void testDecorate() throws IOException {
		Path jfrConfig = getJfrConfig();
		Path output = Files.createTempFile("opentracing", ".jfr");

		try {

			// Setup tracers
			MockTracer mockTracer = new MockTracer();
			Tracer tracer = OpenTracingJFR.decorate(mockTracer);
			// Start JFR
			startJFR(jfrConfig);

			// Generate span
			tracer.buildSpan("test span").start().finish();

			// Stop recording
			List<FLREvent> events = stopJfr(output);

			// Validate span was created and recorded in JFR
			assertEquals(1, mockTracer.finishedSpans().size());

			Map<String, MockSpan> finishedSpans = mockTracer.finishedSpans().stream().collect(Collectors.toMap(e -> e.operationName(), e -> e));
			assertEquals(finishedSpans.size(), events.size());
			events.stream()
					.forEach(e -> {
						MockSpan finishedSpan = finishedSpans.get(e.getValue("name").toString());
						assertNotNull(finishedSpan);
						assertEquals(finishedSpan.context().toTraceId(), e.getValue("traceId"));
						assertEquals(finishedSpan.context().toSpanId(), e.getValue("spanId"));
						assertEquals(finishedSpan.operationName(), e.getValue("name"));
					});

		} finally {
			Files.delete(jfrConfig);
			Files.delete(output);
		}
	}
}
