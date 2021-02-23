package com.vertx.reproducer;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.parsetools.JsonEventType;
import io.vertx.core.parsetools.JsonParser;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class JsonParserAsyncFileTest {

    private static final Logger log = LoggerFactory.getLogger(JsonParserAsyncFileTest.class);

    private String jsonObjects;
    private File tmpFile;
    private Vertx vertx;

    @Before
    public void setUp() throws Exception {
        vertx = Vertx.vertx();
        jsonObjects = "" + "{\"field_0\": \"value_0\"}" + "{\"field_1\": \"value_1\"}" + "{\"field_2\": \"value_2\"}"
                + "{\"field_3\": \"value_3\"}";
        tmpFile = File.createTempFile("test", ".json");
        tmpFile.deleteOnExit();
        FileWriter writer = new FileWriter(tmpFile);
        writer.write(jsonObjects);
        writer.flush();
        writer.close();
    }

    @Test
    public void parseWithExceptionHandler(TestContext context) throws Exception {
        Async async = context.async();
        AtomicInteger counter = new AtomicInteger(0);
        AsyncFile file = vertx.fileSystem().openBlocking(tmpFile.getAbsolutePath(), new OpenOptions());
        JsonParser parser = JsonParser.newParser(file);
        parser.objectValueMode();
        parser.handler(event -> {
            parser.pause(); // Needed pause for doing something

            context.assertNotNull(event);
            context.assertEquals(JsonEventType.VALUE, event.type());
            counter.incrementAndGet();

            parser.resume(); // There and then resume
        });
        parser.exceptionHandler(context::fail);
        parser.endHandler(end -> {
            context.assertEquals(4, counter.get());
            log.info("Successfully parsed " + counter.get() + " items of 4");
            async.complete();
        });
    }
}
