/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.talend.sdk.component.studio.ui.guessschema;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.talend.core.model.components.IComponent;
import org.talend.core.model.process.ElementParameterParser;
import org.talend.core.model.process.IConnection;
import org.talend.core.model.process.IConnectionCategory;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.properties.Property;
import org.talend.core.model.utils.NodeUtil;
import org.talend.designer.core.model.components.ElementParameter;
import org.talend.designer.core.model.process.DataProcess;
import org.talend.designer.core.ui.editor.process.Process;
import org.talend.designer.runprocess.IProcessor;
import org.talend.designer.runprocess.ProcessorUtilities;
import org.talend.sdk.component.api.exception.DiscoverSchemaException;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.studio.ComponentModel;
import org.talend.sdk.component.studio.Lookups;
import org.talend.sdk.component.studio.enums.ETaCoKitComponentType;
import org.talend.sdk.component.studio.i18n.Messages;
import org.talend.sdk.component.studio.metadata.model.ComponentModelSpy;
import org.talend.sdk.component.studio.util.TaCoKitConst;
import org.talend.sdk.component.studio.util.TaCoKitSpeicalManager;
import org.talend.sdk.component.studio.util.TaCoKitUtil;

/**
 * DOC cmeng class global comment. Detailled comment
 */
public class TaCoKitGuessSchemaProcess {

    private final Task guessSchemaTask;

    private final ExecutorService executorService = ExecutorService.class.cast(Lookups.uiActionsThreadPool()
            .getExecutor());

    public TaCoKitGuessSchemaProcess(final Property property, final INode node, final IContext context,
            final String discoverSchemaAction, final String connectionName) {
        this.guessSchemaTask = new Task(property, context, node, discoverSchemaAction, connectionName, executorService);
    }

    public Future<GuessSchemaResult> run() {
        return executorService.submit(guessSchemaTask);
    }

    public void kill() {
        guessSchemaTask.kill();
    }

    public static class Task implements Callable<GuessSchemaResult> {

        private final Property property;

        private IProcess process;

        private final IContext context;

        private final INode node;

        private final String actionName;

        private final String connectionName;

        private final ExecutorService executorService;

        private java.lang.Process executeProcess;
        
        private final Map<String, IElementParameter> clonedDatastoreParameters = new HashMap<String, IElementParameter>();

        private final boolean executeProcessorMockJob;

        public Task(final Property property, final IContext context, final INode node, final String actionName,
                final String connectionName, final ExecutorService executorService, final boolean executeProcessorMockJob) {
            this.property = property;
            this.context = context;
            this.node = node;
            this.actionName = actionName;
            this.connectionName = connectionName;
            this.executorService = executorService;
            this.executeProcessorMockJob = executeProcessorMockJob;
        }

        @Override
        public GuessSchemaResult call() throws Exception {
            buildProcess();
            restoreDatastoreParameters(node);
            IProcessor processor = ProcessorUtilities.getProcessor(process, null);
            processor.setContext(context);
            processor.setProxyParameters(TaCoKitSpeicalManager.getProxyForGuessSchema());
            final String debug = System.getProperty("org.talend.tacokit.guessschema.debug", null);
            executeProcess = processor.run(debug == null || debug.isEmpty() ? null :
                            singletonList(debug).toArray(new String[0]),
                    IProcessor.NO_STATISTICS,
                    IProcessor.NO_TRACES);
            
            
            final Future<GuessSchemaResult> result = executorService.submit(() -> {
                final Pattern schemaPattern = Pattern.compile("\\[\\{.*\"talendType\".*\\}]");
                final Pattern errorPattern = Pattern.compile("\\{.*\"possibleHandleErrorWith\".*\\}");
                final List<String> err = new ArrayList();
                final GuessSchemaResult guessSchemaResult = new GuessSchemaResult();
                // read stdout stream
                String out;
                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(executeProcess.getInputStream()))) {
                    out = reader.lines().collect(joining("\n"));
                    err.add(out);
                }
                // read stderr stream
                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(executeProcess.getErrorStream()))) {
                    err.addAll(reader.lines().parallel().collect(toList()));
                }
                guessSchemaResult.setError(err.stream().collect(joining("\n")));
                final String flattened = out.replaceAll("\n", "");
                final Matcher schemaMatcher = Pattern.compile("(\\[\\{.*\"talendType\".*\\}])").matcher(flattened);
                final Matcher errorMatcher = Pattern.compile("(\\{.*\"possibleHandleErrorWith\".*\\})").matcher(flattened);
                if (schemaMatcher.find()) {
                    guessSchemaResult.setResult(schemaMatcher.group());
                }
                if (errorMatcher.find()) {
                    try (final Jsonb jsonb = JsonbBuilder.create()) {
                        DiscoverSchemaException e = jsonb.fromJson(errorMatcher.group(), DiscoverSchemaException.class);
                        guessSchemaResult.setExecuteMock("EXECUTE_MOCK_JOB".equals(e.getPossibleHandleErrorWith().name()));
                        guessSchemaResult.setMessage(e.getMessage());
                    }
                }
                return guessSchemaResult;
            });

            executeProcess.waitFor();
            final GuessSchemaResult guessResult = result.get();
            if (0 != executeProcess.exitValue()){
                return new GuessSchemaResult("", guessResult.getError(), guessResult.getMessage());
            }
            final String resultStr = guessResult.getResult();
            if (resultStr != null && !resultStr.trim().isEmpty()) {
                return guessResult;
            }
            final String errMessage = guessResult.getError();
            if (errMessage != null && !errMessage.isEmpty()) {
                throw new IllegalStateException(errMessage);
            } else {
                throw new IllegalStateException(Messages.getString("guessSchema.error.empty")); //$NON-NLS-1$
            }
        }

        public synchronized void kill() {
            if (executeProcess != null && executeProcess.isAlive()) {
                restoreDatastoreParameters(node);
                final java.lang.Process p = executeProcess.destroyForcibly();
                try {
                    p.waitFor(20, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }     
        

        private void buildProcess() {
            IProcess originalProcess;
            originalProcess = new Process(property);

            List<? extends IConnection> incomingConnections = new ArrayList<>(node.getIncomingConnections());
            List<? extends IConnection> outgoingConnections = new ArrayList<>(node.getOutgoingConnections());
            try {
                node.setOutgoingConnections(new ArrayList<>());

                List<INode> nodes = new ArrayList<>();
                IComponent nodeComp = node.getComponent();

                boolean isProcessor = false;
                if (ComponentModel.class.isInstance(nodeComp)) {
                    ComponentModel compModel = (ComponentModel) nodeComp;
                    if (compModel.getTaCoKitComponentType() != null) {
                        if(compModel.getTaCoKitComponentType() == ETaCoKitComponentType.input || compModel.getTaCoKitComponentType() == ETaCoKitComponentType.standalone) {
                            node.setIncomingConnections(new ArrayList<>());
                        }
                        if(compModel.getTaCoKitComponentType() == ETaCoKitComponentType.processor) {
                            isProcessor = true;
                        }
                        nodes.add(node);
                        if (TaCoKitUtil.isUseExistConnection(node)) {
                            updateDatastoreParameterFromConnection(node);
                        }
                    }
                }
                
                //TODO consider to remove it too as why we need the input line's schema for guessing processor schema? sync columns?
                //here follow the design of DiscoverSchemaExtended, that support to use that schema to guess, in my view, we no need to promise that thing, 1% usage improvement, but with bug risk.
                //And here have a side effect before already: if job design is : (tsetproxy==>on_subjob_ok==>tfixedflowinput==>a tacokit processor connector), that tsetproxy will affect guess schema result, 
                //IMHO, we should never promise that, as user will find that example not works for tck input connector. Here revert the wrong promise for standalone connector, but not processor connector.
                if (isProcessor) {//when processor tck connector, here only keep the old action, TODO remove this special code.
                    final String family = ElementParameterParser.getValue(node, "__FAMILY__");
                    final boolean dataLineOnly = TaCoKitSpeicalManager.JDBC.equals(family);
                    retrieveNodes(nodes, new HashSet<>(), node, dataLineOnly);
                }

                DataProcess dataProcess = new DataProcess(originalProcess);
                dataProcess.buildFromGraphicalProcess(nodes);
                process = dataProcess.getDuplicatedProcess();
                configContext(process, node);
                List<INode> nodeList = dataProcess.getNodeList();
                INode newNode = null;
                // INode newNode = dataProcess.buildNodeFromNode(node, process);
                for (INode curNode : nodeList) {
                    if (curNode.getUniqueName().equals(node.getUniqueName())) {
                        newNode = curNode;
                        break;
                    }
                }

                IComponent component = newNode.getComponent();
                ComponentModelSpy componentSpy = createComponnetModelSpy(component);
                newNode.setComponent(componentSpy);
                if (ComponentModel.class.isInstance(component)) {
                    List<IElementParameter> elementParameters =
                            (List<IElementParameter>) newNode.getElementParameters();
                    final ComponentModel cm = ComponentModel.class.cast(component);
                    final IElementParameter pluginName = new ElementParameter(newNode);
                    pluginName.setName(TaCoKitConst.GUESS_SCHEMA_PARAMETER_PLUGIN_NAME);
                    pluginName.setValue(cm.getPluginName());
                    elementParameters.add(pluginName);

                    final IElementParameter actionNameParam = new ElementParameter(newNode);
                    actionNameParam.setName(TaCoKitConst.GUESS_SCHEMA_PARAMETER_ACTION_NAME);
                    final List<ActionReference> actions = cm.getDiscoverSchemaActions();
                    if (actionName != null && !actions.isEmpty() && actions.stream()
                            .anyMatch(a -> a.getName().equals(actionName))) {
                        actionNameParam.setValue(actions.stream()
                                .filter(a -> a.getName().equals(actionName)).findFirst().get().getName());

                    }
                    elementParameters.add(actionNameParam);

                    final IElementParameter tacokitComponentType = new ElementParameter(newNode);
                    tacokitComponentType.setName(TaCoKitConst.GUESS_SCHEMA_PARAMETER_TACOKIT_COMPONENT_TYPE);
                    tacokitComponentType.setValue(cm.getTaCoKitComponentType().toString());
                    elementParameters.add(tacokitComponentType);

                    final IElementParameter outputConnectionName = new ElementParameter(newNode);
                    outputConnectionName.setName(TaCoKitConst.GUESS_SCHEMA_PARAMETER_OUTPUT_CONNECTION_NAME);
                    outputConnectionName.setValue(connectionName);
                    elementParameters.add(outputConnectionName);

                    final IElementParameter executeProcessorMockJobParam = new ElementParameter(newNode);
                    executeProcessorMockJobParam.setName(TaCoKitConst.GUESS_SCHEMA_PARAMETER_OUTPUT_EXECUTE_MOCKJOB);
                    executeProcessorMockJobParam.setValue(executeProcessorMockJob);
                    elementParameters.add(executeProcessorMockJobParam);
                }

            } finally {
                node.setIncomingConnections(incomingConnections);
                node.setOutgoingConnections(outgoingConnections);
            }
        }

        protected void configContext(IProcess inProcess, INode inNode) {
            IContext selectContext = context;
            inProcess.getContextManager().getListContext().clear();
            inProcess.getContextManager().getListContext().addAll(inNode.getProcess().getContextManager().getListContext());
            inProcess.getContextManager().setDefaultContext(selectContext);
        }

        private void retrieveNodes(final List<INode> nodeList, final Set<INode> recordedNodes,
                final INode currentNode, final boolean dataLineOnly) {
            if (currentNode == null || recordedNodes.contains(currentNode)) {
                return;
            }
            nodeList.add(currentNode);
            recordedNodes.add(currentNode);
            List<? extends IConnection> incomingConnections = dataLineOnly ? NodeUtil.getIncomingConnections(currentNode, IConnectionCategory.MAIN | IConnectionCategory.DATA) : currentNode.getIncomingConnections();
            if (incomingConnections != null && !incomingConnections.isEmpty()) {
                for (IConnection conn : incomingConnections) {
                    if (conn != null) {
                        retrieveNodes(nodeList, recordedNodes, conn.getSource(), dataLineOnly);
                    }
                }
            } else if(dataLineOnly) {
                List<? extends IConnection> dependencyConnections = NodeUtil.getIncomingConnections(currentNode, IConnectionCategory.DEPENDENCY);
                if(dependencyConnections!=null && !dependencyConnections.isEmpty()) {
                    currentNode.setIncomingConnections(new ArrayList<>());
                }
            }
        }

        private ComponentModelSpy createComponnetModelSpy(final IComponent component) {
            ComponentModelSpy componentSpy = new ComponentModelSpy(component);
            IComponent guessComponent = Lookups.taCoKitCache().getTaCoKitGuessSchemaComponent();
            componentSpy.spyName(guessComponent.getName());
            componentSpy.spyOriginalName(guessComponent.getOriginalName());
            componentSpy.spyShortName(guessComponent.getShortName());
            componentSpy.spyTemplateFolder(guessComponent.getTemplateFolder());
            componentSpy.spyTemplateNamePrefix(guessComponent.getTemplateNamePrefix());
            componentSpy.spyAvailableCodeParts(guessComponent.getAvailableCodeParts());
            return componentSpy;
        }

        private void updateDatastoreParameterFromConnection(INode node) {
            clonedDatastoreParameters.clear();
            for (IElementParameter parameter : node.getElementParameters()) {
                if (TaCoKitUtil.isDataStoreParameter(node, parameter.getName())) {
                    IElementParameter clonedParam = parameter.getClone();
                    clonedDatastoreParameters.put(clonedParam.getName(), clonedParam);
                    parameter.setValue(TaCoKitUtil.getParameterValueFromConnection(node, parameter.getName()));
                }
            }
            IElementParameter useExistConnectionParameter = node
                    .getElementParameter(TaCoKitConst.PARAMETER_USE_EXISTING_CONNECTION);
            IElementParameter clonedParam = useExistConnectionParameter.getClone();
            clonedDatastoreParameters.put(clonedParam.getName(), clonedParam);
            useExistConnectionParameter.setValue(false);
        }

        private void restoreDatastoreParameters(INode node) {
            for (IElementParameter parameter : node.getElementParameters()) {
                if (clonedDatastoreParameters.containsKey(parameter.getName())) {
                    parameter.setValue(clonedDatastoreParameters.get(parameter.getName()).getValue());
                }
            }
            clonedDatastoreParameters.clear();
        }
    }

    public static class GuessSchemaResult {

        /**
         * Should only contain the columns list
         */
        private String result;

        /**
         * Error stack trace for ExceptionDialog details
         */
        private String error;

        /**
         * Human-readable message
         */
        private String message;

        /**
         * Should we execute a mock job for guessing schema?
         */
        private boolean executeMock;

        public GuessSchemaResult() {

        }

        public GuessSchemaResult(final String result, final String error, final String message) {
            this.result = result;
            this.error = error;
            this.message = message;
        }

        public GuessSchemaResult(final String result, final String error, final String message, final boolean executeMock) {
            this.result = result;
            this.error = error;
            this.message = message;
            this.executeMock = executeMock;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public boolean isExecuteMock() {
            return executeMock;
        }

        public void setExecuteMock(final boolean executeMock) {
            this.executeMock = executeMock;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(final String message) {
            this.message = message;
        }
    }

}

