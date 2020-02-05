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
package org.xwiki.contrib.activitypub.internal.activities;

import java.io.IOException;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.Actor;
import org.xwiki.contrib.activitypub.entities.Object;
import org.xwiki.contrib.activitypub.entities.activities.Accept;
import org.xwiki.contrib.activitypub.entities.activities.Follow;

@Component
@Singleton
public class AcceptActivityHandler extends AbstractActivityHandler implements ActivityHandler<Accept>
{
    @Override
    public void handleInboxRequest(ActivityRequest<Accept> activityRequest) throws IOException
    {
        this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_NOT_IMPLEMENTED,
            "Only client to server is currently implemented.");
    }

    @Override
    public void handleOutboxRequest(ActivityRequest<Accept> activityRequest) throws IOException
    {
        Accept activity = activityRequest.getActivity();
        Actor activityActor = activity.getActor().getObject(this.activityPubJsonParser);
        Object object = activity.getObject().getObject(this.activityPubJsonParser);

        if (object instanceof Follow) {
            Follow follow = (Follow) object;
            Actor followActor = follow.getActor().getObject(this.activityPubJsonParser);
            followActor.addFollowing(activityActor, this.activityPubJsonParser);
            activityActor.addFollower(followActor, this.activityPubJsonParser);
            this.answer(activityRequest.getResponse(), HttpServletResponse.SC_OK, activity);
        } else {
            this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_NOT_IMPLEMENTED,
                "Only follow activities can be accepted in the current implementation.");
        }
    }
}
