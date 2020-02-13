package alien4cloud.poc.plugin.lrm;

import alien4cloud.paas.wf.TopologyContext;
import alien4cloud.paas.wf.WorkflowSimplifyService;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.paas.wf.validation.WorkflowValidator;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.parser.ToscaParser;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.TopologyModifierSupport;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.utils.TopologyNavigationUtil;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

/**
 * Use at post-node-match phase, after the alien4cloud-poc-lrm-modifier.
 */
@Slf4j
@Component("alien4cloud-poc-mls-post-modifier")
public class MLSLikeModifierPost extends TopologyModifierSupport {

    @Inject
    private WorkflowSimplifyService workflowSimplifyService;

    @Inject
    private WorkflowsBuilderService workflowBuilderService;

    @PostConstruct
    private void init() {

    }

    @Override
    @ToscaContextual
    public void process(Topology topology, FlowExecutionContext context) {
        try {
            WorkflowValidator.disableValidationThreadLocal.set(true);
            doProcess(topology, context);

            TopologyContext topologyContext = workflowBuilderService.buildCachedTopologyContext(new TopologyContext() {
                @Override
                public String getDSLVersion() {
                    return ToscaParser.LATEST_DSL;
                }

                @Override
                public Topology getTopology() {
                    return topology;
                }

                @Override
                public <T extends AbstractToscaType> T findElement(Class<T> clazz, String elementId) {
                    return ToscaContext.get(clazz, elementId);
                }
            });

            workflowSimplifyService.reentrantSimplifyWorklow(topologyContext, topology.getWorkflows().keySet());
        } catch (Exception e) {
            log.warn("Can't process k8s-spark-jobs modifier:", e);
        } finally {
            WorkflowValidator.disableValidationThreadLocal.remove();
        }
    }

    protected void doProcess(Topology topology, FlowExecutionContext context) {
        log.info("MLS processing topology");

        Object cachedObject = context.getExecutionCache().get(Constants.MLS_TOKENS_KEY);
        if (cachedObject == null) {
            return;
        }
        Map<String, Credential> credentialPerModule = (Map<String, Credential>)cachedObject;
        credentialPerModule.forEach((nodeName, credential) -> {
            NodeTemplate clientNode = topology.getNodeTemplates().get(nodeName);

            AbstractPropertyValue varNamesPv = clientNode.getProperties().get(Constants.VAR_VALUES_PROPERTY);
            if (varNamesPv != null && varNamesPv instanceof ComplexPropertyValue) {
                Map<String, Object> varValues = ((ComplexPropertyValue)varNamesPv).getValue();
                processNode(topology, context, clientNode, varValues, credential);
            }
        });

    }

    private void processNode(Topology topology, FlowExecutionContext context, NodeTemplate clientNode, Map<String, Object> varValues, Credential credential) {
        // all relationship to datastores should use these credentials
        Set<RelationshipTemplate> relationships = TopologyNavigationUtil.getRelationshipsFromType(clientNode, Constants.RELATIONSHIP_TYPE_TO_EXPORE);
        relationships.stream().forEach(relationshipTemplate -> {
            log.info("Processing relationship {}", relationshipTemplate.getName());

            NodeTemplate targetNode = topology.getNodeTemplates().get(relationshipTemplate.getTarget());
            AbstractPropertyValue apv = relationshipTemplate.getProperties().get(Constants.VAR_MAPPING_PROPERTY);
            if (apv != null && apv instanceof ComplexPropertyValue) {
                Map<String, Object> mappingProperties = ((ComplexPropertyValue) apv).getValue();
                processCredential(mappingProperties, "username", credential.tokenId, varValues);
                processCredential(mappingProperties, "password", credential.passwordId, varValues);
            }
        });
    }

    private void processCredential(Map<String, Object> mappingProperties, String propertyName, String credentialValue, Map<String, Object> varValues) {
        Object usernameObj = mappingProperties.get(propertyName);
        if (usernameObj != null) {
            String varNames = usernameObj.toString();
            String[] varNamesArray = varNames.split(",");
            for (String varName: varNamesArray) {
                varValues.put(varName, new ScalarPropertyValue(credentialValue));
            }
        }
    }

}
