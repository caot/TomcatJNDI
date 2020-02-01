package hthurow.tomcatjndi;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleEvent;
//import org.apache.catalina.deploy.ContextResource;
import org.apache.tomcat.util.descriptor.web.ContextResource;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 26.07.17
 */
class NamingContextListener extends org.apache.catalina.core.NamingContextListener {

    @Override
    public void lifecycleEvent(LifecycleEvent event) {

        container = event.getLifecycle();
        if (container instanceof Context) {
            ((Context)container).setLoader(new Loader());
        }

        super.lifecycleEvent(event);

    }

    /*
     *
     * TODO Only called in case of a DataSource?
     * TODO Reconsider building of ObjectName.
     *
     * Sonst "Failed to register in JMX: javax.management.RuntimeOperationsException: Object name cannot be null"
     *
     Jul 24, 2017 7:28:36 AM org.apache.tomcat.util.modeler.Registry registerComponent
     SEVERE: Error registering null
     javax.management.RuntimeOperationsException: Object name cannot be null
     ...
     Jul 24, 2017 7:28:36 AM org.apache.catalina.core.NamingContextListener addResource
     WARNING: Failed to register in JMX: javax.management.RuntimeOperationsException: Object name cannot be null
     *
     */
    @Override
    protected ObjectName createObjectName(ContextResource resource) throws MalformedObjectNameException {
        String domain = "Catalina";
        ObjectName name;
        String quotedResourceName = ObjectName.quote(resource.getName());
        name = new ObjectName(domain + ":type=DataSource" +
                ",class=" + resource.getType() +
                ",name=" + quotedResourceName);
        return (name);
    }

}
