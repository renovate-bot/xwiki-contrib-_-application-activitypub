/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.activitypub.internal.signature;

import java.net.URI;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Objects;

import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.httpclient.HttpMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.CryptoService;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.internal.DateProvider;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.store.FileStoreReference;
import org.xwiki.crypto.store.KeyStore;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultSignatureService}.
 *
 * @version $Id$
 * @since 1.1
 */
@ComponentTest
class DefaultSignatureServiceTest
{
    @InjectMockComponents
    private DefaultSignatureService signatureService;

    @MockComponent
    private DateProvider dateProvider;

    @MockComponent
    @Named("X509file")
    private KeyStore keyStore;

    @MockComponent
    private Provider<ActorHandler> actorHandlerProvider;

    @Mock
    private ActorHandler actorHandler;

    @MockComponent
    private CryptoService cryptoService;

    @MockComponent
    private Environment environment;

    private final static byte[] PK = new byte[] {
        48, -126, 1, 84, 2, 1, 0, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 4, -126, 1, 62, 48, -126, 1,
        58, 2, 1, 0, 2, 65, 0, -87, 4, -97, -82, -98, -34, -40, 31, 42, -65, 28, -117, -122, 113, 7, -58, 95, -7, -44,
        -47, -109, 83, -17, -36, -48, 100, -38, 35, -100, 75, -81, -94, -10, -49, 5, 63, -32, 5, -4, -47, -67, -113,
        -80, 38, 86, 31, -80, -51, -60, 41, -125, 88, -49, 78, 15, -77, 42, -24, -76, -22, -73, 110, -41, 105, 2, 3, 1,
        0, 1, 2, 64, 2, 83, -125, -9, 29, 76, -89, -32, -43, -17, -57, 110, -52, 44, -26, 20, 126, -31, -85, 98, 47, 10,
        -22, -76, 57, 82, 10, 6, -113, 114, 35, 58, -81, 60, -46, -2, -127, 7, -39, 68, 12, 86, -104, -24, 104, -11, 76,
        48, -34, -58, -64, -113, -95, -11, -119, 110, 65, 62, 58, 24, 61, -113, 59, -103, 2, 33, 0, -44, 25, -26, -98,
        -46, 57, -56, -87, -72, 31, -118, -59, -13, -23, 75, -80, 109, 116, 102, -70, 122, 49, 9, 93, 25, -56, -77, 41,
        -89, -106, 50, -29, 2, 33, 0, -53, -1, -5, 15, 42, -91, -123, 91, 56, 87, 98, 100, -10, 114, 60, -61, -14, 88,
        53, 124, 32, -113, 74, -126, -73, -34, -22, -22, -104, -53, 66, 67, 2, 33, 0, -109, -125, 96, 13, -35, -112, 42,
        -85, 63, 79, 80, -88, -44, 54, -47, 89, 103, 6, -87, -37, -49, -40, 2, -9, 41, 83, -104, -89, -61, -46, -122,
        -103, 2, 32, 8, 89, -5, 114, 60, -127, -72, 58, -22, -52, -111, 7, -89, 27, 56, 39, -95, 117, 65, 3, 74, -27,
        -14, -37, -11, 33, 24, 38, -16, -120, 105, -73, 2, 32, 30, -84, 13, 117, 79, -28, -117, 5, -112, -40, -119, 71,
        -85, 86, 27, 125, 16, 64, 12, 27, -21, -24, -15, 24, 54, 113, -26, -99, 123, 115, -57, 116
    };

    @BeforeEach
    void setUp(@TempDir Path tempDir)
    {
        when(this.environment.getPermanentDirectory()).thenReturn(tempDir.toFile());
        when(this.actorHandlerProvider.get()).thenReturn(this.actorHandler);
    }

    @Test
    void generateSignature() throws Exception
    {
        HttpMethod postMethod = mock(HttpMethod.class);
        org.apache.commons.httpclient.URI postURI = mock(org.apache.commons.httpclient.URI.class);
        when(postURI.getPath()).thenReturn("/");
        when(postURI.getHost()).thenReturn("targeturi");
        when(postMethod.getURI()).thenReturn(postURI);
        Person actor = mock(Person.class);
        when(actor.getId()).thenReturn(URI.create("http://actoruri/"));
        when(actor.getPreferredUsername()).thenReturn("tmp");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "test");
        when(this.actorHandler.getStoreDocument(actor)).thenReturn(documentReference);

        CertifiedKeyPair certifiedKeyPair = mock(CertifiedKeyPair.class);
        when(this.cryptoService.generateCertifiedKeyPair()).thenReturn(certifiedKeyPair);
        PrivateKeyParameters privateKeyParameters = mock(PrivateKeyParameters.class);
        when(certifiedKeyPair.getPrivateKey()).thenReturn(privateKeyParameters);
        when(privateKeyParameters.getEncoded())
            .thenReturn(KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate().getEncoded());
        when(this.dateProvider.getFormattedDate()).thenReturn("formated date");

        this.signatureService.generateSignature(postMethod, actor, "{}");
        InOrder inOrder = inOrder(postMethod, postMethod);
        inOrder.verify(postMethod).addRequestHeader(eq("Signature"), matches(
            "keyId=\"http:\\/\\/actoruri\\/\",headers=\"\\(request-target\\) host date digest\","
                + "signature=\"[^\"]*\""));
        inOrder.verify(postMethod).addRequestHeader(eq("Date"), anyString());
        inOrder.verify(postMethod).addRequestHeader("Digest", "SHA-256=RBNvo1WzZ4oRRq0W9+hknpT7T8If536DEMBg9hyq/4o=");
    }

    @Test
    void generateSignatureWithInit() throws Exception
    {
        HttpMethod postMethod = mock(HttpMethod.class);
        org.apache.commons.httpclient.URI postURI = mock(org.apache.commons.httpclient.URI.class);
        when(postURI.getPath()).thenReturn("/");
        when(postURI.getHost()).thenReturn("targeturi");
        when(postMethod.getURI()).thenReturn(postURI);
        Person actor = mock(Person.class);
        when(actor.getId()).thenReturn(URI.create("http://actoruri/"));
        when(actor.getPreferredUsername()).thenReturn("tmp");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "test");
        when(this.actorHandler.getStoreDocument(actor)).thenReturn(documentReference);

        CertifiedKeyPair certifiedKeyPair = mock(CertifiedKeyPair.class);
        when(this.cryptoService.generateCertifiedKeyPair()).thenReturn(certifiedKeyPair);
        PrivateKeyParameters privateKeyParameters = mock(PrivateKeyParameters.class);
        when(certifiedKeyPair.getPrivateKey()).thenReturn(privateKeyParameters);
        when(privateKeyParameters.getEncoded())
            .thenReturn(KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate().getEncoded());
        when(this.dateProvider.getFormattedDate()).thenReturn("formatted date");

        this.signatureService.generateSignature(postMethod, actor, "{}");
        InOrder inOrder = inOrder(postMethod, postMethod);
        inOrder.verify(postMethod).addRequestHeader(eq("Signature"), matches(
            "keyId=\"http:\\/\\/actoruri\\/\",headers=\"\\(request-target\\) host date digest\","
                + "signature=\"[^\"]*\""));
        inOrder.verify(postMethod).addRequestHeader(eq("Date"), eq("formatted date"));
        inOrder.verify(postMethod).addRequestHeader("Digest", "SHA-256=RBNvo1WzZ4oRRq0W9+hknpT7T8If536DEMBg9hyq/4o=");
    }

    @Test
    void generateSignatureWithoutInit() throws Exception
    {
        // override the date to make it deterministic
        when(this.dateProvider.getFormattedDate()).thenReturn("Mon, 06 Apr 2020 08:39:20 GMT");

        HttpMethod postMethod = mock(HttpMethod.class);
        org.apache.commons.httpclient.URI postURI = mock(org.apache.commons.httpclient.URI.class);
        when(postURI.getPath()).thenReturn("/");
        when(postURI.getHost()).thenReturn("targeturi");
        when(postMethod.getURI()).thenReturn(postURI);
        Person actor = mock(Person.class);
        when(actor.getId()).thenReturn(URI.create("http://actoruri/"));
        when(actor.getPreferredUsername()).thenReturn("tmp");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "test");
        when(this.actorHandler.getStoreDocument(actor)).thenReturn(documentReference);

        CertifiedKeyPair certifiedKeyPair = mock(CertifiedKeyPair.class);
        when(this.keyStore.retrieve(argThat(
            storeReference -> (storeReference instanceof FileStoreReference)
                && Objects.equals(((FileStoreReference) storeReference).getFile().getName(),
                documentReference.toString() + ".key"))))
            .thenReturn(certifiedKeyPair);
        PrivateKeyParameters privateKeyParameters = mock(PrivateKeyParameters.class);
        when(certifiedKeyPair.getPrivateKey()).thenReturn(privateKeyParameters);
        when(privateKeyParameters.getEncoded()).thenReturn(PK);

        this.signatureService.generateSignature(postMethod, actor, "{}");
        InOrder inOrder = inOrder(postMethod, postMethod);
        inOrder.verify(postMethod).addRequestHeader(eq("Signature"),
            eq("keyId=\"http://actoruri/\",headers=\"(request-target) host date digest\""
                + ",signature=\"gyFYtjF/9JX9moeR9yYHVYf7/B222obL1IIJDqDf5AK7ThyqIKoJHpARj1+eljAkEvXdQrUUg5y/Su7ljmhpCQ==\""));
        inOrder.verify(postMethod).addRequestHeader(eq("Date"), anyString());
        inOrder.verify(postMethod).addRequestHeader("Digest", "SHA-256=RBNvo1WzZ4oRRq0W9+hknpT7T8If536DEMBg9hyq/4o=");
    }
}
