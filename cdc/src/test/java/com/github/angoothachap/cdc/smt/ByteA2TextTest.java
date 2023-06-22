package com.github.angoothachap.cdc.smt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.After;
import org.junit.Test;

public class ByteA2TextTest {

  private ByteA2Text<SourceRecord> xform = new ByteA2Text.Value<>();
  @After
  public void tearDown() throws Exception {
    xform.close();
  }

  @Test
  public void copySchemaAndConvertByteaField() {
    final Map<String, Object> props = new HashMap<>();

    props.put("bytea.field.name", "byteaData");

    xform.configure(props);

    final Schema simpleStructSchema = SchemaBuilder.struct().name("name").version(1).doc("doc").field("byteaData", Schema.OPTIONAL_STRING_SCHEMA).build();
    final Struct simpleStruct = new Struct(simpleStructSchema).put("byteaData", "bWV0cmljcyBvZiBMVg==");

    final SourceRecord record = new SourceRecord(null, null, "test", 0, simpleStructSchema, simpleStruct);
    final SourceRecord transformedRecord = xform.apply(record);

    assertEquals(simpleStructSchema.name(), transformedRecord.valueSchema().name());
    assertEquals(simpleStructSchema.version(), transformedRecord.valueSchema().version());
    assertEquals(simpleStructSchema.doc(), transformedRecord.valueSchema().doc());

    assertEquals(Schema.OPTIONAL_STRING_SCHEMA, transformedRecord.valueSchema().field("byteaData").schema());
    assertEquals("metrics of LV", ((Struct) transformedRecord.value()).getString("byteaData"));

  }

  @Test
  public void schemalessByteaField() {
    final Map<String, Object> props = new HashMap<>();

    props.put("bytea.field.name", "byteaData");

    xform.configure(props);

    final SourceRecord record = new SourceRecord(null, null, "test", 0,
        null, Collections.singletonMap("byteaData", "bWV0cmljcyBvZiBMVg=="));

    final SourceRecord transformedRecord = xform.apply(record);
    assertNotNull(((Map) transformedRecord.value()).get("byteaData"));
    assertEquals("metrics of LV", ((Map) transformedRecord.value()).get("byteaData"));

  }
}
