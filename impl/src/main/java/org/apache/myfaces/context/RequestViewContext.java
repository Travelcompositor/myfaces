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
package org.apache.myfaces.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.ResourceDependency;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import org.apache.myfaces.component.visit.MyFacesVisitHints;

/**
 *
 * @author  Leonardo Uribe (latest modification by $Author$)
 * @version $Revision$ $Date$
 * @since 2.0.2
 */
public class RequestViewContext
{
    public static final String VIEW_CONTEXT_KEY = "oam.VIEW_CONTEXT";
    
    public static final String RESOURCE_DEPENDENCY_INSPECTED_CLASS = "oam.RDClass";

    private RequestViewMetadata requestViewMetadata;
    
    private Map<String, Boolean> renderTargetMap = null;
    private Map<String, List<UIComponent>> renderTargetMapComponents = null;
    
    public RequestViewContext()
    {
        this.requestViewMetadata = new RequestViewMetadata();
    }
    
    public RequestViewContext(RequestViewMetadata rvm)
    {
        this.requestViewMetadata = new RequestViewMetadata();
    }

    static public RequestViewContext getCurrentInstance()
    {
        FacesContext ctx = FacesContext.getCurrentInstance();
        return getCurrentInstance(ctx);
    }
    
    static public RequestViewContext getCurrentInstance(FacesContext ctx)
    {
        return getCurrentInstance(ctx, ctx.getViewRoot());
    }
    
    @SuppressWarnings("unchecked")
    static public RequestViewContext getCurrentInstance(FacesContext ctx, UIViewRoot root)
    {
        Map<UIViewRoot, RequestViewContext> map
                = (Map<UIViewRoot, RequestViewContext>) ctx.getAttributes().get(VIEW_CONTEXT_KEY);
        RequestViewContext rvc = null;        
        if (map == null)
        {
            map = new HashMap<>();
            rvc = new RequestViewContext();
            map.put(root, rvc);
            ctx.getAttributes().put(VIEW_CONTEXT_KEY, map);
            return rvc;
        }
        else
        {
            rvc = map.get(root); 
            if (rvc == null)
            {
                rvc = new RequestViewContext();
                map.put(root, rvc);
            }
            return rvc;
        }
    }
    
    static public RequestViewContext getCurrentInstance(FacesContext ctx, UIViewRoot root, boolean create)
    {
        if (create)
        {
            return getCurrentInstance(ctx, root);
        }
        Map<UIViewRoot, RequestViewContext> map
                = (Map<UIViewRoot, RequestViewContext>) ctx.getAttributes().get(VIEW_CONTEXT_KEY);
        if (map != null)
        {
            return map.get(root);
        }
        return null;
    }
    
    static public RequestViewContext newInstance(RequestViewMetadata rvm)
    {
        RequestViewContext clone = new RequestViewContext(rvm.cloneInstance());
        return clone;
    }
    
    static public void setCurrentInstance(FacesContext ctx, UIViewRoot root, RequestViewContext rvc)
    {
        Map<UIViewRoot, RequestViewContext> map
                = (Map<UIViewRoot, RequestViewContext>) ctx.getAttributes().get(VIEW_CONTEXT_KEY);
        if (map == null)
        {
            map = new HashMap<>();
            rvc = new RequestViewContext();
            map.put(root, rvc);
            ctx.getAttributes().put(VIEW_CONTEXT_KEY, map);
        }
        else
        {
            map.put(root, rvc);
        }
    }

    public boolean isResourceDependencyAlreadyProcessed(ResourceDependency dependency)
    {
        return requestViewMetadata.isResourceDependencyAlreadyProcessed(dependency);
    }
    
    public void setResourceDependencyAsProcessed(ResourceDependency dependency)
    {
        requestViewMetadata.setResourceDependencyAsProcessed(dependency);
    }

    public boolean isClassAlreadyProcessed(Class<?> inspectedClass)
    {
        return requestViewMetadata.isClassAlreadyProcessed(inspectedClass);
    }

    public void setClassProcessed(Class<?> inspectedClass)
    {
        requestViewMetadata.setClassProcessed(inspectedClass);
    }
    
    public boolean isRenderTarget(String target)
    {
        if (renderTargetMap != null)
        {
            return Boolean.TRUE.equals(renderTargetMap.get(target));
        }
        return false;
    }
    
    public void setRenderTarget(String target, boolean value, UIComponent component)
    {
        if (renderTargetMap == null)
        {
            renderTargetMap = new HashMap<>(8);
        }
        renderTargetMap.put(target, value);
        if (renderTargetMapComponents == null)
        {
            renderTargetMapComponents = new HashMap<>(8);
        }
        List<UIComponent> componentList = renderTargetMapComponents.get(target);
        if (componentList == null)
        {
            componentList = new ArrayList<>(8);
            renderTargetMapComponents.put(target, componentList);
        }
        if (!componentList.contains(component))
        {
            componentList.add(component);
        }
    }
    
    public List<UIComponent> getRenderTargetComponentList(String target)
    {
        if (renderTargetMapComponents == null)
        {
            return Collections.emptyList();
        }
        List<UIComponent> list = renderTargetMapComponents.get(target);
        return list != null ? list : Collections.emptyList();
    }
    
    /**
     * Scans UIViewRoot facets with added component resources by the effect of
     * ResourceDependency annotation, and register the associated inspected classes
     * so new component resources will not be added to the component tree again and again.
     * 
     * @param facesContext
     * @param root 
     */
    public void refreshRequestViewContext(FacesContext facesContext, UIViewRoot root)
    {
        RefreshViewContextCallback callback = null;
        
        for (Map.Entry<String, UIComponent> entry : root.getFacets().entrySet())
        {
            UIComponent facet = entry.getValue();
            if (facet.getId() != null && facet.getId().startsWith("javax_faces_location_"))
            {
                try
                {
                    facesContext.getAttributes().put(MyFacesVisitHints.SKIP_ITERATION_HINT, Boolean.TRUE);

                    if (callback == null)
                    {
                        callback = new RefreshViewContextCallback();
                    }

                    VisitContext visitContext = VisitContext.createVisitContext(facesContext,
                            null, MyFacesVisitHints.SET_SKIP_ITERATION);
                    facet.visitTree(visitContext, callback);
                }
                finally
                {
                    // We must remove hint in finally, because an exception can break this phase,
                    // but lifecycle can continue, if custom exception handler swallows the exception
                    facesContext.getAttributes().remove(MyFacesVisitHints.SKIP_ITERATION_HINT);
                }
            }
        }
    }
    
    private class RefreshViewContextCallback implements VisitCallback
    {
        @Override
        public VisitResult visit(VisitContext context, UIComponent target)
        {
            Class<?> inspectedClass = (Class<?>)target.getAttributes().get(RESOURCE_DEPENDENCY_INSPECTED_CLASS);
            if (inspectedClass != null)
            {
                setClassProcessed(inspectedClass);
            }            
            return VisitResult.ACCEPT;
        }
    }
    
    public RequestViewMetadata getRequestViewMetadata()
    {
        return requestViewMetadata;
    }

    public void setRequestViewMetadata(RequestViewMetadata requestViewMetadata)
    {
        this.requestViewMetadata = requestViewMetadata;
    }
}
