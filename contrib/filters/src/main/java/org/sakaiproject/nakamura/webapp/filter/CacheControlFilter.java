/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.webapp.filter;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * The <code>CacheControlFilter</code> class is a request level filter which applies a
 * Cache-Control response header on GET requests which match any of an arbitrary list of
 * configured regex patterns. Each configured pattern must also have a corresponding 
 * maxage value (in seconds) to use if the pattern matches.
 * 
 * When more than one pattern matches, the filter sets the lowest maxage of the
 * collection of matching patterns.
 */
@Service(value=Filter.class)
@Component(immediate=true, metatype=false)
@Properties(value={@Property(name="service.description", value="Nakamura Cache-Control Filter"),
    @Property(name="service.vendor",value="The Sakai Foundation"),
    @Property(name="filter.scope",value="request", propertyPrivate=true),
    @Property(name="filter.order",intValue={10}, propertyPrivate=true)})
public class CacheControlFilter implements Filter {
  private static final Logger LOGGER = LoggerFactory.getLogger(CacheControlFilter.class);

  private final long FIVE_DAYS=432000;
  private Map<String, Long> cachePatterns = new HashMap<String, Long>();

  /**
   * {@inheritDoc}
   * @see javax.servlet.Filter#destroy()
   */
  public void destroy() {

  }

  /**
   * {@inheritDoc}
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
   */
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Applying Expires filter.");
    }
    SlingHttpServletRequest srequest = (SlingHttpServletRequest) request;
    SlingHttpServletResponse sresponse = (SlingHttpServletResponse) response;
    
    Long maxAge = requestedResourceMaxAge(srequest);
    if(maxAge > 0) {
      sresponse.addHeader("Cache-Control", "max-age=" + maxAge);
    }
    
    chain.doFilter(request, response);
  }

  private Long requestedResourceMaxAge(SlingHttpServletRequest srequest) {
    if (! "GET".equals(srequest.getMethod())) {
      return new Long(0);
    }
    
    String path = srequest.getPathInfo();
    List<Long> expirations = new ArrayList<Long>();
    for (String pattern : cachePatterns.keySet()) {
      boolean doesMatch = path.matches(pattern);
      if(doesMatch) {
        expirations.add(cachePatterns.get(pattern));
      }
    }
    
    if (expirations.isEmpty()) {
      return new Long(0);
    } else {
      return Collections.min(expirations);
    }
  }

  /**
   * {@inheritDoc}
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  public void init(FilterConfig filterConfig) throws ServletException {

  }
  
  @SuppressWarnings("unchecked")
  protected void activate(ComponentContext context) {
    if (context == null) {
      cachePatterns.put(".*\\.(ico|pdf|flv|jpg|jpeg|png|gif|js|css|swf)$", new Long(FIVE_DAYS));
    } else {
      Dictionary props = context.getProperties();
      for(Enumeration<Object> iter = props.keys(); iter.hasMoreElements();) {
        Object key = iter.nextElement();
        if (key instanceof String) {
          if (((String)key).startsWith("pattern")) {
            String patternNum = ((String)key).split("pattern")[1];
            Long maxAge = Long.parseLong((String)props.get("maxage" + patternNum));
            cachePatterns.put((String)props.get(key), maxAge);
          }
        }
      }
    }
  }
  

}