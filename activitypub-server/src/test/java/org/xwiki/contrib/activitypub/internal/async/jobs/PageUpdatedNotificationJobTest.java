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
package org.xwiki.contrib.activitypub.internal.async.jobs;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubConfiguration;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.HTMLRenderer;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Page;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.contrib.activitypub.entities.Service;
import org.xwiki.contrib.activitypub.entities.Update;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.contrib.activitypub.internal.XWikiUserBridge;
import org.xwiki.contrib.activitypub.internal.async.PageChangedRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;

import ch.qos.logback.classic.Level;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.contrib.activitypub.ActivityPubConfiguration.PageNotificationPolicy.WIKI;
import static org.xwiki.contrib.activitypub.ActivityPubConfiguration.PageNotificationPolicy.WIKIANDUSER;

/**
 * Tests of {@link PageUpdatedNotificationJob}.
 *
 * @version $Id$
 * @since 1.2
 */
@ComponentTest
class PageUpdatedNotificationJobTest
{
    private static final DocumentReference GUEST_USER =
        new DocumentReference("xwiki", "XWiki", XWikiRightService.GUEST_USER);

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.DEBUG);

    @InjectMockComponents
    private PageUpdatedNotificationJob job;

    @MockComponent
    private ActivityHandler<Update> updateActivityHandler;

    @MockComponent
    private DefaultURLHandler urlHandler;

    @MockComponent
    private XWikiUserBridge xWikiUserBridge;

    @MockComponent
    private ActivityPubStorage activityPubStorage;

    @MockComponent
    private ActorHandler actorHandler;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private ActivityPubObjectReferenceResolver objectReferenceResolver;

    @MockComponent
    private ActivityPubConfiguration configuration;

    @MockComponent
    protected EntityReferenceSerializer<String> stringEntityReferenceSerializer;

    @MockComponent
    private HTMLRenderer htmlRenderer;

    @Mock
    private XWikiDocument document;

    @Mock
    private XWikiContext context;

    private Person person;

    @Mock
    private UserReference authorReference;

    private Service service;

    @Mock
    private OrderedCollection<AbstractActor> serviceFollowers;

    @BeforeEach
    public void setup() throws Exception
    {
        this.person = new Person()
            .setPreferredUsername("Foobar")
            .setFollowers(
                new ActivityPubObjectReference<OrderedCollection<AbstractActor>>()
                    .setLink(new URI("http://foobar/followers")));

        when(this.xWikiUserBridge.resolveDocumentReference(this.document.getAuthorReference()))
            .thenReturn(this.authorReference);
        when(this.actorHandler.getActor(this.authorReference)).thenReturn(this.person);
        ActivityPubObjectReference followers = mock(ActivityPubObjectReference.class);
        when(followers.getLink()).thenReturn(URI.create("http://followerswiki"));
        this.service = new Service()
            .setFollowers(followers)
            .setId(URI.create("http://domain.tld/xwiki/1"));
        when(this.actorHandler.getActor(new WikiReference("xwiki"))).thenReturn(this.service);
        when(this.objectReferenceResolver.resolveReference(followers)).thenReturn(this.serviceFollowers);
        when(this.serviceFollowers.isEmpty()).thenReturn(true);
        when(this.configuration.getPageNotificationPolicy()).thenReturn(WIKIANDUSER);
        when(this.objectReferenceResolver.resolveDocumentReference(any())).thenReturn(new Page());
    }

    @Test
    void getType()
    {
        assertEquals("activitypub-update-page", this.job.getType());
    }

    @Test
    void runInternalNoFollowers() throws Exception
    {
        PageChangedRequest t = mock(PageChangedRequest.class);
        when(t.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "Foo", "Bar"));
        when(this.xWikiUserBridge.resolveDocumentReference(t.getAuthorReference()))
            .thenReturn(this.authorReference);

        when(this.actorHandler.getActor(this.authorReference)).thenReturn(mock(Person.class));
        when(this.objectReferenceResolver.resolveReference(any())).thenReturn(new OrderedCollection<AbstractActor>());
        this.job.initialize(t);
        this.job.runInternal();
        verify(this.updateActivityHandler, times(0)).handleOutboxRequest(any());
    }

    @Test
    void runInternalOneFollowerNoRight() throws Exception
    {
        PageChangedRequest t = mock(PageChangedRequest.class);
        when(t.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "Foo", "Bar"));
        when(this.xWikiUserBridge.resolveDocumentReference(t.getAuthorReference()))
            .thenReturn(this.authorReference);

        when(this.actorHandler.getActor(this.authorReference)).thenReturn(mock(Person.class));
        when(this.objectReferenceResolver.resolveReference(any()))
            .thenReturn(new OrderedCollection<AbstractActor>().addItem(new Person().setPreferredUsername("user1")));
        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, t.getDocumentReference())).thenReturn(false);
        this.job.initialize(t);
        this.job.runInternal();
        verify(this.updateActivityHandler, times(0)).handleOutboxRequest(any());
    }

    @Test
    public void runInternal() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, this.document.getDocumentReference()))
            .thenReturn(true);
        when(this.objectReferenceResolver.resolveReference(this.person.getFollowers())).thenReturn(
            new OrderedCollection<AbstractActor>()
                .addItem(new Person())
                .setId(new URI("http://followers"))
        );

        String absoluteDocumentUrl = "http://www.xwiki.org/xwiki/bin/view/Main";
        String relativeDocumentUrl = "/xwiki/bin/view/Main";
        String documentTile = "A document title";
        Date creationDate = new Date();

        when(this.document.getURL("view", this.context)).thenReturn(relativeDocumentUrl);
        when(this.urlHandler.getAbsoluteURI(new URI(relativeDocumentUrl))).thenReturn(new URI(absoluteDocumentUrl));
        when(this.document.getCreationDate()).thenReturn(creationDate);
        when(this.document.getTitle()).thenReturn(documentTile);

        ActivityPubObjectReference<AbstractActor> userReference =
            new ActivityPubObjectReference<AbstractActor>().setObject(this.person);
        Page apDoc = new Page()
            .setName(documentTile)
            .setAttributedTo(Arrays.asList(userReference))
            .setPublished(creationDate)
            .setUrl(singletonList(new URI(absoluteDocumentUrl)));
        Update update = new Update()
            .setActor(this.person)
            .setObject(apDoc)
            .setName("Update of document [A document title]")
            .setPublished(creationDate)
            .setTo(Arrays.asList(
                new ProxyActor(this.service.getFollowers().getLink()),
                new ProxyActor(this.person.getFollowers().getLink())
            ));
        ActivityRequest<Update> activityRequest = new ActivityRequest<>(this.person, update);

        PageChangedRequest request =
            new PageChangedRequest()
                .setDocumentReference(this.document.getDocumentReference())
                .setAuthorReference(this.document.getAuthorReference())
                .setDocumentTitle(this.document.getTitle())
                .setContent(this.document.getXDOM())
                .setCreationDate(this.document.getCreationDate())
                .setViewURL(this.document.getURL("view", this.context));
        request.setId("activitypub-update-page", this.document.getKey());

        PageChangedRequest t = mock(PageChangedRequest.class);
        when(t.getViewURL()).thenReturn("http://pageurl");

        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "TEST");
        when(t.getDocumentReference()).thenReturn(documentReference);
        when(t.getDocumentTitle()).thenReturn("A document title");
        when(t.getCreationDate()).thenReturn(creationDate);
        when(t.getViewURL()).thenReturn(absoluteDocumentUrl);
        when(this.actorHandler.getActor(documentReference.getWikiReference()))
            .thenReturn(this.service);

        when(this.urlHandler.getAbsoluteURI(new URI(absoluteDocumentUrl))).thenReturn(URI.create(absoluteDocumentUrl));

        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, documentReference)).thenReturn(true);

        when(this.configuration.getPageNotificationPolicy()).thenReturn(WIKIANDUSER);

        when(this.stringEntityReferenceSerializer.serialize(documentReference))
            .thenReturn(documentReference.toString());

        // the updated document is not found in storage
        when(this.activityPubStorage.query(Page.class, "filter(xwikiReference:xwiki\\:XWiki.TEST)", 1))
            .thenReturn(emptyList());

        this.job.initialize(t);
        this.job.runInternal();

        verify(this.activityPubStorage).storeEntity(apDoc);
        verify(this.updateActivityHandler).handleOutboxRequest(activityRequest);
    }

    @Test
    public void runInternalUserConfDeactivated() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, this.document.getDocumentReference()))
            .thenReturn(true);
        when(this.objectReferenceResolver.resolveReference(this.person.getFollowers())).thenReturn(
            new OrderedCollection<AbstractActor>()
                .addItem(new Person())
                .setId(new URI("http://followers"))
        );

        String absoluteDocumentUrl = "http://www.xwiki.org/xwiki/bin/view/Main";
        String relativeDocumentUrl = "/xwiki/bin/view/Main";
        String documentTile = "A document title";
        Date creationDate = new Date();

        when(this.document.getURL("view", this.context)).thenReturn(relativeDocumentUrl);
        when(this.urlHandler.getAbsoluteURI(new URI(relativeDocumentUrl))).thenReturn(new URI(absoluteDocumentUrl));
        when(this.document.getCreationDate()).thenReturn(creationDate);
        when(this.document.getTitle()).thenReturn(documentTile);

        Page apDoc = new Page()
            .setName(documentTile)
            .setAttributedTo(singletonList(new ActivityPubObjectReference<AbstractActor>().setObject(this.person)))
            .setPublished(creationDate)
            .setUrl(singletonList(new URI(absoluteDocumentUrl)));
        Update update = new Update()
            .setActor(this.person)
            .setObject(apDoc)
            .setName("Update of document [A document title]")
            .setPublished(creationDate)
            .setTo(singletonList(new ProxyActor(this.service.getFollowers().getLink())));
        ActivityRequest<Update> activityRequest = new ActivityRequest<>(this.person, update);

        PageChangedRequest request =
            new PageChangedRequest()
                .setDocumentReference(this.document.getDocumentReference())
                .setAuthorReference(this.document.getAuthorReference())
                .setDocumentTitle(this.document.getTitle())
                .setContent(this.document.getXDOM())
                .setCreationDate(this.document.getCreationDate())
                .setViewURL(this.document.getURL("view", this.context));
        request.setId("activitypub-update-page", this.document.getKey());

        PageChangedRequest t = mock(PageChangedRequest.class);
        when(t.getViewURL()).thenReturn("http://pageurl");

        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "TEST");
        when(t.getDocumentReference()).thenReturn(documentReference);
        when(t.getDocumentTitle()).thenReturn("A document title");
        when(t.getCreationDate()).thenReturn(creationDate);
        when(t.getViewURL()).thenReturn(absoluteDocumentUrl);
        when(this.actorHandler.getActor(documentReference.getWikiReference()))
            .thenReturn(this.service);
        when(this.serviceFollowers.isEmpty()).thenReturn(false);

        when(this.urlHandler.getAbsoluteURI(new URI(absoluteDocumentUrl))).thenReturn(URI.create(absoluteDocumentUrl));

        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, documentReference)).thenReturn(true);

        when(this.configuration.getPageNotificationPolicy()).thenReturn(WIKI);

        when(this.stringEntityReferenceSerializer.serialize(documentReference))
            .thenReturn(documentReference.toString());

        when(this.activityPubStorage.query(Page.class, "filter(xwikiReference:xwiki\\:XWiki.TEST)", 1))
            .thenReturn(emptyList());

        this.job.initialize(t);
        this.job.runInternal();

        verify(this.activityPubStorage).storeEntity(apDoc);
        verify(this.updateActivityHandler).handleOutboxRequest(activityRequest);
    }

    @Test
    public void runWithActivityPubException() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, this.document.getDocumentReference()))
            .thenReturn(true);
        when(this.objectReferenceResolver.resolveReference(this.person.getFollowers()))
            .thenThrow(new ActivityPubException("ERR"));

        String absoluteDocumentUrl = "http://www.xwiki.org/xwiki/bin/view/Main";
        String relativeDocumentUrl = "/xwiki/bin/view/Main";
        String documentTile = "A document title";
        Date creationDate = new Date();

        when(this.document.getURL("view", this.context)).thenReturn(relativeDocumentUrl);
        when(this.urlHandler.getAbsoluteURI(new URI(relativeDocumentUrl))).thenReturn(new URI(absoluteDocumentUrl));
        when(this.document.getCreationDate()).thenReturn(creationDate);
        when(this.document.getTitle()).thenReturn(documentTile);

        PageChangedRequest request =
            new PageChangedRequest()
                .setDocumentReference(this.document.getDocumentReference())
                .setAuthorReference(this.document.getAuthorReference())
                .setDocumentTitle(this.document.getTitle())
                .setContent(this.document.getXDOM())
                .setCreationDate(this.document.getCreationDate())
                .setViewURL(this.document.getURL("view", this.context));
        request.setId("activitypub-update-page", this.document.getKey());

        PageChangedRequest t = mock(PageChangedRequest.class);
        when(t.getViewURL()).thenReturn("http://pageurl");

        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "TEST");
        when(t.getDocumentReference()).thenReturn(documentReference);

        when(this.urlHandler.getAbsoluteURI(new URI(absoluteDocumentUrl))).thenReturn(URI.create(absoluteDocumentUrl));

        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, documentReference)).thenReturn(true);

        this.job.initialize(t);
        this.job.runInternal();

        verify(this.activityPubStorage, times(0)).storeEntity(any());
        verify(this.updateActivityHandler, times(0)).handleOutboxRequest(any());

        assertEquals(1, this.logCapture.size());
        assertEquals(Level.ERROR, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Error while trying to handle notifications for document [xwiki:XWiki.TEST]",
            this.logCapture.getMessage(0));
    }

    @Test
    public void runWithActivityPubExceptionUserNotFound() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, this.document.getDocumentReference()))
            .thenReturn(true);
        when(this.objectReferenceResolver.resolveReference(this.person.getFollowers()))
            .thenThrow(new ActivityPubException("Cannot find any user with reference"));

        String absoluteDocumentUrl = "http://www.xwiki.org/xwiki/bin/view/Main";
        String relativeDocumentUrl = "/xwiki/bin/view/Main";
        String documentTile = "A document title";
        Date creationDate = new Date();

        when(this.document.getURL("view", this.context)).thenReturn(relativeDocumentUrl);
        when(this.urlHandler.getAbsoluteURI(new URI(relativeDocumentUrl))).thenReturn(new URI(absoluteDocumentUrl));
        when(this.document.getCreationDate()).thenReturn(creationDate);
        when(this.document.getTitle()).thenReturn(documentTile);

        PageChangedRequest request =
            new PageChangedRequest()
                .setDocumentReference(this.document.getDocumentReference())
                .setAuthorReference(this.document.getAuthorReference())
                .setDocumentTitle(this.document.getTitle())
                .setContent(this.document.getXDOM())
                .setCreationDate(this.document.getCreationDate())
                .setViewURL(this.document.getURL("view", this.context));
        request.setId("activitypub-update-page", this.document.getKey());

        PageChangedRequest t = mock(PageChangedRequest.class);
        when(t.getViewURL()).thenReturn("http://pageurl");

        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "TEST");
        when(t.getDocumentReference()).thenReturn(documentReference);

        when(this.urlHandler.getAbsoluteURI(new URI(absoluteDocumentUrl))).thenReturn(URI.create(absoluteDocumentUrl));

        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, documentReference)).thenReturn(true);

        this.job.initialize(t);
        this.job.runInternal();

        verify(this.activityPubStorage, times(0)).storeEntity(any());
        verify(this.updateActivityHandler, times(0)).handleOutboxRequest(any());

        assertEquals(1, this.logCapture.size());
        assertEquals(Level.DEBUG, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Error while trying to handle notifications for document [xwiki:XWiki.TEST]",
            this.logCapture.getMessage(0));
    }

    @Test
    public void runWithIOExceptionException() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, this.document.getDocumentReference()))
            .thenReturn(true);
        when(this.objectReferenceResolver.resolveReference(this.person.getFollowers())).thenReturn(
            new OrderedCollection<AbstractActor>()
                .addItem(new Person().setId(URI.create("http://wiki/follower/OfFooBar")))
                .addItem(new Person().setId(URI.create("http://wiki/follower/OfBoth")))
                .setId(new URI("http://followers"))
        );

        when(this.objectReferenceResolver.resolveReference(this.service.getFollowers())).thenReturn(
            new OrderedCollection<AbstractActor>()
                .addItem(new Person().setId(URI.create("http://wiki/follower/OfWiki")))
                .addItem(new Person().setId(URI.create("http://wiki/follower/OfBoth")))
                .setId(new URI("http://followerswiki"))
        );

        when(this.objectReferenceResolver.resolveReference(new ActivityPubObjectReference<>().setObject(this.service)))
            .thenReturn(this.service);

        String absoluteDocumentUrl = "http://www.xwiki.org/xwiki/bin/view/Main";
        String relativeDocumentUrl = "/xwiki/bin/view/Main";
        String documentTile = "A document title";
        Date creationDate = new Date();

        when(this.document.getURL("view", this.context)).thenReturn(relativeDocumentUrl);
        when(this.urlHandler.getAbsoluteURI(new URI(relativeDocumentUrl))).thenReturn(new URI(absoluteDocumentUrl));
        when(this.document.getCreationDate()).thenReturn(creationDate);
        when(this.document.getTitle()).thenReturn(documentTile);

        Page apDoc = new Page()
            .setName(documentTile)
            .setAttributedTo(singletonList(new ActivityPubObjectReference<AbstractActor>().setObject(this.person)))
            .setPublished(creationDate)
            .setUrl(singletonList(new URI(absoluteDocumentUrl)));
        Update update = new Update()
            .setActor(this.person)
            .setObject(apDoc)
            .setName("Update of document [A document title]")
            .setPublished(creationDate)
            .setTo(singletonList(new ProxyActor(this.service.getFollowers().getLink())));
        ActivityRequest<Update> activityRequest = new ActivityRequest<>(this.person, update);
        PageChangedRequest request =
            new PageChangedRequest()
                .setDocumentReference(this.document.getDocumentReference())
                .setAuthorReference(this.document.getAuthorReference())
                .setDocumentTitle(this.document.getTitle())
                .setContent(this.document.getXDOM())
                .setCreationDate(this.document.getCreationDate())
                .setViewURL(this.document.getURL("view", this.context));
        request.setId("activitypub-update-page", this.document.getKey());

        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "TEST");
        PageChangedRequest t = mock(PageChangedRequest.class);
        when(t.getViewURL()).thenReturn("http://pageurl");

        when(t.getDocumentReference()).thenReturn(documentReference);
        when(t.getDocumentTitle()).thenReturn("A document title");
        when(t.getCreationDate()).thenReturn(creationDate);
        when(t.getViewURL()).thenReturn(absoluteDocumentUrl);
        when(this.actorHandler.getActor(documentReference.getWikiReference()))
            .thenReturn(this.service);
        when(this.serviceFollowers.isEmpty()).thenReturn(false);
        when(this.configuration.getPageNotificationPolicy()).thenReturn(WIKI);

        when(this.urlHandler.getAbsoluteURI(new URI(absoluteDocumentUrl))).thenReturn(URI.create(absoluteDocumentUrl));

        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, documentReference)).thenReturn(true);

        doThrow(new IOException()).when(this.updateActivityHandler).handleOutboxRequest(eq(activityRequest));

        when(this.stringEntityReferenceSerializer.serialize(documentReference))
            .thenReturn(documentReference.toString());

        when(this.activityPubStorage.query(Page.class, "filter(xwikiReference:xwiki\\:XWiki.TEST)", 1))
            .thenReturn(emptyList());

        this.job.initialize(t);
        this.job.runInternal();

        verify(this.activityPubStorage).storeEntity(apDoc);

        assertEquals(1, this.logCapture.size());
        assertEquals(Level.ERROR, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Error while trying to handle notifications for document [xwiki:XWiki.TEST]",
            this.logCapture.getMessage(0));
    }
}