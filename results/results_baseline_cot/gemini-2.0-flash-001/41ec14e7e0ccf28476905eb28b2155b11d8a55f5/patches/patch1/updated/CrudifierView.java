package com.premiumminds.wicket.crudifier.view;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.IGenericComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.premiumminds.wicket.crudifier.IObjectRenderer;
import com.premiumminds.wicket.crudifier.LabelProperty;
import com.premiumminds.wicket.crudifier.form.CrudifierEntitySettings;

import java.beans.Introspector;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;

public class CrudifierView<T> extends Panel implements IGenericComponent<T, CrudifierView<T>> {
	private static final long serialVersionUID = -151637566983702881L;

	private CrudifierEntitySettings entitySettings = new CrudifierEntitySettings();
	private Map<Class<?>, IObjectRenderer<?>> renderers = new HashMap<Class<?>, IObjectRenderer<?>>();

	public CrudifierView(String id, IModel<T> model) {
		super(id, model);

		StringResourceModel stringResourceModel = new StringResourceModel("viewLegend", this, getModel());
		stringResourceModel.setDefaultValue("Unknown");
		add(new Label("viewLegend", stringResourceModel){
			private static final long serialVersionUID = -7854751811138463187L;

			@Override
			protected void onConfigure() {
				super.onConfigure();

				setVisible(!getDefaultModelObjectAsString().isEmpty());
			}
		});



	}
	@Override
	protected void onConfigure() {
		super.onConfigure();

		RepeatingView view = new RepeatingView("control");
		if(null != getModelObject()) {
			for(final String property : getPropertiesByOrder(getModelObject().getClass())){
				WebMarkupContainer control = new WebMarkupContainer(view.newChildId());
				view.addOrReplace(control);

				StringResourceModel stringResourceModel = new StringResourceModel("controls."+property+".label", this, getModel());
				stringResourceModel.setDefaultValue("Unknown");
				control.addOrReplace(new Label("label", stringResourceModel));
				control.addOrReplace(new LabelProperty("input", new PropertyModel<Object>(getModel(), property), renderers) {
					private static final long serialVersionUID = 8561120757563362569L;

					@Override
					protected String getResourceString(String key, String defaultValue) {
						return getLocalizer().getStringIgnoreSettings("controls."+property+"."+key, CrudifierView.this, null, defaultValue);
					}
				});
			}
		}
		addOrReplace(view);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
	}

	private Set<String> getPropertiesByOrder(Class<?> modelClass) {
		Set<String> properties = new LinkedHashSet<String>();

		for(String property : entitySettings.getOrderOfFields()){
			if(!entitySettings.getHiddenFields().contains(property))
				properties.add(property);
		}

		try {
			BeanInfo beanInfo = Introspector.getBeanInfo(modelClass);
			for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
				if (!entitySettings.getHiddenFields().contains(descriptor.getName()) &&
						!properties.contains(descriptor.getName()) &&
						!descriptor.getName().equals("class")) {
					properties.add(descriptor.getName());
				}
			}
		} catch (IntrospectionException e) {
			// Handle exception appropriately, maybe log it or throw a runtime exception
			e.printStackTrace();
		}

		return properties;
	}

	public CrudifierEntitySettings getEntitySettings() {
		return entitySettings;
	}

	public void setEntitySettings(CrudifierEntitySettings entitySettings) {
		this.entitySettings = entitySettings;
	}

	public Map<Class<?>, IObjectRenderer<?>> getRenderers() {
		return renderers;
	}

	public void setRenderers(Map<Class<?>, IObjectRenderer<?>> renderers) {
		this.renderers = renderers;
	}
}