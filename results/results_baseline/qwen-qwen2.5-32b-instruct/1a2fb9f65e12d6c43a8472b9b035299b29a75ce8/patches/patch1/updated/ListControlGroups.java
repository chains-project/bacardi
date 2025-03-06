/**
 * Copyright (C) 2014 Premium Minds.
 *
 * This file is part of wicket-crudifier.
 *
 * wicket-crudifier is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * wicket-crudifier is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with wicket-crudifier. If not, see <http://www.gnu.org/licenses/>.
 */
package com.premiumminds.wicket.crudifier.form.elements;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public abstract class ListControlGroups<T> extends Panel {
	private static final long serialVersionUID = 7205285700113097720L;

	private Map<String, AbstractControlGroup<?>> fieldComponents = new HashMap<String, AbstractControlGroup<?>>();

	private Map<Class<?>, ControlGroupProvider<?>> controlGroupProviders = new HashMap<Class<?>, ControlGroupProvider<?>>();

	@SuppressWarnings("rawtypes")
	private final Map<Class<?>, Class<? extends AbstractControlGroup>> typesControlGroups = new HashMap<Class<?>, Class<? extends AbstractControlGroup>>();

	private List<ObjectProperties> objectProperties;
	private CrudifierEntitySettings entitySettings;
	private Map<Class<?>, IObjectRenderer<?>> renderers;

	public ListControlGroups(String id, IModel<T> model, CrudifierEntitySettings entitySettings, Map<Class<?>, IObjectRenderer<?>> renderers) {
		super(id, model);

		typesControlGroups.put(Date.class, DateControlGroup.class);
		typesControlGroups.put(LocalDateTime.class, TemporalControlGroup.class);
		typesControlGroups.put(Temporal.class, TemporalControlGroup.class);
		typesControlGroups.put(String.class, TextFieldControlGroup.class);
		typesControlGroups.put(Integer.class, TextFieldControlGroup.class);
		typesControlGroups.put(int.class, TextFieldControlGroup.class);
		typesControlGroups.put(Long.class, TextFieldControlGroup.class);
		typesControlGroups.put(long.class, TextFieldControlGroup.class);
		typesControlGroups.put(Double.class, TextFieldControlGroup.class);
		typesControlGroups.put(double.class, TextFieldControlGroup.class);
		typesControlGroups.put(BigDecimal.class, TextFieldControlGroup.class);
		typesControlGroups.put(BigInteger.class, TextFieldControlGroup.class);
		typesControlGroups.put(Boolean.class, CheckboxControlGroup.class);
		typesControlGroups.put(boolean.class, CheckboxControlGroup.class);
		typesControlGroups.put(Set.class, CollectionControlGroup.class);

		objectProperties = new ArrayList<ObjectProperties>();
		this.entitySettings = entitySettings;
		this.renderers = renderers;
	}

	private Set<String> getPropertiesByOrder(Class<?> modelClass) {
		Set<String> properties = new LinkedHashSet<String>();

		for(String property : entitySettings.getOrderOfFields()){
			if(!entitySettings.getHiddenFields().contains(property))
				properties.add(property);
		}
		for(PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(modelClass)){
			if(!entitySettings.getHiddenFields().contains(descriptor.getName()) &&
			   !properties.contains(descriptor.getName()) &&
			   !descriptor.getName().equals("class"))
				properties.add(descriptor.getName());
		}

		return properties;
	}

	protected abstract EntityProvider<?> getEntityProvider(String name);

	public IModel<T> getModel(){
		return (IModel<T>) getDefaultModel();
	}

	public Component getResourceBase(){
		return this;
	}

	public Map<String, AbstractControlGroup<?>> getFieldsControlGroup(){
		return Collections.unmodifiableMap(fieldComponents);
	}

	@SuppressWarnings("rawtypes")
	public Map<Class<?>, Class<? extends AbstractControlGroup>> getControlGroupsTypesMap(){
		return typesControlGroups;
	}

	@SuppressWarnings("rawtypes")
	public Map<Class<?>, ControlGroupProvider<? extends AbstractControlGroup<?>>> getControlGroupProviders(){
		return this.controlGroupProviders;
	}

	private static final class ObjectProperties implements Serializable {
		private static final long serialVersionUID = 1747577998897955928L;
		private String name;
		private boolean enabled;
		private Class<?> type;
		private boolean required;

		public ObjectProperties(PropertyDescriptor descriptor, boolean required){
			this.name = descriptor.getName();
			this.enabled = descriptor.getWriteMethod()!=null;
			this.type = descriptor.getPropertyType();
			this.required = required;
		}
	}
}