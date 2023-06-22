package com.github.angoothachap.cdc.smt;

import static org.apache.kafka.connect.transforms.util.Requirements.requireMap;
import static org.apache.kafka.connect.transforms.util.Requirements.requireStruct;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.cache.Cache;
import org.apache.kafka.common.cache.LRUCache;
import org.apache.kafka.common.cache.SynchronizedCache;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.transforms.util.SimpleConfig;

public abstract class ByteA2Text<R extends ConnectRecord<R>> implements Transformation<R> {


  private interface ConfigName {

    String BYTEA_FIELD_NAME = "bytea.field.name";
  }

  public static final ConfigDef CONFIG_DEF = new ConfigDef()
      .define(ConfigName.BYTEA_FIELD_NAME, ConfigDef.Type.STRING, "byteaData",
          ConfigDef.Importance.HIGH,
          "Field name for bytea");

  private static final String PURPOSE = "Converting bytea to String";

  private String fieldName;

  private Cache<Schema, Schema> schemaUpdateCache;

  @Override
  public void configure(Map<String, ?> props) {
    final SimpleConfig config = new SimpleConfig(CONFIG_DEF, props);
    fieldName = config.getString(ConfigName.BYTEA_FIELD_NAME);

    schemaUpdateCache = new SynchronizedCache<>(new LRUCache<Schema, Schema>(16));
  }


  @Override
  public R apply(R record) {
    if (operatingSchema(record) == null) {
      return applySchemaless(record);
    } else {
      return applyWithSchema(record);
    }
  }

  private R applySchemaless(R record) {
    final Map<String, Object> value = requireMap(operatingValue(record), PURPOSE);

    final byte[] decodedBytes = Base64.getDecoder().decode((String) value.get(fieldName));
    final String decodedString = new String(decodedBytes);

    final Map<String, Object> updatedValue = new HashMap<>(value);
    updatedValue.put(fieldName,
        decodedString);
    return newRecord(record, null, updatedValue);
  }

  private R applyWithSchema(R record) {
    final Struct value = requireStruct(operatingValue(record), PURPOSE);
    final byte[] decodedBytes = Base64.getDecoder().decode((String) value.get(fieldName));
    final String decodedString = new String(decodedBytes);

    final Struct updatedValue = new Struct(value.schema());

    for (Field field : value.schema().fields()) {
      if (!field.name().equals(fieldName)) {
        updatedValue.put(field.name(), value.get(field));
      }
      updatedValue.put(field.name(), decodedString);

    }
    return newRecord(record, value.schema(), updatedValue);
  }

  @Override
  public ConfigDef config() {
    return CONFIG_DEF;
  }

  @Override
  public void close() {
    schemaUpdateCache = null;
  }

  protected abstract Schema operatingSchema(R record);

  protected abstract Object operatingValue(R record);

  protected abstract R newRecord(R record, Schema updatedSchema, Object updatedValue);

  public static class Key<R extends ConnectRecord<R>> extends ByteA2Text<R> {

    @Override
    protected Schema operatingSchema(R record) {
      return record.keySchema();
    }

    @Override
    protected Object operatingValue(R record) {
      return record.key();
    }

    @Override
    protected R newRecord(R record, Schema updatedSchema, Object updatedValue) {
      return record.newRecord(record.topic(), record.kafkaPartition(), updatedSchema, updatedValue,
          record.valueSchema(), record.value(), record.timestamp());
    }

  }

  public static class Value<R extends ConnectRecord<R>> extends ByteA2Text<R> {

    @Override
    protected Schema operatingSchema(R record) {
      return record.valueSchema();
    }

    @Override
    protected Object operatingValue(R record) {
      return record.value();
    }

    @Override
    protected R newRecord(R record, Schema updatedSchema, Object updatedValue) {
      return record.newRecord(record.topic(), record.kafkaPartition(), record.keySchema(),
          record.key(), updatedSchema, updatedValue, record.timestamp());
    }

  }
}

