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
package org.sakaiproject.nakamura.rules;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.rules.RuleContext;
import org.sakaiproject.nakamura.api.rules.RuleExecutionException;

import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;


public class RuleExecutionServiceImpleTest {

  @Mock
  private RuleContext ruleContext;
  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private ResourceResolver resourceResolver;
  @Mock
  private Resource ruleSet;
  @Mock
  private Node ruleSetNode;
  @Mock
  private ComponentContext context;
  @Mock
  private BundleContext bundleContext;
  @Mock
  private NodeIterator nodeIterator;
  @Mock
  private Node packageNode;
  @Mock
  private NodeType packageNodeType;
  @Mock
  private Property property;
  @Mock
  private Node packageFileNode;
  @Mock
  private Property packageFileBody;
  @Mock
  private Binary binary;
  @Mock
  private Session session;

  public RuleExecutionServiceImpleTest() {
    MockitoAnnotations.initMocks(this);
  }
  
  
  @Test
  public void testRuleExecution() throws RepositoryException, RuleExecutionException {
    String path = "/test/ruleset";
    
    Mockito.when(request.getResourceResolver()).thenReturn(resourceResolver);
    Mockito.when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
    Mockito.when(session.getUserID()).thenReturn("ieb");
    
    Mockito.when(resourceResolver.getResource(path)).thenReturn(ruleSet);
    Mockito.when(ruleSet.getResourceType()).thenReturn("sakai/rule-set");
    Mockito.when(ruleSet.adaptTo(Node.class)).thenReturn(ruleSetNode);
    Mockito.when(context.getBundleContext()).thenReturn(bundleContext);
    
    
    Mockito.when(ruleSetNode.getPath()).thenReturn(path);
    Mockito.when(ruleSetNode.getNodes()).thenReturn(nodeIterator);
    Mockito.when(nodeIterator.hasNext()).thenReturn(true, false, true, false);
    Mockito.when(nodeIterator.nextNode()).thenReturn(packageNode, packageNode);
    Mockito.when(packageNode.getPrimaryNodeType()).thenReturn(packageNodeType);
    Mockito.when(packageNodeType.getName()).thenReturn(NodeType.NT_FILE);
        
    Mockito.when(packageNode.getPath()).thenReturn(path+"/package1");
    
    Mockito.when(packageNode.getNode(Node.JCR_CONTENT)).thenReturn(packageFileNode);
    
    Mockito.when(packageFileNode.getProperty(Property.JCR_LAST_MODIFIED)).thenReturn(property);
    Calendar lastModified = GregorianCalendar.getInstance();
    lastModified.setTimeInMillis(System.currentTimeMillis());
    Mockito.when(property.getDate()).thenReturn(lastModified);
    
    Mockito.when(packageFileNode.getProperty(Property.JCR_DATA)).thenReturn(packageFileBody);
    
    Mockito.when(packageFileBody.getBinary()).thenReturn(binary);
    
    
    
    Mockito.when(binary.getStream()).thenAnswer(new Answer<InputStream>() {

      public InputStream answer(InvocationOnMock invocation) throws Throwable {
        return  this.getClass().getResourceAsStream("/SLING-INF/content/var/rules/org.sakaiproject.nakamura.rules/org.sakaiproject.nakamura.rules.example/0.7-SNAPSHOT/org.sakaiproject.nakamura.rules.example-0.7-SNAPSHOT.pkg");
      }
    });

    
    
      
    
    RuleExecutionServiceImpl res = new RuleExecutionServiceImpl();
    res.activate(context);
    Map<String, Object> result = res.executeRuleSet(path, request, ruleContext, null);
    
    Assert.assertNotNull(result);
    res.deactivate(context);
  }
  
}
