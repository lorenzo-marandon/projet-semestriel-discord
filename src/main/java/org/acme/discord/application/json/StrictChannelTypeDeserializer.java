package org.acme.discord.application.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.acme.discord.domain.ChannelType;

import java.io.IOException;

/**
 * Deserializes channel types from their exact textual API representation.
 */
public final class StrictChannelTypeDeserializer extends JsonDeserializer<ChannelType> {

    @Override
    public ChannelType deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            return (ChannelType) context.handleUnexpectedToken(ChannelType.class, parser);
        }

        String value = parser.getText();
        try {
            return ChannelType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return (ChannelType) context.handleWeirdStringValue(
                    ChannelType.class,
                    value,
                    "Expected one of: TEXT, VOICE");
        }
    }
}
