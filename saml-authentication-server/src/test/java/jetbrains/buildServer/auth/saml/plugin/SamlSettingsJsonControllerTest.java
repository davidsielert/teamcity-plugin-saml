package jetbrains.buildServer.auth.saml.plugin;

import jetbrains.buildServer.RootUrlHolder;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import lombok.var;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.testng.reporters.Files;

import javax.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SamlSettingsJsonControllerTest {

    private SamlSettingsJsonController controller;
    private RootUrlHolder rootUrlHolder;
    private SamlPluginSettingsStorage settingsStorage;
    private WebControllerManager webControllerManager;

    @Before
    public void setUp() throws Exception {
        Mockito.reset();
        this.rootUrlHolder = mock(RootUrlHolder.class);
        this.webControllerManager = mock(WebControllerManager.class);

        this.settingsStorage = new InMemorySamlPluginSettingsStorage();
        controller = new SamlSettingsJsonController(this.settingsStorage, this.webControllerManager, this.rootUrlHolder);
    }

    @Test
    public void shouldInitializeCallbackUrlWithRootUrl() {
        var request = mock(HttpServletRequest.class);
        when(rootUrlHolder.getRootUrl()).thenReturn("http://my.url");

        var settings = this.controller.getSettings(request);
        var actualUrl = settings.getResult().getSsoCallbackUrl();

        assertThat(actualUrl, org.hamcrest.core.StringStartsWith.startsWith("http://my.url"));
    }

    @Test
    public void shouldSetDefaultEntityIdToCallbackUrl() {
        var request = mock(HttpServletRequest.class);
        when(rootUrlHolder.getRootUrl()).thenReturn("http://my.url");

        var settings = this.controller.getSettings(request);

        assertThat(settings, notNullValue());
        assertThat(settings.getErrors(), nullValue());
        assertThat(settings.getResult(), notNullValue());

        var enitityId = settings.getResult().getEntityId();
        var callbackUrl = settings.getResult().getSsoCallbackUrl();

        assertThat(callbackUrl, notNullValue());
        assertThat(enitityId, equalTo(callbackUrl));
    }

    @Test
    public void shouldParseMetadataIntoSettings() throws IOException {
        var metadataFilePath = "src/test/resources/metadata.xml";
        var metadataPubCert = "src/test/resources/metadata_pub.key";

        var metadataXml = Files.readFile(Paths.get(metadataFilePath).toAbsolutePath().toFile());
        var metadataCert = Files.readFile(Paths.get(metadataPubCert).toAbsolutePath().toFile());

        var reader = new BufferedReader(new FileReader(metadataFilePath));

        var request = mock(HttpServletRequest.class);
        when(request.getReader()).thenReturn(reader);

        assertThat(metadataXml, notNullValue());

        var result = this.controller.importMetadata(request);

        assertThat(result.getErrors(), nullValue());

        var settings = result.getResult();
        assertThat(settings, notNullValue());

        assertThat(settings.getIssuerUrl(), equalTo("entityid"));
        assertThat(settings.getSsoEndpoint(), equalTo("https://mycert.lan"));
        assertThat(settings.getPublicCertificate(), equalTo(metadataCert));
    }

    @Test
    public void shouldErrorWhenMetadataEmpty() {
        var reader = new BufferedReader(new StringReader(""));

    }

    @Test
    public void shouldErrorWhenMetadataWrong() {

    }
}
