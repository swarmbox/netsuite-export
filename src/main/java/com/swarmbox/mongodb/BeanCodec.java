/*
 * Copyright (c) 2015
 * SwarmBox
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the {organization} nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.swarmbox.mongodb;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.NotImplementedException;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static com.swarmbox.netsuite.Helpers.wrapCheckedException;

public class BeanCodec<T> implements Codec<T> {

    private final Class<T> type;
    private final Codec<Document> codec;

    public final Map<String,Function<T,Object>> addProperties = new HashMap<>();
    public final Set<String> skipProperties = new HashSet<>();

    public BeanCodec(final Class<T> type, CodecRegistry registry) {
        this.type = type;
        this.codec = registry.get(Document.class);
        this.skipProperties.add("class");
    }

    @Override
    public void encode(BsonWriter writer, T bean, EncoderContext encoderContext) {
        final Map<String,Object> attributes;
        try {
            attributes = PropertyUtils.describe(bean);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw wrapCheckedException("Failed to get attributes for bean", e);
        }
        addProperties.forEach((k,v) -> {
            attributes.put(k, v.apply(bean));
        });
        skipProperties.forEach(attributes::remove);
        codec.encode(writer, new Document(attributes), encoderContext);
    }

    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new NotImplementedException("BeanCodec.decode is not implemented");
    }

    @Override
    public Class<T> getEncoderClass() {
        return this.type;
    }
}
