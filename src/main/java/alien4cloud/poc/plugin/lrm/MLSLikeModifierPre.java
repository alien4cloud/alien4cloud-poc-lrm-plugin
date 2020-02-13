package alien4cloud.poc.plugin.lrm;

import alien4cloud.paas.wf.WorkflowSimplifyService;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.tosca.context.ToscaContextual;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.TopologyModifierSupport;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.utils.TopologyNavigationUtil;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

/**
 * Just define a token / password tuple per client node.
 *
 * Use at post-location-match, or any phase, but before alien4cloud-poc-lrm-modifier phase.
 */
@Slf4j
@Component("alien4cloud-poc-mls-pre-modifier")
public class MLSLikeModifierPre extends TopologyModifierSupport {

    private static final String MLS_USERNAME = "_myMlsToken";
    private static final String MLS_PASSWORD = "_MyMlsPwd";

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

        Map<String, Credential> credentialPerModule = Maps.newHashMap();
        Set<NodeTemplate> nodes = TopologyNavigationUtil.getNodesOfType(topology, Constants.NODE_TYPE_TO_EXPORE, true);
        nodes.stream().forEach(nodeTemplate -> {
            Credential credential = new Credential(nodeTemplate.getName() + MLS_USERNAME, nodeTemplate.getName() + MLS_PASSWORD);
            credentialPerModule.put(nodeTemplate.getName(), credential);
        });

        context.getExecutionCache().put(Constants.MLS_TOKENS_KEY, credentialPerModule);
    }

}
