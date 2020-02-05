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

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.contrib.activitypub.internal.json.ObjectDeserializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = ObjectDeserializer.class)
public class Object extends JSONLDObjects
{
    private String type;
    private URI id;
    private String name;
    private Date published;
    private String summary;
    private List<ObjectReference<Actor>> to;
    private String content;
    private List<ObjectReference<Actor>> attributedTo;

    public Object()
    {
        this.to = new ArrayList<>();
        this.attributedTo = new ArrayList<>();
    }

    public String getType()
    {
        return getClass().getSimpleName();
    }

    public void setType(String type)
    {
        if (!StringUtils.isEmpty(type) && !getType().toLowerCase().equals(type.toLowerCase())) {
            throw new IllegalArgumentException(String.format("Error while parsing [%s]: illegal type [%s].",
                getClass().toString(), type));
        }
    }

    public URI getId()
    {
        return id;
    }

    public void setId(URI id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Date getPublished()
    {
        return published;
    }

    public void setPublished(Date published)
    {
        this.published = published;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public List<ObjectReference<Actor>> getTo()
    {
        return to;
    }

    public void setTo(
        List<ObjectReference<Actor>> to)
    {
        this.to = to;
    }

    public List<ObjectReference<Actor>> getAttributedTo()
    {
        return attributedTo;
    }

    public void setAttributedTo(
        List<ObjectReference<Actor>> attributedTo)
    {
        this.attributedTo = attributedTo;
    }

    @Override
    public boolean equals(java.lang.Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Object object = (Object) o;
        return Objects.equals(type, object.type) &&
            Objects.equals(id, object.id) &&
            Objects.equals(name, object.name) &&
            Objects.equals(published, object.published) &&
            Objects.equals(summary, object.summary) &&
            Objects.equals(to, object.to) &&
            Objects.equals(content, object.content) &&
            Objects.equals(attributedTo, object.attributedTo);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(type, id, name, published, summary, to, content, attributedTo);
    }
}
