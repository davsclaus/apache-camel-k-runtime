/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.k.cron;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.GeneratedPropertyConfigurer;
import org.apache.camel.spi.PropertyConfigurerGetter;
import org.apache.camel.util.CaseInsensitiveMap;
import org.apache.camel.k.cron.CronSourceLoaderInterceptor;

/**
 * Generated by camel build tools - do NOT edit this file!
 */
@SuppressWarnings("unchecked")
public class CronSourceLoaderInterceptorConfigurer extends org.apache.camel.support.component.PropertyConfigurerSupport implements GeneratedPropertyConfigurer, PropertyConfigurerGetter {

    @Override
    public boolean configure(CamelContext camelContext, Object obj, String name, Object value, boolean ignoreCase) {
        org.apache.camel.k.cron.CronSourceLoaderInterceptor target = (org.apache.camel.k.cron.CronSourceLoaderInterceptor) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "overridablecomponents":
        case "OverridableComponents": target.setOverridableComponents(property(camelContext, java.lang.String.class, value)); return true;
        case "runtime":
        case "Runtime": target.setRuntime(property(camelContext, org.apache.camel.k.Runtime.class, value)); return true;
        case "timeruri":
        case "TimerUri": target.setTimerUri(property(camelContext, java.lang.String.class, value)); return true;
        default: return false;
        }
    }

    @Override
    public Map<String, Object> getAllOptions(Object target) {
        Map<String, Object> answer = new CaseInsensitiveMap();
        answer.put("OverridableComponents", java.lang.String.class);
        answer.put("Runtime", org.apache.camel.k.Runtime.class);
        answer.put("TimerUri", java.lang.String.class);
        return answer;
    }

    @Override
    public Object getOptionValue(Object obj, String name, boolean ignoreCase) {
        org.apache.camel.k.cron.CronSourceLoaderInterceptor target = (org.apache.camel.k.cron.CronSourceLoaderInterceptor) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "overridablecomponents":
        case "OverridableComponents": return target.getOverridableComponents();
        case "runtime":
        case "Runtime": return target.getRuntime();
        case "timeruri":
        case "TimerUri": return target.getTimerUri();
        default: return null;
        }
    }
}

