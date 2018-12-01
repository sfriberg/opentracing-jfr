package io.opentracing.contrib.jfr;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutorService;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DifferentSpanTest {

	@Test
	public void spansInMultipleThreads() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Path output = Files.createTempFile("test-recording", ".jfr");
		try {
			// Setup tracers
			MockTracer mockTracer = new MockTracer();
			Tracer tracer = JFRTracer.wrap(mockTracer);

			// Start JFR
			Recording recording = new Recording();
			recording.enable(JFRSpan.class);
			recording.start();

			// Generate spans
			Span span = tracer.buildSpan("test span").start();
			TracedExecutorService executor = new TracedExecutorService(Executors.newSingleThreadExecutor(), tracer);
			executor.submit(() -> {
				tracer.buildSpan("executor span").start().finish();
			}).get(5, TimeUnit.SECONDS);
			span.finish();

			// Stop recording
			recording.stop();
			recording.dump(output);

			// Validate span was created and recorded in JFR
			assertEquals(2, mockTracer.finishedSpans().size());

			Map<String, MockSpan> finishedSpans = mockTracer.finishedSpans().stream().collect(Collectors.toMap(e -> e.operationName(), e -> e));
			List<RecordedEvent> events = RecordingFile.readAllEvents(output);
			assertEquals(finishedSpans.size(), events.size());
			events.stream()
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

	@Test
	public void passingSpanBetweenThreads() throws IOException, InterruptedException, TimeoutException, ExecutionException {
		Path output = Files.createTempFile("test-recording", ".jfr");
		try {
			// Setup tracers
			MockTracer mockTracer = new MockTracer();
			Tracer tracer = JFRTracer.wrap(mockTracer);

			// Start JFR
			Recording recording = new Recording();
			recording.enable(JFRSpan.class);
			recording.start();

			// Generate spans
			Span span = tracer.buildSpan("test span").start();
			TracedExecutorService executor = new TracedExecutorService(Executors.newSingleThreadExecutor(), tracer);
			executor.submit(() -> {
				span.finish();
			}).get(5, TimeUnit.SECONDS);

			// Stop recording
			recording.stop();
			recording.dump(output);

			// Validate span was created and recorded in JFR
			assertEquals(1, mockTracer.finishedSpans().size());

			Map<String, MockSpan> finishedSpans = mockTracer.finishedSpans().stream().collect(Collectors.toMap(e -> e.operationName(), e -> e));
			List<RecordedEvent> events = RecordingFile.readAllEvents(output);
			assertEquals(finishedSpans.size(), events.size());
			events.stream()
					.forEach(e -> {
						MockSpan finishedSpan = finishedSpans.get(e.getString("name"));
						assertNotNull(finishedSpan);
						assertEquals(Long.toString(finishedSpan.context().traceId()), e.getString("traceId"));
						assertEquals(Long.toString(finishedSpan.context().spanId()), e.getString("spanId"));
						assertEquals(finishedSpan.operationName(), e.getString("name"));
						assertNotEquals(Thread.currentThread().getName(), e.getThread().getJavaName());
					});

		} finally {
			Files.delete(output);
		}
	}
}
