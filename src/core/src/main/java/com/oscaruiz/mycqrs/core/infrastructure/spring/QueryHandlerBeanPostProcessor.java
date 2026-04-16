package com.oscaruiz.mycqrs.core.infrastructure.spring;

import com.oscaruiz.mycqrs.core.contracts.query.Query;
import com.oscaruiz.mycqrs.core.contracts.query.QueryBus;
import com.oscaruiz.mycqrs.core.contracts.query.QueryHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class QueryHandlerBeanPostProcessor implements BeanPostProcessor {

    private final QueryBus queryBus;

    public QueryHandlerBeanPostProcessor(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof QueryHandler<?, ?> rawHandler) {
            for (Type iface : rawHandler.getClass().getGenericInterfaces()) {
                if (iface instanceof ParameterizedType pType &&
                        pType.getRawType() instanceof Class<?> rawType &&
                        QueryHandler.class.isAssignableFrom(rawType)) {

                    Type queryTypeArg = pType.getActualTypeArguments()[0];
                    if (queryTypeArg instanceof Class<?> queryClass) {
                        @SuppressWarnings("unchecked")
                        QueryHandler<Query<Object>, Object> handler = (QueryHandler<Query<Object>, Object>) rawHandler;
                        @SuppressWarnings("unchecked")
                        Class<Query<Object>> queryType = (Class<Query<Object>>) queryClass;

                        queryBus.registerHandler(queryType, handler);
                    }
                }
            }
        }
        return bean;
    }
}
