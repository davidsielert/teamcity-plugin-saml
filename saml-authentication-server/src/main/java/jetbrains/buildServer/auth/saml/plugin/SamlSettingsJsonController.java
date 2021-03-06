package jetbrains.buildServer.auth.saml.plugin;

import com.intellij.openapi.diagnostic.Logger;
import com.onelogin.saml2.settings.IdPMetadataParser;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.util.Util;
import jetbrains.buildServer.RootUrlHolder;
import jetbrains.buildServer.auth.saml.plugin.pojo.MetadataImport;
import jetbrains.buildServer.auth.saml.plugin.pojo.SamlAttributeMappingSettings;
import jetbrains.buildServer.auth.saml.plugin.pojo.SamlPluginSettings;
import jetbrains.buildServer.controllers.json.BaseJsonController;
import jetbrains.buildServer.controllers.json.JsonActionError;
import jetbrains.buildServer.controllers.json.JsonActionResult;
import jetbrains.buildServer.controllers.json.JsonControllerAction;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import lombok.var;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Validation;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

public class SamlSettingsJsonController extends BaseJsonController {

    private static Logger log = Loggers.SERVER;

    private SamlPluginSettingsStorage settingsStorage;
    private RootUrlHolder rootUrlHolder;

    protected SamlSettingsJsonController(@NotNull SamlPluginSettingsStorage settingsStorage,
                                         WebControllerManager controllerManager,
                                         RootUrlHolder rootUrlHolder) {
        super("/admin/samlSettingsApi.html", controllerManager);
        this.settingsStorage = settingsStorage;
        this.rootUrlHolder = rootUrlHolder;

        registerAction(JsonControllerAction.forParam("action", "get").using(HttpMethod.GET).run(this::getSettings));
        registerAction(JsonControllerAction.forParam("action", "save").using(HttpMethod.POST).run(this::saveSettings));
        registerAction(JsonControllerAction.forParam("action", "import").using(HttpMethod.POST).run(this::importMetadata));
    }

    @NotNull
    public JsonActionResult<SamlPluginSettings> importMetadata(HttpServletRequest request) {
        try {

            var metadataImport = bindFromRequest(request, MetadataImport.class);

            if (metadataImport == null) {
                throw new Exception("Invalid request");
            }

            var metadataXml = metadataImport.getMetadataXml();

            var documentMetadata = Util.loadXML(metadataXml);

            Map<String, Object> metadataInfo = IdPMetadataParser.parseXML(documentMetadata);

            var saml2Settings = new Saml2Settings();
            IdPMetadataParser.injectIntoSettings(saml2Settings, metadataInfo);

            var getSettingsResult = this.getSettings(request);

            if (getSettingsResult.getErrors() != null) {
                return JsonActionResult.fail(getSettingsResult.getErrors());
            }

            var result = getSettingsResult.getResult();

            result.setIssuerUrl(saml2Settings.getIdpEntityId());
            result.setSsoEndpoint(saml2Settings.getIdpSingleSignOnServiceUrl().toString());

            result.setPublicCertificate(null);
            if (saml2Settings.getIdpx509cert() != null) {
                var encoded = saml2Settings.getIdpx509cert().getEncoded();
                var base64encoded = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
                if (!StringUtil.isEmpty(base64encoded)) {
                    result.setPublicCertificate("-----BEGIN CERTIFICATE-----\n" + base64encoded + "\n-----END CERTIFICATE-----\n");
                }
            }

            return JsonActionResult.ok(result);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return JsonActionResult.fail(e);
        }
    }

    public JsonActionResult<String> saveSettings(HttpServletRequest request) {
        try {
            var settings = bindFromRequest(request, SamlPluginSettings.class);

            var validator = Validation.buildDefaultValidatorFactory().getValidator();

            var constraintViolations = validator.validate(settings);

            var errors = constraintViolations.stream().map(cv -> new JsonActionError(cv.getMessage())).collect(Collectors.toList());

            if (settings.isCreateUsersAutomatically() && settings.isLimitToPostfixes() && StringUtil.isEmpty(settings.getAllowedPostfixes())) {
                errors.add(new JsonActionError("You must specify allowed postfixes"));
            }

            if (settings.isCreateUsersAutomatically()
                    && settings.getEmailAttributeMapping().getMappingType().equals(SamlAttributeMappingSettings.TYPE_OTHER)
                    && StringUtil.isEmpty(settings.getEmailAttributeMapping().getCustomAttributeName())) {
                errors.add(new JsonActionError("You must specify non-empty attribute name for the e-mail attribute mapping"));
            }

            if (settings.isCreateUsersAutomatically()
                    && settings.getNameAttributeMapping().getMappingType().equals(SamlAttributeMappingSettings.TYPE_OTHER)
                    && StringUtil.isEmpty(settings.getNameAttributeMapping().getCustomAttributeName())) {
                errors.add(new JsonActionError("You must specify non-empty attribute name for the full name attribute mapping"));
            }

            if (errors.size() > 0) {
                return JsonActionResult.fail(errors);
            }

            settingsStorage.save(settings);
            return JsonActionResult.ok(settings);

        } catch (Exception e) {
            return JsonActionResult.fail(e);
        }
    }

    public JsonActionResult<SamlPluginSettings> getSettings(HttpServletRequest request) {
        try {
            var samlPluginSettings = settingsStorage.load();
            String rootUrl = this.rootUrlHolder.getRootUrl();
            var callbackUrl = new URL(new URL(rootUrl), SamlPluginConstants.SAML_CALLBACK_URL.replace("**", ""));
            samlPluginSettings.setSsoCallbackUrl(callbackUrl.toString());

            if (StringUtil.isEmpty(samlPluginSettings.getEntityId())) {
                samlPluginSettings.setEntityId(callbackUrl.toString());
            }

            return JsonActionResult.ok(samlPluginSettings);
        } catch (IOException e) {
            return JsonActionResult.fail(e);
        }
    }
}
