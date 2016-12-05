package org.doogie.liquido.rest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Mapper that can convert a Mongo ObjectID into a 24 char hex String.
 * This is needed, because standard Jackson would serialize the ObjectId class into a JSON object wit timestampe, machineid etc.
 */
@Component
public class MongoMapperModule extends SimpleModule {

  @Override
  public void setupModule(SetupContext context) {
    SimpleSerializers serializers = new SimpleSerializers();
    //SimpleDeserializers deserializers = new SimpleDeserializers();

    serializers.addSerializer(ObjectId.class, new JsonSerializer<ObjectId>() {
      @Override
      public void serialize(ObjectId objectId, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        //System.out.println("======= serialize("+objectId.toHexString()+")");
        jsonGenerator.writeString(objectId.toHexString());
      }
    });

    context.addSerializers(serializers);
  }


}
