package com.sapienter.jbilling.csrf

import com.sapienter.jbilling.csrf.RequiresValidFormToken
import org.apache.commons.lang.WordUtils

/**
 * Created by vivek on 20/7/15.
 */
class ControllerAnnotationHelper {

    private static Map<String, Map<String, List<Class>>> _actionMap = [:]
    private static Map<String, Class> _controllerAnnotationMap = [:]

    /**
     * Find controller annotation information. Called by BootStrap.init().
     */
    static void init (def grailsApplication) {
        grailsApplication.controllerClasses.each { controllerClass ->
            def clazz = controllerClass.clazz
            String controllerName = WordUtils.uncapitalize(controllerClass.name)
            mapClassAnnotation clazz, RequiresValidFormToken, controllerName

            Map<String, List<Class>> annotatedClosures = findAnnotatedMethods(clazz, RequiresValidFormToken)
            if (annotatedClosures) {
                _actionMap[controllerName] = annotatedClosures
            }
        }
    }

    // for testing
    static void reset() {
        _actionMap.clear()
        _controllerAnnotationMap.clear()
    }

    // for testing
    static Map<String, Map<String, List<Class>>> getActionMap() {
        return _actionMap
    }

    // for testing
    static Map<String, Class> getControllerAnnotationMap() {
        return _controllerAnnotationMap
    }

    private static void mapClassAnnotation(clazz, annotationClass, controllerName) {
        if (clazz.isAnnotationPresent(annotationClass)) {
            def list = _controllerAnnotationMap[controllerName] ?: []
            list << annotationClass
            _controllerAnnotationMap[controllerName] = list
        }
    }

    /**
     * Check if the specified controller action requires POST.
     * @param controllerName the controller name
     * @param actionName the action name (closure name)
     */
    static boolean requiresValidFormToken(String controllerName, String actionName) {
        return requiresAnnotation(RequiresValidFormToken, controllerName, actionName)
    }

    private static boolean requiresAnnotation(Class annotationClass, String controllerName, String actionName) {

        // see if the controller has the annotation
//        def annotations = _controllerAnnotationMap[controllerName]
        def annotations = _actionMap[controllerName]
        if (annotations && annotations.containsKey(actionName) && annotations.get(actionName).contains(annotationClass)) {
            return true
        }

        // otherwise check the action
        Map<String, List<Class>> controllerClosureAnnotations = _actionMap[controllerName] ?: [:]
        List<Class> annotationClasses = controllerClosureAnnotations[actionName]
        return annotationClasses && annotationClasses.contains(annotationClass)
    }

    private static Map<String, List<Class>> findAnnotatedMethods(Class clazz, Class... annotationClasses) {

        // since action closures are defined as "def foo = ..." they're
        // fields, but they end up private
        def map = [:]
        for (method in clazz.declaredMethods) {
            def fieldAnnotations = []
            for (annotationClass in annotationClasses) {
                if (method.isAnnotationPresent(annotationClass)) {
                    fieldAnnotations << annotationClass
                }
            }
            if (fieldAnnotations) {
                map[method.name] = fieldAnnotations
            }
        }

        return map
    }
}
