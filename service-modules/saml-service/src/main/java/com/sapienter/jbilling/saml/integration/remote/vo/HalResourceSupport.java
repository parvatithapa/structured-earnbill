package com.sapienter.jbilling.saml.integration.remote.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extend this class if a resource (WS object) embeds other resources.
 */
@Getter
public abstract class HalResourceSupport extends ResourceSupport {
    @XmlTransient
    @JsonInclude(Include.NON_EMPTY)
    @JsonProperty("_embedded")
    private final Map<String, Object> embedded = new HashMap<>();

    @Override
    @XmlElement(name = "link", namespace = Link.ATOM_NAMESPACE)
    @JsonProperty("links")
    @JsonInclude(Include.NON_EMPTY)
    public List<Link> getLinks() {
        return super.getLinks();
    }

    public HalResourceSupport embed(String relationship, Object resource) {
        Preconditions.checkArgument(StringUtils.isNotBlank(relationship));
        embedded.put(relationship, resource);
        return this;
    }
}
