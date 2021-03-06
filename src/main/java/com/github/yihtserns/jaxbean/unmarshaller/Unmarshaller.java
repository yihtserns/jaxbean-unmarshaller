/*
 * Copyright 2015 yihtserns.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.yihtserns.jaxbean.unmarshaller;

import com.github.yihtserns.jaxbean.unmarshaller.api.BeanHandler;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author yihtserns
 */
interface Unmarshaller<N extends Node> {

    Object unmarshal(N node, BeanHandler beanHandler) throws Exception;

    interface InitializableElementUnmarshaller extends Unmarshaller<Element> {

        public void init(ElementUnmarshallerProvider unmarshallerFactory) throws Exception;
    }

    interface ElementUnmarshallerProvider {

        Unmarshaller<Element> getUnmarshallerForType(Class<?> type) throws Exception;

        void forGlobalUnmarshallerCompatibleWith(Class<?> type, Handler handler);

        interface Handler {

            void handle(String globalName, Unmarshaller<Element> unmarshaller);
        }
    }
}
