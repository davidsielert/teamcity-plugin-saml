package jetbrains.buildServer.auth.saml.plugin;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;

public class OpenSAMLInit {
    private final Logger LOG = Loggers.SERVER;
    public OpenSAMLInit() {
        try {
            LOG.info("Initializing OpenSAML 3");
            InitializationService.initialize();
        } catch (InitializationException e) {
            String msg = "OpenSAML 3 Initialization failed";
            LOG.error(msg);
            throw new RuntimeException(msg);
        }
    }

}
