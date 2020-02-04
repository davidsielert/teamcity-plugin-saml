package jetbrains.buildServer.auth.saml.plugin;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;

public class SamlCallbackController extends BaseController {

    private final Logger LOG = Loggers.SERVER;

    public SamlCallbackController(@NotNull SBuildServer server,
                                  @NotNull WebControllerManager webControllerManager,
                                  @NotNull AuthorizationInterceptor interceptor) {
        super(server);

        LOG.info("Initializing SAML callback controller");
        interceptor.addPathNotRequiringAuth(SamlPluginConstants.SAML_CALLBACK_URL);
        webControllerManager.registerController(SamlPluginConstants.SAML_CALLBACK_URL, this);
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
        LOG.info("Received SAML Callback request");
        return new ModelAndView(new RedirectView("/"));
    }
}
