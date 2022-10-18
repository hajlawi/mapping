package eu.sakarah.tool.mapping.mappingSheet;

import org.milyn.SmooksException;
import org.milyn.cdr.annotation.ConfigParam;
import org.milyn.container.ExecutionContext;
import org.milyn.delivery.ExecutionLifecycleInitializable;
import org.milyn.delivery.dom.DOMVisitAfter;
import org.milyn.delivery.dom.DOMVisitBefore;
import org.w3c.dom.Element;

/**
 * Created by proussel on 28/08/2017.
 */
public class Init implements ExecutionLifecycleInitializable, DOMVisitBefore, DOMVisitAfter
{
    @ConfigParam
    private String encoding = "UTF-8";

    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
    }

    public void visitAfter(Element element, ExecutionContext executionContext) throws SmooksException {
    }

    public void executeExecutionLifecycleInitialize(ExecutionContext executionContext) {
        executionContext.setContentEncoding(encoding);
    }

}

