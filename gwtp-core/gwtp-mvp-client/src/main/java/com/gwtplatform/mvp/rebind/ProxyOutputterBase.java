/**
 * Copyright 2010 ArcBees Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gwtplatform.mvp.rebind;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.inject.client.AsyncProvider;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.gwtplatform.common.client.CodeSplitBundleProvider;
import com.gwtplatform.common.client.CodeSplitProvider;
import com.gwtplatform.common.client.StandardProvider;
import com.gwtplatform.mvp.client.DelayedBindRegistry;
import com.gwtplatform.mvp.client.TabData;
import com.gwtplatform.mvp.client.TabDataBasic;
import com.gwtplatform.mvp.client.proxy.ProxyFailureHandler;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;

/**
 * The base class of any {@link ProxyOutputter} implementation.
 *
 * @author Philippe Beaudoin
 */
public abstract class ProxyOutputterBase implements ProxyOutputter {

  protected final TypeOracle oracle;
  protected final TreeLogger logger;
  protected final ClassCollection classCollection;
  protected final GinjectorInspector ginjectorInspector;
  protected final PresenterInspector presenterInspector;
  protected final ProxyOutputterBase parent;

  private JClassType proxyInterface;
  private List<ProxyEventDescription> proxyEvents;

  ProxyOutputterBase(TypeOracle oracle,
      TreeLogger logger,
      ClassCollection classCollection,
      GinjectorInspector ginjectorInspector,
      PresenterInspector presenterInspector,
      ProxyOutputterBase parent) {
    this.oracle = oracle;
    this.logger = logger;
    this.classCollection = classCollection;
    this.ginjectorInspector = ginjectorInspector;
    this.presenterInspector = presenterInspector;
    this.parent = parent;
  }

  @Override
  public final void init(JClassType proxyInterface) throws UnableToCompleteException {
    this.proxyInterface = proxyInterface;
    proxyEvents = presenterInspector.findProxyEvents();
    initSubclass(proxyInterface);
  }

  abstract void initSubclass(JClassType proxyInterface) throws UnableToCompleteException;

  @Override
  public void initComposerFactory(ClassSourceFileComposerFactory composerFactory) {
    addImports(composerFactory);
    setInterfacesAndSuperclass(composerFactory);
  }

  /**
   * Finds the root of this hierarchy of {@link ProxyOutputter}. A hierarchy is build via
   * the {@link CompositeProxyOutputter}. If this {@link ProxyOutputter} has no parent, returns
   * itself.
   *
   * @return The {@link ProxyOutputter} at the root of the hierarchy.
   */
  final ProxyOutputter getRootProxyOutputter() {
    if (parent == null) {
      return this;
    }
    return parent.getRootProxyOutputter();
  }

  /**
   * Add all the imports required to output this proxy.
   *
   * @param composerFactory The composer factory used to generate to proxy.
   */
  private void addImports(ClassSourceFileComposerFactory composerFactory) {
    // TODO Distribute among subclasses in addSubclassImports()
    composerFactory.addImport(GWT.class.getCanonicalName());
    composerFactory.addImport(Inject.class.getCanonicalName()); // Obsolete?
    composerFactory.addImport(Provider.class.getCanonicalName());
    composerFactory.addImport(AsyncProvider.class.getCanonicalName());
    composerFactory.addImport(EventBus.class.getCanonicalName());
    composerFactory.addImport(StandardProvider.class.getCanonicalName());
    composerFactory.addImport(CodeSplitProvider.class.getCanonicalName());
    composerFactory.addImport(CodeSplitBundleProvider.class.getCanonicalName());
    composerFactory.addImport(ProxyFailureHandler.class.getCanonicalName());
    composerFactory.addImport(ClassCollection.proxyImplClassName);
    composerFactory.addImport(ClassCollection.proxyPlaceImplClassName);
    composerFactory.addImport(RevealContentHandler.class.getCanonicalName());
    composerFactory.addImport(ClassCollection.delayedBindClassName);
    composerFactory.addImport(ClassCollection.tabContentProxyPlaceImplClassName);
    composerFactory.addImport(DelayedBindRegistry.class.getCanonicalName());
    composerFactory.addImport(Ginjector.class.getCanonicalName());
    composerFactory.addImport(RevealContentEvent.class.getCanonicalName()); // Obsolete?
    composerFactory.addImport(Scheduler.class.getCanonicalName());
    composerFactory.addImport(Command.class.getCanonicalName());
    composerFactory.addImport(AsyncCallback.class.getCanonicalName());
    composerFactory.addImport(TabData.class.getCanonicalName());
    composerFactory.addImport(TabDataBasic.class.getCanonicalName());
    addSubclassImports(composerFactory);
  }

  abstract void addSubclassImports(ClassSourceFileComposerFactory composerFactory);

  /**
   * Set all the interfaces implemented by this proxy and its superclass.
   *
   * @param composerFactory The composer factory used to generate to proxy.
   */
  private void setInterfacesAndSuperclass(ClassSourceFileComposerFactory composerFactory) {
    // Sets interfaces and superclass
    composerFactory.addImplementedInterface(proxyInterface.getParameterizedQualifiedSourceName());
    composerFactory.addImplementedInterface(ClassCollection.delayedBindClassName);
    composerFactory.setSuperclass(getSuperclassName() + "<"
        + presenterInspector.getPresenterClassName() + ">");

    // Add all implemented handlers
    for (ProxyEventDescription desc : proxyEvents) {
      composerFactory.addImplementedInterface(desc.handlerFullName);
    }
  }

  /**
   * Access the name of the superclass from which the proxy implementation
   * should inherit.
   *
   * @return The name of the superclass.
   */
  abstract String getSuperclassName();

  @Override
  public void writeFields(SourceWriter writer) {
    writer.println();
    writer.println("private " + ginjectorInspector.getGinjectorClassName() + " ginjector;");
  }

  @Override
  public final void writeAddHandlerForProxyEvents(SourceWriter writer) {
    for (ProxyEventDescription desc : proxyEvents) {
      writer.println("getEventBus().addHandler( " + desc.eventFullName
          + ".getType(), this );");
    }
  }

  @Override
  public final void writeHandlerMethodsForProxyEvents(SourceWriter writer) {
    for (ProxyEventDescription desc : proxyEvents) {
      writer.println("");
      writeHandlerMethod(presenterInspector.getPresenterClassName(), desc, writer);
    }
  }

  private void writeHandlerMethod(String presenterClassName,
      ProxyEventDescription desc, SourceWriter writer) {
    writer.println("@Override");
    writer.println("public final void " + desc.handlerMethodName + "( final "
        + desc.eventFullName + " event ) {");
    writer.indent();
    writer.println("getPresenter( new AsyncCallback<" + presenterClassName
        + ">() {");
    writer.indent();
    writer.println("@Override");
    writer.println("public void onFailure(Throwable caught) {");
    writer.indent();
    writer.println("failureHandler.onFailedGetPresenter(caught);");
    writer.outdent();
    writer.println("}");
    writer.println("@Override");
    writer.println("public void onSuccess(final " + presenterClassName
        + " presenter) {");
    writer.indent();
    writer.println("Scheduler.get().scheduleDeferred( new Command() {");
    writer.indent();
    writer.println("@Override");
    writer.println("public void execute() {");
    writer.indent();
    writer.println("presenter." + desc.functionName + "( event );");
    writer.outdent();
    writer.println("}");
    writer.outdent();
    writer.println("} );");
    writer.outdent();
    writer.println("}");
    writer.outdent();
    writer.println("} );");
    writer.outdent();
    writer.println("}");
  }
}