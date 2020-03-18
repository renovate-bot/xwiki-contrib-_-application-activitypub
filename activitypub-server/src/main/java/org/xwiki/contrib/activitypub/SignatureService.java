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
package org.xwiki.contrib.activitypub;

import java.net.URI;
import java.security.PublicKey;

import org.apache.commons.httpclient.HttpMethod;
import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.stability.Unstable;

/**
 * Deal with the concerns related to the management, storage and use of asymetric keys.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
@Role
public interface SignatureService
{
    /**
     * Generate the signature of a message send to an external ActivityPub inbox.
     *
     * @param postMethod The post method to sign. 
     * @param targetURI The URI where the request is send.
     * @param actorURI The URI of the ActivityPub actor the sign the request.
     * @param actor The actor that sign the request.
     * @throws ActivityPubException In case of error when signing the request.
     */
    void generateSignature(HttpMethod postMethod, URI targetURI, URI actorURI, AbstractActor actor)
        throws ActivityPubException;

    /**
     * Initialize the keys of an actor.
     * @param actorId The id of the actor.
     * @return The public key of the actor.
     * @throws ActivityPubException In case of error when initializing the keys.
     */
    PublicKey initKey(String actorId) throws ActivityPubException;
}