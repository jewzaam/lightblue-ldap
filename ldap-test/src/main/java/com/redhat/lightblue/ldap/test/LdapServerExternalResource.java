package com.redhat.lightblue.ldap.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFException;

public class LdapServerExternalResource extends ExternalResource {

    public static final String DEFAULT_BASE_DN = "dc=com";
    public static final String DEFAULT_BINDABLE_DN = "uid=admin,dc=example,dc=com";
    public static final String DEFAULT_PASSWORD = "password";
    public static final int DEFAULT_PORT = 38900;

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Documented
    public @interface InMemoryLdapServer {
        String[] baseDns() default {DEFAULT_BASE_DN};
        BindCriteria[] bindCriteria() default {@BindCriteria()};
        String name() default "test";
        int port() default DEFAULT_PORT;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.ANNOTATION_TYPE)
    @Documented
    public @interface BindCriteria {
        String bindableDn() default DEFAULT_BINDABLE_DN;
        String password() default DEFAULT_PASSWORD;
    }

    private InMemoryDirectoryServer server = null;
    private InMemoryLdapServer imlsAnnotation = null;
    private final LinkedHashMap<String, Attribute[]> preloadDnData;

    public LdapServerExternalResource(){
        this.preloadDnData = null;
    }

    public LdapServerExternalResource(LinkedHashMap<String, Attribute[]> preload){
        this.preloadDnData = preload;
    }

    @Override
    public Statement apply(Statement base, Description description){
        imlsAnnotation = description.getAnnotation(InMemoryLdapServer.class);
        if(description.isTest() && imlsAnnotation == null){
            imlsAnnotation = description.getTestClass().getAnnotation(InMemoryLdapServer.class);
        }

        if(imlsAnnotation ==  null){
            throw new IllegalStateException("@InMemoryLdapServer must be set on suite or test level.");
        }

        return super.apply(base, description);
    }

    @Override
    protected void before() throws Throwable {
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(imlsAnnotation.baseDns());

        for(BindCriteria bindCriteria : imlsAnnotation.bindCriteria()){
            config.addAdditionalBindCredentials(bindCriteria.bindableDn(), bindCriteria.password());
        }

        InMemoryListenerConfig listenerConfig = new InMemoryListenerConfig(
                imlsAnnotation.name(), null, imlsAnnotation.port(), null, null, null);
        config.setListenerConfigs(listenerConfig);
        config.setSchema(null); // do not check (attribute) schema

        server = new InMemoryDirectoryServer(config);
        server.startListening();

        if(preloadDnData != null){
            for(Entry<String, Attribute[]> entry : preloadDnData.entrySet()){
                add(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    protected void after() {
        if(server != null){
            server.shutDown(true);
            server = null;
        }
    }

    public void add(String dn, Attribute... attributes) throws LDIFException, LDAPException{
        server.add(dn, attributes);
    }

}
