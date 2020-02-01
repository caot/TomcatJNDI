package hthurow.tomcatjndi;

import java.beans.PropertyChangeListener;

import org.apache.catalina.Container;
import org.apache.catalina.Context;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 10.09.17
 */
class Loader implements org.apache.catalina.Loader {
    private Context context;
    
    @Override
    public void backgroundProcess() {

    }

    @Override
    public ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }

//    public Container getContainer() {
//        return null;
//    }

//    public void setContainer(Container container) {

//    }

    @Override
    public boolean getDelegate() {
        return false;
    }

    @Override
    public void setDelegate(boolean delegate) {

    }

//    public String getInfo() {
//        return null;
//    }

    @Override
    public boolean getReloadable() {
        return false;
    }

    @Override
    public void setReloadable(boolean reloadable) {

    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {

    }

//    public void addRepository(String repository) {

//    }

//    public String[] findRepositories() {
//        return new String[0];
//    }

    @Override
    public boolean modified() {
        return false;
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {

    }

	@Override
	public Context getContext() {
		// TODO Auto-generated method stub
		return this.context;
	}

	@Override
	public void setContext(Context context) {
		// TODO Auto-generated method stub
		this.context = context;
	}
}
