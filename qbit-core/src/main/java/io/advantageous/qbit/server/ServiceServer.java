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

package io.advantageous.qbit.server;

/**
 * Represents a server that marshals method calls to a service.
 * Created by Richard on 11/14/14.
 */
public interface ServiceServer extends Server {
    default ServiceServer initServices(Object... services) {
        throw new IllegalStateException("Not implemented");
    }


    default ServiceServer initServices(Iterable services) {
        throw new IllegalStateException("Not implemented");
    }

    default ServiceServer flush() {
        throw new IllegalStateException("Not implemented");
    }


    default ServiceServer startServer() {
        start();
        return this;
    }
}
