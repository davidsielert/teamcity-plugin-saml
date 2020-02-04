package jetbrains.buildServer.auth.saml.plugin;

import jetbrains.buildServer.RootUrlHolder;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.auth.LoginConfiguration;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import lombok.var;
import org.jetbrains.annotations.NotNull;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class SamlPluginConfiguration {

    @Bean
    SamlAuthenticationScheme samlAuthenticationScheme(LoginConfiguration loginConfiguration, SamlPluginSettingsStorage SamlPluginSettingsStorage, UserModel userModel) {
        SamlAuthenticationScheme samlAuthenticationScheme = new SamlAuthenticationScheme(SamlPluginSettingsStorage, userModel);
        loginConfiguration.registerAuthModuleType(samlAuthenticationScheme);

        return samlAuthenticationScheme;
    }

    @Bean
    SamlLoginPageExtension samlLoginPageExtension(@NotNull PagePlaces pagePlaces, @NotNull PluginDescriptor descriptor, SamlPluginSettingsStorage settingsStorage, LoginConfiguration loginConfiguration) {
        return new SamlLoginPageExtension(pagePlaces, descriptor, settingsStorage, loginConfiguration);
    }

    @Bean
    SamlLoginController samlLoginController(SBuildServer server, WebControllerManager webControllerManager, AuthorizationInterceptor interceptor, SamlPluginSettingsStorage settingsStorage,RootUrlHolder rootUrlHolder) {
        return new SamlLoginController(server, webControllerManager, interceptor, settingsStorage,rootUrlHolder);
    }

    @Bean
    SamlCallbackController samlCallbackController(SBuildServer server, WebControllerManager webControllerManager, AuthorizationInterceptor interceptor) {
        return new SamlCallbackController(server, webControllerManager,interceptor);
    }

    @Bean
    SamlSettingsAdminPage samlPluginAdminPage(@NotNull PagePlaces pagePlaces, @NotNull PluginDescriptor descriptor, SamlPluginSettingsStorage settingsStorage, LoginConfiguration loginConfiguration) {
        return new SamlSettingsAdminPage(pagePlaces, descriptor, settingsStorage, loginConfiguration);
    }

    @Bean
    SamlPluginSettingsStorage samlPluginSettingsStorage(ServerPaths serverPaths) throws IOException {
        Path configPath =   Paths.get(serverPaths.getConfigDir(), SamlPluginConstants.CONFIG_FILE_NAME);
        SamlPluginSettingsStorageImpl samlPluginSettingsStorage = new SamlPluginSettingsStorageImpl(configPath);
        samlPluginSettingsStorage.init();
        return samlPluginSettingsStorage;
    }

    @Bean
    SamlSettingsJsonController samlSettingsAjaxController(WebControllerManager controllerManager, RootUrlHolder rootUrlHolder) throws IOException {
        return new SamlSettingsJsonController(samlPluginSettingsStorage(null), controllerManager, rootUrlHolder);
    }

}
