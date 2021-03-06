/*
 * Copyright 2009-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.impalaframework.extension.mvc.annotation.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.support.HandlerMethodResolver;
import org.springframework.web.util.UrlPathHelper;

/**
 * Provides a simplified mechanism for mapping requests to method names, based
 * on URI path literals. Maintains a cache of these. Wraps
 * {@link HandlerMethodResolver} for handler metadata.
 * 
 * @author Phil Zoio
 */
public class AnnotationHandlerMethodResolver {
        
    /**
     * Cache of URI path literals to handler methods
     */
    private Map<String,Method> pathMethodCache = new ConcurrentHashMap<String, Method>();   

    private UrlPathHelper urlPathHelper = new UrlPathHelper();
    
    /**
     * {@link HandlerMethodResolver} instance. Should be initialised before being wired in to this instance
     */
    private HandlerMethodResolver handlerMethodResolver;
    
    /**
     * The adapated handler class
     */
    private Class<?> handlerClass;
    
    /**
     * Class level annotations which are themselves annotated using the {@link Handler} annotation.
     * Primary purpose is to determine which flavour of handler adapter implementation to apply for a given
     * controller class
     */
    private Collection<Annotation> handlerAnnotations = new ArrayList<Annotation>();
    
    /**
     * Passes in initialized {@link HandlerMethodResolver} instance
     * @param handlerClass
     * @param handlerMethodResolver
     */
    public AnnotationHandlerMethodResolver(Class<?> handlerClass, HandlerMethodResolver handlerMethodResolver) {
        this.handlerMethodResolver = handlerMethodResolver;
        this.handlerClass = handlerClass;
    }
    
    public void init() {
        List<Annotation> handlerAnnotations = new ArrayList<Annotation>();
        final Annotation[] annotations = handlerClass.getAnnotations();
        for (Annotation annotation : annotations) {
            
            if (annotation.annotationType().getAnnotation(Handler.class) != null) {
                handlerAnnotations.add(annotation);
            }
        }
        this.handlerAnnotations = Collections.unmodifiableList(handlerAnnotations);
    }
    
    public Collection<Annotation> getHandlerAnnotations() {
        return handlerAnnotations;
    }

    public Method resolveHandlerMethod(HttpServletRequest request) throws ServletException {
        
        String lookupPath = urlPathHelper.getLookupPathForRequest(request);
        String methodLookupPath = lookupPath+"." + request.getMethod();
        
        Method methodToReturn = pathMethodCache.get(methodLookupPath);
        
        if (methodToReturn == null) {
            methodToReturn = getHandlerMethod(lookupPath, methodLookupPath, request);
        }
        
        return methodToReturn;
    }

    private Method getHandlerMethod(String lookupPath, String methodLookupPath, HttpServletRequest request) {

        
        for (Method handlerMethod : getHandlerMethods()) {
            RequestMapping mapping = AnnotationUtils.findAnnotation(handlerMethod, RequestMapping.class);
            String[] values = mapping.value();
            
            if (values[0].equals(lookupPath)) {
                
                RequestMethod[] method = mapping.method();
                if (method != null && method.length > 0) {
                    for (RequestMethod requestMethod : method) {
                        if (request.getMethod().equals(requestMethod.toString())) {
                            pathMethodCache.put(methodLookupPath, handlerMethod);
                            return handlerMethod;
                        }
                    }
                } else {
                    pathMethodCache.put(methodLookupPath, handlerMethod);
                    return handlerMethod;
                }
            }               
        }
        
        return null;
    }

    public Set<Method> getHandlerMethods() {
        return handlerMethodResolver.getHandlerMethods();
    }

    public Set<Method> getModelAttributeMethods() {
        return handlerMethodResolver.getModelAttributeMethods();
    }
    
    public final boolean hasHandlerMethods() {
        return handlerMethodResolver.hasHandlerMethods();
    }
    
    
    
}
