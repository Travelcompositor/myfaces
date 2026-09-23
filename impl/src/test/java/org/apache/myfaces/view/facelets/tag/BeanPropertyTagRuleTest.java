/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.view.facelets.tag;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.el.ExpressionFactory;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class BeanPropertyTagRuleTest
{
    //@Test
    public void testConcurrentPrimitiveLiteralMetadata() throws Exception
    {
        FaceletContext context = Mockito.mock(FaceletContext.class);
        ExpressionFactory expressionFactory = Mockito.mock(ExpressionFactory.class);
        TagAttribute attribute = Mockito.mock(TagAttribute.class);
        Mockito.when(context.getExpressionFactory()).thenReturn(expressionFactory);
        Mockito.when(attribute.getValue()).thenReturn("100");
        Mockito.when(expressionFactory.coerceToType("100", Integer.TYPE)).thenAnswer(invocation ->
        {
            Thread.sleep(50);
            return Integer.valueOf(100);
        });

        Method setter = PrimitiveValueHolder.class.getMethod("setValue", Integer.TYPE);
        BeanPropertyTagRule.LiteralPropertyMetadata metadata =
                new BeanPropertyTagRule.LiteralPropertyMetadata(Integer.TYPE, setter, attribute);
        int workerCount = 16;
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        CountDownLatch ready = new CountDownLatch(workerCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<PrimitiveValueHolder>> futures = new ArrayList<Future<PrimitiveValueHolder>>(workerCount);

        try
        {
            for (int i = 0; i < workerCount; i++)
            {
                futures.add(executor.submit(() ->
                {
                    PrimitiveValueHolder holder = new PrimitiveValueHolder();
                    ready.countDown();
                    start.await();
                    metadata.applyMetadata(context, holder);
                    return holder;
                }));
            }

            Assert.assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            for (Future<PrimitiveValueHolder> future : futures)
            {
                Assert.assertEquals(100, future.get(5, TimeUnit.SECONDS).getValue());
            }
            Mockito.verify(expressionFactory, Mockito.times(1)).coerceToType("100", Integer.TYPE);
        }
        finally
        {
            start.countDown();
            executor.shutdownNow();
        }
    }

    public static final class PrimitiveValueHolder
    {
        private int value;

        public int getValue()
        {
            return value;
        }

        public void setValue(int value)
        {
            this.value = value;
        }
    }
}
