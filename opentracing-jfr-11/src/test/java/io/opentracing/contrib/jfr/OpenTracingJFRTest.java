package io.opentracing.contrib.jfr;

import io.opentracing.Tracer;
import io.opentracing.contrib.jfr.internal.JFRSpan;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OpenTracingJFRTest {

	/**
	 * Test JFR gets the generated span
	 *
	 * @throws java.io.IOException on error
	 */
	@Test
	public void testDecorate() throws IOException {
		Path output = Files.createTempFile("test-recording", ".jfr");
		try {
			// Setup tracers
			MockTracer mockTracer = new MockTracer();
			Tracer tracer = JFRTracer.wrap(mockTracer);

			// Start JFR
			Recording recording = new Recording();
			recording.enable(JFRSpan.class);
			recording.start();

			// Generate span
 			tracer.buildSpan("test span").start().finish();

			// Stop recording
			recording.stop();
			recording.dump(output);

			// Validate span was created and recorded in JFR
			assertEquals(1, mockTracer.finishedSpans().size());

			Map<String, MockSpan> finishedSpans = mockTracer.finishedSpans().stream().collect(Collectors.toMap(e -> e.operationName(), e -> e));
			List<RecordedEvent> readAllEvents = RecordingFile.readAllEvents(output);
			assertEquals(finishedSpans.size(), readAllEvents.size());
			RecordingFile.readAllEvents(output).stream()
					.forEach(e -> {
						MockSpan finishedSpan = finishedSpans.get(e.getString("name"));
						assertNotNull(finishedSpan);
						assertEquals(Long.toString(finishedSpan.context().traceId()), e.getString("traceId"));
						assertEquals(Long.toString(finishedSpan.context().spanId()), e.getString("spanId"));
						assertEquals(finishedSpan.operationName(), e.getString("name"));
					});

		} finally {
			Files.delete(output);
		}
	}
}
