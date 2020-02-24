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
package org.xwiki.contrib.activitypub.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents a Follow activity as defined by ActivityStream.
 * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-follow">ActivityStream Follow definition</a>
 * @version $Id$
 */
@JsonDeserialize(as = Follow.class)
public class Follow extends AbstractActivity
{
    private boolean accepted;
    private boolean rejected;

    /**
     * @return {@code true} if the follow request has been accepted.
     */
    public boolean isAccepted()
    {
        return this.accepted;
    }

    /**
     * @param accepted set to {@code true} if the follow request has been accepted.
     */
    public void setAccepted(boolean accepted)
    {
        this.accepted = accepted;
    }

    /**
     * @return {@code true} if the follow request has been rejected.
     */
    public boolean isRejected()
    {
        return this.rejected;
    }

    /**
     * @param rejected set to {@code true} if the follow request has been rejected.
     */
    public void setRejected(boolean rejected)
    {
        this.rejected = rejected;
    }
}