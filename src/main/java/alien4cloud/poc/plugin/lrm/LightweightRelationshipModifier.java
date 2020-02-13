package alien4cloud.poc.plugin.lrm;

import alien4cloud.paas.wf.TopologyContext;
import alien4cloud.paas.wf.WorkflowSimplifyService;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.paas.wf.validation.WorkflowValidator;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.parser.ToscaParser;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.TopologyModifierSupport;
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
 * This modifier :
 * <ul>
 *     <li>Looks for all AbstractStandardClient</li>
 *     <li>explore it's DatastoreRelationship</li>
 *     <li>browse it's var_mapping property</li>
 *     <li>get the capability's properties</li>
 *     <li>finally feed the nodes var_values map property</li>
 * </ul>
 *
 * This modifier should act at post-node-match phase (it use property of matched service target capability).
 */
@Slf4j
@Component("alien4cloud-poc-lrm-modifier")
public class LightweightRelationshipModifier extends TopologyModifierSupport {

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
        log.info("ARM processing topology");

        Set<NodeTemplate> nodes = TopologyNavigationUtil.getNodesOfType(topology, Constants.NODE_TYPE_TO_EXPORE, true);
        nodes.stream().forEach(nodeTemplate -> processNode(topology, nodeTemplate));

    }

    private void processNode(Topology topology, NodeTemplate nodeTemplate) {
        log.info("Processing node {}", nodeTemplate.getName());
        Set<RelationshipTemplate> relationships = TopologyNavigationUtil.getRelationshipsFromType(nodeTemplate, Constants.RELATIONSHIP_TYPE_TO_EXPORE);
        Map<String, Object> varValues = Maps.newHashMap();
        relationships.stream().forEach(relationshipTemplate -> {
            log.info("Processing relationship {}", relationshipTemplate.getName());

            NodeTemplate targetNode = topology.getNodeTemplates().get(relationshipTemplate.getTarget());
            String targetCapability = relationshipTemplate.getTargetedCapabilityName();
            // find the var mapping property
            AbstractPropertyValue apv = relationshipTemplate.getProperties().get(Constants.VAR_MAPPING_PROPERTY);
            if (apv != null && apv instanceof ComplexPropertyValue) {
                Map<String, Object> mappingProperties = ((ComplexPropertyValue)apv).getValue();
                mappingProperties.forEach((propertyName, propertyValue) -> {
                    String varNames = propertyValue.toString();
                    // get the corresponding property value
                    AbstractPropertyValue targetPropertyValue = getPropertyFromCapabilityOrNode(targetNode, targetCapability, propertyName);
                    if (targetPropertyValue != null && targetPropertyValue instanceof ScalarPropertyValue) {
                        String varValue = ((ScalarPropertyValue)targetPropertyValue).getValue();
                        // we accept CSV var names, so a capability property can be mapped to several variables
                        String[] varNamesArray = varNames.split(",");
                        for (String varName: varNamesArray) {
                            varValues.put(varName, varValue);
                        }
                    }
                });
            }
        });

        log.info("Here are the var values: {}", varValues);
        if (!varValues.isEmpty()) {
            // finally feed the var_values node property
            ComplexPropertyValue complexPropertyValue = new ComplexPropertyValue(varValues);
            nodeTemplate.getProperties().put(Constants.VAR_VALUES_PROPERTY, complexPropertyValue);
        }
    }

    /**
     * Look for the property value in the given capability properties, falling back into the the node properties.
     */
    private AbstractPropertyValue getPropertyFromCapabilityOrNode(NodeTemplate nodeTemplate, String capabilityName, String propertyName) {
        AbstractPropertyValue value = nodeTemplate.getCapabilities().get(capabilityName).getProperties().get(propertyName);
        if (value == null) {
            value = nodeTemplate.getProperties().get(propertyName);
        }
        return value;
    }

}
