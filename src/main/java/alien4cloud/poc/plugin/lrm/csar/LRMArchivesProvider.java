package alien4cloud.poc.plugin.lrm.csar;

import alien4cloud.plugin.archives.AbstractArchiveProviderPlugin;
import org.springframework.stereotype.Component;

@Component("alien4cloud-poc-lrm-archive-provider")
public class LRMArchivesProvider extends AbstractArchiveProviderPlugin {

    @Override
    protected String[] getArchivesPaths() {
        return new String[] { "csar" };
    }
}
