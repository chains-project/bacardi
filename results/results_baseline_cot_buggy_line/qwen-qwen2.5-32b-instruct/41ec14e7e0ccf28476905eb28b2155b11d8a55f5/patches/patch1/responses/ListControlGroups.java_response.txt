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

import javax.validation.Validator;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.premiumminds.webapp.wicket.validators.HibernateValidatorProperty;
import com.premiumminds.wicket.crudifier.IObjectRenderer;
import com.premiumminds.wicket.crudifier.form.CrudifierEntitySettings;
import com.premiumminds.wicket.crudifier.form.EntityProvider;

public abstract class ListControlGroups<T> extends Panel {
    private static final long serialVersionUID = 7205285700113097720L;

    private Map<String, AbstractControlGroup<?>> fieldComponents = new HashMap<String, AbstractControlGroup<?>>();

    private Map<Class<?>, ControlGroupProvider<? extends AbstractControlGroup<?>>> controlGroupProviders = new HashMap<Class<?>, ControlGroupProvider<? extends AbstractControlGroup<?>>>();

    @SuppressWarnings("rawtypes")
    private Map<Class<?>, Class<? extends AbstractControlGroup>> typesControlGroups = new HashMap<Class<?>, Class<? extends AbstractControlGroup>>();

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
        for(PropertyDescriptor descriptor : FieldUtils.getAllFieldsList(modelClass)){
            if(!entitySettings.getHiddenFields().contains(descriptor.getName()) &&
               !properties.contains(descriptor.getName()) &&
               !descriptor.getName().equals("class"))
                properties.add(descriptor.getName());
        }

        return properties;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Class<?> modelClass = getModel().getObject().getClass();

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<Object>> violations = validator.validate(getModel().getObject());

        for(ObjectProperties objectProperty : objectProperties){
            boolean required = false;

            for (ConstraintViolation<Object> violation : violations) {
                if (violation.getPropertyPath().toString().equals(objectProperty.name)) {
                    required = true;
                    break;
                }
            }

            objectProperty.required = required;
        }

        RepeatingView view = new RepeatingView("controlGroup");
        for(ObjectProperties objectProperty : objectProperties){
            try {
                AbstractControlGroup<?> controlGroup;
                if(!controlGroupProviders.containsKey(objectProperty.type)) {
                    Constructor<?> constructor = getControlGroupByType(objectProperty.type).getConstructor(String.class, IModel.class);

                    controlGroup = (AbstractControlGroup<?>) constructor.newInstance(view.newChildId(), new PropertyModel<Object>(ListControlGroups.this.getModel(), objectProperty.name));
                    controlGroup.init(objectProperty.name, getResourceBase(), objectProperty.required, objectProperty.type, entitySettings);
                    controlGroup.setEnabled(objectProperty.enabled);

                    if(getControlGroupByType(objectProperty.type) == CollectionControlGroup.class){
                        ((CollectionControlGroup<?>) controlGroup).setConfiguration(getEntityProvider(objectProperty.name), renderers);
                    }
                } else {
                    controlGroup = controlGroupProviders
                            .get(objectProperty.type)
                            .createControlGroup(view.newChildId()
                                    , new PropertyModel<Object>(ListControlGroups.this.getModel(), objectProperty.name)
                                    , objectProperty.name, getResourceBase(), objectProperty.required, objectProperty.type, entitySettings);
                }
                view.add(controlGroup);

                fieldComponents.put(objectProperty.name, controlGroup);
            } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        add(view);
    }

    @SuppressWarnings("rawtypes")
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
    private Class<? extends AbstractControlGroup> getControlGroupByType(Class<?> type){
        for(Class<?> mapType : typesControlGroups.keySet()){
            if(type.isAssignableFrom(mapType)) return typesControlGroups.get(mapType);
        }
        return null;
    }

    private static final class ObjectProperties implements Serializable {
        private static final long serialVersionUID = 1747577998897955928L;
        private String name;
        private boolean enabled;
        private Class<?> type;
        private boolean required;

        public ObjectProperties(PropertyDescriptor descriptor, boolean required){
            this.name = descriptor.getName();
            this.enabled = descriptor.getWriteMethod() != null;
            this.type = descriptor.getPropertyType();
            this.required = required;
        }
    }

    @SuppressWarnings("rawtypes")
    public Map<Class<?>, Class<? extends AbstractControlGroup>> getControlGroupsTypesMap(){
        return typesControlGroups;
    }

    @SuppressWarnings("rawtypes")
    public Map<Class<?>, ControlGroupProvider<? extends AbstractControlGroup<?>>> getControlGroupProviders(){
        return this.controlGroupProviders;
    }
}