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
package org.apache.myfaces.resource;

import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import javax.faces.application.ResourceVisitOption;
import javax.faces.context.FacesContext;
import org.apache.myfaces.util.ClassUtils;

/**
 *
 * @author Leonardo Uribe
 */
public class ClassLoaderContractResourceLoader extends ContractResourceLoader
{

    public ClassLoaderContractResourceLoader(String prefix)
    {
        super(prefix);
    }

    @Override
    public String getLibraryVersion(String path, String contractName)
    {
        return null;
    }

    @Override
    public InputStream getResourceInputStream(ResourceMeta resourceMeta)
    {
        InputStream is = null;

        String prefix = getPrefix();
        if (prefix != null && !prefix.isEmpty())
        {
            String name = prefix + '/' + resourceMeta.getContractName() + '/' + resourceMeta.getResourceIdentifier();
            is = getClassLoader().getResourceAsStream(name);
            if (is == null)
            {
                is = this.getClass().getClassLoader().getResourceAsStream(name);
            }
            return is;
        }
        else
        {
            String name = resourceMeta.getContractName() + '/' + resourceMeta.getResourceIdentifier();
            is = getClassLoader().getResourceAsStream(name);
            if (is == null)
            {
                is = this.getClass().getClassLoader().getResourceAsStream(name);
            }
            return is;
        }
    }

    @Override
    public URL getResourceURL(ResourceMeta resourceMeta)
    {
        URL url = null;

        String prefix = getPrefix();
        if (prefix != null && !prefix.isEmpty())
        {
            String name = prefix + '/' + resourceMeta.getContractName() + '/' + resourceMeta.getResourceIdentifier();
            url = getClassLoader().getResource(name);
            if (url == null)
            {
                url = this.getClass().getClassLoader().getResource(name);
            }
            return url;
        }
        else
        {
            String name = resourceMeta.getContractName() + '/' + resourceMeta.getResourceIdentifier();
            url = getClassLoader().getResource(name);
            if (url == null)
            {
                url = this.getClass().getClassLoader().getResource(name);
            }
            return url;
        }
    }

    @Override
    public String getResourceVersion(String path, String contractName)
    {
        return null;
    }

    @Override
    public ResourceMeta createResourceMeta(String prefix, String libraryName, String libraryVersion,
                                           String resourceName, String resourceVersion, 
                                           String contractName)
    {
        return new ResourceMetaImpl(prefix, libraryName, libraryVersion, resourceName, 
            resourceVersion, contractName);
    }

    /**
     * Returns the ClassLoader to use when looking up resources under the top level package. By default, this is the
     * context class loader.
     * 
     * @return the ClassLoader used to lookup resources
     */
    protected ClassLoader getClassLoader()
    {
        return ClassUtils.getContextClassLoader();
    }

    @Override
    public boolean libraryExists(String libraryName, String contractName)
    {
        String prefix = getPrefix();
        if (prefix != null && !prefix.isEmpty())
        {
            String name = prefix + '/' + contractName + '/' + libraryName;
            URL url = getClassLoader().getResource(name);
            if (url == null)
            {
                url = this.getClass().getClassLoader().getResource(name);
            }
            if (url != null)
            {
                return true;
            }
        }
        else
        {
            String name = contractName + '/' + libraryName;
            URL url = getClassLoader().getResource(name);
            if (url == null)
            {
                url = this.getClass().getClassLoader().getResource(name);
            }
            if (url != null)
            {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public Iterator<String> iterator(FacesContext facesContext, String path, 
            int maxDepth, ResourceVisitOption... options)
    {
        String basePath = path;

        String prefix = getPrefix();
        if (prefix != null)
        {
            basePath = prefix + '/' + (path.startsWith("/") ? path.substring(1) : path);
        }

        URL url = getClassLoader().getResource(basePath);

        return new ClassLoaderResourceLoaderIterator(url, basePath, maxDepth, options);
    }

}
