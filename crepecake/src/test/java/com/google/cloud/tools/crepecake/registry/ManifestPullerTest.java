/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.crepecake.registry;

import com.google.cloud.tools.crepecake.blob.Blobs;
import com.google.cloud.tools.crepecake.http.Response;
import com.google.cloud.tools.crepecake.image.json.ManifestTemplate;
import com.google.cloud.tools.crepecake.image.json.UnknownManifestFormatException;
import com.google.cloud.tools.crepecake.image.json.V21ManifestTemplate;
import com.google.cloud.tools.crepecake.image.json.V22ManifestTemplate;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/** Tests for {@link ManifestPuller}. */
@RunWith(MockitoJUnitRunner.class)
public class ManifestPullerTest {

  @Mock private Response mockResponse;

  private final RegistryEndpointProperties fakeRegistryEndpointProperties =
      new RegistryEndpointProperties("someServerUrl", "someImageName");
  private final ManifestPuller<ManifestTemplate> testManifestPuller =
      new ManifestPuller<>(
          fakeRegistryEndpointProperties, "test-image-tag", ManifestTemplate.class);

  @Test
  public void testHandleResponse_v21()
      throws URISyntaxException, IOException, UnknownManifestFormatException {
    File v21ManifestFile = new File(Resources.getResource("json/v21manifest.json").toURI());

    Mockito.when(mockResponse.getBody()).thenReturn(Blobs.from(v21ManifestFile));
    ManifestTemplate manifestTemplate =
        new ManifestPuller<>(
                fakeRegistryEndpointProperties, "test-image-tag", V21ManifestTemplate.class)
            .handleResponse(mockResponse);

    Assert.assertThat(manifestTemplate, CoreMatchers.instanceOf(V21ManifestTemplate.class));
  }

  @Test
  public void testHandleResponse_v22()
      throws URISyntaxException, IOException, UnknownManifestFormatException {
    File v22ManifestFile = new File(Resources.getResource("json/v22manifest.json").toURI());

    Mockito.when(mockResponse.getBody()).thenReturn(Blobs.from(v22ManifestFile));
    ManifestTemplate manifestTemplate =
        new ManifestPuller<>(
                fakeRegistryEndpointProperties, "test-image-tag", V22ManifestTemplate.class)
            .handleResponse(mockResponse);

    Assert.assertThat(manifestTemplate, CoreMatchers.instanceOf(V22ManifestTemplate.class));
  }

  @Test
  public void testHandleResponse_noSchemaVersion() throws IOException {
    Mockito.when(mockResponse.getBody()).thenReturn(Blobs.from("{}", false));
    try {
      testManifestPuller.handleResponse(mockResponse);
      Assert.fail("An empty manifest should throw an error");

    } catch (UnknownManifestFormatException ex) {
      Assert.assertEquals("Cannot find field 'schemaVersion' in manifest", ex.getMessage());
    }
  }

  @Test
  public void testHandleResponse_invalidSchemaVersion() throws IOException {
    Mockito.when(mockResponse.getBody())
        .thenReturn(Blobs.from("{\"schemaVersion\":\"not valid\"}", false));
    try {
      testManifestPuller.handleResponse(mockResponse);
      Assert.fail("A non-integer schemaVersion should throw an error");

    } catch (UnknownManifestFormatException ex) {
      Assert.assertEquals("`schemaVersion` field is not an integer", ex.getMessage());
    }
  }

  @Test
  public void testHandleResponse_unknownSchemaVersion() throws IOException {
    Mockito.when(mockResponse.getBody()).thenReturn(Blobs.from("{\"schemaVersion\":0}", false));
    try {
      testManifestPuller.handleResponse(mockResponse);
      Assert.fail("An unknown manifest schemaVersion should throw an error");

    } catch (UnknownManifestFormatException ex) {
      Assert.assertEquals("Unknown schemaVersion: 0 - only 1 and 2 are supported", ex.getMessage());
    }
  }

  @Test
  public void testGetApiRoute() throws MalformedURLException {
    Assert.assertEquals(
        new URL("http://someApiBase/manifests/test-image-tag"),
        testManifestPuller.getApiRoute("http://someApiBase"));
  }

  @Test
  public void testGetHttpMethod() {
    Assert.assertEquals("GET", testManifestPuller.getHttpMethod());
  }

  @Test
  public void testGetActionDescription() {
    Assert.assertEquals(
        "pull image manifest for someServerUrl/someImageName:test-image-tag",
        testManifestPuller.getActionDescription());
  }
}
