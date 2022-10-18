package eu.sakarah.tool.mapping.mappingSheet;

import org.jdom.Namespace;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by proussel on 31/08/2017.
 */
public class RootContent extends RowContent
{
    private Set<Namespace> namespaces = new HashSet<Namespace>();

    public RootContent() {
        super(null);
    }

    @Override
    public RowContent getParent() {
        return this;
    }

    @Override
    public int getOutputLevel() {
        return -1;
    }

    @Override
    public void addNamespace(Namespace ns) {
        namespaces.add(ns);
    }

    public Set<Namespace> getNamespaces() {
        return namespaces;
    }

}
