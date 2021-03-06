/*
 * Copyright (c) 2015. Rick Hightower, Geoff Chandler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * QBit - The Microservice lib for Java : JSON, WebSocket, REST. Be The Web!
 */

package io.advantageous.qbit.spi;

import io.advantageous.qbit.message.Message;
import io.advantageous.qbit.message.MethodCall;
import io.advantageous.qbit.message.Response;
import io.advantageous.qbit.service.Protocol;
import io.advantageous.qbit.util.MultiMap;
import org.boon.core.reflection.fields.FieldAccess;
import org.boon.json.JsonSerializer;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.serializers.FieldFilter;
import org.boon.primitive.CharBuf;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static io.advantageous.qbit.service.Protocol.*;

/**
 * Protocol encoder.
 *
 * @author Rick Hightower
 */
public class BoonProtocolEncoder implements ProtocolEncoder {

    private ThreadLocal<JsonSerializer> jsonSerializer = new ThreadLocal<JsonSerializer>() {
        @Override
        protected JsonSerializer initialValue() {
            return new JsonSerializerFactory().addFilter(new FieldFilter() {
                @Override
                public boolean include(Object parent, FieldAccess fieldAccess) {
                    return !fieldAccess.name().equals("metaClass");
                }
            }).create();
        }
    };


    private ThreadLocal<CharBuf> bufRef = new ThreadLocal<CharBuf>() {
        @Override
        protected CharBuf initialValue() {
            CharBuf buf = CharBuf.createCharBuf(1000);

            return buf;
        }

        ;
    };

    @Override
    public String encodeAsString(Response<Object> response) {
        CharBuf buf = CharBuf.createCharBuf();
        encodeAsString(buf, response, true);
        return buf.toString();
    }

    @Override
    public String encodeAsString(MethodCall<Object> methodCall) {
        CharBuf buf = CharBuf.createCharBuf();
        encodeAsString(buf, methodCall, true);
        return buf.toString();
    }

    @Override
    public String encodeAsString(Collection<Message<Object>> messages) {
        CharBuf buf = bufRef.get();
        buf.recycle();

        buf.addChar(PROTOCOL_MARKER);
        buf.addChar(PROTOCOL_VERSION_1_GROUP);
        int index = 0;

        for ( Message<Object> message : messages ) {

            boolean encodeAddress = index == 0;

            if ( message instanceof MethodCall ) {
                encodeAsString(buf, ( MethodCall<Object> ) message, encodeAddress);
            } else if ( message instanceof Response ) {
                encodeAsString(buf, ( Response<Object> ) message, encodeAddress);
            }
            buf.addChar(PROTOCOL_MESSAGE_SEPARATOR);

            index++;
        }

        return buf.toString();

    }


    private void encodeAsString(CharBuf buf, MethodCall<Object> methodCall, boolean encodeAddress) {
        buf.addChar(PROTOCOL_MARKER);
        buf.addChar(PROTOCOL_VERSION_1);
        buf.addChar(PROTOCOL_SEPARATOR);
        buf.add(methodCall.id());
        buf.addChar(PROTOCOL_SEPARATOR);
        buf.add(methodCall.address());
        buf.addChar(PROTOCOL_SEPARATOR);

        buf.add(methodCall.returnAddress());

//        if (encodeAddress) {
//            buf.add(methodCall.returnAddress());
//
//        } else {
//            buf.add("same");
//        }

        buf.addChar(PROTOCOL_SEPARATOR);
        encodeHeadersAndParams(buf, methodCall.headers());
        buf.addChar(PROTOCOL_SEPARATOR);
        encodeHeadersAndParams(buf, methodCall.params());
        buf.addChar(PROTOCOL_SEPARATOR);
        buf.add(methodCall.objectName());
        buf.addChar(PROTOCOL_SEPARATOR);
        buf.add(methodCall.name());
        buf.addChar(PROTOCOL_SEPARATOR);
        buf.add(methodCall.timestamp());
        buf.addChar(PROTOCOL_SEPARATOR);
        final Object body = methodCall.body();
        final JsonSerializer serializer = jsonSerializer.get();
        if ( body instanceof Iterable ) {
            Iterable iter = ( Iterable ) body;
            for ( Object bodyPart : iter ) {

                serializer.serialize(buf, bodyPart);
                buf.addChar(PROTOCOL_ARG_SEPARATOR);
            }
        } else if ( body instanceof Object[] ) {
            Object[] args = ( Object[] ) body;

            for ( int index = 0; index < args.length; index++ ) {
                Object bodyPart = args[ index ];
                serializer.serialize(buf, bodyPart);
                buf.addChar(PROTOCOL_ARG_SEPARATOR);
            }
        } else if ( body != null ) {
            serializer.serialize(buf, body);
        }
    }


    private void encodeAsString(CharBuf buf, Response<Object> response, boolean encodeAddress) {
        buf.addChar(PROTOCOL_MARKER);
        buf.addChar(PROTOCOL_VERSION_1_RESPONSE);
        buf.addChar(PROTOCOL_SEPARATOR);
        buf.add(response.id());
        buf.addChar(PROTOCOL_SEPARATOR);
        buf.add(response.address());
        buf.addChar(PROTOCOL_SEPARATOR);


        buf.add(response.returnAddress());
//        if (encodeAddress) {
//            buf.add(response.returnAddress());
//        } else {
//            buf.add("same");
//        }
        buf.addChar(PROTOCOL_SEPARATOR);
        buf.addChar(PROTOCOL_SEPARATOR); //reserved for header
        buf.addChar(PROTOCOL_SEPARATOR); //reserved for params
        buf.addChar(PROTOCOL_SEPARATOR); //reserved for object name
        buf.addChar(PROTOCOL_SEPARATOR); //reserved for method name
        buf.add(response.timestamp());
        buf.addChar(PROTOCOL_SEPARATOR);
        buf.add(response.wasErrors() ? 1 : 0);
        buf.addChar(PROTOCOL_SEPARATOR);

        final Object body = response.body();
        final JsonSerializer serializer = jsonSerializer.get();

        if ( body != null ) {
            serializer.serialize(buf, body);
        } else {
            buf.addNull();
        }
    }

    private void encodeHeadersAndParams(CharBuf buf, MultiMap<String, String> headerOrParams) {

        if ( headerOrParams == null ) {
            return;
        }

        final Map<? extends String, ? extends Collection<String>> map = headerOrParams.baseMap();
        final Set<? extends Map.Entry<? extends String, ? extends Collection<String>>> entries = map.entrySet();
        for ( Map.Entry<? extends String, ? extends Collection<String>> entry : entries ) {

            final Collection<String> values = entry.getValue();

            if ( values.size() == 0 ) {
                continue;
            }

            buf.add(entry.getKey());
            buf.addChar(Protocol.PROTOCOL_KEY_HEADER_DELIM);

            for ( String value : values ) {
                buf.add(value);
                buf.addChar(Protocol.PROTOCOL_VALUE_HEADER_DELIM);
            }
            buf.addChar(Protocol.PROTOCOL_ENTRY_HEADER_DELIM);

        }
    }
}
