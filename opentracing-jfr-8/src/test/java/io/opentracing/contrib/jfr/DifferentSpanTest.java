package io.opentracing.contrib.jfr;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import oracle.jrockit.jfr.parser.FLREvent;
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

import static io.opentracing.contrib.jfr.JFRTestUtils.getJfrConfig;
import static io.opentracing.contrib.jfr.JFRTestUtils.startJFR;
import static io.opentracing.contrib.jfr.JFRTestUtils.stopJfr;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DifferentSpanTest {

	@Test
	public void spansInMultipleThreads() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Path jfrConfig = getJfrConfig();
		Path output = Files.createTempFile("test-recording", ".jfr");

		try {
			// Setup tracers
			MockTracer mockTracer = new MockTracer();
			Tracer tracer = OpenTracingJFR.decorate(mockTracer);

			// Start JFR
			startJFR(jfrConfig);

			// Generate spans
			Span span = tracer.buildSpan("test span").start();
			TracedExecutorService executor = new TracedExecutorService(Executors.newSingleThreadExecutor(), tracer);
			executor.submit(() -> {
				tracer.buildSpan("executor span").start().finish();
			}).get(5, TimeUnit.SECONDS);
			span.finish();

			// Stop recording
			//Ugly Sleep for now
			Thread.sleep(100);
			List<FLREvent> events = stopJfr(output);

			// Validate span was created and recorded in JFR
			assertEquals(2, mockTracer.finishedSpans().size());

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
			Files.delete(output);
		}
	}

	@Test
	public void passingSpanBetweenThreads() throws IOException, InterruptedException, TimeoutException, ExecutionException {
		Path jfrConfig = getJfrConfig();
		Path output = Files.createTempFile("test-recording", ".jfr");

		try {
			// Setup tracers
			MockTracer mockTracer = new MockTracer();
			Tracer tracer = OpenTracingJFR.decorate(mockTracer);

			// Start JFR
			startJFR(jfrConfig);

			// Generate spans
			Span span = tracer.buildSpan("test span").start();
			TracedExecutorService executor = new TracedExecutorService(Executors.newSingleThreadExecutor(), tracer);
			executor.submit(() -> {
				span.finish();
			}).get(5, TimeUnit.SECONDS);

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
						assertNotEquals(Thread.currentThread().getName(), e.getThread());
					});

		} finally {
			Files.delete(output);
		}
	}
}
