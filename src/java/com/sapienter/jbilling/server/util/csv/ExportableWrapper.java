package com.sapienter.jbilling.server.util.csv;

/**
 * 
 * @author Krunal Bhavsar
 * Marks a class as a Wrapper of Class which implements {@link Exportable} Interface 
 * @param <T>
 */
public interface ExportableWrapper<T extends Exportable> extends Exportable {
	
	public T getWrappedInstance();
	public void setDynamicExport(DynamicExport dynamicExport);

}
