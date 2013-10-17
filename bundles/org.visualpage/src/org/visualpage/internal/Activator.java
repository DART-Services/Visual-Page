package org.visualpage.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	
	private static Activator instance;
	
	public static Activator getDefault() 
	{
		return instance;
	}
	
	private BundleContext context;
	
	public Activator() {
		instance = this;
	}
	
	@Override
	public void start(BundleContext ctx) throws Exception {
        this.context = ctx;
    }
    
    @Override
    public void stop(BundleContext ctx) throws Exception {
    	this.context = null;
    }
    
    public BundleContext getContext() 
    {
    	return context;
    }
}